package com.javaguy.nhxserver.service.security.jwt;

import com.javaguy.nhxserver.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${nhx.app.jwtCookieName}")
    private String jwtCookieName;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        logger.debug("Processing JWT authentication for: {}", request.getServletPath());

        try {
            String jwt = parseJwt(request);
            logger.debug("Extracted JWT token: {}", jwt != null ? jwt.substring(0, Math.min(jwt.length(), 20)) + "..." : "null");

            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUsernameFromToken(jwt);
                logger.debug("Username from JWT: {}", username);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtUtils.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        logger.debug("Successfully authenticated user: {}", username);
                    } else {
                        logger.debug("JWT token validation failed for user: {}", username);
                    }
                }
            } else {
                logger.debug("No valid JWT token found");
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header or cookies
     */
    private String parseJwt(HttpServletRequest request) {
        // First try Authorization header
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        logger.debug("Authorization header: {}", authHeader != null ? "Bearer ***" : "null");

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        // Then try cookies
        logger.debug("Looking for JWT cookie: {}", jwtCookieName);
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                logger.debug("Found cookie: {} = {}", cookie.getName(), cookie.getValue().substring(0, Math.min(cookie.getValue().length(), 10)) + "...");
                if (cookie.getName().equals(jwtCookieName)) {
                    logger.debug("JWT cookie found!");
                    return cookie.getValue();
                }
            }
        } else {
            logger.debug("No cookies found in request");
        }

        return null;
    }
}