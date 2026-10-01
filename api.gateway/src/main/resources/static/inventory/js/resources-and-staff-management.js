let csrfToken = '';
let csrfHeader = '';

document.addEventListener("DOMContentLoaded", () => {
    initializeSecurity();
    initializeTabsAndModals();
    initializeUnitsCRUD();
    initializeWorkersCRUD();
    initializeSchedules();
    initializeGlobalEvents();
    initializeInfiniteScrolls();
    setTimeout(() => {
        if (typeof window.fetchAndDisplayServiceNames === 'function') {
            window.fetchAndDisplayServiceNames();
        }
    }, 100);
});


function initializeSecurity() {
    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');

    if (!csrfTokenMeta || !csrfHeaderMeta) {
        console.error("CSRF tokens not found. Make sure meta tags are included in HTML.");
        return;
    }
    csrfToken = csrfTokenMeta.content;
    csrfHeader = csrfHeaderMeta.content;
}

function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `<span>${message}</span>`;
    container.appendChild(toast);
    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

function setButtonLoading(button, isLoading) {
    if (isLoading) {
        button.disabled = true;
        button.classList.add('btn-loading');
    } else {
        button.disabled = false;
        button.classList.remove('btn-loading');
    }
}

async function sendJsonRequest(url, method, payload) {
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        }
    };
    if (payload) options.body = JSON.stringify(payload);
    const response = await fetch(url, options);
    if (!response.ok) {
        let errMsg = 'Помилка запиту';
        try { errMsg = await response.text(); } catch (e) { }
        throw new Error(errMsg);
    }
    return response;
}


function initializeTabsAndModals() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            tabBtns.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));
            const targetId = e.currentTarget.getAttribute('data-target');
            e.currentTarget.classList.add('active');
            document.getElementById(targetId).classList.add('active');

            setTimeout(() => {
                window.dispatchEvent(new Event('resize'));
                window.dispatchEvent(new Event('scroll'));
            }, 50);
        });
    });

    document.querySelectorAll('.modal-close-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.currentTarget.closest('.post-form-container').classList.add('hidden');
        });
    });
}


