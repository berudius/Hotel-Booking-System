"use strict"

document.addEventListener("DOMContentLoaded", () => {
    initializeLoginOverlayClick()
})

function initializeLoginOverlayClick() {
    const loginOverlay = document.getElementById('login-overlay')

    if (loginOverlay) {
        const loginOverlayAuthLink = loginOverlay.querySelector(".login-overlay-auth-link");
        loginOverlayAuthLink.addEventListener("click", (event) => {
            event.preventDefault()
            const authRedirect = event.target.getAttribute("href")
            window.location.href = authRedirect + "?redirect=" + window.location.href
        })
    }

}