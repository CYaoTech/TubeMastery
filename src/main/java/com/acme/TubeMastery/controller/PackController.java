package com.acme.TubeMastery.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.acme.TubeMastery.service.Card;
import com.acme.TubeMastery.service.CollectionService;
import com.acme.TubeMastery.service.PackService;

@Controller
public class PackController {

    private static final Logger log = LoggerFactory.getLogger(PackController.class);

    private final PackService packService;
    private final CollectionService collectionService;

    public PackController(PackService packService, CollectionService collectionService) {
        this.packService = packService;
        this.collectionService = collectionService;
    }

    @GetMapping("/packs")
    public String packs(Model model, Principal principal) {
        PackService.PackStatus status = packService.getStatus(principal.getName());
        model.addAttribute("username", principal.getName());
        model.addAttribute("tubecoin", collectionService.getUser(principal.getName()).getTubecoin());
        model.addAttribute("packs", status.packs());
        model.addAttribute("secondsUntilNext", status.secondsUntilNext());
        return "packs";
    }

    @PostMapping("/packs/open")
    public String open(Principal principal, RedirectAttributes redirectAttributes) {
        try {
            Optional<List<Card>> cards = packService.openPack(principal.getName());
            if (cards.isPresent()) {
                redirectAttributes.addFlashAttribute("openedCards", cards.get());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Tu n'as aucun paquet à ouvrir pour le moment.");
            }
        } catch (RuntimeException e) {
            log.error("Impossible d'ouvrir le paquet", e);
            redirectAttributes.addFlashAttribute(
                "errorMessage", "Impossible d'ouvrir le paquet (scraping YouTube en échec). Réessaie.");
        }
        return "redirect:/packs";
    }
}
