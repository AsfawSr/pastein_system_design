package com.asfaw.pastebin.error;

import com.asfaw.pastebin.paste.PasteNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PasteNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handlePasteNotFound() {
        return "error/404";
    }
}
