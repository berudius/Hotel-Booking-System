"use strict"
document.addEventListener("DOMContentLoaded", () => {
    initializeListInputEvents();
    initializePairInputEvents();
    initializePostModalOpenerEvent();
    initializePostFormEvent();
    initializeEditModalOpenerEvent();
    initializePutFormEvent();
    initializeDeleteModalOpenerEvent();
    InitializeDeleteRoomEvent();
    initializeFormOverlayClick();
    initializeImageSliders();
    initializeLightbox();
    initializeInfiniteScroll();
    initializeSpaInfoModals();
});


function initializeListInputEvents() {
    const listContainers = document.querySelectorAll(".form-facility-container");

    listContainers.forEach(container => {
        let draggedEl = null;
        let placeholder = document.createElement("div");
        placeholder.className = "facility-placeholder";
        const inputName = container.getAttribute("data-list-name") || "listItem";

        const createInput = (value = "") => {
            const div = document.createElement("div");
            div.className = "facility-item";
            div.draggable = true;
            div.innerHTML = `<input type="text" name="${inputName}" placeholder="Значення..." value="${value}">`;

            div.addEventListener("dragstart", e => {
                draggedEl = div;
                div.classList.add("dragging");
                setTimeout(() => div.style.display = "none", 0);
            });
            div.addEventListener("dragend", () => {
                div.style.display = "flex";
                div.classList.remove("dragging");
                if (placeholder.parentNode) placeholder.remove();
                draggedEl = null;
            });
            div.querySelector("input").addEventListener("input", () => manageInputs(container));
            return div;
        };

        container.addEventListener("dragover", e => {
            e.preventDefault();
            const afterElement = getDragAfterElement(container, e.clientY);
            if (afterElement == null) {
                container.append(placeholder);
            } else {
                container.insertBefore(placeholder, afterElement);
            }
        });

        container.addEventListener("drop", e => {
            e.preventDefault();
            if (draggedEl) {
                container.insertBefore(draggedEl, placeholder);
            }
        });

        function manageInputs(currentContainer) {
            const inputs = [...currentContainer.querySelectorAll("input")];
            const empty = inputs.filter(inp => inp.value.trim() === "");
            if (empty.length === 0) currentContainer.append(createInput());
            if (empty.length > 1) {
                for (let i = 1; i < empty.length; i++) {
                    if (currentContainer.children.length > 1) empty[i].parentElement.remove();
                }
            }
        }

        container.clearAndSetList = (array = []) => {
            container.innerHTML = "";
            array.forEach(val => container.append(createInput(val)));
            container.append(createInput(""));
        };
        container.clearAndSetList([]);
    });
}

function getDragAfterElement(container, y) {
    const draggableElements = [...container.querySelectorAll("div[draggable='true']:not(.dragging)")];
    return draggableElements.reduce((closest, child) => {
        const box = child.getBoundingClientRect();
        const offset = y - box.top - box.height / 2;
        if (offset < 0 && offset > closest.offset) return { offset: offset, element: child };
        return closest;
    }, { offset: Number.NEGATIVE_INFINITY }).element;
}


