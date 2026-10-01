"use strict"
document.addEventListener("DOMContentLoaded", () => {
    initializeFacilityInputEvents();
    initializePostModalOpenerEvent();
    initializePostFormEvent();
    initializeEditModalOpenerEvent();
    initializePutFormEvent()
    initializeDeleteModalOpenerEvent();
    InitializeDeleteRoomEvent();

    initializeFormOverlayClick();
    initializeImageSliders()
    initializeLightbox()
    initializeInfiniteScroll()
});

function initializeFacilityInputEvents() {
    const facilityContainers = document.querySelectorAll(".form-facility-container");

    facilityContainers.forEach(container => {
        let draggedEl = null;
        let placeholder = document.createElement("div");
        placeholder.className = "facility-placeholder";

        const createFacilityInput = (value = "") => {
            const div = document.createElement("div");
            div.className = "facility-item";
            div.draggable = true;
            div.innerHTML = `<input type="text" name="facility" placeholder="Напр., 'Wi-Fi'" value="${value}">`;

            div.addEventListener("mouseenter", () => div.classList.add("hover"));
            div.addEventListener("mouseleave", () => div.classList.remove("hover"));

            div.addEventListener("dragstart", e => {
                draggedEl = div;
                div.classList.add("dragging");
                e.dataTransfer.effectAllowed = "move";
                setTimeout(() => div.style.display = "none", 0);
            });

            div.addEventListener("dragend", () => {
                div.style.display = "flex";
                div.classList.remove("dragging");
                placeholder.remove();
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

        function getDragAfterElement(currentContainer, y) {
            const draggableElements = [...currentContainer.querySelectorAll(".facility-item:not(.dragging)")];
            return draggableElements.reduce((closest, child) => {
                const box = child.getBoundingClientRect();
                const offset = y - box.top - box.height / 2;
                if (offset < 0 && offset > closest.offset) {
                    return { offset: offset, element: child };
                } else {
                    return closest;
                }
            }, { offset: Number.NEGATIVE_INFINITY }).element;
        }

        function manageInputs(currentContainer) {
            const inputs = [...currentContainer.querySelectorAll("input[name='facility']")];
            const empty = inputs.filter(inp => inp.value.trim() === "");

            if (empty.length === 0) {
                currentContainer.append(createFacilityInput());
            }

            if (empty.length > 1) {
                for (let i = 1; i < empty.length; i++) {
                    if (currentContainer.children.length > 1) empty[i].parentElement.remove();
                }
            }
        }

        container.clearAndSetFacilities = (facilitiesArray = []) => {
            container.innerHTML = "";

            facilitiesArray.forEach(fac => {
                container.append(createFacilityInput(fac));
            });

            container.append(createFacilityInput(""));
        };

        container.clearAndSetFacilities([]);
    });
}


function initializePostFormEvent() {
    const tokenHeader = document.querySelector("meta[name='_csrf_header']").content
    const token = document.querySelector("meta[name='_csrf']").content

    const postForm = document.getElementById("post-form")
    if (postForm) {
        postForm.addEventListener("submit", async (event) => {
            event.preventDefault()

            try {
                const facilities = [...postForm.querySelectorAll("input[name='facility']")]
                    .map(input => input.value.trim())
                    .filter(facility => facility.length > 0)

                const roomDto = {
                    price: parseFloat(postForm.querySelector("input[name='price']").value),
                    description: postForm.querySelector("textarea[name='description']").value,
                    type: postForm.querySelector("input[name='type']").value,
                    hiddenFromClient: postForm.querySelector("input[name='hiddenFromClient']").checked,
                    guestCapacity: postForm.querySelector("input[name='guestCapacity']").value,
                    facilities: facilities,
                    classType: "ROOM"
                }

                const postImages = postForm.querySelector("input[name='post-images']").files

                const formData = new FormData()

                formData.append("room", new Blob(
                    [JSON.stringify(roomDto)],
                    { type: 'application/json' }
                ))

                for (let postImage of postImages) {
                    formData.append("imageFiles", postImage)
                }

                const res = await fetch(postForm.action, {
                    method: "POST",
                    body: formData,
                    headers: {
                        [tokenHeader]: token
                    }
                })

                if (!res.ok) {
                    const errorData = await res.json(); 
                    alert(errorData.error);             
                    return;
                }

                window.location.reload()

            } catch (error) {
                alert("Помилка мережі. Перевірте підключення або спробуйте пізніше.")
            }
        })
    }
}

function initializePostModalOpenerEvent() {
    const postFormContainer = document.querySelector(".post-form-container");
    const postFormOpener = document.getElementById("post-form-opener");

    if (postFormOpener && postFormContainer) {
        postFormOpener.addEventListener("click", () => {
            postFormContainer.classList.remove("hidden");
        });
    }
}

function initializeEditModalOpenerEvent() {
    const putFormContainer = document.querySelector(".put-form-container");
    const putFacilitiesContainer = document.getElementById("put-facilities");
    const gridContainer = document.querySelector(".cards-grid");

    if (!gridContainer || !putFormContainer) return;

    gridContainer.addEventListener("click", (event) => {
        
        const opener = event.target.closest(".edit-modal-opener");
        if (!opener) return;

        const card = opener.closest(".room-card");

        const roomId = card.getAttribute("data-service-id");
        const priceText = card.querySelector(".room-meta span:nth-child(1)").textContent;
        const price = parseFloat(priceText);
        const capacityText = card.querySelector(".room-meta span:nth-child(2)").textContent;
        const capacity = parseInt(capacityText);
        const type = card.querySelector("h3").textContent;
        const description = card.querySelector("p").textContent;

        const facilityElements = card.querySelectorAll(".room-facility-tag");
        const facilities = Array.from(facilityElements).map(el => {
            let text = el.textContent.trim();
            if (text.startsWith('"') && text.endsWith('"')) {
                text = text.substring(1, text.length - 1);
            }
            return text;
        });

        document.getElementById("put-room-id").value = roomId;
        document.getElementById("put-price").value = price;
        document.getElementById("put-type").value = type;
        document.getElementById("put-capacity").value = capacity;
        document.getElementById("put-description").value = description;
        document.getElementById("put-hidden").checked = card.getAttribute("data-hidden") === "true";

        if (putFacilitiesContainer && putFacilitiesContainer.clearAndSetFacilities) {
            putFacilitiesContainer.clearAndSetFacilities(facilities);
        }

        putFormContainer.classList.remove("hidden");
    });
}

function initializePutFormEvent() {
    const tokenHeader = document.querySelector("meta[name='_csrf_header']").content
    const token = document.querySelector("meta[name='_csrf']").content

    const putForm = document.getElementById("put-form")
    if (putForm) {
        putForm.addEventListener("submit", async (event) => {
            event.preventDefault()

            try {
                const facilities = [...putForm.querySelectorAll("input[name='facility']")]
                    .map(input => input.value.trim())
                    .filter(facility => facility.length > 0)

                const roomDto = {
                    id: putForm.querySelector("#put-room-id").value,
                    price: parseFloat(putForm.querySelector("#put-price").value),
                    description: putForm.querySelector("#put-description").value,
                    type: putForm.querySelector("#put-type").value,
                    hiddenFromClient: putForm.querySelector("input[name='hiddenFromClient']").checked,
                    guestCapacity: putForm.querySelector("#put-capacity").value,
                    facilities: facilities,
                    classType: "ROOM"
                }

                const putImages = putForm.querySelector("input[name='post-images']").files

                const formData = new FormData()

                formData.append("room", new Blob(
                    [JSON.stringify(roomDto)],
                    { type: 'application/json' }
                ))

                for (let putImage of putImages) {
                    formData.append("imageFiles", putImage)
                }

                const res = await fetch(putForm.action, {
                    method: "PUT",
                    body: formData,
                    headers: {
                        [tokenHeader]: token
                    }
                })

                if (!res.ok) {
                    const errorData = await res.json(); 
                    alert(errorData.error);             
                    return;
                }

                window.location.reload()

            } catch (error) {
                alert("Помилка мережі. Перевірте підключення або спробуйте пізніше.")
            }
        })
    }
}

function initializeFormOverlayClick() {
    const modalContainers = document.querySelectorAll('.post-form-container, .put-form-container, .delete-modal-window');

    modalContainers.forEach(container => {
        container.addEventListener("click", (event) => {
            if (event.target === container) {
                container.classList.add("hidden");
            }
        });

        const closerBtn = container.querySelector('.form-closer, .cancel-delete-btn');
        if (closerBtn) {
            closerBtn.addEventListener("click", (e) => {
                e.preventDefault();
                container.classList.add("hidden");
            });
        }
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

        const card = opener.closest(".room-card");
        const roomId = card.getAttribute("data-service-id");

        confirmDeleteBtn.setAttribute("data-service-id", roomId);
        deleteModal.classList.remove("hidden");
    });
}

function InitializeDeleteRoomEvent() {
    const confirmDeleteBtn = document.querySelector(".confirm-delete-btn");
    const deleteModal = document.querySelector(".delete-modal-window");

    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener("click", async (event) => {
            const roomId = event.target.getAttribute("data-service-id");
            if (!roomId) return;

            const tokenHeader = document.querySelector('meta[name="_csrf_header"]').content;
            const token = document.querySelector('meta[name="_csrf"]').content;

            try {
                const res = await fetch(`/api/rooms/${roomId}`, {
                    method: 'DELETE',
                    headers: {
                        [tokenHeader]: token
                    }
                });

                if (res.ok) {
                    const cardToRemove = document.querySelector(`.room-card[data-service-id="${roomId}"]`);
                    if (cardToRemove) cardToRemove.remove();

                    deleteModal.classList.add("hidden");
                } else {
                    alert("Помилка при видаленні, у випадку якщо хоч одне бронювання містить дану послугу, видалення неможливе!");
                }
            } catch (error) {
            }
        });
    }
}


function initializeImageSliders() {
    const imageContainers = document.querySelectorAll(".room-image-container");

    imageContainers.forEach(container => {
        const prevBtn = container.querySelector(".slider-btn-prev");
        const nextBtn = container.querySelector(".slider-btn-next");

        const getFreshImages = () => Array.from(container.querySelectorAll(".room-slide-image"));

        let images = getFreshImages();
        if (images.length === 0) return;

        container.classList.add("js-loaded");

        let currentIndex = 0;

        const updateSlider = () => {
            images = getFreshImages();

            if (images.length === 0) {
                if (prevBtn) prevBtn.style.display = "none";
                if (nextBtn) nextBtn.style.display = "none";
                return;
            }

            if (currentIndex >= images.length) {
                currentIndex = images.length - 1;
            }

            images.forEach((img, index) => {
                img.classList.toggle("active", index === currentIndex);
            });

            if (images.length <= 1) {
                if (prevBtn) prevBtn.style.display = "none";
                if (nextBtn) nextBtn.style.display = "none";
            } else {
                if (prevBtn) prevBtn.style.display = "block";
                if (nextBtn) nextBtn.style.display = "block";
            }
        };

        updateSlider();

        if (nextBtn) {
            nextBtn.addEventListener("click", (e) => {
                e.preventDefault();
                e.stopPropagation();
                images = getFreshImages();
                if (images.length <= 1) return;

                currentIndex = (currentIndex + 1) % images.length;
                updateSlider();
            });
        }

        if (prevBtn) {
            prevBtn.addEventListener("click", (e) => {
                e.preventDefault();
                e.stopPropagation();
                images = getFreshImages();
                if (images.length <= 1) return;

                currentIndex = (currentIndex - 1 + images.length) % images.length;
                updateSlider();
            });
        }

        const observer = new MutationObserver((mutationsList) => {
            for (const mutation of mutationsList) {
                if (mutation.removedNodes.length > 0) {
                    updateSlider();
                }
            }
        });

        observer.observe(container, { childList: true });
    });
}

function initializeLightbox() {
    const lightbox = document.getElementById("lightbox");
    const lightboxImg = document.getElementById("lightbox-img");
    const closeBtn = document.getElementById("lightbox-close");
    const prevBtn = document.getElementById("lightbox-prev");
    const nextBtn = document.getElementById("lightbox-next");
    const dotsBtn = document.getElementById("lightbox-dots");
    const dropdown = document.getElementById("lightbox-dropdown");
    const deleteBtn = document.getElementById("lightbox-delete");

    if (!lightbox) return;

    let currentImages = [];
    let currentIndex = 0;

    const updateLightboxView = () => {
        if (currentImages.length === 0) {
            lightbox.classList.add("hidden");
            return;
        }
        lightboxImg.src = currentImages[currentIndex].src;
        prevBtn.style.display = currentImages.length > 1 ? "flex" : "none";
        nextBtn.style.display = currentImages.length > 1 ? "flex" : "none";
        dropdown.classList.add("hidden");
    };

    const gridContainer = document.querySelector(".cards-grid");
    if (gridContainer) {
        gridContainer.addEventListener("click", (e) => {
            
            const container = e.target.closest(".room-image-container");
            if (!container) return; 

            if (e.target.closest(".slider-btn")) return;

            const imageElements = Array.from(container.querySelectorAll(".room-slide-image"));
            if (imageElements.length === 0) return;

            currentImages = imageElements.map(img => ({
                id: img.getAttribute("data-id"),
                src: img.getAttribute("src"),
                element: img
            }));

            const clickedImg = e.target.closest(".room-slide-image");
            if (clickedImg) {
                currentIndex = currentImages.findIndex(img => img.id === clickedImg.getAttribute("data-id"));
            } else {
                const activeImg = container.querySelector(".room-slide-image.active");
                currentIndex = currentImages.findIndex(img => img.id === activeImg.getAttribute("data-id"));
            }

            if (currentIndex === -1) currentIndex = 0;

            updateLightboxView();
            lightbox.classList.remove("hidden");
        });
    }

    closeBtn.addEventListener("click", () => lightbox.classList.add("hidden"));

    prevBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        currentIndex = (currentIndex - 1 + currentImages.length) % currentImages.length;
        updateLightboxView();
    });

    nextBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        currentIndex = (currentIndex + 1) % currentImages.length;
        updateLightboxView();
    });

    dotsBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        dropdown.classList.toggle("hidden");
    });

    document.addEventListener("click", (e) => {
        if (!dotsBtn.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.classList.add("hidden");
        }
    });

    lightbox.addEventListener("click", (e) => {
        if (e.target === lightbox || e.target.classList.contains("lightbox-content")) {
            lightbox.classList.add("hidden");
        }
    });

    if (deleteBtn) {
        deleteBtn.addEventListener("click", async () => {
            const imageObj = currentImages[currentIndex];

            const container = imageObj.element ? imageObj.element.closest(".room-image-container") : null;

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
                    updateLightboxView();
                } else {
                    lightbox.classList.add("hidden");
                    
                    if (container) {
                        container.innerHTML = `<img class="room-slide-image active" src="/common/images/placeholder.png" alt="Зображення відсутнє">`;
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
        loader.classList.add("hidden");
        errorBlock.classList.add("hidden");
        endBlock.classList.add("hidden");

        if (state === "loading") loader.classList.remove("hidden");
        if (state === "error") errorBlock.classList.remove("hidden");
        if (state === "end") endBlock.classList.remove("hidden");
    };

    const createRoomCard = (room) => {
        const isAdmin = document.querySelector(".edit-modal-opener") !== null;

        const card = document.createElement("div");
        card.className = "card room-card " + (room.hiddenFromClient == true ? "room-card-client-hidden" : "");;
        card.setAttribute("data-service-id", room.id);
        card.setAttribute("data-hidden", room.hiddenFromClient === true);

        const hiddenBadgeHtml = room.hiddenFromClient ? `
            <div class="admin-hidden-badge">Приховано</div>
        ` : '';

        let imagesHtml = '';
        let navButtonsHtml = '';

        if (room.imageRecords && room.imageRecords.length > 0) {
            room.imageRecords.forEach((img, index) => {
                const activeClass = index === 0 ? "active" : "";
                
                const imageUrl = img.url ? img.url : "/common/images/placeholder.png";
                
                imagesHtml += `<img class="room-slide-image ${activeClass}" data-id="${img.id}" src="${imageUrl}" alt="${img.position || index + 1}">`;
            });

            if (room.imageRecords.length > 1) {
                navButtonsHtml = `
                    <button class="slider-btn slider-btn-prev" aria-label="Previous image">&lt;</button>
                    <button class="slider-btn slider-btn-next" aria-label="Next image">&gt;</button>
                `;
            }
        } else {
            imagesHtml = `<img class="room-slide-image active" src="/common/images/placeholder.png" alt="Зображення відсутнє">`;
        }

        const facilitiesHtml = (room.facilities || []).map(fac =>
            `<span class="room-facility-tag">${fac}</span>`
        ).join("");

        const adminButtonsHtml = isAdmin ? `
            <div class="modal-buttons-container">
                <button class="edit-modal-opener">Редагувати</button>
                <button class="delete-modal-opener">Видалити</button>
            </div>
        ` : "";

        card.innerHTML = `
            ${hiddenBadgeHtml}
            <div class="room-image-container js-loaded">
                ${imagesHtml}
                ${navButtonsHtml}
            </div>

            <div class="room-card-content">
                <div class="room-meta">
                    <span>${room.price} / доба</span>
                    <span>${room.guestCapacity} гостей</span>
                </div>

                <h3>${room.type || "Не вказано"}</h3>
                <p>${room.description || ""}</p>

                <h4>Зручності:</h4>
                <div class="facilities-list">
                    ${facilitiesHtml}
                </div>

                <div class="user-actions-container">
                    <button class="cta btn-select full-width"
                        data-service-id="${room.id}"
                        data-service-type="${room.type}"
                        data-service-price="${room.price}"
                        data-service-capacity="${room.guestCapacity}"
                        data-class-type="ROOM">
                        Обрати
                    </button>

                    <div class="service-quantity-controls hidden"
                        data-service-id="${room.id}">
                        
                        <button class="btn-qty-minus"
                            data-service-id="${room.id}">-</button>

                        <span class="service-qty-value"
                            data-service-id="${room.id}">1</span>

                        <button class="btn-qty-plus"
                            data-service-id="${room.id}"
                            data-service-type="${room.type}"
                            data-service-price="${room.price}"
                            data-service-capacity="${room.guestCapacity}">+</button>
                    </div>
                </div>
            </div>

            ${adminButtonsHtml}
        `;

        return card;
    };

    const fetchMoreRooms = async () => {
        if (isLoading || isLastPage) return;

        isLoading = true;
        setScrollState("loading");

        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 10000);

        try {
            const response = await fetch(`/api/rooms?page=${currentPage}&size=${pageSize}`, {
                signal: controller.signal
            });

            clearTimeout(timeoutId);

            if (!response.ok) throw new Error("Помилка сервера");

            const data = await response.json();

            const rooms = Array.isArray(data.content) ? data.content : (Array.isArray(data) ? data : []);

            
            if (rooms.length === 0) {
                isLastPage = true;
                setScrollState("end");
                return; 
            }

            rooms.forEach(room => {
                gridContainer.appendChild(createRoomCard(room));
            });

            if (typeof initializeImageSliders === "function") initializeImageSliders();

            
            
            if (data.last === true || rooms.length < pageSize) {
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
                fetchMoreRooms();
            }
        }
    }, {
        rootMargin: "100px",
        threshold: 0.1
    });

    observer.observe(statusContainer);

    if (retryBtn) {
        retryBtn.addEventListener("click", () => {
            fetchMoreRooms();
        });
    }
}









