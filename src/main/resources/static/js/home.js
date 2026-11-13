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

    const channel = document.getElementById('modal-channel');
    channel.hidden = !card.dataset.handle;
    if (card.dataset.handle) {
        channel.href = 'https://www.youtube.com/@' + encodeURIComponent(card.dataset.handle);
    }

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

    const button = discardForm.querySelector('button[type="submit"]');
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

        updateCount();
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

// ---------- Filtre par rareté ----------
// Aucune rareté sélectionnée = toutes les cartes affichées.

const rarityButtons = document.querySelectorAll('.rarity-btn');
const discardAll = document.getElementById('discard-all');
const discardAllForm = document.getElementById('discard-all-form');

function selectedRarities() {
    return [...rarityButtons]
        .filter(b => b.getAttribute('aria-pressed') === 'true')
        .map(b => b.dataset.rarity);
}

function updateCount() {
    const count = document.querySelectorAll('.grid .card').length;
    document.getElementById('card-count').textContent = count;
    document.getElementById('card-count-label').textContent = count > 1 ? 'cartes' : 'carte';
}

function applyFilter() {
    const selected = selectedRarities();
    document.querySelectorAll('.grid .card').forEach(card => {
        card.hidden = selected.length > 0 && !selected.includes(card.dataset.rarity);
    });
    if (discardAll) {
        discardAll.disabled = selected.length === 0;
        discardAll.title = selected.length === 0
            ? 'Sélectionne au moins une rareté'
            : 'Défausser toutes les cartes des raretés sélectionnées';
    }
}

rarityButtons.forEach(button => {
    button.addEventListener('click', () => {
        const pressed = button.getAttribute('aria-pressed') === 'true';
        button.setAttribute('aria-pressed', String(!pressed));
        applyFilter();
    });
});

if (discardAll) {
    discardAllForm.addEventListener('submit', async event => {
        event.preventDefault();
        const selected = selectedRarities();
        if (selected.length === 0) return;

        const targets = [...document.querySelectorAll('.grid .card')]
            .filter(card => selected.includes(card.dataset.rarity));
        if (targets.length === 0) {
            alert('Aucune carte à défausser pour ces raretés.');
            return;
        }
        if (!confirm('Défausser ' + targets.length + ' carte(s) contre ' + targets.length + ' TubeCoin(s) ?')) return;

        discardAll.disabled = true;
        try {
            // Le formulaire fournit le jeton CSRF (ajouté par Thymeleaf)
            const body = new URLSearchParams(new FormData(discardAllForm));
            selected.forEach(r => body.append('rarities', r));
            const response = await fetch(discardAllForm.action, { method: 'POST', body });
            if (!response.ok) throw new Error('HTTP ' + response.status);
            const data = await response.json();

            document.getElementById('coin-count').textContent = data.tubecoin;
            targets.forEach(card => card.remove());
            updateCount();
        } catch (e) {
            alert('Impossible de défausser les cartes pour le moment.');
        } finally {
            applyFilter();
        }
    });
}


// ---------- Mise aux enchères ----------
const auctionModal = document.getElementById('auction-modal');
const auctionForm = document.getElementById('auction-form');

document.getElementById('modal-auction').addEventListener('click', () => {
    if (!currentCard) return;
    document.getElementById('auction-card-id').value = currentCard.dataset.cardId;
    document.getElementById('auction-card-name').textContent =
        currentCard.querySelector('h2').textContent;
    document.getElementById('auction-error').hidden = true;
    modal.close();
    auctionModal.showModal();
});

auctionModal.querySelector('.modal-close').addEventListener('click', () => auctionModal.close());
auctionModal.addEventListener('click', event => {
    if (event.target === auctionModal) auctionModal.close();
});

auctionForm.addEventListener('submit', async event => {
    event.preventDefault();
    const error = document.getElementById('auction-error');
    error.hidden = true;
    const button = auctionForm.querySelector('button[type="submit"]');
    button.disabled = true;
    try {
        const response = await fetch(auctionForm.action, {
            method: 'POST',
            body: new URLSearchParams(new FormData(auctionForm)),
        });
        if (!response.ok) {
            const data = await response.json().catch(() => ({}));
            throw new Error(data.error || 'Impossible de créer l\'enchère.');
        }
        // La carte quitte la collection : on la retire de la grille
        if (currentCard) {
            currentCard.remove();
            currentCard = null;
            updateCount();
        }
        auctionModal.close();
    } catch (e) {
        error.textContent = e.message;
        error.hidden = false;
    } finally {
        button.disabled = false;
    }
});