function initializeUnitsCRUD() {
    let selectedUnitServices = [];
    let unitDebounceTimer;
    let skipNextFocusFetch = false;

    const unitModal = document.getElementById('unit-modal');
    const unitServiceBox = document.getElementById('selected-unit-service-box');
    const unitSearchInput = document.getElementById('unit-service-search-input');
    const unitDropdown = document.getElementById('unit-service-dropdown');
    const hiddenUnitServiceInput = document.getElementById('unit-service-id');

    function renderSelectedUnitServices() {
        unitServiceBox.innerHTML = '';
        selectedUnitServices.forEach(service => {
            const chip = document.createElement('div');
            chip.className = 'association-chip';
            chip.innerHTML = `<span>#${service.id} ${service.displayName}</span> <button type="button" data-id="${service.id}" class="remove-unit-service">×</button>`;
            unitServiceBox.appendChild(chip);
        });

        
        hiddenUnitServiceInput.value = selectedUnitServices.map(s => s.id).join(',');

        if (selectedUnitServices.length > 0) {
            unitSearchInput.placeholder = "Додати ще...";
        } else {
            unitSearchInput.placeholder = "Пошук за назвою або ID...";
        }
        unitSearchInput.style.display = 'block';
    }

    unitServiceBox?.addEventListener('click', (e) => {
        if (e.target.classList.contains('remove-unit-service')) {
            const idToRemove = parseInt(e.target.getAttribute('data-id'));
            selectedUnitServices = selectedUnitServices.filter(s => s.id !== idToRemove);
            renderSelectedUnitServices();
        }
    });

    document.querySelectorAll('input[name="unitCategory"]').forEach(radio => {
        radio.addEventListener('change', () => {
            selectedUnitServices = [];
            renderSelectedUnitServices();
        });
    });

    const handleUnitSearch = (e) => {

        if (e.type === 'focus' && skipNextFocusFetch) return;

        clearTimeout(unitDebounceTimer);
        const query = e.target.value.trim();
        const selectedCategory = document.querySelector('input[name="unitCategory"]:checked').value;

        unitDebounceTimer = setTimeout(async () => {
            try {
                const res = await fetch(`/api/service-units/search?q=${encodeURIComponent(query)}&category=${selectedCategory}`);
                if (!res.ok) throw new Error(`Помилка сервера: ${res.status}`);
                const data = await res.json();

                if (!Array.isArray(data)) throw new Error("Некоректний формат даних від сервера: " + data);

                const availableServices = data.filter(item => !selectedUnitServices.find(s => s.id === item.id));

                if (availableServices.length === 0) {
                    unitDropdown.innerHTML = '<div class="dropdown-item"><span class="dropdown-item-type">Нічого не знайдено</span></div>';
                } else {
                    unitDropdown.innerHTML = availableServices.map(item => {
                        const imgUrl = item.imageUrl ? item.imageUrl : '/common/images/placeholder.png';
                        const displayName = item.name || item.type;
                        return `
                        <div class="dropdown-item" data-id="${item.id}" data-name="${displayName}">
                            <img src="${imgUrl}" class="dropdown-item-img" alt="img">
                            <div class="dropdown-item-info">
                                <span class="dropdown-item-name">#${item.id} - ${displayName}</span>
                                <span class="dropdown-item-type">${item.type}</span>
                            </div>
                        </div>
                        `;
                    }).join('');
                }
                unitDropdown.classList.remove('hidden');
            } catch (err) {
                console.error("Помилка пошуку послуг", err);
            }
        }, 300);
    };

    unitSearchInput?.addEventListener('input', handleUnitSearch);
    unitSearchInput?.addEventListener('focus', handleUnitSearch);

    
    unitDropdown?.addEventListener('click', (e) => {
        e.stopPropagation();
        const item = e.target.closest('.dropdown-item');
        if (item && item.hasAttribute('data-id')) {
            selectedUnitServices.push({
                id: parseInt(item.getAttribute('data-id')),
                displayName: item.getAttribute('data-name')
            });
            renderSelectedUnitServices();

            item.remove();

            if (unitDropdown.querySelectorAll('.dropdown-item').length === 0) {
                unitDropdown.classList.add('hidden');
                unitSearchInput.value = '';
            }

            skipNextFocusFetch = true;
            unitSearchInput.focus();

            setTimeout(() => skipNextFocusFetch = false, 50);
        }
    });

    document.getElementById('add-unit-btn')?.addEventListener('click', () => {
        document.getElementById('unit-form').reset();
        document.getElementById('unit-id').value = '';
        selectedUnitServices = [];
        renderSelectedUnitServices();
        document.getElementById('unit-modal-title').innerText = 'Додати приміщення';
        unitModal.classList.remove('hidden');
    });

    document.getElementById('units-section').addEventListener('click', async (e) => {
        const editBtn = e.target.closest('.unit-edit-btn');
        const deleteBtn = e.target.closest('.unit-delete-btn');
        const card = e.target.closest('.admin-card');

        if (!card) return;

        
        if (editBtn) {
            document.getElementById('unit-modal-title').innerText = 'Редагувати приміщення';
            document.getElementById('unit-id').value = card.dataset.id;
            document.getElementById('unit-premises').value = card.dataset.premises;
            document.getElementById('unit-capacity').value = card.dataset.capacity;
            document.getElementById('unit-cleaning').value = card.dataset.cleaning;
            document.getElementById('unit-out-of-service').checked = (card.dataset.outOfService === 'true');

            const rawIds = card.dataset.serviceId;
            selectedUnitServices = [];

            if (rawIds && rawIds.trim() !== "") {
                try {
                    const res = await fetch(`/api/service-units/short-by-ids?ids=${rawIds}`);
                    if (!res.ok) throw new Error();
                    const data = await res.json();

                    selectedUnitServices = data.map(item => ({
                        id: item.id,
                        displayName: item.name || item.type
                    }));

                    if (data.length > 0) {
                        const categoryRadio = document.querySelector(`input[name="unitCategory"][value="${data[0].category}"]`);
                        if (categoryRadio) categoryRadio.checked = true;
                    }
                } catch (err) {
                    console.error("Не вдалося завантажити імена послуг");
                }
            }
            renderSelectedUnitServices();
            unitModal.classList.remove('hidden');
        }

        if (deleteBtn) {
            const id = card.dataset.id;

            if (!confirm(`Увага! Ви впевнені, що хочете видалити це приміщення?`)) return;

            const originalText = deleteBtn.innerText;
            deleteBtn.innerText = "...";
            deleteBtn.disabled = true;

            try {
                await sendJsonRequest(`/api/physical-units/${id}`, 'DELETE');
                showToast("Приміщення успішно видалено");
                card.remove();
            } catch (error) {
                showToast(error.message, "error");
                deleteBtn.innerText = originalText;
                deleteBtn.disabled = false;
            }
        }
    });

    document.getElementById('unit-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const form = e.target;
        const submitBtn = form.querySelector('button[type="submit"]');

        const serviceIdsRaw = form.querySelector('#unit-service-id').value;
        if (!serviceIdsRaw) {
            showToast("Будь ласка, оберіть хоча б одну послугу з каталогу!", "warning");
            return;
        }

        const data = {
            id: form.querySelector('#unit-id').value || null,
            premisesNumber: form.querySelector('#unit-premises').value,
            serviceUnitIds: serviceIdsRaw.split(',').map(Number),
            clientCapacity: parseInt(form.querySelector('#unit-capacity').value),
            cleaningTimeInMinutes: parseInt(form.querySelector('#unit-cleaning').value),
            outOfService: form.querySelector('#unit-out-of-service').checked
        };

        const url = data.id ? `/api/physical-units/${data.id}` : '/api/physical-units';
        const method = data.id ? 'PUT' : 'POST';

        setButtonLoading(submitBtn, true);
        try {
            await sendJsonRequest(url, method, data);
            showToast("Приміщення успішно збережено!");

            if (method === 'PUT') {
                const card = document.querySelector(`#units-section .admin-card[data-id="${data.id}"]`);
                if (card) {
                    card.dataset.premises = data.premisesNumber;
                    card.dataset.capacity = data.clientCapacity;
                    card.dataset.cleaning = data.cleaningTimeInMinutes;
                    card.dataset.outOfService = data.outOfService;
                    card.dataset.serviceId = data.serviceUnitIds.join(',');

                    card.querySelector('.card-header-row h3 span').innerText = data.premisesNumber;

                    const statusDiv = card.querySelector('.card-status');
                    statusDiv.className = `card-status ${data.outOfService ? 'status-danger' : 'status-success'}`;
                    statusDiv.innerText = data.outOfService ? 'Не працює' : 'Активно';

                    const pTags = card.querySelectorAll('p');
                    pTags[0].querySelector('span').innerText = data.clientCapacity;
                    pTags[1].querySelector('span').innerText = data.cleaningTimeInMinutes;

                    const nameContainer = card.querySelector('.service-name-display');
                    if (nameContainer) nameContainer.innerHTML = '';
                    fetchAndDisplayServiceNames();
                }

                document.getElementById('unit-modal').classList.add('hidden');
            } else {
                setTimeout(() => window.location.reload(), 800);
            }
        } catch (error) {
            showToast(error.message, "error");
        } finally {
            setButtonLoading(submitBtn, false);
        }
    });
}


