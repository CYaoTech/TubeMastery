// Page des enchères : polling de l'API REST + compte à rebours local.
const grid = document.getElementById('auctions-grid');
const coinCount = document.getElementById('coin-count');

// Décalage horloge serveur / navigateur, pour un compte à rebours juste
let skew = 0;

function format(ms) {
    if (ms <= 0) return 'Terminée';
    const s = Math.ceil(ms / 1000);
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    const pad = n => String(n).padStart(2, '0');
    return (h > 0 ? h + ':' : '') + pad(m) + ':' + pad(sec);
}

function tick() {
    document.querySelectorAll('.auction-card').forEach(card => {
        const left = Number(card.dataset.endsAt) - (Date.now() + skew);
        const timer = card.querySelector('.js-timer');
        timer.textContent = format(left);
        timer.classList.toggle('urgent', left > 0 && left < 60000);
    });
}

function apply(dto) {
    const card = grid.querySelector('[data-auction-id="' + dto.id + '"]');
    if (!card) return;
    card.dataset.endsAt = dto.endsAt;
    card.querySelector('.js-price').textContent = dto.currentPrice;
    card.querySelector('.js-bidder').textContent =
        dto.highestBidder ? 'Meilleure mise : ' + dto.highestBidder : 'Aucune mise';
}

async function poll() {
    try {
        const response = await fetch('/api/v1/auctions', { headers: { Accept: 'application/json' } });
        if (!response.ok) return;
        const list = await response.json();
        if (list.length) skew = list[0].serverNow - Date.now();

        const ids = new Set(list.map(a => String(a.id)));
        let changed = false;
        grid.querySelectorAll('.auction-card').forEach(card => {
            if (!ids.has(card.dataset.auctionId)) {
                card.remove();      // terminée (vendue, expirée ou annulée)
                changed = true;
            }
        });
        list.forEach(apply);
        // Nouvelle enchère postée par quelqu'un d'autre : rechargement de la page
        if (list.some(a => !grid.querySelector('[data-auction-id="' + a.id + '"]'))) {
            location.reload();
            return;
        }
        if (changed) refreshCoins();
    } catch (e) { /* réseau indisponible : on réessaie au prochain tour */ }
}

// Le solde change quand une enchère se clôt (vente) ou qu'on est surenchéri (remboursement)
async function refreshCoins() {
    try {
        const html = await (await fetch(location.href)).text();
        const m = html.match(/id="coin-count"[^>]*>(\d+)</);
        if (m) coinCount.textContent = m[1];
    } catch (e) { /* ignoré */ }
}

grid.addEventListener('submit', async event => {
    const form = event.target.closest('.bid-form');
    if (!form) return;
    event.preventDefault();
    const card = form.closest('.auction-card');
    const error = card.querySelector('.js-error');
    error.hidden = true;
    const button = form.querySelector('button');
    button.disabled = true;
    try {
        const response = await fetch(form.action, {
            method: 'POST',
            body: new URLSearchParams(new FormData(form)),   // inclut le jeton CSRF
        });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.error || 'Mise impossible.');
        coinCount.textContent = data.tubecoin;
        apply(data.auction);
        form.reset();
    } catch (e) {
        error.textContent = e.message;
        error.hidden = false;
    } finally {
        button.disabled = false;
    }
});

tick();
setInterval(tick, 1000);
setInterval(poll, 2500);
poll();
