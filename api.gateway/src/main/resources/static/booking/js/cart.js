"use strict";

document.addEventListener("DOMContentLoaded", () => {
    initializeCardInteractions();
});


function initializeCardInteractions() {

    window.syncCardsUI = function () {
        let cart = JSON.parse(localStorage.getItem('edemium_cart')) || [];

        const counts = {};
        cart.forEach(item => {
            counts[item.serviceUnitId] = (counts[item.serviceUnitId] || 0) + 1;
        });

        document.querySelectorAll('.card').forEach(card => {
            
            const rawId = card.dataset.serviceId;
            if (!rawId) return; 

            const serviceId = parseInt(rawId);
            const count = counts[serviceId] || 0;

            
            const selectBtn = card.querySelector('.btn-select, .btn-select-spa');
            const controlsBlock = card.querySelector('.service-quantity-controls');
            const qtySpan = card.querySelector('.service-qty-value');

            if (count > 0) {
                card.classList.add('selected-in-cart');
                if (selectBtn) selectBtn.classList.add('hidden');
                if (controlsBlock) controlsBlock.classList.remove('hidden');
                if (qtySpan) qtySpan.textContent = count;
            } else {
                card.classList.remove('selected-in-cart');
                if (selectBtn) selectBtn.classList.remove('hidden');
                if (controlsBlock) controlsBlock.classList.add('hidden');
            }
        });

        if (typeof window.updateGlobalCartBadge === 'function') {
            window.updateGlobalCartBadge();
        }
    };

    function addToCart(btn) {
        const classType = btn.getAttribute('data-class-type') + "_BOOKING";
        
        const newItem = {
            id: Date.now(),
            serviceUnitId: parseInt(btn.dataset.serviceId),
            serviceName: btn.dataset.serviceType,
            price: parseFloat(btn.dataset.servicePrice),
            maxCapacity: parseInt(btn.dataset.serviceCapacity),
            classType: classType, 
            duration: parseInt(btn.getAttribute('data-duration')) || 0, 
            start: '',
            end: '',
            clientCount: 1,
            preferedGender: 'ANY' 
        };

        let cart = JSON.parse(localStorage.getItem('edemium_cart')) || [];
        cart.push(newItem);
        localStorage.setItem('edemium_cart', JSON.stringify(cart));

        window.syncCardsUI();

        if (typeof window.triggerGlobalCartNudge === 'function') {
            window.triggerGlobalCartNudge();
        }
    }

    function removeFromCart(serviceId) {
        let cart = JSON.parse(localStorage.getItem('edemium_cart')) || [];
        
        const indexToRemove = cart.map(item => item.serviceUnitId).lastIndexOf(parseInt(serviceId));

        if (indexToRemove !== -1) {
            cart.splice(indexToRemove, 1);
            localStorage.setItem('edemium_cart', JSON.stringify(cart));

            window.syncCardsUI();

            if (typeof window.triggerGlobalCartNudge === 'function') {
                window.triggerGlobalCartNudge();
            }
        }
    }

    
    document.body.addEventListener('click', (e) => {
        
        if (e.target.classList.contains('btn-select') || e.target.classList.contains('btn-qty-plus')) {
            addToCart(e.target);
        }

        
        if (e.target.classList.contains('btn-qty-minus')) {
            removeFromCart(e.target.dataset.serviceId);
        }
    });

    
    window.syncCardsUI();
}