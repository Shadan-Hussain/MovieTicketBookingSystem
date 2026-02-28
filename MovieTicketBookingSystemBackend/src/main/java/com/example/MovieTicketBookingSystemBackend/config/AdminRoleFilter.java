package com.example.MovieTicketBookingSystemBackend.config;

import com.example.MovieTicketBookingSystemBackend.model.User;
import com.example.MovieTicketBookingSystemBackend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * For /admin/** requests, ensures the authenticated user has role ADMIN. Must run after JwtAuthFilter.
 */
@Component
@Order(2)
public class AdminRoleFilter extends OncePerRequestFilter {

    private static final String ADMIN_PATH_PREFIX = "/admin/";

    private final UserRepository userRepository;

    public AdminRoleFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith(ADMIN_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        Long userId = (Long) request.getAttribute(JwtAuthFilter.REQUEST_ATTR_USER_ID);
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }
        if (!userRepository.findById(userId).map(u -> User.ROLE_ADMIN.equals(u.getRole())).orElse(false)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Admin role required\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