function initializePairInputEvents() {
    const pairContainers = document.querySelectorAll(".form-pair-container");

    pairContainers.forEach(container => {
        let draggedEl = null;
        let placeholder = document.createElement("div");
        placeholder.className = "facility-placeholder";
        const inputName = container.getAttribute("data-list-name");

        const createPairInput = (nameVal = "", descVal = "") => {
            const div = document.createElement("div");
            div.className = "pair-item";
            div.draggable = true;
            div.innerHTML = `
                <input type="text" class="pair-name" placeholder="Назва" value="${nameVal}">
                <textarea class="pair-desc" placeholder="Опис">${descVal}</textarea>
            `;

            div.addEventListener("dragstart", e => { draggedEl = div; div.classList.add("dragging"); setTimeout(() => div.style.display = "none", 0); });
            div.addEventListener("dragend", () => { div.style.display = "flex"; div.classList.remove("dragging"); if (placeholder.parentNode) placeholder.remove(); draggedEl = null; });

            div.querySelectorAll("input, textarea").forEach(el => el.addEventListener("input", () => managePairInputs(container)));
            return div;
        };

        container.addEventListener("dragover", e => {
            e.preventDefault();
            const afterElement = getDragAfterElement(container, e.clientY);
            if (afterElement == null) container.append(placeholder);
            else container.insertBefore(placeholder, afterElement);
        });
        container.addEventListener("drop", e => { e.preventDefault(); if (draggedEl) container.insertBefore(draggedEl, placeholder); });

        function managePairInputs(currentContainer) {
            const pairs = [...currentContainer.querySelectorAll(".pair-item")];
            const empty = pairs.filter(p => p.querySelector(".pair-name").value.trim() === "" && p.querySelector(".pair-desc").value.trim() === "");
            if (empty.length === 0) currentContainer.append(createPairInput());
            if (empty.length > 1) {
                for (let i = 1; i < empty.length; i++) {
                    if (currentContainer.children.length > 1) empty[i].remove();
                }
            }
        }

        container.clearAndSetPairs = (array = []) => {
            container.innerHTML = "";
            array.forEach(val => container.append(createPairInput(val.name, val.description)));
            container.append(createPairInput("", ""));
        };
        container.clearAndSetPairs([]);
    });
}

function extractList(form, listName) {
    return [...form.querySelectorAll(`.form-facility-container[data-list-name='${listName}'] input`)]
        .map(input => input.value.trim()).filter(v => v.length > 0);
}

function extractPairs(form, listName) {
    return [...form.querySelectorAll(`.form-pair-container[data-list-name='${listName}'] .pair-item`)]
        .map(div => ({
            name: div.querySelector(".pair-name").value.trim(),
            description: div.querySelector(".pair-desc").value.trim()
        }))
        .filter(p => p.name.length > 0 || p.description.length > 0);
}

function initializePostFormEvent() {
    const tokenHeader = document.querySelector("meta[name='_csrf_header']").content;
    const token = document.querySelector("meta[name='_csrf']").content;
    const postForm = document.getElementById("post-form");

    if (postForm) {
        postForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            try {
                const spaDto = {
                    name: postForm.querySelector("input[name='name']").value,
                    price: parseFloat(postForm.querySelector("input[name='price']").value),
                    description: postForm.querySelector("textarea[name='description']").value,
                    type: postForm.querySelector("input[name='type']").value,
                    guestCapacity: parseInt(postForm.querySelector("input[name='guestCapacity']").value),
                    preparingInfoForClient: postForm.querySelector("textarea[name='preparingInfoForClient']").value,
                    durationInMinutes: parseInt(postForm.querySelector("input[name='durationInMinutes']").value),

                    hiddenFromClient: postForm.querySelector("input[name='hiddenFromClient']").checked,

                    facilities: extractList(postForm, "facilities"),
                    contraindications: extractList(postForm, "contraindications"),
                    cautionNotes: extractList(postForm, "cautionNotes"),
                    spaStagesDescriptions: extractPairs(postForm, "spaStagesDescriptions"),
                    careProductsDescriptions: extractPairs(postForm, "careProductsDescriptions"),
                    classType: "SPA"
                };

                const postImages = postForm.querySelector("input[name='post-images']").files;
                const formData = new FormData();
                formData.append("spa", new Blob([JSON.stringify(spaDto)], { type: 'application/json' }));
                for (let postImage of postImages) formData.append("imageFiles", postImage);

                const res = await fetch(postForm.action, { method: "POST", body: formData, headers: { [tokenHeader]: token } });

                if (!res.ok) {
                    const errorData = await res.json(); 
                    alert(errorData.error);             
                    return;
                }

                window.location.reload();
            } catch (error) { alert("Помилка мережі."); }
        });
    }
}

function initializePostModalOpenerEvent() {
    const postFormContainer = document.querySelector(".post-form-container");
    const postFormOpener = document.getElementById("post-form-opener");
    if (postFormOpener && postFormContainer) {
        postFormOpener.addEventListener("click", () => postFormContainer.classList.remove("hidden"));
    }
}