function initializeWorkersCRUD() {
    let selectedSkills = [];
    let debounceTimer;

    const workerModal = document.getElementById('worker-modal');
    const skillsBox = document.getElementById('selected-association-box');
    const searchInput = document.getElementById('skill-search-input');
    const dropdown = document.getElementById('skills-dropdown');
    const hiddenSkillsInput = document.getElementById('worker-skills-hidden');

    function renderSelectedSkills() {
        skillsBox.innerHTML = '';
        selectedSkills.forEach(skill => {
            const chip = document.createElement('div');
            chip.className = 'association-chip';
            chip.innerHTML = `<span>#${skill.id} ${skill.displayName}</span> <button type="button" data-id="${skill.id}">×</button>`;
            skillsBox.appendChild(chip);
        });
        hiddenSkillsInput.value = selectedSkills.map(s => s.id).join(',');
    }

    skillsBox?.addEventListener('click', (e) => {
        if (e.target.tagName === 'BUTTON') {
            const idToRemove = parseInt(e.target.getAttribute('data-id'));
            selectedSkills = selectedSkills.filter(s => s.id !== idToRemove);
            renderSelectedSkills();
        }
    });

    const handleSkillSearch = (e) => {
        clearTimeout(debounceTimer);
        const query = e.target.value.trim();

        debounceTimer = setTimeout(async () => {
            try {
                const res = await fetch(`/api/service-units/search?q=${encodeURIComponent(query)}&category=SPA`);
                if (!res.ok) throw new Error(`Помилка сервера: ${res.status}`);
                const data = await res.json();
                if (!Array.isArray(data)) throw new Error("Некоректний формат даних від сервера: " + data);

                const availableSkills = data.filter(item => !selectedSkills.find(s => s.id === item.id));

                if (availableSkills.length === 0) {
                    dropdown.innerHTML = '<div class="dropdown-item"><span class="dropdown-item-type">Нічого не знайдено</span></div>';
                } else {
                    dropdown.innerHTML = availableSkills.map(item => {
                        const imgUrl = item.imageUrl ? item.imageUrl : '/common/images/placeholder.png';
                        const displayName = item.name || item.type;
                        return `
                        <div class="dropdown-item" data-id="${item.id}" data-name="${displayName}">
                            <img src="${imgUrl}" class="dropdown-item-img" alt="img">
                            <div class="dropdown-item-info">
                                <span class="dropdown-item-name">#${item.id} - ${displayName}</span>
                                <span class="dropdown-item-type">${item.type}</span>
                            </div>
                        </div>
                        `;
                    }).join('');
                }
                dropdown.classList.remove('hidden');
            } catch (err) {
                console.error("Помилка пошуку послуг", err);
            }
        }, 300);
    };

    searchInput?.addEventListener('input', handleSkillSearch);
    searchInput?.addEventListener('focus', handleSkillSearch);

    dropdown?.addEventListener('click', (e) => {
        e.stopPropagation();
        const item = e.target.closest('.dropdown-item');
        if (item && item.hasAttribute('data-id')) {
            selectedSkills.push({
                id: parseInt(item.getAttribute('data-id')),
                displayName: item.getAttribute('data-name')
            });
            renderSelectedSkills();
            item.remove();

            if (dropdown.querySelectorAll('.dropdown-item').length === 0) {
                dropdown.classList.add('hidden');
                searchInput.value = '';
            }
            searchInput.focus();
        }
    });

    document.getElementById('add-worker-btn')?.addEventListener('click', () => {
        document.getElementById('worker-form').reset();
        document.getElementById('worker-id').value = '';
        selectedSkills = [];
        renderSelectedSkills();
        document.getElementById('worker-modal-title').innerText = 'Додати працівника';
        workerModal.classList.remove('hidden');
    });

    document.getElementById('workers-section').addEventListener('click', async (e) => {
        const editBtn = e.target.closest('.worker-edit-btn');
        const deleteBtn = e.target.closest('.worker-delete-btn');
        const card = e.target.closest('.admin-card');

        if (editBtn && card) {
            document.getElementById('worker-modal-title').innerText = 'Редагувати працівника';
            document.getElementById('worker-id').value = card.dataset.id;
            document.getElementById('worker-firstname').value = card.dataset.firstname;
            document.getElementById('worker-lastname').value = card.dataset.lastname;
            document.getElementById('worker-phone').value = card.dataset.phone;
            document.getElementById('worker-gender').value = card.dataset.gender;
            document.getElementById('worker-status').value = card.dataset.status;

            const rawIds = card.dataset.skills;
            if (rawIds && rawIds.trim() !== "") {
                try {
                    const res = await fetch(`/api/service-units/short-by-ids?ids=${rawIds}`);
                    if (!res.ok) throw new Error();
                    const data = await res.json();

                    selectedSkills = data.map(item => ({
                        id: item.id,
                        displayName: item.name || item.type
                    }));
                } catch (err) {
                    selectedSkills = [];
                    console.error("Не вдалося завантажити імена послуг");
                }
            } else {
                selectedSkills = [];
            }

            renderSelectedSkills();
            workerModal.classList.remove('hidden');
        }


        if (deleteBtn && card) {
            const id = card.dataset.id;

            if (!confirm(`Увага! Ви впевнені, що хочете БЕЗПОВОРОТНО видалити цього працівника? Всі пов'язані з ним розклади також можуть бути видалені. Цю дію неможливо скасувати.`)) return;

            const originalText = deleteBtn.innerText;
            deleteBtn.innerText = "...";
            deleteBtn.disabled = true;

            try {
                await sendJsonRequest(`/api/spa-workers/${id}`, 'DELETE');
                showToast("Працівника успішно видалено");
                card.remove();
            } catch (error) {
                showToast(error.message, "error");
                deleteBtn.innerText = originalText;
                deleteBtn.disabled = false;
            }
        }
    });

    document.getElementById('worker-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const form = e.target;
        const submitBtn = form.querySelector('button[type="submit"]');

        const skillsRaw = document.getElementById('worker-skills-hidden').value;
        const competentSpaUnitIds = skillsRaw ? skillsRaw.split(',').map(Number) : [];

        if (competentSpaUnitIds.length === 0) {
            showToast("Оберіть хоча б одну компетенцію!", "warning");
            return;
        }

        const data = {
            id: form.querySelector('#worker-id').value || null,
            firstName: form.querySelector('#worker-firstname').value,
            lastName: form.querySelector('#worker-lastname').value,
            workPhoneNumber: form.querySelector('#worker-phone').value,
            gender: form.querySelector('#worker-gender').value,
            status: form.querySelector('#worker-status').value,
            competentSpaUnitIds: competentSpaUnitIds
        };

        const url = data.id ? `/api/spa-workers/${data.id}` : '/api/spa-workers';
        const method = data.id ? 'PUT' : 'POST';

        setButtonLoading(submitBtn, true);
        try {
            await sendJsonRequest(url, method, data);
            showToast("Працівника збережено!");

            if (method === 'PUT') {
                const card = document.querySelector(`#workers-section .admin-card[data-id="${data.id}"]`);
                if (card) {
                    
                    card.dataset.firstname = data.firstName;
                    card.dataset.lastname = data.lastName;
                    card.dataset.phone = data.workPhoneNumber;
                    card.dataset.gender = data.gender;
                    card.dataset.status = data.status;
                    card.dataset.skills = data.competentSpaUnitIds.join(',');

                    
                    card.querySelector('.card-header-row h3').innerText = `${data.firstName} ${data.lastName}`;

                    const statusDiv = card.querySelector('.card-status');
                    statusDiv.className = `card-status ${data.status === 'ACTIVE' ? 'status-success' : 'status-warning'}`;
                    statusDiv.innerText = data.status;

                    const pTags = card.querySelectorAll('p');
                    pTags[0].querySelector('span').innerText = data.gender === 'MALE' ? 'Чоловік' : 'Жінка';
                    pTags[1].querySelector('span').innerText = data.workPhoneNumber;

                    
                    const nameContainer = card.querySelector('.service-name-display');
                    if (nameContainer) nameContainer.innerHTML = '';
                    fetchAndDisplayServiceNames();
                }

                
                document.getElementById('worker-modal').classList.add('hidden');
            } else {
                
                setTimeout(() => window.location.reload(), 800);
            }
        } catch (error) {
            showToast(error.message, "error");
        } finally {
            setButtonLoading(submitBtn, false);
        }
    });
}




