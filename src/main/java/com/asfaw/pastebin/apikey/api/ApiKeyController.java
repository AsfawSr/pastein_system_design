package com.asfaw.pastebin.apikey.api;

import com.asfaw.pastebin.apikey.ApiKey;
import com.asfaw.pastebin.apikey.ApiKeyNotFoundException;
import com.asfaw.pastebin.apikey.ApiKeyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public record CreateApiKeyRequest(@NotBlank @Size(max = 50) String label) {
    }

    public record IssuedKeyResponse(Long id, String label, String key) {
    }

    public record ApiKeyResponse(Long id, String label, int rateLimitPerMinute, Instant createdAt) {
    }

    @PostMapping
    public ResponseEntity<IssuedKeyResponse> create(@Valid @RequestBody CreateApiKeyRequest request,
                                                    Authentication authentication) {
        ApiKeyService.IssuedKey issued = apiKeyService.issue(authentication.getName(), request.label());
        return ResponseEntity
                .created(URI.create("/api/keys/" + issued.id()))
                .body(new IssuedKeyResponse(issued.id(), issued.label(), issued.plaintextKey()));
    }

    @GetMapping
    public List<ApiKeyResponse> list(Authentication authentication) {
        return apiKeyService.listFor(authentication.getName()).stream()
                .map(this::toResponse)
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable Long id, Authentication authentication) {
        apiKeyService.revoke(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ProblemDetail handleNotFound(ApiKeyNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    private ApiKeyResponse toResponse(ApiKey key) {
        return new ApiKeyResponse(key.getId(), key.getLabel(), key.getRateLimitPerMinute(), key.getCreatedAt());
    }
}
