const timer = document.getElementById('pack-timer');

// Pas de compteur quand le stock de paquets est plein
if (timer) {
    let remaining = parseInt(timer.dataset.seconds, 10);

    const render = () => {
        const m = String(Math.floor(remaining / 60)).padStart(2, '0');
        const s = String(remaining % 60).padStart(2, '0');
        timer.textContent = m + ':' + s;
    };

    render();
    const interval = setInterval(() => {
        remaining -= 1;
        if (remaining <= 0) {
            clearInterval(interval);
            window.location.href = window.location.pathname;
            return;
        }
        render();
    }, 1000);
}

document.getElementById('open-form').addEventListener('submit', event => {
    const button = event.target.querySelector('button');
    button.disabled = true;
    button.textContent = 'Ouverture…';
});

document.querySelectorAll('.pack-slot').forEach(slot => {
    const reveal = () => slot.classList.add('revealed');
    slot.addEventListener('click', reveal);
    slot.addEventListener('keydown', event => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            reveal();
        }
    });
});