function initializeSchedules() {
    const scheduleModal = document.getElementById('schedule-modal');
    const breaksContainer = document.getElementById('breaks-container');
    const addBreakBtn = document.getElementById('add-break-btn');
    const scheduleForm = document.getElementById('schedule-form');
    const schedulesList = document.getElementById('existing-schedules-list');

    function formatTime(timeString) {
        if (!timeString) return '';
        
        
        return timeString.substring(0, 5);
    }

    async function loadSchedules(workerId) {
        schedulesList.innerHTML = '<p style="color:var(--text-muted); font-size:14px;">Завантаження...</p>';
        try {
            const res = await fetch(`/api/spa-workers/${workerId}/schedules`);
            if (!res.ok) throw new Error("Не вдалося завантажити розклад");
            const schedules = await res.json();

            if (schedules.length === 0) {
                schedulesList.innerHTML = '<p style="color:var(--text-muted); font-size:14px;">Немає запланованих змін</p>';
                return;
            }

            
            const dayOrder = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

            const daysMap = {
                'MONDAY': 'Понеділок', 'TUESDAY': 'Вівторок', 'WEDNESDAY': 'Середа',
                'THURSDAY': 'Четвер', 'FRIDAY': 'П\'ятниця', 'SATURDAY': 'Субота', 'SUNDAY': 'Неділя'
            };

            
            const sortedSchedules = schedules.sort((a, b) => {
                return dayOrder.indexOf(a.dayOfWeek) - dayOrder.indexOf(b.dayOfWeek);
            });

            
            schedulesList.innerHTML = sortedSchedules.map(sch => {
                const breaksHtml = sch.breaks && sch.breaks.length > 0
                    ? `<div class="schedule-breaks">Перерви: ${sch.breaks.map(b => `${formatTime(b.breakStart)} - ${formatTime(b.breakEnd)}`).join(', ')}</div>`
                    : '<div class="schedule-breaks">Без перерв</div>';

                const startStr = formatTime(sch.startTime);
                const endStr = formatTime(sch.endTime);

                return `
                <div class="schedule-card">
                    <div class="schedule-info">
                        <strong>Кожен ${daysMap[sch.dayOfWeek] || sch.dayOfWeek}</strong>
                        <span>⌚ ${startStr} - ${endStr}</span>
                        ${breaksHtml}
                    </div>
                    <button type="button" class="btn-delete-schedule" data-id="${sch.id}">Видалити</button>
                </div>
            `;
            }).join('');

            
            schedulesList.querySelectorAll('.btn-delete-schedule').forEach(btn => {
                btn.addEventListener('click', async (e) => {
                    if (!confirm("Видалити цю зміну?")) return;
                    const schId = e.target.getAttribute('data-id');
                    const deleteBtn = e.target;
                    deleteBtn.disabled = true;
                    deleteBtn.innerText = '...';

                    try {
                        await sendJsonRequest(`/api/spa-workers/schedules/${schId}`, 'DELETE');
                        showToast("Зміну видалено");
                        await loadSchedules(workerId);
                    } catch (error) {
                        showToast(error.message, "error");
                        deleteBtn.disabled = false;
                        deleteBtn.innerText = 'Видалити';
                    }
                });
            });
        } catch (error) {
            schedulesList.innerHTML = '<p style="color:var(--danger-text); font-size:14px;">Помилка завантаження</p>';
        }
    }

    document.getElementById('workers-section').addEventListener('click', async (e) => {
        const scheduleBtn = e.target.closest('.worker-schedule-btn');
        if (!scheduleBtn) return;

        const card = scheduleBtn.closest('.admin-card');
        if (!card) return;

        const workerId = card.dataset.id;
        const firstName = card.dataset.firstname;
        const lastName = card.dataset.lastname;

        document.getElementById('schedule-worker-id').value = workerId;
        document.getElementById('schedule-modal-worker-name').innerText = `${firstName} ${lastName}`;

        scheduleForm.reset();
        breaksContainer.innerHTML = '';

        await loadSchedules(workerId);
        scheduleModal.classList.remove('hidden');
    });

    addBreakBtn?.addEventListener('click', () => {
        const breakRow = document.createElement('div');
        breakRow.className = 'break-row';
        breakRow.innerHTML = `
            <input type="time" class="break-start" required placeholder="Початок">
            <input type="time" class="break-end" required placeholder="Кінець">
            <button type="button" class="btn-remove-break" title="Видалити перерву">×</button>
        `;
        breakRow.querySelector('.btn-remove-break').addEventListener('click', () => breakRow.remove());
        breaksContainer.appendChild(breakRow);
    });

    scheduleForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const submitBtn = e.target.querySelector('button[type="submit"]');

        const workerId = document.getElementById('schedule-worker-id').value;

        
        const dayOfWeekVal = document.getElementById('schedule-day-of-week').value;

        const startVal = document.getElementById('schedule-start').value;
        const endVal = document.getElementById('schedule-end').value;

        if (startVal >= endVal) {
            showToast("Час закінчення зміни має бути пізніше за час початку!", "error");
            return;
        }

        let breaksValid = true;
        const breaks = Array.from(document.querySelectorAll('.break-row')).map(row => {
            const bStart = row.querySelector('.break-start').value;
            const bEnd = row.querySelector('.break-end').value;

            if (bStart >= bEnd) {
                showToast("Час закінчення перерви має бути пізніше за її початок!", "error");
                breaksValid = false;
            }
            if (bStart < startVal || bEnd > endVal) {
                showToast("Перерва повинна бути в межах робочої зміни!", "error");
                breaksValid = false;
            }
            return {
                
                breakStart: `${bStart}:00`,
                breakEnd: `${bEnd}:00`
            };
        });

        if (!breaksValid) return;

        const payload = {
            
            dayOfWeek: dayOfWeekVal,
            
            startTime: `${startVal}:00`,
            endTime: `${endVal}:00`,
            breaks: breaks
        };

        setButtonLoading(submitBtn, true);
        try {
            await sendJsonRequest(`/api/spa-workers/${workerId}/schedules`, 'POST', payload);
            showToast("Зміну успішно збережено!");
            scheduleForm.reset();
            document.getElementById('breaks-container').innerHTML = '';
            await loadSchedules(workerId);
        } catch (error) {
            showToast(error.message, "error");
        } finally {
            setButtonLoading(submitBtn, false);
        }
    });
}




