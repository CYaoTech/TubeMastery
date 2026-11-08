package com.acme.TubeMastery.controller;

import com.acme.TubeMastery.dto.RegisterRequest;
import com.acme.TubeMastery.service.UserService;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {

        model.addAttribute(
            "registerRequest",
            new RegisterRequest()
        );

        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("registerRequest")
            RegisterRequest request,

            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "register";
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {

            result.rejectValue(
                "confirmPassword",
                "password.mismatch",
                "Les mots de passe ne correspondent pas."
            );

            return "register";
        }

        try {

            userService.registerUser(request);

            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Inscription réussie ! Connecte-toi."
            );

            return "redirect:/auth/login";

        } catch (IllegalArgumentException e) {

            model.addAttribute("errorMessage", e.getMessage());

            return "register";

        } catch (DataIntegrityViolationException e) {

            model.addAttribute(
                "errorMessage",
                "Ce pseudo ou cette adresse e-mail est déjà utilisé."
            );

            return "register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }
}