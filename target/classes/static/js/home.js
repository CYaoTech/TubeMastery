// Clic sur une carte de la collection : l'affiche en grand dans un modal
// avec la description complète.
const modal = document.getElementById('card-modal');

let currentCard = null;
const flip = document.getElementById('modal-flip');

function openCard(card) {
    currentCard = card;
    const img = card.querySelector('img');
    const subs = card.querySelector('.card-subs');

    document.getElementById('modal-img').src = img.src;
    document.getElementById('modal-img').alt = img.alt;
    document.getElementById('modal-name').textContent = card.querySelector('h2').textContent;
    document.getElementById('modal-subs').textContent = subs ? subs.textContent : '';
    document.getElementById('modal-desc').textContent = card.querySelector('.card-desc').textContent;

    document.getElementById('modal-card-id').value = card.dataset.cardId;

    flip.classList.remove('flipped');
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

const discardForm = modal.querySelector('.modal-actions');
discardForm.addEventListener('submit', async event => {
    event.preventDefault();
    if (!currentCard) return;

    const button = discardForm.querySelector('button');
    button.disabled = true;
    try {
        const response = await fetch(discardForm.action, {
            method: 'POST',
            body: new URLSearchParams(new FormData(discardForm)),
        });
        if (!response.ok) throw new Error('HTTP ' + response.status);
        const data = await response.json();

        document.getElementById('coin-count').textContent = data.tubecoin;
        currentCard.remove();
        currentCard = null;

        const count = document.querySelectorAll('.grid .card').length;
        document.getElementById('card-count').textContent = count;
        document.getElementById('card-count-label').textContent = count > 1 ? 'cartes' : 'carte';
        modal.close();
    } catch (e) {
        alert('Impossible de défausser la carte pour le moment.');
    } finally {
        button.disabled = false;
    }
});

flip.addEventListener('click', event => {
    if (event.target.closest('.modal-actions')) return;
    flip.classList.toggle('flipped');
});
flip.addEventListener('keydown', event => {
    if (event.target === flip && (event.key === 'Enter' || event.key === ' ')) {
        event.preventDefault();
        flip.classList.toggle('flipped');
    }
});