function initializeGlobalEvents() {
    document.addEventListener('click', (e) => {
        const unitDropdown = document.getElementById('unit-service-dropdown');
        if (unitDropdown && !unitDropdown.classList.contains('hidden')) {
            if (!e.target.closest('#unit-service-container')) {
                unitDropdown.classList.add('hidden');
            }
        }

        const skillsDropdown = document.getElementById('skills-dropdown');
        if (skillsDropdown && !skillsDropdown.classList.contains('hidden')) {
            const workerContainer = skillsDropdown.closest('.autocomplete-container');
            if (workerContainer && !workerContainer.contains(e.target)) {
                skillsDropdown.classList.add('hidden');
            }
        }
    });
}


function initializeInfiniteScrolls() {
    
    setupInfiniteScroll({
        apiUrl: '/api/physical-units',
        gridSelector: '#units-section .inventory-grid',
        statusSelector: '#units-scroll-status',
        renderFunction: createUnitCard
    });

    
    setupInfiniteScroll({
        apiUrl: '/api/spa-workers',
        gridSelector: '#workers-section .inventory-grid',
        statusSelector: '#workers-scroll-status',
        renderFunction: createWorkerCard
    });
}

function setupInfiniteScroll(config) {
    const gridContainer = document.querySelector(config.gridSelector);
    const statusContainer = document.querySelector(config.statusSelector);
    if (!gridContainer || !statusContainer) return;

    const loader = statusContainer.querySelector('.scroll-loader');
    const endBlock = statusContainer.querySelector('.scroll-end');

    let currentPage = 1; 
    const pageSize = 4; 
    let isLoading = false;
    let isLastPage = false;

    const setScrollState = (state) => {
        if (loader) loader.classList.add("hidden");
        if (endBlock) endBlock.classList.add("hidden");

        if (state === "loading" && loader) loader.classList.remove("hidden");
        if (state === "end" && endBlock) endBlock.classList.remove("hidden");
    };

    const fetchMoreData = async () => {
        if (isLoading || isLastPage) return;

        isLoading = true;
        setScrollState("loading");

        try {
            const response = await fetch(`${config.apiUrl}?page=${currentPage}&size=${pageSize}`);
            if (!response.ok) throw new Error("Помилка сервера");

            const data = await response.json();

            if (!Array.isArray(data) || data.length === 0) {
                isLastPage = true;
                setScrollState("end");
                return;
            }

            
            data.forEach(item => {
                gridContainer.insertAdjacentHTML('beforeend', config.renderFunction(item));
            });

            
            if (typeof fetchAndDisplayServiceNames === 'function') {
                fetchAndDisplayServiceNames();
            }

            
            if (data.length < pageSize) {
                isLastPage = true;
                setScrollState("end");
            } else {
                setScrollState("none");
                currentPage++;
            }

        } catch (error) {
            console.error("Помилка пагінації", error);
            setScrollState("none");
        } finally {
            isLoading = false;
        }
    };

    const observer = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting && !isLoading && !isLastPage) {
            fetchMoreData();
        }
    }, { rootMargin: "200px", threshold: 0 });

    observer.observe(statusContainer);
    
    
    
    
}



