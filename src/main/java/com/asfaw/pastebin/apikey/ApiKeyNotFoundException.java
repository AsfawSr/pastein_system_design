package com.asfaw.pastebin.apikey;

public class ApiKeyNotFoundException extends RuntimeException {

    public ApiKeyNotFoundException(Long id) {
        super("API key not found: " + id);
    }
}
