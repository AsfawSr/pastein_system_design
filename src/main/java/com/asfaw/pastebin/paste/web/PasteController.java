package com.asfaw.pastebin.paste.web;

import com.asfaw.pastebin.paste.CreatePasteCommand;
import com.asfaw.pastebin.paste.Paste;
import com.asfaw.pastebin.paste.PasteService;
import com.asfaw.pastebin.paste.ViewOutcome;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PasteController {

    static final List<String> LANGUAGES = List.of(
            "plaintext", "bash", "c", "cpp", "csharp", "css", "go", "html", "java",
            "javascript", "json", "kotlin", "python", "rust", "sql", "typescript", "xml", "yaml");

    private final PasteService service;

    @GetMapping("/")
    public String index(Model model, Principal principal) {
        model.addAttribute("form", new CreatePasteForm());
        model.addAttribute("languages", LANGUAGES);
        model.addAttribute("username", principal == null ? null : principal.getName());
        return "index";
    }

    @PostMapping("/paste")
    public String create(@Valid @ModelAttribute("form") CreatePasteForm form, BindingResult binding,
                         Model model, Principal principal) {
        if (binding.hasErrors()) {
            model.addAttribute("languages", LANGUAGES);
            return "index";
        }
        Paste paste = service.create(new CreatePasteCommand(form.getTitle(), form.getContent(),
                form.getExpiry().getDuration(), form.isBurnAfterRead(), form.getPassword(),
                form.getVisibility(), form.getLanguage()), principal == null ? null : principal.getName());
        return "redirect:/p/" + paste.getId();
    }

    @GetMapping("/mine")
    public String myPastes(@RequestParam(defaultValue = "0") int page, Model model, Principal principal) {
        Page<Paste> pastes = service.listOwnedBy(principal.getName(), Math.max(page, 0), 20);
        model.addAttribute("pastes", pastes);
        return "paste/mine";
    }

    @GetMapping("/public")
    public String publicList(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Paste> pastes = service.listPublic(Math.max(page, 0), 20);
        model.addAttribute("pastes", pastes);
        return "paste/list";
    }

    @GetMapping("/p/{id}")
    public String view(@PathVariable String id, Model model, Principal principal) {
        return render(service.view(id, null), id, model, principal);
    }

    @PostMapping("/p/{id}/unlock")
    public String unlock(@PathVariable String id, @RequestParam String password, Model model, Principal principal) {
        return render(service.view(id, password), id, model, principal);
    }

    // same view semantics as the HTML page: counts views and burns burn-after-read pastes
    @GetMapping(value = "/p/{id}/raw", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public ResponseEntity<String> raw(@PathVariable String id) {
        return switch (service.view(id, null)) {
            case ViewOutcome.Viewed viewed -> ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(viewed.paste().getContent());
            case ViewOutcome.PasswordRequired ignored -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("This paste is password-protected.");
        };
    }

    @GetMapping("/p/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String editForm(@PathVariable String id, Model model, Principal principal) {
        Paste paste = service.getOwned(id, principal.getName());
        EditPasteForm form = new EditPasteForm();
        form.setTitle(paste.getTitle());
        form.setContent(paste.getContent());
        form.setLanguage(paste.getLanguage());
        model.addAttribute("form", form);
        model.addAttribute("pasteId", id);
        model.addAttribute("languages", LANGUAGES);
        return "paste/edit";
    }

    @PostMapping("/p/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String edit(@PathVariable String id, @Valid @ModelAttribute("form") EditPasteForm form,
                       BindingResult binding, Model model, Principal principal) {
        if (binding.hasErrors()) {
            model.addAttribute("pasteId", id);
            model.addAttribute("languages", LANGUAGES);
            return "paste/edit";
        }
        service.updateOwned(id, principal.getName(), form.getTitle(), form.getContent(), form.getLanguage());
        return "redirect:/p/" + id;
    }

    @PostMapping("/p/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    public String delete(@PathVariable String id, Principal principal) {
        service.deleteOwned(id, principal.getName());
        return "redirect:/mine";
    }

    private String render(ViewOutcome outcome, String id, Model model, Principal principal) {
        return switch (outcome) {
            case ViewOutcome.Viewed viewed -> {
                model.addAttribute("paste", viewed.paste());
                model.addAttribute("burned", viewed.burned());
                model.addAttribute("canEdit", !viewed.burned()
                        && viewed.ownerUsername() != null
                        && principal != null
                        && viewed.ownerUsername().equals(principal.getName()));
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
