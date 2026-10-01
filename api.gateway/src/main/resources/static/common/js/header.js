"use strict"

document.addEventListener("DOMContentLoaded", () => {
    initializeLogoutEvent()
    initializeHandleScrolling()
    initializeGlobalCartNav()
    initializeChangePhoneModal();
})

function initializeLogoutEvent() {
    const imageProfile = document.querySelector(".image-profile")
    const profileOptions = document.querySelector(".profile-options")
    const logoutLink = document.getElementById("logout-link")

    if (logoutLink) {
        logoutLink.addEventListener("click", (event) => {
            event.preventDefault()

            const tokenHeader = document.querySelector("meta[name='_csrf_header']").content
            const token = document.querySelector("meta[name='_csrf']").content

            fetch(logoutLink.href, {
                method: "POST",
                headers: {
                    [tokenHeader]: token
                }
            }).then(responce => {
                if (responce.redirected) {
                    window.location.href = responce.url
                }
            })
        })
    }

    if (imageProfile && profileOptions) {
        imageProfile.addEventListener("click", () => {
            profileOptions.classList.toggle("hidden")
        })
    }
}

function initializeHandleScrolling() {
    let lastScroll = window.scrollY;
    const header = document.querySelector("header");

    
    let forceShowPos = null;

    if (!header) return;

    window.addEventListener("scroll", () => {
        const currentScroll = window.scrollY;

        
        if (currentScroll <= 50) {
            header.classList.remove("header-hidden");
            lastScroll = currentScroll;
            forceShowPos = null;
            return;
        }

        
        if (forceShowPos !== null) {
            if (currentScroll < forceShowPos) {
                
                forceShowPos = null;
            } else if (currentScroll > forceShowPos + 50) {
                
                header.classList.add("header-hidden");
                forceShowPos = null;
            }
            
            lastScroll = currentScroll;
            return;
        }

        
        if (currentScroll > lastScroll && !header.classList.contains("header-hidden")) {
            header.classList.add("header-hidden");
        } else if (currentScroll < lastScroll && header.classList.contains("header-hidden")) {
            header.classList.remove("header-hidden");
        }

        lastScroll = currentScroll;
    });

    
    window.revealHeaderSmoothly = function () {
        
        if (header.classList.contains("header-hidden")) {
            header.classList.remove("header-hidden");
            
            forceShowPos = window.scrollY;
        }
    };
}


