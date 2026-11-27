package com.acme.TubeMastery.service;

import com.acme.TubeMastery.model.Card;
import com.acme.TubeMastery.model.User;
import com.acme.TubeMastery.model.UserProfile;
import com.acme.TubeMastery.repository.UserProfileRepository;
import com.acme.TubeMastery.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProfileService {

    /** Ce que la page de profil affiche : le joueur, son profil et ses cartes. */
    public record ProfileView(User user, UserProfile profile, List<Card> cards) {}

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    public ProfileService(UserRepository userRepository, UserProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    /** Charge le profil d'un joueur (le crée s'il n'existe pas, pour les anciens comptes). */
    @Transactional
    public ProfileView getProfile(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new NoSuchElementException("Joueur introuvable"));

        UserProfile profile = getOrCreate(user);

        // Cartes triées de la plus populaire à la moins populaire
        List<Card> cards = new ArrayList<>(user.getCards());
        cards.sort(Comparator.comparing(
            Card::getSubscriberCount,
            Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return new ProfileView(user, profile, cards);
    }

    @Transactional
    public void updateProfile(String username, String bio, String avatarUrl) {
        User user = userRepository.findByUsername(username).orElseThrow();
        UserProfile profile = getOrCreate(user);

        profile.setBio(emptyToNull(bio));
        profile.setAvatarUrl(emptyToNull(avatarUrl));
    }

    private UserProfile getOrCreate(User user) {
        if (user.getProfile() == null) {
            UserProfile profile = new UserProfile();
            profile.setUser(user);
            user.setProfile(profileRepository.save(profile));
        }
        return user.getProfile();
    }

    private String emptyToNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }
}
