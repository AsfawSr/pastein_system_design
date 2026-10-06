package com.asfaw.pastebin.paste.web;

import com.asfaw.pastebin.paste.CreatePasteCommand;
import com.asfaw.pastebin.paste.Paste;
import com.asfaw.pastebin.paste.PasteService;
import com.asfaw.pastebin.paste.ViewOutcome;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PasteController {

    static final List<String> LANGUAGES = List.of(
            "plaintext", "bash", "c", "cpp", "csharp", "css", "go", "html", "java",
            "javascript", "json", "kotlin", "python", "rust", "sql", "typescript", "xml", "yaml");

    private final PasteService service;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("form", new CreatePasteForm());
        model.addAttribute("languages", LANGUAGES);
        return "index";
    }

    @PostMapping("/paste")
    public String create(@Valid @ModelAttribute("form") CreatePasteForm form, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("languages", LANGUAGES);
            return "index";
        }
        Paste paste = service.create(new CreatePasteCommand(form.getTitle(), form.getContent(),
                form.getExpiry().getDuration(), form.isBurnAfterRead(), form.getPassword(),
                form.getVisibility(), form.getLanguage()));
        return "redirect:/p/" + paste.getId();
    }

    @GetMapping("/public")
    public String publicList(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Paste> pastes = service.listPublic(Math.max(page, 0), 20);
        model.addAttribute("pastes", pastes);
        return "paste/list";
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
