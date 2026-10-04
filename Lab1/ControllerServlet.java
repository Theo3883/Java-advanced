package org.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

public class ControllerServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        process(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        process(request, response);
    }

    private void process(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logRequest(request);

        String value = request.getParameter("value");
        if (!"1".equals(value) && !"2".equals(value)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "The value parameter must be 1 or 2.");
            return;
        }

        if ("text".equals(request.getParameter("response"))) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(value);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/page" + value + ".html");
    }

    private void logRequest(HttpServletRequest request) {
        StringJoiner languages = new StringJoiner(", ");
        Enumeration<Locale> locales = request.getLocales();
        while (locales.hasMoreElements()) {
            languages.add(locales.nextElement().toLanguageTag());
        }

        StringJoiner parameters = new StringJoiner(", ");
        for (Map.Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
            parameters.add(parameter.getKey() + "=" + String.join(",", parameter.getValue()));
        }

        System.out.println("====== REQUEST LOG ======\n"
                + "Request method=" + request.getMethod()
                + ", clientIp=" + request.getRemoteAddr()
                + ", userAgent=" + request.getHeader("User-Agent")
                + ", languages=" + languages
                + ", parameters=" + parameters
                + "\n=========================");
    }
}