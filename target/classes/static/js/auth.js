// Bouton "œil" : affiche / cache le mot de passe du champ associé
document.querySelectorAll('.toggle-password').forEach(function (button) {
    button.addEventListener('click', function () {
        var input = document.getElementById(button.dataset.target);
        var visible = input.type === 'text';

        input.type = visible ? 'password' : 'text';
        button.classList.toggle('visible', !visible);
        button.setAttribute('aria-pressed', String(!visible));
        button.setAttribute('aria-label',
            visible ? 'Afficher le mot de passe' : 'Masquer le mot de passe');
    });
});
