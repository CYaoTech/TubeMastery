package com.acme.TubeMastery.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.acme.TubeMastery.service.YoutubeServiceFinal;

@Controller
public class HomeController {

    private final YoutubeServiceFinal youtubeService;

    public HomeController(YoutubeServiceFinal youtubeService) {
        this.youtubeService = youtubeService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Hello World");
        model.addAttribute("mrBeast", youtubeService.getYoutuber("mrBeast"));
        model.addAttribute("cyprien", youtubeService.getYoutuber("cyprien"));
        return "index";
    }
}
