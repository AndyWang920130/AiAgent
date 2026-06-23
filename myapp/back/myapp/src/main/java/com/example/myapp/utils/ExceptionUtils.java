package com.example.myapp.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class ExceptionUtils {

    public static ProblemDetail buildProblemDetail(URI type, String defaultMessage) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, defaultMessage);
        pd.setTitle("Bad Request");
        pd.setType(type);
        return pd;
    }

    public static ProblemDetail buildProblemDetail(URI type, String defaultMessage, String entityName, String errorKey) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, defaultMessage);
        pd.setTitle("Bad Request");
        pd.setType(type);
        pd.setProperty("entityName", entityName);
        pd.setProperty("errorKey", errorKey);
        return pd;
    }
}
