"use strict";

const API = {
    getCsrf: () => ({
        header: document.querySelector('meta[name="_csrf_header"]').content,
        token: document.querySelector('meta[name="_csrf"]').content
    }),

    
    async postForm(url, formData) {
        const csrf = this.getCsrf();
        const response = await fetch(url, {
            method: "POST",
            body: new URLSearchParams(formData),
            headers: {
                [csrf.header]: csrf.token,
                "Content-Type": "application/x-www-form-urlencoded"
            }
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || `Помилка сервера: ${response.status}`);
        }

        
        if (response.status === 204 || response.headers.get("content-length") === "0") {
            return {};
        }
        return response.json().catch(() => ({}));
    }
};

const UI = {
    showError: (elementId, message) => {
        const el = document.getElementById(elementId) || document.querySelector(elementId);
        if (el) el.textContent = message;
    },
    clearError: (elementId) => UI.showError(elementId, ""),
    
    setLoadingState: (button, isLoading, loadingText = "Зачекайте...") => {
        if (isLoading) {
            button.dataset.originalText = button.textContent;
            button.textContent = loadingText;
            button.disabled = true;
        } else {
            button.textContent = button.dataset.originalText || button.textContent;
            button.disabled = false;
        }
    }
};


class OtpTimer {
    constructor(buttonId, defaultText = 'Відправити код ще раз', duration = 60) {
        this.button = document.getElementById(buttonId);
        this.defaultText = defaultText;
        this.duration = duration;
        this.timerId = null;
    }

    start() {
        let secondsLeft = this.duration;
        this.button.disabled = true;
        clearInterval(this.timerId);
        
        this.timerId = setInterval(() => {
            secondsLeft--;
            this.button.textContent = `Надіслати ще раз (${secondsLeft} с)`;
            
            if (secondsLeft <= 0) {
                clearInterval(this.timerId);
                this.button.disabled = false;
                this.button.textContent = this.defaultText;
            }
        }, 1000);
    }
}





const FormSwitcher = {
    containers: {
        signIn: document.querySelector('.sign-in-form-container'),
        signUp: document.querySelector('.sign-up-form-container'),
        forgot: document.querySelector('.forgot-password-form-container')
    },

    show(targetKey) {
        Object.values(this.containers).forEach(container => {
            if (!container) return;
            container.classList.remove('active');
            container.style.display = 'none';
        });

        const targetContainer = this.containers[targetKey];
        if (targetContainer) {
            setTimeout(() => {
                targetContainer.style.display = 'block';
                setTimeout(() => targetContainer.classList.add('active'), 10);
            }, 100);
        }
    }
};


document.addEventListener('click', (e) => {
    if (e.target.closest('.registration-link')) FormSwitcher.show('signUp');
    if (e.target.closest('.login-link') || e.target.closest('.login-link-from-forgot')) FormSwitcher.show('signIn');
    if (e.target.closest('.forgot-password-link')) FormSwitcher.show('forgot');
});





function bindAuthSubmit(formId, endpoint, onSuccess, errorSelector) {
    const form = document.getElementById(formId);
    if (!form) return;

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        UI.clearError(errorSelector);
        
        const submitBtn = form.querySelector('button[type="submit"]');
        UI.setLoadingState(submitBtn, true);

        try {
            const data = await API.postForm(endpoint, new FormData(form));
            onSuccess(data);
        } catch (error) {
            UI.showError(errorSelector, error.message || "Помилка з'єднання");
        } finally {
            UI.setLoadingState(submitBtn, false);
        }
    });
}


bindAuthSubmit("sign-in-form", "/sign_in", (data) => {
    window.location.href = data.redirect || "/";
}, "#sign-in-form .error-span");


bindAuthSubmit("sign-up-form", "/api/sign_up", () => {
    FormSwitcher.show('signIn');
    setTimeout(() => {
        document.getElementById("sign-up-form").reset();
        document.getElementById('signup-step-2').style.display = 'none';
        document.getElementById('signup-step-1').style.display = 'block';
    }, 500);
}, "step2-error");