function initializeGlobalCartNav() {
    const bookingNoteContainer = document.querySelector(".booking-note-container");

    const cartOverlay = document.getElementById('cart-drawer-overlay');
    const cartDrawer = document.getElementById('cart-drawer');
    const closeBtn = document.getElementById('close-cart-drawer');
    const itemsContainer = document.getElementById('cart-items-container');
    const emptyMsg = document.getElementById('empty-cart-message');
    const proceedBtn = document.getElementById('proceed-to-checkout');
    const browseBtn = document.getElementById('browse-services-btn');

    let cartBadge = document.getElementById('global-cart-badge');

    if (!cartBadge) {
        cartBadge = document.createElement('div');
        cartBadge.className = 'cart-badge hidden';
        cartBadge.id = 'global-cart-badge';
    }

    if (bookingNoteContainer && !document.getElementById('global-cart-badge')) {
        bookingNoteContainer.appendChild(cartBadge);
    }

    window.updateGlobalCartBadge = function () {
        if (!cartBadge) return;
        let cart = JSON.parse(localStorage.getItem('edemium_cart')) || [];
        if (cart.length > 0) {
            cartBadge.textContent = cart.length;
            cartBadge.classList.remove('hidden');
        } else {
            cartBadge.classList.add('hidden');
        }
    };
    window.updateGlobalCartBadge();

    window.triggerGlobalCartNudge = function () {
        if (typeof window.revealHeaderSmoothly === 'function') {
            window.revealHeaderSmoothly();
        }

        if (!bookingNoteContainer) return;

        bookingNoteContainer.classList.remove('nudge');
        void bookingNoteContainer.offsetWidth;
        bookingNoteContainer.classList.add('nudge');

        if (cartBadge) {
            cartBadge.classList.add('pop');
            setTimeout(() => cartBadge.classList.remove('pop'), 200);
        }
    };

    function updateGlobalDatesFallback(cartArray) {
        if (cartArray.length > 0) {
            let global = JSON.parse(localStorage.getItem('edemium_global_dates')) || { start: '', end: '' };
            if (!global.start && cartArray[0].start) global.start = cartArray[0].start;
            if (!global.end && cartArray[0].end) global.end = cartArray[0].end;
            localStorage.setItem('edemium_global_dates', JSON.stringify(global));
        }
    }

    function renderCartDrawer() {
        let cart = JSON.parse(localStorage.getItem('edemium_cart')) || [];
        let globalDates = JSON.parse(localStorage.getItem('edemium_global_dates')) || { start: '', end: '' };
        itemsContainer.innerHTML = '';

        if (cart.length === 0) {
            emptyMsg.classList.remove('hidden');
            if (proceedBtn) proceedBtn.classList.add('hidden');
            if (browseBtn) browseBtn.classList.remove('hidden');
            return;
        }

        emptyMsg.classList.add('hidden');
        if (proceedBtn) proceedBtn.classList.remove('hidden');
        if (browseBtn) browseBtn.classList.add('hidden');

        let cartModified = false;
        cart.forEach(item => {
            
            const isSpa = item.classType && item.classType.includes('SPA');

            if (!item.start && globalDates.start) { item.start = globalDates.start; cartModified = true; }
            
            if (!isSpa && !item.end && globalDates.end) { item.end = globalDates.end; cartModified = true; }
        });
        if (cartModified) localStorage.setItem('edemium_cart', JSON.stringify(cart));

        cart.forEach((item, index) => {
            const startVal = (item.start || '').substring(0, 16);
            const endVal = (item.end || '').substring(0, 16);
            const isSpa = item.classType && item.classType.includes('SPA'); 

            
            let specificInputs = '';
            const minDateTime = getCurrentLocalMinDateTime();
            if (isSpa) {
                specificInputs = `
                    <div class="cart-item-form-group">
                        <label>Дата та час початку (Тривалість: ${item.duration} хв)</label>
                        <input type="datetime-local" class="cart-start-date sync-trigger" value="${startVal}" min="${minDateTime}" required>
                        <input type="hidden" class="cart-end-date" value="${endVal}"> 
                    </div>
                    <div class="cart-item-form-group">
                        <label>Бажана стать спеціаліста</label>
                        <select class="cart-prefered-gender" style="width:100%; padding:8px; border:1px solid #cbd5e1; border-radius:6px;">
                            <option value="ANY" ${item.preferedGender === 'ANY' ? 'selected' : ''}>Не має значення</option>
                            <option value="MALE" ${item.preferedGender === 'MALE' ? 'selected' : ''}>Чоловік</option>
                            <option value="FEMALE" ${item.preferedGender === 'FEMALE' ? 'selected' : ''}>Жінка</option>
                        </select>
                    </div>
                `;
            } else {
                specificInputs = `
                    <div class="cart-item-form-group">
                        <label>Заїзд / Початок</label>
                        <input type="datetime-local" class="cart-start-date sync-trigger" value="${startVal}" min="${minDateTime}" required>
                    </div>
                    <div class="cart-item-form-group">
                        <label>Виїзд / Кінець</label>
                        <input type="datetime-local" class="cart-end-date sync-trigger" value="${endVal}" min="${minDateTime}" required>
                    </div>
                `;
            }


            const itemHTML = `
                <div class="cart-item-card" data-cart-id="${item.id}" data-index="${index}" data-class-type="${item.classType}" data-duration="${item.duration || 0}">
                    <div class="cart-item-header">
                        <div>
                            <h4 class="cart-item-title">${item.serviceName}</h4>
                            <div class="cart-item-price">${item.price} ₴ / ${isSpa ? 'сеанс' : 'доба'}</div>
                        </div>
                        <button type="button" class="remove-item-btn-drawer" data-index="${index}">×</button>
                    </div>
                    
                    ${specificInputs}
                    
                    <div class="cart-item-form-group">
                        <label>${isSpa ? 'Кількість клієнтів' : 'Гостей'} (Макс: ${item.maxCapacity})</label>
                        <input type="number" class="cart-client-count" min="1" max="${item.maxCapacity}" value="${item.clientCount || 1}" required>
                    </div>
                </div>
            `;
            itemsContainer.insertAdjacentHTML('beforeend', itemHTML);
        });

        document.querySelectorAll('.remove-item-btn-drawer').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const idx = e.target.dataset.index;
                let currentCart = JSON.parse(localStorage.getItem('edemium_cart')) || [];
                currentCart.splice(idx, 1);
                localStorage.setItem('edemium_cart', JSON.stringify(currentCart));

                if (currentCart.length === 0) localStorage.removeItem('edemium_global_dates');

                renderCartDrawer();
                window.updateGlobalCartBadge();
                if (typeof window.syncCardsUI === 'function') window.syncCardsUI();
            });
        });

        attachDateSyncLogic();
    }

    function getCurrentLocalMinDateTime() {
        const now = new Date();
        
        const offset = now.getTimezoneOffset() * 60000;
        
        return (new Date(now.getTime() - offset)).toISOString().slice(0, 16);
    }

    function attachDateSyncLogic() {
        const inputs = itemsContainer.querySelectorAll('.sync-trigger');

        inputs.forEach(input => {
            input.addEventListener('change', (e) => {
                saveDrawerData();

                const card = input.closest('.cart-item-card');
                const currentIndex = parseInt(card.dataset.index);
                const isStart = input.classList.contains('cart-start-date');
                const rawVal = input.value;

                if (!rawVal) return;
                const newVal = rawVal.length === 16 ? rawVal + ':00' : rawVal;

                let currentCart = JSON.parse(localStorage.getItem('edemium_cart')) || [];
                if (currentCart.length <= 1) {
                    let globalDates = JSON.parse(localStorage.getItem('edemium_global_dates')) || { start: '', end: '' };
                    if (isStart) globalDates.start = newVal; else globalDates.end = newVal;
                    localStorage.setItem('edemium_global_dates', JSON.stringify(globalDates));
                    return;
                }

                let othersEmpty = true;
                currentCart.forEach((item, idx) => {
                    if (idx !== currentIndex) {
                        const val = isStart ? item.start : item.end;
                        if (val && val !== '') othersEmpty = false;
                    }
                });

                if (othersEmpty) {
                    currentCart.forEach((item, idx) => {
                        if (idx !== currentIndex) {
                            if (isStart) item.start = newVal; else item.end = newVal;
                        }
                    });

                    let globalDates = JSON.parse(localStorage.getItem('edemium_global_dates')) || { start: '', end: '' };
                    if (isStart) globalDates.start = newVal; else globalDates.end = newVal;

                    localStorage.setItem('edemium_global_dates', JSON.stringify(globalDates));
                    localStorage.setItem('edemium_cart', JSON.stringify(currentCart));
                    renderCartDrawer();
                } else {
                    showSyncPopover(input, currentIndex, isStart, newVal, currentCart);
                }
            });
        });
    }

    function showSyncPopover(input, currentIndex, isStart, newVal, currentCart) {
        if (window.activeDatePopover) window.activeDatePopover.remove();

        const popover = document.createElement('div');
        popover.className = 'date-sync-popover';

        let listHtml = '';
        currentCart.forEach((item, idx) => {
            if (idx === currentIndex) return;
            listHtml += `
                <label class="sync-item">
                    <input type="checkbox" value="${idx}" checked class="sync-checkbox">
                    <span class="sync-item-name">${item.serviceName}</span>
                </label>
            `;
        });

        popover.innerHTML = `
            <div class="sync-header">Застосувати цю дату до інших?</div>
            <div class="sync-list">
                <label class="sync-item sync-all">
                    <input type="checkbox" id="sync-all-checkbox" checked>
                    <strong>Обрати всі послуги</strong>
                </label>
                ${listHtml}
            </div>
            <div class="sync-actions">
                <button type="button" class="btn-sync-apply">Так</button>
                <button type="button" class="btn-sync-cancel">Ні, тільки сюди</button>
            </div>
        `;

        input.parentElement.appendChild(popover);
        window.activeDatePopover = popover;

        const selectAll = popover.querySelector('#sync-all-checkbox');
        const checkboxes = popover.querySelectorAll('.sync-checkbox');

        selectAll.addEventListener('change', (e) => {
            checkboxes.forEach(cb => cb.checked = e.target.checked);
        });

        checkboxes.forEach(cb => cb.addEventListener('change', () => {
            if (!cb.checked) selectAll.checked = false;
        }));

        popover.querySelector('.btn-sync-cancel').addEventListener('click', () => {
            popover.remove();
            window.activeDatePopover = null;
        });

        popover.querySelector('.btn-sync-apply').addEventListener('click', () => {
            const selectedIndexes = Array.from(checkboxes).filter(cb => cb.checked).map(cb => parseInt(cb.value));
            let updatedCart = JSON.parse(localStorage.getItem('edemium_cart')) || [];

            selectedIndexes.forEach(idx => {
                if (isStart) updatedCart[idx].start = newVal; else updatedCart[idx].end = newVal;
            });

            if (selectedIndexes.length === checkboxes.length) {
                let globalDates = JSON.parse(localStorage.getItem('edemium_global_dates')) || { start: '', end: '' };
                if (isStart) globalDates.start = newVal; else globalDates.end = newVal;
                localStorage.setItem('edemium_global_dates', JSON.stringify(globalDates));
            }

            localStorage.setItem('edemium_cart', JSON.stringify(updatedCart));
            popover.remove();
            window.activeDatePopover = null;
            renderCartDrawer();
        });
    }

    function saveDrawerData() {
        let cart = JSON.parse(localStorage.getItem('edemium_cart')) || [];
        const itemCards = document.querySelectorAll('.cart-item-card');

        itemCards.forEach((card, index) => {
            if (cart[index]) {
                const classType = card.dataset.classType;
                const startVal = card.querySelector('.cart-start-date').value;
                cart[index].start = startVal.length === 16 ? startVal + ':00' : startVal;

                if (classType === 'SPA_BOOKING') {
                    
                    if (startVal) {
                        const duration = parseInt(card.dataset.duration) || 0;
                        const startDate = new Date(startVal);
                        startDate.setMinutes(startDate.getMinutes() + duration);

                        
                        const offset = startDate.getTimezoneOffset() * 60000;
                        const localISOTime = (new Date(startDate - offset)).toISOString().slice(0, 16);
                        cart[index].end = localISOTime + ':00';
                    }

                    const genderSelect = card.querySelector('.cart-prefered-gender');
                    if (genderSelect) {
                        cart[index].preferedGender = genderSelect.value;
                    }
                } else {
                    const endVal = card.querySelector('.cart-end-date').value;
                    cart[index].end = endVal.length === 16 ? endVal + ':00' : endVal;
                }

                let count = parseInt(card.querySelector('.cart-client-count').value) || 1;

                if (count > cart[index].maxCapacity) {
                    count = cart[index].maxCapacity;
                    alert("Кількість гостей зменшено до " + cart[index].maxCapacity)
                } else if (count < 1) {
                    count = 1;
                }

                cart[index].clientCount = count;

                card.querySelector('.cart-client-count').value = count;
            }
        });
        localStorage.setItem('edemium_cart', JSON.stringify(cart));
        updateGlobalDatesFallback(cart);
    }

    
    let isResizing = false;

    if (cartDrawer) {
        cartDrawer.addEventListener('mousemove', (e) => {
            if (isResizing) return;
            const rect = cartDrawer.getBoundingClientRect();
            if (e.clientX - rect.left <= 15) {
                cartDrawer.style.cursor = 'col-resize';
            } else {
                cartDrawer.style.cursor = '';
            }
        });

        cartDrawer.addEventListener('mousedown', (e) => {
            const rect = cartDrawer.getBoundingClientRect();
            if (e.clientX - rect.left <= 15) {
                isResizing = true;
                document.body.style.userSelect = 'none';
                document.body.style.cursor = 'col-resize';

                document.addEventListener('mousemove', resizeDrawer);
                document.addEventListener('mouseup', stopResize);
            }
        });

        cartDrawer.addEventListener('touchstart', (e) => {
            const rect = cartDrawer.getBoundingClientRect();
            if (e.touches[0].clientX - rect.left <= 25) {
                isResizing = true;
                document.body.style.userSelect = 'none';

                document.addEventListener('touchmove', resizeDrawer, { passive: true });
                document.addEventListener('touchend', stopResize);
            }
        }, { passive: true });
    }

    function resizeDrawer(e) {
        if (!isResizing) return;
        let clientX = e.clientX || (e.touches && e.touches[0].clientX);
        if (clientX === undefined) return;

        let newWidth = window.innerWidth - clientX;
        if (newWidth < 350) newWidth = 350;
        if (newWidth > window.innerWidth * 0.9) newWidth = window.innerWidth * 0.9;

        cartDrawer.style.width = newWidth + 'px';
        cartDrawer.style.maxWidth = '100%';
    }

    function stopResize() {
        if (!isResizing) return;
        isResizing = false;

        document.body.style.userSelect = '';
        document.body.style.cursor = '';
        cartDrawer.style.cursor = '';

        document.removeEventListener('mousemove', resizeDrawer);
        document.removeEventListener('mouseup', stopResize);
        document.removeEventListener('touchmove', resizeDrawer);
        document.removeEventListener('touchend', stopResize);

        if (cartDrawer) {
            localStorage.setItem('edemium_cart_width', cartDrawer.style.width);
        }
    }

    function openDrawer() {
        let savedWidth = localStorage.getItem('edemium_cart_width');
        if (savedWidth && cartDrawer) {
            cartDrawer.style.width = savedWidth;
            cartDrawer.style.maxWidth = '100%';
        }

        renderCartDrawer();
        if (cartOverlay) cartOverlay.classList.remove('hidden');
        if (cartDrawer) cartDrawer.classList.remove('hidden');
        document.body.style.overflow = 'hidden';
    }

    function closeDrawer() {
        if (window.activeDatePopover) window.activeDatePopover.remove();
        saveDrawerData();
        if (cartOverlay) cartOverlay.classList.add('hidden');
        if (cartDrawer) cartDrawer.classList.add('hidden');
        document.body.style.overflow = '';
    }

    if (bookingNoteContainer) bookingNoteContainer.addEventListener("click", openDrawer);
    if (closeBtn) closeBtn.addEventListener("click", closeDrawer);
    if (cartOverlay) cartOverlay.addEventListener("click", closeDrawer);

    if (proceedBtn) {
        proceedBtn.addEventListener('click', () => {
            const inputs = document.querySelectorAll('.cart-item-card input[required]');
            let isFilled = true;
            inputs.forEach(input => {
                if (!input.value) {
                    isFilled = false;
                    input.style.borderColor = 'red';
                } else {
                    input.style.borderColor = '#cbd5e1';
                }
            });

            if (!isFilled) {
                alert('Будь ласка, заповніть дати заїзду та виїзду для всіх номерів та послуг.');
                return;
            }
            
            saveDrawerData();
            
            const currentCart = JSON.parse(localStorage.getItem('edemium_cart')) || [];
            
            const now = new Date();
            const gracePeriod = new Date(now.getTime() - 2 * 60000);

            for (const item of currentCart) {
                const startDate = new Date(item.start);
                const endDate = new Date(item.end);
                const isSpa = item.classType && item.classType.includes('SPA');

                if (startDate < gracePeriod) {
                    alert(`Час початку для послуги "${item.serviceName}" не може бути в минулому.`);
                    return; 
                }

                if (endDate <= startDate) {
                    alert(`Час завершення/виїзду для "${item.serviceName}" повинен бути пізніше за час початку.`);
                    return;
                }
                
                if (!isSpa) {
                    const diffHours = (endDate - startDate) / (1000 * 60 * 60);
                    if (diffHours < 24) {
                        alert(`Мінімальний час бронювання для номеру "${item.serviceName}" — 24 години.`);
                        return;
                    }
                }
            }

            
            const href = proceedBtn.getAttribute("data-href") || "/bookings-submit";
            window.location.href = href;
        });
    }

    if (browseBtn) {
        browseBtn.addEventListener('click', () => {
            const currentPath = window.location.pathname.toLowerCase();
            if (currentPath.includes('/services')) {
                closeDrawer();
            } else {
                window.location.href = '/services';
            }
        });
    }
}

