package com.asfaw.pastebin.paste.web;

import com.asfaw.pastebin.paste.Paste;
import com.asfaw.pastebin.paste.PasteNotFoundException;
import com.asfaw.pastebin.paste.PasteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

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
        Paste paste = service.create(form.getTitle(), form.getContent());
        return "redirect:/p/" + paste.getId();
    }

    @GetMapping("/p/{id}")
    public String view(@PathVariable String id, Model model) {
        Paste paste = service.find(id)
                .orElseThrow(() -> new PasteNotFoundException(id));
        model.addAttribute("paste", paste);
        return "paste/view";
    }
}
