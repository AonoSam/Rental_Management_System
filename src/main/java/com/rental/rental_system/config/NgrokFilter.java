package com.rental.rental_system.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class NgrokFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse res = (HttpServletResponse) response;

        // Allow ngrok to pass through without browser warning
        res.setHeader("ngrok-skip-browser-warning", "true");

        chain.doFilter(request, response);
    }
}