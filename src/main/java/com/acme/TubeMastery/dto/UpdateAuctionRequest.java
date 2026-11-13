package com.acme.TubeMastery.dto;

import jakarta.validation.constraints.Min;

public record UpdateAuctionRequest(
        @Min(value = 1, message = "Le prix de départ est de 1 TubeCoin minimum") int startPrice,
        @Min(value = 1, message = "Durée invalide") int hours) {}