function initializeEditModalOpenerEvent() {
    const putFormContainer = document.querySelector(".put-form-container");
    const gridContainer = document.querySelector(".cards-grid");

    if (!gridContainer || !putFormContainer) return;

    gridContainer.addEventListener("click", async (event) => {
        
        const opener = event.target.closest(".edit-modal-opener");
        if (!opener) return;

        const card = opener.closest(".spa-card");
        const spaId = card.getAttribute("data-service-id");

        try {
            document.getElementById("put-spa-id").value = spaId;
            document.getElementById("put-name").value = card.querySelector("h3").textContent;
            document.getElementById("put-type").value = card.querySelector(".type-badge").textContent;
            document.getElementById("put-price").value = parseFloat(card.querySelector(".spa-meta span:nth-child(1)").textContent);
            document.getElementById("put-duration").value = parseInt(card.querySelector(".spa-meta span:nth-child(2)").textContent);
            document.getElementById("put-description").value = card.querySelector("p:not(.type-badge)").textContent;
            let boo = card.getAttribute("data-hidden") === "true";
            document.getElementById("put-hidden").checked = card.getAttribute("data-hidden") === "true";

            const selectBtn = card.querySelector(".btn-select");
            if (selectBtn) {
                document.getElementById("put-capacity").value = selectBtn.getAttribute("data-service-capacity");
            }
            document.getElementById("put-preparing").value = card.getAttribute("data-preparing") || "";

            const facTags = card.querySelectorAll(".spa-facility-tag");
            const putFacilities = document.getElementById("put-facilities");

            if (putFacilities && typeof putFacilities.clearAndSetList === "function") {
                putFacilities.clearAndSetList(Array.from(facTags).map(el => el.textContent.trim()));
            }

            putFormContainer.classList.remove("hidden");
        } catch (e) {
            console.error("Помилка завантаження даних", e);
        }
    });
}

function initializePutFormEvent() {
    const tokenHeader = document.querySelector("meta[name='_csrf_header']").content;
    const token = document.querySelector("meta[name='_csrf']").content;
    const putForm = document.getElementById("put-form");

    if (putForm) {
        putForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            try {
                const spaDto = {
                    id: putForm.querySelector("#put-spa-id").value,
                    name: putForm.querySelector("#put-name").value,
                    price: parseFloat(putForm.querySelector("#put-price").value),
                    description: putForm.querySelector("#put-description").value,
                    type: putForm.querySelector("#put-type").value,
                    guestCapacity: parseInt(putForm.querySelector("#put-capacity").value),
                    preparingInfoForClient: putForm.querySelector("#put-preparing").value,
                    durationInMinutes: parseInt(putForm.querySelector("#put-duration").value),

                    hiddenFromClient: putForm.querySelector("#put-hidden").checked,

                    facilities: extractList(putForm, "facilities"),
                    contraindications: extractList(putForm, "contraindications"),
                    cautionNotes: extractList(putForm, "cautionNotes"),
                    spaStagesDescriptions: extractPairs(putForm, "spaStagesDescriptions"),
                    careProductsDescriptions: extractPairs(putForm, "careProductsDescriptions"),
                    classType: "SPA"
                };

                const putImages = putForm.querySelector("input[name='post-images']").files;
                const formData = new FormData();
                formData.append("spa", new Blob([JSON.stringify(spaDto)], { type: 'application/json' }));
                for (let putImage of putImages) formData.append("imageFiles", putImage);

                const res = await fetch(putForm.action, { method: "PUT", body: formData, headers: { [tokenHeader]: token } });

                if (!res.ok) {
                    const errorData = await res.json(); 
                    alert(errorData.error);             
                    return;
                }
                window.location.reload();
            } catch (error) { alert("Помилка мережі."); }
        });
    }
}

function initializeFormOverlayClick() {
    const modalContainers = document.querySelectorAll('.post-form-container, .put-form-container, .delete-modal-window');
    modalContainers.forEach(container => {
        container.addEventListener("click", (event) => { if (event.target === container) container.classList.add("hidden"); });
        const closerBtn = container.querySelector('.form-closer, .cancel-delete-btn');
        if (closerBtn) closerBtn.addEventListener("click", (e) => { e.preventDefault(); container.classList.add("hidden"); });
    });
}

