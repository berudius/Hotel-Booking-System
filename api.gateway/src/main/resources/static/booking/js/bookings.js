"use strict";

document.addEventListener("DOMContentLoaded", () => {
    initializeTabs();
    initializeBookingAccordions();
    initializeInfiniteScrollForBookings();
    
    initializeBookingActions();
});


function initializeTabs() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabPanes = document.querySelectorAll('.tab-pane');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            tabBtns.forEach(b => b.classList.remove('active'));
            tabPanes.forEach(p => p.classList.remove('active'));

            btn.classList.add('active');
            const targetId = btn.getAttribute('data-target');
            const targetPane = document.getElementById(targetId);

            if (targetPane) {
                targetPane.classList.add('active');
            }
        });
    });
}


function initializeBookingAccordions() {
    document.body.addEventListener('click', (e) => {
        const header = e.target.closest('.booking-card-header');
        if (!header) return;

        const card = header.closest('.booking-card');
        const body = card.querySelector('.booking-card-body');
        const expandBtn = header.querySelector('.btn-expand-details');

        body.classList.toggle('hidden');

        if (body.classList.contains('hidden')) {
            expandBtn.textContent = 'Деталі ▼';
            header.style.backgroundColor = '';
        } else {
            expandBtn.textContent = 'Приховати ▲';
            header.style.backgroundColor = '#f8fafc';
        }
    });
}


