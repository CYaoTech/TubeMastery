package com.acme.TubeMastery.controller;

import java.security.Principal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.acme.TubeMastery.model.User;
import com.acme.TubeMastery.service.Card;
import com.acme.TubeMastery.service.CollectionService;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final CollectionService collectionService;

    public HomeController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    /** La racine renvoie vers la collection (ou vers le login si non connecté). */
    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String collection(Model model, Principal principal) {
        User user = collectionService.getUser(principal.getName());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("tubecoin", user.getTubecoin());
        model.addAttribute("cards", user.getCards());
        model.addAttribute("totalCards", collectionService.totalCards());
        return "home";
    }

    @PostMapping("/home/draw")
    public String draw(Principal principal, RedirectAttributes redirectAttributes) {
        try {
            Optional<Card> card = collectionService.drawCard(principal.getName());
            if (card.isPresent()) {
                redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Nouvelle carte obtenue : " + card.get().getName() + " !"
                );
                redirectAttributes.addFlashAttribute("newCardId", card.get().getId());
            } else {
                redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Bravo, ta collection est complète !"
                );
            }
        } catch (RuntimeException e) {
            log.error("Impossible de tirer une carte", e);
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Impossible de récupérer une carte pour le moment (scraping YouTube en échec)."
            );
        }
        return "redirect:/home";
    }
}
