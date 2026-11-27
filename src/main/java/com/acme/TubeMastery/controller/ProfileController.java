package com.acme.TubeMastery.controller;

import com.acme.TubeMastery.dto.UpdateProfileRequest;
import com.acme.TubeMastery.model.User;
import com.acme.TubeMastery.service.CollectionService;
import com.acme.TubeMastery.service.ProfileService;
import com.acme.TubeMastery.service.ProfileService.ProfileView;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.NoSuchElementException;

/** Profil d'un joueur : visible par tous les joueurs connectés, modifiable par son propriétaire. */
@Controller
public class ProfileController {

    private final ProfileService profileService;
    private final CollectionService collectionService;

    public ProfileController(ProfileService profileService, CollectionService collectionService) {
        this.profileService = profileService;
        this.collectionService = collectionService;
    }

    @GetMapping("/profile/{username}")
    public String show(@PathVariable String username, Model model, Principal principal) {
        ProfileView view;
        try {
            view = profileService.getProfile(username);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        addTopbarData(model, principal);
        model.addAttribute("owner", view.user());
        model.addAttribute("profile", view.profile());
        model.addAttribute("cards", view.cards());
        model.addAttribute("isOwner", view.user().getUsername().equals(principal.getName()));
        return "profile";
    }

    @GetMapping("/profile/{username}/edit")
    public String editForm(@PathVariable String username, Model model, Principal principal) {
        checkOwner(username, principal);

        ProfileView view = profileService.getProfile(principal.getName());

        UpdateProfileRequest form = new UpdateProfileRequest();
        form.setBio(view.profile().getBio());
        form.setAvatarUrl(view.profile().getAvatarUrl());

        addTopbarData(model, principal);
        model.addAttribute("form", form);
        return "profile-edit";
    }

    @PostMapping("/profile/{username}/edit")
    public String edit(@PathVariable String username,
                       @Valid @ModelAttribute("form") UpdateProfileRequest form,
                       BindingResult result,
                       Model model,
                       Principal principal,
                       RedirectAttributes redirectAttributes) {

        checkOwner(username, principal);

        if (result.hasErrors()) {
            addTopbarData(model, principal);
            return "profile-edit";
        }

        profileService.updateProfile(principal.getName(), form.getBio(), form.getAvatarUrl());
        redirectAttributes.addFlashAttribute("successMessage", "Profil mis à jour.");
        return "redirect:/profile/" + principal.getName();
    }

    /** Seul le propriétaire peut modifier son profil : sinon erreur 403. */
    private void checkOwner(String username, Principal principal) {
        if (!username.equalsIgnoreCase(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    /** Données de la barre du haut (pseudo et TubeCoins du joueur connecté). */
    private void addTopbarData(Model model, Principal principal) {
        User current = collectionService.getUser(principal.getName());
        model.addAttribute("username", current.getUsername());
        model.addAttribute("tubecoin", current.getTubecoin());
    }
}