function initializeInfiniteScrollForBookings() {
    
    const archiveToggle = document.getElementById('toggle-archive');

    
    const scrollStates = {
        'all-bookings': { page: 1, size: 12, isLoading: false, isLast: false, url: '/api/admin/bookings' },
        'my-bookings': { page: 1, size: 12, isLoading: false, isLast: false, url: '/api/bookings' }
    };

    
    const fetchFunctions = {};

    
    const reloadAll = () => {
        Object.keys(scrollStates).forEach(key => {
            const state = scrollStates[key];
            state.page = 0;
            state.isLast = false;
            state.isLoading = false;

            const pane = document.getElementById(key);
            if (pane) {
                const list = pane.querySelector('.booking-list');
                if (list) list.innerHTML = ''; 

                
                const endBlock = pane.querySelector('.scroll-end');
                if (endBlock) endBlock.classList.add('hidden');
            }
        });

        
        const activePane = document.querySelector('.tab-pane.active');
        if (activePane && fetchFunctions[activePane.id]) {
            fetchFunctions[activePane.id]();
        }
    };

    if (archiveToggle) {
        archiveToggle.addEventListener('change', reloadAll);
    }

    
    const formatDate = (dateObj) => {
        if (!dateObj) return '';
        let d;
        if (Array.isArray(dateObj)) {
            d = new Date(dateObj[0], dateObj[1] - 1, dateObj[2], dateObj[3] || 0, dateObj[4] || 0);
        } else {
            d = new Date(dateObj);
        }
        return d.toLocaleString('uk-UA', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    };

    
    const createBookingCard = (booking) => {
        const card = document.createElement('div');
        card.className = 'booking-card';
        card.dataset.id = booking.id;

        const statusLower = booking.status ? booking.status.toLowerCase() : 'pending';
        const isTerminalStatus = ['CANCELLED_BY_CLIENT', 'REJECTED', 'COMPLETED', 'EXPIRED'].includes(booking.status);

        
        const isAdmin = document.querySelector('.tab-btn[data-target="all-bookings"]') !== null;

        let unitsHtml = '';
        if (booking.bookingUnits && booking.bookingUnits.length > 0) {
            booking.bookingUnits.forEach(unit => {
                const service = unit.serviceUnit || {};
                const imageUrl = (service.imageRecords && service.imageRecords.length > 0) ? service.imageRecords[0].url : null;
                const imageHtml = imageUrl
                    ? `<img src="${imageUrl}" alt="Фото послуги">`
                    : `<div class="no-image">Немає фото</div>`;

                const unitStatusLower = unit.status ? unit.status.toLowerCase() : 'pending';
                const isUnitTerminal = ['CANCELLED_BY_CLIENT', 'REJECTED', 'COMPLETED', 'EXPIRED'].includes(unit.status);

                let editDatesBtnHtml = '';
                if (isAdmin && unit.status === 'CONFIRMED') {
                    editDatesBtnHtml = `
                        <button class="btn-edit-dates" 
                                data-unit-id="${unit.id}" 
                                data-start="${unit.start}" 
                                data-end="${unit.end}">
                                Змінити час
                        </button>
                    `;
                }

                
                let unitStatusHtml = '';
                if (isAdmin) {
                    unitStatusHtml = `
                        <select class="admin-unit-status-select status-badge unit-status status-${unitStatusLower}" data-unit-id="${unit.id}">
                            <option value="PENDING" ${unit.status === 'PENDING' ? 'selected' : ''}>PENDING</option>
                            <option value="CONFIRMED" ${unit.status === 'CONFIRMED' ? 'selected' : ''}>CONFIRMED</option>
                            <option value="COMPLETED" ${unit.status === 'COMPLETED' ? 'selected' : ''}>COMPLETED</option>
                            <option value="REJECTED" ${unit.status === 'REJECTED' ? 'selected' : ''}>REJECTED</option>
                            <option value="CANCELLED_BY_CLIENT" ${unit.status === 'CANCELLED_BY_CLIENT' ? 'selected' : 'disabled hidden'}>CANCELLED</option>
                            <option value="EXPIRED" ${unit.status === 'EXPIRED' ? 'selected' : 'disabled hidden'}>EXPIRED</option>
                        </select>
                    `;
                } else {
                    unitStatusHtml = `<span class="status-badge unit-status status-${unitStatusLower}">${unit.status}</span>`;
                }

                let workerHtml = '';
                
                if (unit.preferedGender !== undefined && unit.assignedWorkers && unit.assignedWorkers.length > 0) {
                    const workersStr = unit.assignedWorkers.map(w => `${w.firstName} ${w.lastName} (${w.gender})`).join(', ');
                    
                }

                
                let cancelBtnHtml = '';
                if (!isUnitTerminal) {
                    cancelBtnHtml = `
                        <button class="cta-secondary btn-cancel-unit" style="padding: 2px 8px; font-size: 0.8rem;" data-unit-id="${unit.id}">
                            ✖ Скасувати послугу
                        </button>
                    `;
                }

                
                let durationHtml = '';
                if (service.durationInMinutes !== undefined && service.durationInMinutes !== null) {
                    durationHtml = `<span class="unit-tag">Тривалість: ${service.durationInMinutes} хв.</span>`;
                }

                unitsHtml += `
                    <div class="unit-card" data-id="${unit.id}">
                        <div class="unit-image-wrapper">${imageHtml}</div>
                        <div class="unit-info">
                            <h5>${service.h5BlockValue || service.name || 'Послуга'}</h5>
                            <div class="unit-meta-tags">
                                ${unitStatusHtml}
                                <span class="unit-tag">${unit.amount || 0} ₴</span>
                                <span class="unit-tag">Гостей: ${unit.clientCount}</span>
                                ${durationHtml} 
                                ${workerHtml}
                                ${cancelBtnHtml}
                            </div>
                            <div class="unit-dates">
                                <div><strong>Заїзд/Початок:</strong> <span>${formatDate(unit.start)}</span></div>
                                <div><strong>Виїзд/Кінець:</strong> <span>${formatDate(unit.end)}</span></div>
                                ${editDatesBtnHtml}
                            </div>
                        </div>
                    </div>
                `;
            });
        }

        let commentHtml = '';
        if (booking.clientComment) {
            commentHtml = `
                <div class="booking-comment-box">
                    <strong>Коментар:</strong> <span>${booking.clientComment}</span>
                </div>
            `;
        }

        
        const totalPrice = booking.bookingUnits ? booking.bookingUnits.reduce((sum, u) => sum + (u.amount || 0), 0) : 0;

        
        let actionsPanelHtml = '';
        if (!isTerminalStatus) {
            let adminControls = '';
            if (isAdmin) {
                adminControls = `
                    <div class="admin-status-control" style="display: flex; gap: 10px; align-items: center;">
                        <select class="admin-general-status-select" data-booking-id="${booking.id}">
                            <option value="PENDING" ${booking.status === 'PENDING' ? 'selected' : ''}>PENDING</option>
                            <option value="CONFIRMED" ${booking.status === 'CONFIRMED' ? 'selected' : ''}>CONFIRMED</option>
                            <option value="COMPLETED" ${booking.status === 'COMPLETED' ? 'selected' : ''}>COMPLETED</option>
                            <option value="REJECTED" ${booking.status === 'REJECTED' ? 'selected' : ''}>REJECTED</option>
                            <option value="CANCELLED_BY_CLIENT" ${booking.status === 'CANCELLED_BY_CLIENT' ? 'selected' : 'disabled hidden'}>CANCELLED_BY_CLIENT</option>
                            <option value="EXPIRED" ${booking.status === 'EXPIRED' ? 'selected' : 'disabled hidden'}>EXPIRED</option>
                        </select>
                        <button class="cta-secondary btn-update-general-status">Оновити статус</button>
                    </div>
                `;
            } else {
                adminControls = `
                    <button class="cta-secondary btn-cancel-general" data-booking-id="${booking.id}">
                        Скасувати все замовлення
                    </button>
                `;
            }

            actionsPanelHtml = `
                <div class="booking-actions-panel" style="margin-bottom: 15px; display: flex; gap: 10px; flex-wrap: wrap;">
                    ${adminControls}
                    <button class="btn-add-from-cart" data-booking-id="${booking.id}">
                        + Додати послуги з кошика
                    </button>
                </div>
            `;
        }

        card.innerHTML = `
            <div class="booking-card-header">
                <div class="booking-info-primary">
                    <span class="booking-id">#<span>${booking.id}</span></span>
                    <span class="booking-date">${formatDate(booking.orderDateTime)}</span>
                    <span class="booking-total-price" style="margin-left: 15px; font-weight: bold;">
                        Загальна сума: <span>${totalPrice}</span> ₴
                    </span>
                </div>
                <div class="booking-info-secondary">
                    <span class="booking-phone">${booking.phoneNumber || ''}</span>
                    <span class="status-badge status-${statusLower}">${booking.status}</span>
                </div>
                <button class="btn-expand-details">Деталі ▼</button>
            </div>
            <div class="booking-card-body hidden">
                ${actionsPanelHtml}
                ${commentHtml}
                <h4 class="units-title">Послуги у цьому бронюванні:</h4>
                <div class="units-grid">
                    ${unitsHtml}
                </div>
            </div>
        `;
        return card;
    };

    
    const tabPanes = document.querySelectorAll('.tab-pane');

    tabPanes.forEach(pane => {
        const tabId = pane.id;
        const state = scrollStates[tabId];
        if (!state) return;

        const bookingList = pane.querySelector('.booking-list');
        const statusContainer = pane.querySelector('.infinite-scroll-status');

        if (!bookingList || !statusContainer) return;
        statusContainer.classList.remove('hidden-initially');

        const loader = statusContainer.querySelector('.scroll-loader');
        const errorBlock = statusContainer.querySelector('.scroll-error');
        const endBlock = statusContainer.querySelector('.scroll-end');
        const retryBtn = statusContainer.querySelector('.retry-btn');

        const setDisplay = (type) => {
            loader.classList.add("hidden");
            errorBlock.classList.add("hidden");
            endBlock.classList.add("hidden");
            if (type === "loading") loader.classList.remove("hidden");
            if (type === "error") errorBlock.classList.remove("hidden");
            if (type === "end") endBlock.classList.remove("hidden");
        };

        const fetchBookings = async () => {
            if (state.isLoading || state.isLast) return;

            state.isLoading = true;
            setDisplay("loading");

            
            const showAll = archiveToggle ? archiveToggle.checked : false;

            const tokenHeaderEl = document.querySelector("meta[name='_csrf_header']");
            const tokenEl = document.querySelector("meta[name='_csrf']");
            const headers = {};
            if (tokenHeaderEl && tokenEl) headers[tokenHeaderEl.content] = tokenEl.content;

            try {
                const url = `${state.url}?page=${state.page}&size=${state.size}&showAll=${showAll}`; // я хз що там на backend за магія "!showAll" працює правильно — "showAll" не працює правильно
                const response = await fetch(url, { headers: headers });

                if (!response.ok) throw new Error("Помилка сервера");

                const data = await response.json();
                const bookings = Array.isArray(data.content) ? data.content : (Array.isArray(data) ? data : []);

                const thymeleafEmptyMsg = pane.querySelector('.general-booking-container > .empty-bookings');
                if (thymeleafEmptyMsg) {
                    if (state.page === 0 || bookings.length > 0) {
                        thymeleafEmptyMsg.remove();
                    }
                }

                if (bookings.length === 0 && state.page === 0) {
                    bookingList.innerHTML = `<div class="empty-bookings"><div class="empty-icon">📂</div><p>У вас немає активних бронювань</p></div>`;
                    state.isLast = true;
                    setDisplay("none");
                    return;
                }

                bookings.forEach(booking => {
                    bookingList.appendChild(createBookingCard(booking));
                });

                if (data.last === true || bookings.length < state.size) {
                    state.isLast = true;
                    setDisplay("end");
                } else {
                    setDisplay("none");
                    state.page++;
                }
            } catch (error) {
                setDisplay("error");
            } finally {
                state.isLoading = false;
            }
        };

        
        fetchFunctions[tabId] = fetchBookings;

        
        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting && pane.classList.contains('active')) {
                fetchBookings();
            }
        }, { rootMargin: "100px", threshold: 0.1 });

        observer.observe(statusContainer);

        if (retryBtn) {
            retryBtn.addEventListener('click', () => fetchBookings());
        }

        
        const tabBtn = document.querySelector(`.tab-btn[data-target="${tabId}"]`);
        if (tabBtn) {
            tabBtn.addEventListener('click', () => {
                if (bookingList.children.length === 0 && !state.isLoading && !state.isLast) {
                    fetchBookings();
                }
            });
        }
    });
}



