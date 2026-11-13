package com.acme.TubeMastery.controller;

import com.acme.TubeMastery.dto.AuctionDto;
import com.acme.TubeMastery.dto.BidRequest;
import com.acme.TubeMastery.dto.CreateAuctionRequest;
import com.acme.TubeMastery.dto.UpdateAuctionRequest;
import com.acme.TubeMastery.service.AuctionException;
import com.acme.TubeMastery.service.AuctionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/** API REST CRUD des enchères (utilisée aussi par le polling JS de la page /auctions). */
@RestController
@RequestMapping("/api/v1/auctions")
public class AuctionRestController {

    private final AuctionService auctionService;

    public AuctionRestController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @GetMapping
    public List<AuctionDto> list() {
        return auctionService.listActive();
    }

    @GetMapping("/{id}")
    public AuctionDto get(@PathVariable Long id) {
        return auctionService.get(id);
    }

    @PostMapping
    public ResponseEntity<AuctionDto> create(@Valid @RequestBody CreateAuctionRequest req, Principal principal) {
        AuctionDto dto = auctionService.create(principal.getName(), req.cardId(), req.startPrice(), req.hours());
        return ResponseEntity.created(URI.create("/api/v1/auctions/" + dto.id())).body(dto);
    }

    @PutMapping("/{id}")
    public AuctionDto update(@PathVariable Long id, @Valid @RequestBody UpdateAuctionRequest req, Principal principal) {
        return auctionService.update(principal.getName(), id, req.startPrice(), req.hours());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id, Principal principal) {
        auctionService.cancel(principal.getName(), id);
    }

    @PostMapping("/{id}/bids")
    public AuctionDto bid(@PathVariable Long id, @Valid @RequestBody BidRequest req, Principal principal) {
        return auctionService.bid(principal.getName(), id, req.amount());
    }

    @ExceptionHandler(AuctionException.class)
    public ResponseEntity<Map<String, String>> onAuctionError(AuctionException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> onInvalid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .findFirst().map(f -> f.getDefaultMessage()).orElse("Requête invalide");
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }
}
