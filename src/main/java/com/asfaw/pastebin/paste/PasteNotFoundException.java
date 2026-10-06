package com.asfaw.pastebin.paste;

public class PasteNotFoundException extends RuntimeException {

    public PasteNotFoundException(String id) {
        super("Paste not found: " + id);
    }
}
