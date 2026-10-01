const toggle = document.getElementById('contactToggle');
const body = document.getElementById('contactBody');

toggle.addEventListener('click', () => {
    const expanded = toggle.getAttribute('aria-expanded') === 'true';
    toggle.setAttribute('aria-expanded', String(!expanded));

    if (!expanded) {
        body.classList.add('show-body');
        body.setAttribute('aria-hidden', 'false');
        toggle.querySelector('span').textContent = '▴';
    } else {
        body.classList.remove('show-body');
        body.setAttribute('aria-hidden', 'true');
        toggle.querySelector('span').textContent = '▾';
    }
});