function initializeChangePhoneModal() {
    const link = document.getElementById('change-phone-link');
    const overlay = document.getElementById('change-phone-overlay');
    if (!link || !overlay) return; 

    const closeBtn = document.getElementById('close-phone-modal');
    const form = document.getElementById('change-phone-form');

    const step1 = document.getElementById('update-phone-step-1');
    const step2 = document.getElementById('update-phone-step-2');

    const phoneInput = form.querySelector('input[name="newPhone"]');
    const otpInput = form.querySelector('input[name="updateOtpCode"]');
    const displayPhone = document.getElementById('update-display-phone');

    const error1 = document.getElementById('update-step1-error');
    const error2 = document.getElementById('update-step2-error');
    const successMsg = document.getElementById('update-success-msg');

    const btnRequestOtp = document.getElementById('btn-request-update-otp');
    const btnResendOtp = document.getElementById('btn-resend-update-otp');

    let cooldownTimer;

    
    link.addEventListener('click', (e) => {
        e.preventDefault();
        document.querySelector('.profile-options').classList.add('hidden'); 
        overlay.classList.remove('hidden');
        document.body.style.overflow = 'hidden'; 
    });

    
    function closeModal() {
        overlay.classList.add('hidden');
        document.body.style.overflow = '';

        
        setTimeout(() => {
            form.reset();
            step1.classList.remove('hidden');
            step2.classList.add('hidden');
            error1.textContent = '';
            error2.textContent = '';
            successMsg.classList.add('hidden');
            clearInterval(cooldownTimer);
            btnResendOtp.disabled = false;
            btnResendOtp.textContent = 'Відправити код ще раз';
        }, 300);
    }

    closeBtn.addEventListener('click', closeModal);
    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) closeModal();
    });

    
    async function requestUpdateOtp(button) {
        if (!phoneInput.checkValidity()) {
            form.reportValidity();
            return;
        }

        error1.textContent = '';
        error2.textContent = '';
        button.disabled = true;
        const originalText = button.textContent;
        button.textContent = 'Відправка...';

        const tokenHeader = document.querySelector("meta[name='_csrf_header']").content;
        const token = document.querySelector("meta[name='_csrf']").content;

        try {
            const res = await fetch('/api/users/profile/phone/request-update', {
                method: 'POST',
                headers: {
                    [tokenHeader]: token,
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: new URLSearchParams({ newPhone: phoneInput.value })
            });

            if (res.ok) {
                step1.classList.add('hidden');
                step2.classList.remove('hidden');
                displayPhone.textContent = phoneInput.value;
                otpInput.required = true;
                startCooldown(btnResendOtp);
            } else {
                const data = await res.json().catch(() => ({}));
                const err = data.error || "Помилка сервера";
                if (button.id === 'btn-request-update-otp') error1.textContent = err;
                else error2.textContent = err;
            }
        } catch (err) {
            error1.textContent = "Помилка з'єднання.";
        } finally {
            button.disabled = false;
            button.textContent = originalText;
        }
    }

    btnRequestOtp.addEventListener('click', () => requestUpdateOtp(btnRequestOtp));
    btnResendOtp.addEventListener('click', () => requestUpdateOtp(btnResendOtp));

    
    function startCooldown(btn) {
        let seconds = 60;
        btn.disabled = true;
        clearInterval(cooldownTimer);
        cooldownTimer = setInterval(() => {
            seconds--;
            btn.textContent = `Відправити ще раз ${seconds} с...`;
            if (seconds <= 0) {
                clearInterval(cooldownTimer);
                btn.disabled = false;
                btn.textContent = 'Відправити код ще раз';
            }
        }, 1000);
    }

    
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (step2.classList.contains('hidden')) return; 

        error2.textContent = '';
        const submitBtn = form.querySelector('button[type="submit"]');
        submitBtn.disabled = true;
        submitBtn.textContent = 'Оновлення...';

        const tokenHeader = document.querySelector("meta[name='_csrf_header']").content;
        const token = document.querySelector("meta[name='_csrf']").content;

        try {
            const res = await fetch('/api/users/profile/phone/verify-update', {
                method: 'POST',
                headers: {
                    [tokenHeader]: token,
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: new URLSearchParams({
                    newPhone: phoneInput.value,
                    otpCode: otpInput.value
                })
            });

            const data = await res.json().catch(() => ({}));

            if (res.ok) {
                successMsg.textContent = data.message || "Номер успішно змінено!";
                successMsg.classList.remove('hidden');

                
                setTimeout(() => {
                    closeModal();

                    const currentPath = window.location.pathname;

                    if (currentPath === '/bookings' || currentPath === '/bookings-submit') {
                        window.location.reload();
                    }

                }, 2000);
            } else {
                error2.textContent = data.error || "Невірний код";
                submitBtn.disabled = false;
                submitBtn.textContent = 'Підтвердити';
            }
        } catch (err) {
            error2.textContent = "Помилка з'єднання.";
            submitBtn.disabled = false;
            submitBtn.textContent = 'Підтвердити';
        }
    });
}