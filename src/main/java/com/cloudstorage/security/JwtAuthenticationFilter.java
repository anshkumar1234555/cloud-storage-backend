package com.cloudstorage.security;

import com.cloudstorage.model.User;
import com.cloudstorage.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            UserRepository userRepository) {

        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader =
                request.getHeader("Authorization");

        // =========================
        // NO JWT
        // =========================
        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // =========================
        // INVALID JWT
        // =========================
        if (!jwtUtil.isTokenValid(token)) {

            filterChain.doFilter(request, response);
            return;
        }

        // =========================
        // EXTRACT EMAIL
        // =========================
        String email = jwtUtil.extractEmail(token);

        // =========================
        // FIND USER
        // =========================
        User user = userRepository
                .findByEmail(email)
                .orElse(null);

        if (user != null) {

            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority(
                            "ROLE_" + user.getRole()
                    );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(authority)
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            // Temporary debug
            System.out.println("========== AUTH DEBUG ==========");
            System.out.println("Request = " + request.getRequestURI());
            System.out.println("JWT Email = " + email);
            System.out.println("User ID = " + user.getId());
            System.out.println("User Email = " + user.getEmail());
            System.out.println("Role = " + user.getRole());
            System.out.println("Authenticated = " +
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication()
                            .isAuthenticated());
            System.out.println("================================");
        }

        filterChain.doFilter(request, response);
    }
}