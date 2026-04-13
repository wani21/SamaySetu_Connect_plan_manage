package com.College.timetable.Filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.College.timetable.Service.TeacherService;
import com.College.timetable.Util.JWTUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JWTRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTRequestFilter.class);

    private final TeacherService teacher;
    private final JWTUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        String method = request.getMethod();

        // Skip JWT processing for auth endpoints
        if (path.startsWith("/auth")) {
            logger.debug("[JWT] Skipping auth path: {} {}", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        logger.debug("[JWT] Processing {} {}", method, path);

        final String authHeader = request.getHeader("Authorization");
        String email = null;
        String jwt = null;

        if (authHeader == null) {
            logger.debug("[JWT] No Authorization header on {} {}", method, path);
        } else if (!authHeader.startsWith("Bearer ")) {
            logger.warn("[JWT] Malformed Authorization header on {} {}", method, path);
        } else {
            jwt = authHeader.substring(7);
            // SECURITY: Never log token content — only confirm it was received
            logger.debug("[JWT] Bearer token received for {} {}", method, path);
            try {
                email = jwtUtil.extractUsername(jwt);
                logger.debug("[JWT] Token resolved to user: {}", email);
            } catch (Exception e) {
                logger.warn("[JWT] Token parsing failed on {} {} - {}", method, path, e.getMessage());
                filterChain.doFilter(request, response);
                return;
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userdet = teacher.loadUserByUsername(email);
                boolean valid = jwtUtil.validateToken(jwt, userdet);

                if (valid) {
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userdet, null, userdet.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("[JWT] Authenticated {} with roles {} for {} {}",
                        email, userdet.getAuthorities(), method, path);
                } else {
                    logger.warn("[JWT] Invalid token for user {} on {} {}", email, method, path);
                }
            } catch (Exception e) {
                logger.warn("[JWT] Auth failed for {} on {} {} - {}", email, method, path, e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
