// Clic sur une carte de la collection : l'affiche en grand dans un modal
// avec la description complète.
const modal = document.getElementById('card-modal');

function openCard(card) {
    const img = card.querySelector('img');
    const subs = card.querySelector('.card-subs');

    document.getElementById('modal-img').src = img.src;
    document.getElementById('modal-img').alt = img.alt;
    document.getElementById('modal-name').textContent = card.querySelector('h2').textContent;
    document.getElementById('modal-subs').textContent = subs ? subs.textContent : '';
    document.getElementById('modal-desc').textContent = card.querySelector('.card-desc').textContent;

    modal.showModal();
}

document.querySelectorAll('.grid .card').forEach(card => {
    card.addEventListener('click', () => openCard(card));
    card.addEventListener('keydown', event => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            openCard(card);
        }
    });
});

modal.querySelector('.modal-close').addEventListener('click', () => modal.close());

// Clic en dehors de la carte (sur le fond assombri) : fermeture
modal.addEventListener('click', event => {
    if (event.target === modal) {
        modal.close();
    }
});