function initializeDeleteModalOpenerEvent() {
    const deleteModal = document.querySelector(".delete-modal-window");
    const confirmDeleteBtn = document.querySelector(".confirm-delete-btn");
    const gridContainer = document.querySelector(".cards-grid");

    if (!deleteModal || !gridContainer) return;

    gridContainer.addEventListener("click", (event) => {
        
        const opener = event.target.closest(".delete-modal-opener");
        if (!opener) return;

        const card = opener.closest(".spa-card");
        confirmDeleteBtn.setAttribute("data-service-id", card.getAttribute("data-service-id"));
        deleteModal.classList.remove("hidden");
    });
}

function InitializeDeleteRoomEvent() {
    const confirmDeleteBtn = document.querySelector(".confirm-delete-btn");
    const deleteModal = document.querySelector(".delete-modal-window");

    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener("click", async (event) => {
            const id = event.target.getAttribute("data-service-id");
            if (!id) return;
            const tokenHeader = document.querySelector('meta[name="_csrf_header"]').content;
            const token = document.querySelector('meta[name="_csrf"]').content;

            try {
                const res = await fetch(`/api/spas/${id}`, { method: 'DELETE', headers: { [tokenHeader]: token } });
                if (res.ok) {
                    const card = document.querySelector(`.spa-card[data-service-id="${id}"]`);
                    if (card) card.remove();
                    deleteModal.classList.add("hidden");
                } else {
                    alert("Помилка при видаленні!");
                }
            } catch (error) { }
        });
    }
}

function initializeImageSliders() {
    const imageContainers = document.querySelectorAll(".spa-image-container");
    imageContainers.forEach(container => {
        const prevBtn = container.querySelector(".slider-btn-prev");
        const nextBtn = container.querySelector(".slider-btn-next");
        let images = Array.from(container.querySelectorAll(".spa-slide-image"));
        if (images.length === 0) return;
        container.classList.add("js-loaded");
        let currentIndex = 0;

        const updateSlider = () => {
            images = Array.from(container.querySelectorAll(".spa-slide-image"));
            images.forEach((img, index) => img.classList.toggle("active", index === currentIndex));
        };
        updateSlider();

        if (nextBtn) nextBtn.addEventListener("click", (e) => { e.preventDefault(); e.stopPropagation(); currentIndex = (currentIndex + 1) % images.length; updateSlider(); });
        if (prevBtn) prevBtn.addEventListener("click", (e) => { e.preventDefault(); e.stopPropagation(); currentIndex = (currentIndex - 1 + images.length) % images.length; updateSlider(); });
    });
}

