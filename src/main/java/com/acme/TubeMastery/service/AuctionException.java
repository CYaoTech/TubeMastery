package com.acme.TubeMastery.service;

import org.springframework.http.HttpStatus;

public class AuctionException extends RuntimeException {

    private final HttpStatus status;

    public AuctionException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