function initRegistration() {
    const form = document.getElementById('sign-up-form');
    if (!form) return;

    const btnRequestOtp = document.getElementById('btn-request-otp');
    const btnResendOtp = document.getElementById('btn-resend-otp');
    const timer = new OtpTimer('btn-resend-otp');

    async function handleOtpRequest(isResend = false) {
        const button = isResend ? btnResendOtp : btnRequestOtp;
        const errorId = isResend ? 'step2-error' : 'step1-error';

        if (!form.checkValidity() && !isResend) {
            form.reportValidity();
            return;
        }

        const pass = document.getElementById('reg-password').value;
        const confirmPass = document.getElementById('reg-confirm-password').value;

        if (!isResend && pass !== confirmPass) {
            UI.showError('step1-error', "Паролі не співпадають!");
            return;
        }

        UI.clearError(errorId);
        UI.setLoadingState(button, true, "Відправка...");

        const formData = new FormData();
        formData.append("login", document.querySelector('input[name="login"]').value);
        formData.append("phoneNumber", document.querySelector('input[name="phoneNumber"]').value);

        try {
            await API.postForm('/api/request-registration-otp', formData);
            
            if (!isResend) {
                document.getElementById('signup-step-1').style.display = 'none';
                document.getElementById('signup-step-2').style.display = 'block';
                document.getElementById('display-phone').textContent = formData.get("phoneNumber");
                document.querySelector('input[name="otpCode"]').required = true;
            }
            timer.start();
        } catch (error) {
            UI.showError(errorId, error.message);
        } finally {
            UI.setLoadingState(button, false);
        }
    }

    btnRequestOtp?.addEventListener('click', () => handleOtpRequest(false));
    btnResendOtp?.addEventListener('click', () => handleOtpRequest(true));
}






function initForgotPassword() {
    const form = document.getElementById('forgot-password-form');
    if (!form) return;

    const btnRequestOtp = document.getElementById('btn-request-forgot-otp');
    const btnResendOtp = document.getElementById('btn-resend-forgot-otp');
    const phoneInput = document.getElementById('forgot-phone');
    const timer = new OtpTimer('btn-resend-forgot-otp');

    async function handleForgotOtpRequest(isResend = false) {
        const button = isResend ? btnResendOtp : btnRequestOtp;
        const errorId = isResend ? 'forgot-step2-error' : 'forgot-step1-error';

        if (!phoneInput.checkValidity()) {
            form.reportValidity();
            return;
        }

        UI.clearError(errorId);
        UI.setLoadingState(button, true, "Відправка...");

        try {
            await API.postForm('/api/request-reset-otp', new FormData(form));
            
            if (!isResend) {
                document.getElementById('forgot-step-1').style.display = 'none';
                document.getElementById('forgot-step-2').style.display = 'block';
                document.getElementById('forgot-display-phone').textContent = phoneInput.value;

                form.querySelector('input[name="otpCode"]').required = true;
            }
            timer.start();
        } catch (error) {
            UI.showError(errorId, error.message);
        } finally {
            UI.setLoadingState(button, false);
        }
    }

    btnRequestOtp?.addEventListener('click', () => handleForgotOtpRequest(false));
    btnResendOtp?.addEventListener('click', () => handleForgotOtpRequest(true));

    
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        UI.clearError('forgot-step2-error');

        const pass = document.getElementById('forgot-password').value;
        const confirmPass = document.getElementById('forgot-confirm-password').value;
        const submitBtn = form.querySelector('button[type="submit"]');

        if (pass !== confirmPass) {
            UI.showError('forgot-step2-error', "Паролі не співпадають!");
            return;
        }

        UI.setLoadingState(submitBtn, true);

        try {
            const data = await API.postForm("/api/reset-password", new FormData(form));
            alert(data.message || "Пароль успішно змінено!");
            
            form.reset();
            document.getElementById('forgot-step-2').style.display = 'none';
            document.getElementById('forgot-step-1').style.display = 'block';
            FormSwitcher.show('signIn');
        } catch (error) {
            UI.showError('forgot-step2-error', error.message);
        } finally {
            UI.setLoadingState(submitBtn, false);
        }
    });
}




initRegistration();
initForgotPassword();