function createUnitCard(unit) {
    const serviceIds = unit.serviceUnitIds ? unit.serviceUnitIds.join(',') : '';
    const statusClass = unit.outOfService ? 'status-danger' : 'status-success';
    const statusText = unit.outOfService ? 'Не працює' : 'Активно';

    return `
        <div class="card admin-card" data-id="${unit.id}" data-premises="${unit.premisesNumber || ''}" data-service-id="${serviceIds}" data-capacity="${unit.clientCapacity}" data-cleaning="${unit.cleaningTimeInMinutes}" data-out-of-service="${unit.outOfService}">
            <div class="admin-card-content">
                <div class="card-header-row">
                    <h3>Номер: <span>${unit.premisesNumber || 'N/A'}</span></h3>
                    <div class="card-status ${statusClass}">${statusText}</div>
                </div>
                <div>
                    <strong>Послуги:</strong>
                    <div class="service-name-display" style="margin-top: 4px;">ID: <span>${serviceIds}</span></div>
                </div>
                <p><strong>Місткість:</strong> <span>${unit.clientCapacity}</span> осіб</p>
                <p><strong>Час прибирання:</strong> <span>${unit.cleaningTimeInMinutes}</span> хв</p>
            </div>
            <div class="modal-buttons-container">
                <button class="edit-modal-opener unit-edit-btn">Редагувати</button>
                <button class="delete-modal-opener unit-delete-btn">Видалити</button>
            </div>
        </div>
    `;
}

