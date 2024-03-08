package ch.uzh.ifi.hase.soprafs24.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@RestController
public class ServerErrorController implements ErrorController {
    private static final String PATH = "/error";

    @RequestMapping(value = PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public String handleError(HttpServletRequest request) {
        String statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE).toString();
        String statusMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE).toString();
        return String.format("{\"status\":%s, \"error\":\"%s\"}", statusCode, statusMessage);
        //{"timestamp":"2024-03-05T04:30:11.658+00:00","status":403,"error":"Forbidden","message":"Access Denied","path":"/users/1"}
    }

    @Override
    public String getErrorPath() {
        return PATH;
    }
}
