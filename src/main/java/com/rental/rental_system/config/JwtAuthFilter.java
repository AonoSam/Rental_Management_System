package com.rental.rental_system.config;

import com.rental.rental_system.auth.JwtService;
import com.rental.rental_system.session.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService             jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final SessionService         sessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String header = request.getHeader("Authorization");

            // No token — just continue, let Security decide
            if (header == null || !header.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token    = header.substring(7);
            String username = null;

            // Safely extract username — don't throw if token is bad
            try {
                username = jwtService.extractUsername(token);
            } catch (Exception e) {
                filterChain.doFilter(request, response);
                return;
            }

            // Only set auth if not already set
            if (username != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails =
                            userDetailsService.loadUserByUsername(username);

                    if (jwtService.isTokenValid(token, userDetails)) {

                        // ── Force logout check ──────────────────────
                        if (sessionService.isForceLoggedOut(token)) {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"message\":\"Your session was terminated by the administrator\"}");
                            return;
                        }

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null,
                                        userDetails.getAuthorities());
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request));
                        SecurityContextHolder.getContext()
                                .setAuthentication(authToken);
                    }

                } catch (Exception e) {
                    SecurityContextHolder.clearContext();
                }
            }

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}