function initializeLightbox() {
    const lightbox = document.getElementById("lightbox");
    if (!lightbox) return;
    const lightboxImg = document.getElementById("lightbox-img");
    const closeBtn = document.getElementById("lightbox-close");
    const prevBtn = document.getElementById("lightbox-prev");
    const nextBtn = document.getElementById("lightbox-next");

    const dotsBtn = document.getElementById("lightbox-dots");
    const dropdown = document.getElementById("lightbox-dropdown");
    const deleteBtn = document.getElementById("lightbox-delete");

    let currentImages = [], currentIndex = 0;

    const updateView = () => {
        if (currentImages.length === 0) { lightbox.classList.add("hidden"); return; }
        lightboxImg.src = currentImages[currentIndex].src;
        prevBtn.style.display = currentImages.length > 1 ? "flex" : "none";
        nextBtn.style.display = currentImages.length > 1 ? "flex" : "none";

        if (dropdown) dropdown.classList.add("hidden");
    };

    const gridContainer = document.querySelector(".cards-grid");

    if (gridContainer) {
        gridContainer.addEventListener("click", (e) => {
            const container = e.target.closest(".spa-image-container");
            if (!container) return;

            if (e.target.closest(".slider-btn")) return;

            const imgs = Array.from(container.querySelectorAll(".spa-slide-image"));
            if (imgs.length === 0) return;

            currentImages = imgs.map(img => ({
                id: img.getAttribute("data-id"),
                src: img.getAttribute("src"),
                element: img
            }));

            const clickedImg = e.target.closest(".spa-slide-image");
            if (clickedImg) {
                currentIndex = currentImages.findIndex(img => img.src === clickedImg.getAttribute("src"));
            } else {
                const activeImg = container.querySelector(".spa-slide-image.active");
                if (activeImg) {
                    currentIndex = currentImages.findIndex(img => img.src === activeImg.getAttribute("src"));
                } else {
                    currentIndex = 0;
                }
            }

            if (currentIndex === -1) currentIndex = 0;

            updateView();
            lightbox.classList.remove("hidden");
        });
    }

    closeBtn.addEventListener("click", () => lightbox.classList.add("hidden"));
    prevBtn.addEventListener("click", (e) => { e.stopPropagation(); currentIndex = (currentIndex - 1 + currentImages.length) % currentImages.length; updateView(); });
    nextBtn.addEventListener("click", (e) => { e.stopPropagation(); currentIndex = (currentIndex + 1) % currentImages.length; updateView(); });

    if (dotsBtn && dropdown) {
        dotsBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            dropdown.classList.toggle("hidden");
        });

        document.addEventListener("click", (e) => {
            if (!dotsBtn.contains(e.target) && !dropdown.contains(e.target)) {
                dropdown.classList.add("hidden");
            }
        });
    }

    if (deleteBtn) {
        deleteBtn.addEventListener("click", async () => {
            const imageObj = currentImages[currentIndex];

            
            const container = imageObj.element ? imageObj.element.closest(".spa-image-container") : null;

            const tokenHeader = document.querySelector("meta[name='_csrf_header']").content;
            const token = document.querySelector("meta[name='_csrf']").content;

            try {
                deleteBtn.textContent = "Видалення...";
                const baseUrl = deleteBtn.getAttribute("data-url");
                const encodedImageUrl = encodeURIComponent(imageObj.src);

                const res = await fetch(`${baseUrl}?imageId=${imageObj.id}&imageUrl=${encodedImageUrl}`, {
                    method: "DELETE",
                    headers: { [tokenHeader]: token }
                });

                if (!res.ok) throw new Error("Помилка видалення на сервері");

                if (imageObj.element) {
                    imageObj.element.remove();
                }

                currentImages.splice(currentIndex, 1);

                if (currentImages.length > 0) {
                    if (currentIndex >= currentImages.length) {
                        currentIndex = currentImages.length - 1;
                    }
                    updateView();
                } else {
                    lightbox.classList.add("hidden");

                    
                    if (container) {
                        container.innerHTML = `<img class="spa-slide-image active" src="/common/images/placeholder.png" alt="Зображення відсутнє">`;
                    }
                }

            } catch (error) {
                alert("Не вдалося видалити фотографію. Перевірте підключення.");
            } finally {
                deleteBtn.textContent = "Видалити фото";
                dropdown.classList.add("hidden");
            }
        });
    }
}

