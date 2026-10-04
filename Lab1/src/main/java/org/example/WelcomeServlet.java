package org.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class WelcomeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(
                "<!DOCTYPE html>"
                        + "<html lang=\"en\"><head><meta charset=\"UTF-8\">"
                        + "<title>Lab 1</title></head><body>"
                        + "<h1>Welcome to Lab 1</h1>"
                        + "<form action=\"controller\" method=\"post\">"
                        + "<label for=\"value\">Choose a page:</label>"
                        + "<select id=\"value\" name=\"value\">"
                        + "<option value=\"1\">Page 1</option>"
                        + "<option value=\"2\">Page 2</option>"
                        + "</select>"
                        + "<button type=\"submit\">Open</button>"
                        + "</form></body></html>");
    }
}