function createWorkerCard(worker) {
    const skills = worker.competentSpaUnitIds ? worker.competentSpaUnitIds.join(',') : '';
    const statusClass = worker.status === 'ACTIVE' ? 'status-success' : 'status-warning';
    const genderText = worker.gender === 'MALE' ? 'Чоловік' : 'Жінка';

    return `
        <div class="card admin-card" data-id="${worker.id}" data-firstname="${worker.firstName}" data-lastname="${worker.lastName}" data-phone="${worker.workPhoneNumber}" data-gender="${worker.gender}" data-status="${worker.status}" data-skills="${skills}">
            <div class="admin-card-content">
                <div class="card-header-row">
                    <h3>${worker.firstName} ${worker.lastName}</h3>
                    <div class="card-status ${statusClass}">${worker.status}</div>
                </div>
                <p><strong>Стать:</strong> <span>${genderText}</span></p>
                <p><strong>Телефон:</strong> <span>${worker.workPhoneNumber || ''}</span></p>
                <div class="mt-8">
                    <p class="mb-8"><strong>Навички (ID послуг):</strong></p>
                    <div class="service-name-display">ID: <span>${skills}</span></div>
                </div>
            </div>
            <div class="modal-buttons-container">
                <button class="schedule-modal-opener worker-schedule-btn">Розклад</button>
                <button class="edit-modal-opener worker-edit-btn">Редаг.</button>
                <button class="delete-modal-opener worker-delete-btn">Звільнити</button>
            </div>
        </div>
    `;
}




