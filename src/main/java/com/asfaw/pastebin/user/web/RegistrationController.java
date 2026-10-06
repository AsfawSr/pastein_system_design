package com.asfaw.pastebin.user.web;

import com.asfaw.pastebin.user.UserService;
import com.asfaw.pastebin.user.UsernameTakenException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class RegistrationController {

    private final UserService userService;

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "user/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult binding,
                           RedirectAttributes redirectAttributes) {
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            binding.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (binding.hasErrors()) {
            return "user/register";
        }
        try {
            userService.register(form.getUsername(), form.getPassword());
        } catch (UsernameTakenException ex) {
            binding.rejectValue("username", "taken", "Username is already taken");
            return "user/register";
        }
        redirectAttributes.addFlashAttribute("registered", true);
        return "redirect:/login";
    }
}
