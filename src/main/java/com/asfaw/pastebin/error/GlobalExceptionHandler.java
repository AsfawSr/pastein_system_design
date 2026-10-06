package com.asfaw.pastebin.error;

import com.asfaw.pastebin.paste.PasteNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

// HTML error pages for the MVC controllers only; the API package has its own ProblemDetail advice
@ControllerAdvice(basePackages = "com.asfaw.pastebin.paste.web")
public class GlobalExceptionHandler {

    @ExceptionHandler(PasteNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handlePasteNotFound() {
        return "error/404";
    }
}
