package com.asfaw.pastebin.paste.api;

import com.asfaw.pastebin.paste.PasteNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PasteApiController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(PasteNotFoundException.class)
    public ProblemDetail handlePasteNotFound(PasteNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