function initializeBookingActions() {
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const headers = {
        'Content-Type': 'application/json',
        [csrfHeader]: csrfToken
    };

    
    document.body.addEventListener('click', async (e) => {

        
        if (e.target.classList.contains('btn-cancel-general')) {
            const bookingId = e.target.getAttribute('data-booking-id');
            if (!confirm('Ви впевнені, що хочете скасувати все бронювання?')) return;

            try {
                const res = await fetch(`/api/bookings/${bookingId}/status?status=CANCELLED_BY_CLIENT`, {
                    method: 'PATCH', headers
                });
                if (res.ok) location.reload();
                else alert('Не вдалося скасувати бронювання.');
            } catch (err) { console.error(err); alert('Помилка мережі.'); }
        }

        
        if (e.target.classList.contains('btn-cancel-unit')) {
            const btn = e.target;
            const unitId = btn.getAttribute('data-unit-id');
            if (!confirm('Скасувати цю послугу?')) return;

            const originalText = btn.innerHTML;
            btn.innerHTML = 'Скасовуємо...';
            btn.disabled = true;

            try {
                const res = await fetch(`/api/bookings/units/${unitId}/status?status=CANCELLED_BY_CLIENT`, {
                    method: 'PATCH', headers
                });

                if (res.ok) {
                    const unitCard = btn.closest('.unit-card');
                    const unitBadge = unitCard.querySelector('.unit-status');

                    
                    const updatedUnit = await res.json();
                    unitBadge.textContent = updatedUnit.status;
                    unitBadge.className = `status-badge unit-status status-${updatedUnit.status.toLowerCase()}`;
                    btn.remove();

                    const cardBody = unitCard.closest('.booking-card-body');
                    const allUnitBadges = cardBody.querySelectorAll('.unit-status');

                    let allCancelled = true;
                    let hasRejected = false;

                    allUnitBadges.forEach(badge => {
                        const status = badge.textContent.trim().toUpperCase();
                        if (status !== 'CANCELLED_BY_CLIENT' && status !== 'REJECTED' && status !== 'EXPIRED') {
                            allCancelled = false;
                        }
                        if (status === 'REJECTED') hasRejected = true;
                    });

                    if (allCancelled) {
                        const mainCard = unitCard.closest('.booking-card');
                        const mainBadge = mainCard.querySelector('.booking-info-secondary .status-badge');

                        const finalParentStatus = hasRejected ? 'REJECTED' : 'CANCELLED_BY_CLIENT';
                        mainBadge.textContent = finalParentStatus;
                        mainBadge.className = `status-badge status-${finalParentStatus.toLowerCase()}`;

                        const actionsPanel = cardBody.querySelector('.booking-actions-panel');
                        if (actionsPanel) {
                            actionsPanel.style.opacity = '0';
                            setTimeout(() => actionsPanel.style.display = 'none', 300);
                        }
                    }
                } else {
                    alert('Не вдалося скасувати послугу.');
                    btn.innerHTML = originalText;
                    btn.disabled = false;
                }
            } catch (err) {
                console.error(err);
                alert('Помилка мережі.');
                btn.innerHTML = originalText;
                btn.disabled = false;
            }
        }

        
        if (e.target.classList.contains('btn-update-general-status')) {
            const container = e.target.closest('.admin-status-control');
            const select = container.querySelector('.admin-general-status-select');
            const bookingId = select.getAttribute('data-booking-id');
            const newStatus = select.value;

            
            sendBookingStatusPatch(bookingId, newStatus, null, select, headers);
        }

        
        if (e.target.classList.contains('btn-add-from-cart')) {
            const bookingId = e.target.getAttribute('data-booking-id');

            let cart = JSON.parse(localStorage.getItem('edemium_cart')) || [];
            if (cart.length === 0) {
                alert("Ваш кошик порожній. Відкрийте кошик та додайте послуги.");
                const noteContainer = document.querySelector(".booking-note-container");
                if (noteContainer) noteContainer.click();
                return;
            }

            const payload = cart.map(item => {
                const mappedClassType = (item.classType && item.classType.includes('SPA')) ? 'SPA_BOOKING' : 'ROOM_BOOKING';
                const unit = {
                    classType: mappedClassType,
                    serviceUnitId: item.serviceUnitId,
                    start: item.start,
                    end: item.end,
                    clientCount: item.clientCount
                };
                if (mappedClassType === 'SPA_BOOKING') {
                    unit.preferedGender = (item.preferedGender && item.preferedGender !== 'ANY') ? item.preferedGender : null;
                }
                return unit;
            });

            const isInvalid = payload.some(u => !u.start || !u.end);
            if (isInvalid) {
                alert("Заповніть дати у кошику перед додаванням!");
                const noteContainer = document.querySelector(".booking-note-container");
                if (noteContainer) noteContainer.click();
                return;
            }

            e.target.disabled = true;
            e.target.textContent = 'Додаємо...';

            try {
                const res = await fetch(`/api/bookings/${bookingId}/units`, {
                    method: 'POST',
                    headers: headers,
                    body: JSON.stringify(payload)
                });

                if (res.ok) {
                    localStorage.removeItem('edemium_cart');
                    alert("Послуги успішно додані до бронювання!");
                    location.reload();
                } else {
                    const err = await res.text();
                    alert(`Не вдалося додати послуги: ${err}`);
                    e.target.disabled = false;
                    e.target.textContent = '+ Додати послуги з кошика';
                }
            } catch (err) {
                console.error(err);
                alert('Помилка мережі.');
                e.target.disabled = false;
            }
        }

        
        if (e.target.classList.contains('btn-edit-dates')) {
            const btn = e.target;
            const unitId = btn.getAttribute('data-unit-id');
            
            const startVal = btn.getAttribute('data-start').substring(0, 16);
            const endVal = btn.getAttribute('data-end').substring(0, 16);

            document.getElementById('edit-dates-unit-id').value = unitId;
            document.getElementById('edit-start-date').value = startVal;
            document.getElementById('edit-end-date').value = endVal;

            document.getElementById('edit-dates-modal').classList.remove('hidden');
        }

        
        if (e.target.classList.contains('modal-close-btn') || e.target.id === 'edit-dates-modal') {
            const editModal = document.getElementById('edit-dates-modal');
            if (editModal) editModal.classList.add('hidden');
        }
    });

    
    document.body.addEventListener('change', async (e) => {
        if (e.target.classList.contains('admin-unit-status-select')) {
            const select = e.target;
            const unitId = select.getAttribute('data-unit-id');
            const newStatus = select.value;
            const currentStatus = Array.from(select.options).find(opt => opt.defaultSelected)?.value || 'PENDING';

            if (!confirm(`Ви дійсно хочете змінити статус цієї послуги на ${newStatus}?`)) {
                select.value = currentStatus; 
                return;
            }

            try {
                select.disabled = true;
                const res = await fetch(`/api/bookings/units/${unitId}/status?status=${newStatus}`, {
                    method: 'PATCH',
                    headers: headers
                });

                if (res.ok) {
                    location.reload();
                } else {
                    const err = await res.json();
                    alert(`Не вдалося оновити статус: ${err.error || 'Помилка'}`);
                    select.value = currentStatus;
                    select.disabled = false;
                }
            } catch (err) {
                alert('Помилка мережі.');
                select.value = currentStatus;
                select.disabled = false;
            }
        }
    });

    
    const editDatesForm = document.getElementById('edit-dates-form');
    if (editDatesForm) {
        editDatesForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const unitId = document.getElementById('edit-dates-unit-id').value;
            
            const newStart = document.getElementById('edit-start-date').value + ":00";
            const newEnd = document.getElementById('edit-end-date').value + ":00";
            const submitBtn = document.getElementById('btn-submit-dates');

            submitBtn.disabled = true;
            submitBtn.textContent = "Перевірка ресурсів...";

            try {
                const res = await fetch(`/api/bookings/units/${unitId}/dates?start=${newStart}&end=${newEnd}`, {
                    method: 'PATCH',
                    headers: headers
                });

                if (res.ok) {
                    alert("Час успішно змінено!");
                    location.reload();
                } else {
                    const err = await res.json();
                    alert(`Не вдалося змінити час: ${err.error || 'Конфлікт розкладу'}`);
                    submitBtn.disabled = false;
                    submitBtn.textContent = "Зберегти зміни";
                }
            } catch (err) {
                alert('Помилка мережі.');
                submitBtn.disabled = false;
                submitBtn.textContent = "Зберегти зміни";
            }
        });
    }
}


