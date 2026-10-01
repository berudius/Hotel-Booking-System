"use strict";

document.addEventListener("DOMContentLoaded", () => {
    initAltPhoneToggle();
    initCart();
    initRemoveFromCart();
    initBookingSubmit();
});



function initAltPhoneToggle() {
    const checkbox = document.getElementById("use_alt_phone");

    if (!checkbox) return;

    checkbox.addEventListener("change", toggleAltPhone);
}

function toggleAltPhone() {
    const checkbox = document.getElementById("use_alt_phone");
    const container = document.getElementById("alt_phone_container");
    const input = document.getElementById("alt_phone_number");

    if (!checkbox || !container || !input) return;

    if (checkbox.checked) {
        container.classList.add("open");
        input.setAttribute("required", "required");
    } else {
        container.classList.remove("open");
        input.removeAttribute("required");
        input.value = "";
    }
}




let cart = [];

function initCart() {
    cart = JSON.parse(localStorage.getItem("edemium_cart")) || [];
    renderCart();
}

function renderCart() {
    const cartBody = document.getElementById("cart-items-body");
    const emptyContainer = document.getElementById("empty-cart-container");
    const cartTable = document.getElementById("cart-table");
    const totalContainer = document.getElementById("total-price-container");
    const totalPriceValue = document.getElementById("total-price-value");
    const submitBtn = document.getElementById("submit-btn");

    if (!cartBody) return;

    cartBody.innerHTML = "";
    let totalSum = 0;

    if (cart.length === 0) {
        emptyContainer.classList.remove("hidden");
        cartTable.classList.add("hidden");
        totalContainer.classList.add("hidden");
        submitBtn.disabled = true;
        return;
    }

    emptyContainer.classList.add("hidden");
    cartTable.classList.remove("hidden");
    totalContainer.classList.remove("hidden");
    submitBtn.disabled = false;

    cart.forEach((item, index) => {
        const row = document.createElement("tr");
        let itemTotal = 0;

        
        const isSpa = item.classType && item.classType.includes('SPA');

        if (isSpa) {
            
            itemTotal = item.price * (item.clientCount || 1);
        } else {
            
            const start = new Date(item.start);
            const end = new Date(item.end);

            const totalHours = Math.abs(end - start) / 36e5;
            const fullDays = Math.floor(totalHours / 24);
            const extraHours = totalHours % 24;

            let multiplier = fullDays;

            if (fullDays === 0) {
                
                multiplier = 1; 
            } else {
                if (extraHours > 0 && extraHours <= 12) {
                    multiplier += 0.5;
                } else if (extraHours > 12) {
                    multiplier += 1;
                }
            }

            itemTotal = item.price * multiplier;
        }

        totalSum += itemTotal;

        row.innerHTML = `
            <td>
                <strong>${item.serviceName}</strong><br>
                <span class="text-muted small-text">Гостей: ${item.clientCount}</span>
                <br>
                <button type="button" class="remove-btn" data-index="${index}">Видалити</button>
            </td>
            <td>
                <span class="text-sm">Заїзд: ${formatDateTime(item.start)}</span><br>
                <span class="text-sm">Виїзд: ${formatDateTime(item.end)}</span>
            </td>
            <td>${itemTotal.toFixed(2)} UAH</td>
        `;

        cartBody.appendChild(row);
    });

    totalPriceValue.textContent = `${totalSum.toFixed(2)} UAH`;
}

function formatDateTime(isoString) {
    const date = new Date(isoString);
    return date.toLocaleString("uk-UA", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}




function initRemoveFromCart() {
    const cartBody = document.getElementById("cart-items-body");

    if (!cartBody) return;

    cartBody.addEventListener("click", handleRemoveClick);
}

function handleRemoveClick(e) {
    if (!e.target.classList.contains("remove-btn")) return;

    const index = e.target.dataset.index;

    cart.splice(index, 1);
    localStorage.setItem("edemium_cart", JSON.stringify(cart));

    renderCart();
}




function initBookingSubmit() {
    const form = document.getElementById("booking-form");

    if (!form) return;

    form.addEventListener("submit", handleBookingSubmit);
}

async function handleBookingSubmit(e) {
    e.preventDefault();

    const submitBtn = document.getElementById("submit-btn");
    submitBtn.disabled = true;
    submitBtn.textContent = "Обробка...";

    try {
        const payload = buildBookingPayload();

        const { csrfToken, csrfHeader } = getCsrfData();

        const response = await sendBookingRequest(payload, csrfToken, csrfHeader);

        if (response.ok) {
            handleSuccess();
        } else {
            const err = await response.text();
            handleError("Помилка бронювання: " + err);
        }

    } catch (error) {
        console.error(error);
        handleError("Сталася мережева помилка під час оформлення.");
    }
}




function buildBookingPayload() {
    return {
        phoneNumber: resolvePhoneNumber(),
        clientComment: document.getElementById("clientComment").value,
        bookingUnits: cart.map(item => {
            
            
            
            const mappedClassType = (item.classType && item.classType.includes('SPA')) 
                                    ? 'SPA_BOOKING' 
                                    : 'ROOM_BOOKING';

            const unit = {
                classType: mappedClassType, 
                serviceUnitId: item.serviceUnitId,
                start: item.start,
                end: item.end,
                clientCount: item.clientCount
            };
            
            
            if (mappedClassType === 'SPA_BOOKING') {
                
                unit.preferedGender = (item.preferedGender && item.preferedGender !== 'ANY') 
                                      ? item.preferedGender 
                                      : null;
            }
            
            return unit;
        })
    };
}

function resolvePhoneNumber() {
    const savedPhone = document.getElementById("saved_phone_number");
    const useAlt = document.getElementById("use_alt_phone");
    const altPhone = document.getElementById("alt_phone_number");
    const guestPhone = document.getElementById("guest_phone_number");

    if (savedPhone) {
        return (useAlt && useAlt.checked)
            ? altPhone.value
            : savedPhone.value;
    }

    if (guestPhone) {
        return guestPhone.value;
    }

    return "";
}

function getCsrfData() {
    return {
        csrfToken: document.querySelector('meta[name="_csrf"]').getAttribute("content"),
        csrfHeader: document.querySelector('meta[name="_csrf_header"]').getAttribute("content")
    };
}

function sendBookingRequest(payload, csrfToken, csrfHeader) {
    return fetch("/api/bookings", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify(payload)
    });
}

function handleSuccess() {
    
    localStorage.removeItem("edemium_cart");

    const isGuest = document.getElementById("guest_phone_number") !== null;

    if (isGuest) {
        const submitBtn = document.getElementById("submit-btn");
        if (submitBtn) {
            submitBtn.textContent = "Успішно!";
            submitBtn.classList.add("cta-success");
        }

        const overlay = document.createElement("div");
        overlay.className = "booking-success-overlay";
        
        overlay.innerHTML = `
            <div class="booking-success-modal">
                <div class="booking-success-icon">✓</div>
                <strong>Бронювання успішно оформлено!</strong>
                <p>Зачекайте, вас буде перенаправлено до списку ваших бронювань...</p>
            </div>
        `;
        
        
        document.body.appendChild(overlay);

        
        setTimeout(() => {
            window.location.href = "/bookings";
        }, 2000);
    } else {
        
        window.location.href = "/bookings";
    }
}

function handleError(message) {
    alert(message);

    const submitBtn = document.getElementById("submit-btn");
    submitBtn.disabled = false;
    submitBtn.textContent = "Підтвердити та забронювати";
}