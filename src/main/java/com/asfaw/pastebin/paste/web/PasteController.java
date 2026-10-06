package com.asfaw.pastebin.paste.web;

import com.asfaw.pastebin.paste.Paste;
import com.asfaw.pastebin.paste.PasteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

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
    public String create(@ModelAttribute("form") CreatePasteForm form) {
        Paste paste = service.create(form.getTitle(), form.getContent());
        return "redirect:/p/" + paste.getId();
    }

    @GetMapping("/p/{id}")
    public String view(@PathVariable String id, Model model) {
        Paste paste = service.find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("paste", paste);
        return "paste/view";
    }
}
