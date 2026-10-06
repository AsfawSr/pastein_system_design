package com.asfaw.pastebin.paste.api;

import com.asfaw.pastebin.paste.CreatePasteCommand;
import com.asfaw.pastebin.paste.PasteVisibility;
import com.asfaw.pastebin.paste.web.ExpiryOption;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePasteRequest(
        @Size(max = 120) String title,
        @NotBlank @Size(max = 100_000) String content,
        ExpiryOption expiry,
        Boolean burnAfterRead,
        @Size(max = 72) String password,
        PasteVisibility visibility) {

    public CreatePasteCommand toCommand() {
        return new CreatePasteCommand(
                title,
                content,
                expiry == null ? null : expiry.getDuration(),
                Boolean.TRUE.equals(burnAfterRead),
                password,
                visibility == null ? PasteVisibility.UNLISTED : visibility);
    }
}
