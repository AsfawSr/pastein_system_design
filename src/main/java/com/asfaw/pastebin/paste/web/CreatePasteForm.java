package com.asfaw.pastebin.paste.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePasteForm {

    @Size(max = 120, message = "Title must be at most 120 characters")
    private String title;

    @NotBlank(message = "Content must not be empty")
    @Size(max = 100_000, message = "Content must be at most 100,000 characters")
    private String content;

    @NotNull
    private ExpiryOption expiry = ExpiryOption.NEVER;

    private boolean burnAfterRead;
}
