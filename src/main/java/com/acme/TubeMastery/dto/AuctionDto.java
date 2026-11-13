package com.acme.TubeMastery.dto;

import com.acme.TubeMastery.model.Auction;

public record AuctionDto(
        Long id,
        Long cardId,
        String cardName,
        String thumbnailUrl,
        String rarityCode,
        Long subscriberCount,
        String seller,
        int startPrice,
        int currentPrice,
        String highestBidder,
        int bidCount,
        long endsAt,
        long serverNow,
        String status) {

    public static AuctionDto from(Auction a) {
        return new AuctionDto(
            a.getId(),
            a.getCard().getId(),
            a.getCard().getName(),
            a.getCard().getThumbnailUrl(),
            a.getCard().getRarityCode(),
            a.getCard().getSubscriberCount(),
            a.getSeller().getUsername(),
            a.getStartPrice(),
            a.getCurrentPrice(),
            a.getHighestBidder() == null ? null : a.getHighestBidder().getUsername(),
            a.getBids().size(),
            a.getEndsAt().toEpochMilli(),
            System.currentTimeMillis(),
            a.getStatus().name());
    }
}