async function sendBookingStatusPatch(bookingId, newStatus, bodyData, selectElement, headers) {
    try {
        const reqOptions = { method: 'PATCH', headers: headers };
        if (bodyData) reqOptions.body = JSON.stringify(bodyData);

        const res = await fetch(`/api/bookings/${bookingId}/status?status=${newStatus}`, reqOptions);
        if (res.ok) location.reload();
        else {
            const err = await res.json();
            alert(`Помилка: ${err.error || 'Непередбачена помилка'}`);
            if (selectElement) selectElement.value = selectElement.querySelector('option[selected]')?.value || 'PENDING';
        }
    } catch (err) {
        console.error(err); alert('Помилка мережі.');
    }
}


async function sendBookingStatusPatch(bookingId, newStatus, bodyData, selectElement, headers) {
    try {
        const reqOptions = { method: 'PATCH', headers: headers };
        if (bodyData) reqOptions.body = JSON.stringify(bodyData);

        const res = await fetch(`/api/bookings/${bookingId}/status?status=${newStatus}`, reqOptions);
        if (res.ok) location.reload();
        else {
            const err = await res.text();
            alert(`Помилка: ${err}`);
            if (selectElement) selectElement.value = selectElement.querySelector('option[selected]')?.value || 'PENDING';
        }
    } catch (err) {
        console.error(err); alert('Помилка мережі.');
    }
}