window.fetchAndDisplayServiceNames = async function () {
    const allCards = document.querySelectorAll('.admin-card');
    const serviceIdsToFetch = new Set();
    const cardsToProcess = [];

    
    
    allCards.forEach(card => {
        const nameContainer = card.querySelector('.service-name-display');

        if (nameContainer && !nameContainer.querySelector('.service-mini-card')) {
            cardsToProcess.push(card); 

            const rawIds = card.dataset.serviceId || card.dataset.skills;
            if (rawIds && rawIds.trim() !== '') {
                rawIds.split(',').forEach(id => serviceIdsToFetch.add(id.trim()));
            }
        }
    });

    
    if (cardsToProcess.length === 0 || serviceIdsToFetch.size === 0) return;

    try {
        const res = await fetch(`/api/service-units/short-by-ids?ids=${Array.from(serviceIdsToFetch).join(',')}`);
        if (!res.ok) throw new Error("Сервер відповів помилкою");

        const data = await res.json();
        const idToDataMap = {};
        data.forEach(item => { idToDataMap[item.id] = item; });

        
        cardsToProcess.forEach(card => {
            const rawIds = card.dataset.serviceId || card.dataset.skills;
            const nameContainer = card.querySelector('.service-name-display');

            if (rawIds && rawIds.trim() !== '' && nameContainer) {
                const idsArray = rawIds.split(',').map(id => id.trim());
                let finalHtml = '';

                idsArray.forEach(sId => {
                    if (idToDataMap[sId]) {
                        const svc = idToDataMap[sId];
                        const imgUrl = svc.imageUrl ? svc.imageUrl : '/common/images/placeholder.png';
                        const displayName = svc.name || svc.type || "Невідомо";

                        finalHtml += `
                            <div class="service-mini-card">
                                <img src="${imgUrl}" class="service-mini-img" alt="img">
                                <div class="service-mini-info">
                                    <span class="service-mini-title">#${svc.id} - ${displayName}</span>
                                    <span class="service-mini-type">${svc.type}</span>
                                </div>
                            </div>
                        `;
                    } else {
                        finalHtml += `<div style="color:var(--danger-text); font-weight:bold; margin-top:8px;">ID: ${sId} (Видалено)</div>`;
                    }
                });

                nameContainer.innerHTML = finalHtml;

                
                const maxVisible = 3;
                if (idsArray.length > maxVisible) {
                    const btn = document.createElement('button');
                    btn.className = 'show-more-btn';
                    btn.type = 'button';
                    btn.innerText = `Ще (+${idsArray.length - maxVisible})`;

                    btn.addEventListener('click', () => {
                        nameContainer.classList.toggle('expanded');
                        if (nameContainer.classList.contains('expanded')) {
                            btn.innerText = 'Сховати';
                        } else {
                            btn.innerText = `Ще (+${idsArray.length - maxVisible})`;
                        }
                    });
                    nameContainer.appendChild(btn);
                }
            }
        });
    } catch (e) {
        console.error("Помилка підвантаження назв послуг", e);
    }
};