function initializeInfiniteScroll() {
    
    const gridContainer = document.querySelector(".cards-grid");
    if (!gridContainer) {
        return;
    }

    const statusContainer = document.getElementById("infinite-scroll-status");
    if (!statusContainer) {
        return;
    }

    const loader = document.getElementById("scroll-loader");
    const errorBlock = document.getElementById("scroll-error");
    const endBlock = document.getElementById("scroll-end");
    const retryBtn = document.getElementById("scroll-retry-btn");

    let currentPage = 1;
    const pageSize = 12;
    let isLoading = false;
    let isLastPage = false;

    const setScrollState = (state) => {
        if (loader) loader.classList.add("hidden");
        if (errorBlock) errorBlock.classList.add("hidden");
        if (endBlock) endBlock.classList.add("hidden");

        if (state === "loading" && loader) loader.classList.remove("hidden");
        if (state === "error" && errorBlock) errorBlock.classList.remove("hidden");
        if (state === "end" && endBlock) endBlock.classList.remove("hidden");
    };


    
    const createSpaCard = (spa) => {
        const isAdmin = document.querySelector(".edit-modal-opener") !== null;

        const card = document.createElement("div");
        card.className = "card spa-card " + (spa.hiddenFromClient == true ? "spa-card-client-hidden" : "");
        card.setAttribute("data-service-id", spa.id);
        card.setAttribute("data-preparing", spa.preparingInfoForClient || "");
        card.setAttribute("data-hidden", spa.hiddenFromClient === true);

        const hiddenBadgeHtml = spa.hiddenFromClient ? `
            <div class="admin-hidden-badge">👁️‍🗨️ Приховано</div>
        ` : '';

        let imagesHtml = '';
        let navButtonsHtml = '';

        if (spa.imageRecords && spa.imageRecords.length > 0) {
            spa.imageRecords.forEach((img, index) => {
                const activeClass = index === 0 ? "active" : "";
                
                const imageUrl = img.url ? img.url : "/common/images/placeholder.png";

                imagesHtml += `<img class="spa-slide-image ${activeClass}" data-id="${img.id}" src="${imageUrl}" alt="${img.position || index + 1}">`;
            });

            if (spa.imageRecords.length > 1) {
                navButtonsHtml = `
                    <button class="slider-btn slider-btn-prev" aria-label="Previous image">&lt;</button>
                    <button class="slider-btn slider-btn-next" aria-label="Next image">&gt;</button>
                `;
            }
        } else {
            
            imagesHtml = `<img class="spa-slide-image active" src="/common/images/placeholder.png" alt="Зображення відсутнє">`;
        }

        const facilitiesHtml = (spa.facilities || []).map(fac =>
            `<span class="spa-facility-tag">${fac}</span>`
        ).join("");

        const adminButtonsHtml = isAdmin ? `
            <div class="modal-buttons-container">
                <button class="edit-modal-opener">Редагувати</button>
                <button class="delete-modal-opener">Видалити</button>
            </div>
        ` : "";

        
        let modalBodyHtml = `
            <h3>Опис процедури</h3>
            <p>${spa.description || ""}</p>
        `;

        if (spa.preparingInfoForClient && spa.preparingInfoForClient.trim() !== '') {
            modalBodyHtml += `<h3>Підготовка до процедури</h3><p>${spa.preparingInfoForClient}</p>`;
        }
        if (spa.contraindications && spa.contraindications.length > 0) {
            modalBodyHtml += `<h3>Протипоказання</h3><ul class="modal-list">${spa.contraindications.map(i => `<li>${i}</li>`).join('')}</ul>`;
        }
        if (spa.cautionNotes && spa.cautionNotes.length > 0) {
            modalBodyHtml += `<h3>Важливі застереження</h3><ul class="modal-list">${spa.cautionNotes.map(i => `<li>${i}</li>`).join('')}</ul>`;
        }
        if (spa.spaStagesDescriptions && spa.spaStagesDescriptions.length > 0) {
            modalBodyHtml += `<h3>Етапи процедури</h3><ul class="modal-list">${spa.spaStagesDescriptions.map(s => `<li><strong>${s.name}</strong>: <span>${s.description}</span></li>`).join('')}</ul>`;
        }
        if (spa.careProductsDescriptions && spa.careProductsDescriptions.length > 0) {
            modalBodyHtml += `<h3>Продукти догляду</h3><ul class="modal-list">${spa.careProductsDescriptions.map(p => `<li><strong>${p.name}</strong>: <span>${p.description}</span></li>`).join('')}</ul>`;
        }

        
        const modalOverlay = document.createElement("div");
        modalOverlay.className = "spa-info-overlay hidden";
        modalOverlay.id = `spa-modal-${spa.id}`;
        modalOverlay.innerHTML = `
            <div class="spa-info-modal">
                <button class="spa-info-close">✖</button>
                <h2>${spa.name || "Назва процедури"}</h2>
                <p class="type-badge">${spa.type || "SPA"}</p>
                <div class="spa-info-body">${modalBodyHtml}</div>
            </div>
        `;
        document.body.appendChild(modalOverlay);

        card.innerHTML = `
            ${hiddenBadgeHtml}
            <div class="spa-image-container js-loaded">
                ${imagesHtml}
                ${navButtonsHtml}
            </div>

            <div class="spa-card-content cursor-pointer">
                <div class="spa-meta">
                    <span>${spa.price} ₴</span>
                    <span>${spa.durationInMinutes} хв</span>
                </div>
                
                <h3>${spa.name || "Не вказано"}</h3>
                <p class="type-badge">${spa.type || "Не вказано"}</p>
                <p>${spa.description || ""}</p>
                
                <button class="read-more-btn">Детальніше про процедуру</button>

                <h4>Зручності:</h4>
                <div class="facilities-list">
                    ${facilitiesHtml}
                </div>

                <div class="user-actions-container">
                    <button class="cta btn-select full-width"
                        data-service-id="${spa.id}"
                        data-service-type="${spa.name}"
                        data-service-price="${spa.price}"
                        data-service-capacity="${spa.guestCapacity || 1}"
                        data-class-type="SPA"
                        data-duration="${spa.durationInMinutes}">
                        Обрати
                    </button>

                    <div class="service-quantity-controls hidden"
                        data-service-id="${spa.id}">
                        <button class="btn-qty-minus" data-service-id="${spa.id}">-</button>
                        <span class="service-qty-value" data-service-id="${spa.id}">1</span>
                        <button class="btn-qty-plus"
                            data-service-id="${spa.id}"
                            data-service-type="${spa.name}"
                            data-service-price="${spa.price}"
                            data-service-capacity="${spa.guestCapacity || 1}"
                            data-class-type="SPA"
                            data-duration="${spa.durationInMinutes}">+</button>
                    </div>
                </div>
            </div>

            ${adminButtonsHtml}
        `;

        return card;
    };

    const fetchMoreSpas = async () => {
        if (isLoading || isLastPage) return;

        isLoading = true;
        setScrollState("loading");

        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 10000);

        try {
            const response = await fetch(`/api/spas?page=${currentPage}&size=${pageSize}`, {
                signal: controller.signal
            });

            clearTimeout(timeoutId);

            if (!response.ok) throw new Error("Помилка сервера");

            const data = await response.json();
            const spas = Array.isArray(data.content) ? data.content : (Array.isArray(data) ? data : []);

            if (spas.length === 0) {
                isLastPage = true;
                setScrollState("end");
                return;
            }

            spas.forEach(spa => {
                gridContainer.appendChild(createSpaCard(spa));
            });

            
            if (typeof initializeImageSliders === "function") initializeImageSliders();

            if (data.last === true || spas.length < pageSize) {
                isLastPage = true;
                setScrollState("end");
            } else {
                setScrollState("none");
                currentPage++;
            }

        } catch (error) {
            setScrollState("error");
        } finally {
            isLoading = false;
        }
    };

    const observer = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting) {
            if (!isLoading && !isLastPage) {
                fetchMoreSpas();
            }
        }
    }, {
        rootMargin: "100px",
        threshold: 0.1
    });

    observer.observe(statusContainer);

    if (retryBtn) {
        retryBtn.addEventListener("click", () => {
            fetchMoreSpas();
        });
    }
}



function initializeSpaInfoModals() {
    document.querySelectorAll('.spa-info-overlay').forEach(modal => {
        document.body.appendChild(modal);
    });

    document.body.addEventListener('click', (e) => {
        
        const cardContent = e.target.closest('.spa-card-content');
        const actionsContainer = e.target.closest('.user-actions-container');

        
        if (cardContent && !actionsContainer) {
            const card = cardContent.closest('.spa-card');
            const serviceId = card.getAttribute('data-service-id');
            const modal = document.getElementById('spa-modal-' + serviceId);

            if (modal) {
                modal.classList.remove('hidden');
                document.body.style.overflow = 'hidden'; 
            }
        }

        
        if (e.target.closest('.spa-info-close')) {
            const modal = e.target.closest('.spa-info-overlay');
            modal.classList.add('hidden');
            document.body.style.overflow = '';
        }

        
        if (e.target.classList.contains('spa-info-overlay')) {
            e.target.classList.add('hidden');
            document.body.style.overflow = '';
        }
    });
}