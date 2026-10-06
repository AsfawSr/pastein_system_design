package com.asfaw.pastebin.paste.web;

import com.asfaw.pastebin.paste.Paste;
import com.asfaw.pastebin.paste.PasteService;
import com.asfaw.pastebin.paste.ViewOutcome;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PasteController {

    private final PasteService service;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("form", new CreatePasteForm());
        return "index";
    }

    @PostMapping("/paste")
    public String create(@Valid @ModelAttribute("form") CreatePasteForm form, BindingResult binding) {
        if (binding.hasErrors()) {
            return "index";
        }
        Paste paste = service.create(form.getTitle(), form.getContent(),
                form.getExpiry().getDuration(), form.isBurnAfterRead(), form.getPassword());
        return "redirect:/p/" + paste.getId();
    }

    @GetMapping("/p/{id}")
    public String view(@PathVariable String id, Model model) {
        return render(service.view(id, null), id, model);
    }

    @PostMapping("/p/{id}/unlock")
    public String unlock(@PathVariable String id, @RequestParam String password, Model model) {
        return render(service.view(id, password), id, model);
    }

    private String render(ViewOutcome outcome, String id, Model model) {
        return switch (outcome) {
            case ViewOutcome.Viewed viewed -> {
                model.addAttribute("paste", viewed.paste());
                model.addAttribute("burned", viewed.burned());
                yield "paste/view";
            }
            case ViewOutcome.PasswordRequired required -> {
                model.addAttribute("pasteId", id);
                model.addAttribute("wrongAttempt", required.wrongAttempt());
                yield "paste/password";
            }
        };
    }
}
