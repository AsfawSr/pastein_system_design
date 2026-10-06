package com.asfaw.pastebin.paste.api;

import com.asfaw.pastebin.paste.Paste;
import com.asfaw.pastebin.paste.PasteService;
import com.asfaw.pastebin.paste.ViewOutcome;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/pastes")
@RequiredArgsConstructor
public class PasteApiController {

    private final PasteService service;

    @PostMapping
    public ResponseEntity<PasteResponse> create(@Valid @RequestBody CreatePasteRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        Paste paste = service.create(request.toCommand(), jwt == null ? null : jwt.getSubject());
        return ResponseEntity
                .created(URI.create("/api/pastes/" + paste.getId()))
                .body(PasteResponse.from(paste, false));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id,
                                 @RequestHeader(name = "X-Paste-Password", required = false) String password) {
        return switch (service.view(id, password)) {
            case ViewOutcome.Viewed viewed ->
                    ResponseEntity.ok(PasteResponse.from(viewed.paste(), viewed.burned()));
            case ViewOutcome.PasswordRequired required -> {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                        required.wrongAttempt() ? "Wrong password" : "Password required");
                problem.setProperty("passwordHeader", "X-Paste-Password");
                yield ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
            }
        };
    }
}
