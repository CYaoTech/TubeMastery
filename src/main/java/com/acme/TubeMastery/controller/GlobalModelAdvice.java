package com.acme.TubeMastery.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.acme.TubeMastery.service.ChannelPoolService;

/** Données communes à toutes les pages (header). */
@ControllerAdvice
public class GlobalModelAdvice {

    private final ChannelPoolService poolService;

    public GlobalModelAdvice(ChannelPoolService poolService) {
        this.poolService = poolService;
    }

    /** Nombre de youtubeurs que l'on peut obtenir dans un paquet (vivier de chaînes non mortes). */
    @ModelAttribute("availableYoutubers")
    public long availableYoutubers() {
        return poolService.size();
    }
}
