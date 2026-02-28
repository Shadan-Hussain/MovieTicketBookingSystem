package com.example.MovieTicketBookingSystemBackend.config;

import com.example.MovieTicketBookingSystemBackend.repository.UserRepository;
import com.example.MovieTicketBookingSystemBackend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Requires JWT for all requests except /auth/**, /webhook/**, /redirect/**.
 * /admin/** also requires JWT and is then checked for ADMIN role by AdminRoleFilter.
 */
@Component
@Order(1)
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    static final String REQUEST_ATTR_USER_ID = "userId";

    private static final List<String> PATH_PREFIXES_NO_AUTH = List.of(
            "/auth/",
            "/webhook/",
            "/redirect/"
    );

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path == null) {
            path = "";
        }
        if (pathStartsWithAny(path, PATH_PREFIXES_NO_AUTH)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (!jwtService.validate(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
            return;
        }
        Long userId = jwtService.getUserIdFromToken(token);
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid token\"}");
            return;
        }
        if (!userRepository.existsById(userId)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"User not found\"}");
            return;
        }
        request.setAttribute(REQUEST_ATTR_USER_ID, userId);
        filterChain.doFilter(request, response);
    }

    private static boolean pathStartsWithAny(String path, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
