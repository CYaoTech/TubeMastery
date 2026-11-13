package com.acme.TubeMastery.controller;

import com.acme.TubeMastery.dto.AuctionDto;
import com.acme.TubeMastery.model.User;
import com.acme.TubeMastery.service.AuctionException;
import com.acme.TubeMastery.service.AuctionService;
import com.acme.TubeMastery.service.CollectionService;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/** Pages MVC des enchères ; les mises/créations passent par fetch depuis le formulaire (jeton CSRF inclus). */
@Controller
public class AuctionController {

    private final AuctionService auctionService;
    private final CollectionService collectionService;

    public AuctionController(AuctionService auctionService, CollectionService collectionService) {
        this.auctionService = auctionService;
        this.collectionService = collectionService;
    }

    @GetMapping("/auctions")
    public String auctions(Model model, Principal principal) {
        User user = collectionService.getUser(principal.getName());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("tubecoin", user.getTubecoin());
        model.addAttribute("auctions", auctionService.listActive());
        return "auctions";
    }

    @PostMapping("/auctions/create")
    @ResponseBody
    public AuctionDto create(@RequestParam Long cardId, @RequestParam int startPrice,
                             @RequestParam int hours, Principal principal) {
        return auctionService.create(principal.getName(), cardId, startPrice, hours);
    }

    @PostMapping("/auctions/{id}/bid")
    @ResponseBody
    public Map<String, Object> bid(@PathVariable Long id, @RequestParam int amount, Principal principal) {
        AuctionDto dto = auctionService.bid(principal.getName(), id, amount);
        int coins = collectionService.getUser(principal.getName()).getTubecoin();
        return Map.of("tubecoin", coins, "auction", dto);
    }

    @ExceptionHandler(AuctionException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> onAuctionError(AuctionException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getMessage()));
    }
}