async function openManualAllocationModal(bookingId, selectElement, headers) {
    const modal = document.getElementById('manual-allocation-modal');
    const container = document.getElementById('manual-allocation-units-container');
    document.getElementById('manual-booking-id-display').textContent = bookingId;

    container.innerHTML = '<p>Завантаження послуг...</p>';
    modal.classList.remove('hidden');

    try {
        const res = await fetch(`/api/bookings/${bookingId}`);
        const booking = await res.json();

        container.innerHTML = '';

        booking.bookingUnits.forEach(unit => {
            if (unit.status !== 'PENDING') return;

            const startStr = (unit.start || '').substring(0, 16);
            const endStr = (unit.end || '').substring(0, 16);
            const title = unit.serviceUnit?.h5BlockValue || 'Послуга';

            container.insertAdjacentHTML('beforeend', `
                <div class="manual-unit-item" data-unit-id="${unit.id}" data-service-id="${unit.serviceUnitId}" style="padding: 10px; border: 1px solid #e2e8f0; border-radius: 8px;">
                    <h5 style="margin-bottom: 10px;">${title} (Клієнтів: ${unit.clientCount})</h5>
                    <div style="display: flex; gap: 10px;">
                        <div style="flex: 1;">
                            <label style="display:block; font-size: 0.8rem;">Новий час початку</label>
                            <input type="datetime-local" class="manual-start" value="${startStr}" required style="width: 100%;">
                        </div>
                        <div style="flex: 1;">
                            <label style="display:block; font-size: 0.8rem;">Новий час завершення</label>
                            <input type="datetime-local" class="manual-end" value="${endStr}" required style="width: 100%;">
                        </div>
                    </div>
                </div>
            `);
        });

        const form = document.getElementById('manual-allocation-form');
        form.onsubmit = (e) => {
            e.preventDefault();

            const manualAllocations = [];
            document.querySelectorAll('.manual-unit-item').forEach(el => {
                const startVal = el.querySelector('.manual-start').value;
                const endVal = el.querySelector('.manual-end').value;

                manualAllocations.push({
                    bookingUnitId: parseInt(el.dataset.unitId),
                    serviceUnitId: parseInt(el.dataset.serviceId),
                    start: startVal.length === 16 ? startVal + ':00' : startVal,
                    end: endVal.length === 16 ? endVal + ':00' : endVal,
                    clientCount: booking.bookingUnits.find(u => u.id == el.dataset.unitId).clientCount
                });
            });

            sendBookingStatusPatch(bookingId, 'CONFIRMED', manualAllocations, selectElement, headers);
        };

    } catch (err) {
        container.innerHTML = '<p style="color:red;">Помилка завантаження даних.</p>';
    }
}

