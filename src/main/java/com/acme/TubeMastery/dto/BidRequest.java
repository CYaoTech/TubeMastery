package com.acme.TubeMastery.dto;

import jakarta.validation.constraints.Min;

public record BidRequest(@Min(value = 1, message = "Mise invalide") int amount) {}
