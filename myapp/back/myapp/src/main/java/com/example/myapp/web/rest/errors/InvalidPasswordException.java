package com.example.myapp.web.rest.errors;

import com.example.myapp.utils.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;

@SuppressWarnings("java:S110") // Inheritance tree of classes should not be too deep
public class InvalidPasswordException extends ErrorResponseException {

    private static final long serialVersionUID = 1L;

    public InvalidPasswordException() {
        super(HttpStatus.BAD_REQUEST,
                ExceptionUtils
                        .buildProblemDetail(ErrorConstants.INVALID_PASSWORD_TYPE,
                                "Incorrect password"),
                null
        );

    }

}
