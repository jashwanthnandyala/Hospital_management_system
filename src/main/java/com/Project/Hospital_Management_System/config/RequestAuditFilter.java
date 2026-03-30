package com.Project.Hospital_Management_System.config;

import com.Project.Hospital_Management_System.entity.User;
import com.Project.Hospital_Management_System.repository.UserRepository;
import jakarta.servlet.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class RequestAuditFilter implements Filter {

    private final JdbcTemplate jdbc;
    private final UserRepository userRepo;

    public RequestAuditFilter(JdbcTemplate jdbc, UserRepository userRepo) {
        this.jdbc = jdbc;
        this.userRepo = userRepo;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            Optional<User> userOpt = userRepo.findByUsername(auth.getName());
            userOpt.ifPresent(u -> jdbc.execute("SET @app_user_id := " + u.getId()));
        }

        chain.doFilter(request, response);
    }
}