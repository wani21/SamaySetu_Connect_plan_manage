package com.College.timetable.Filter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Tiered rate limiting for ALL endpoints:
 *   - Auth endpoints (/auth/**): 10 req/min (strictest — prevents brute force)
 *   - Write endpoints (POST/PUT/DELETE): 30 req/min
 *   - Read endpoints (GET): 60 req/min
 *   - Health checks (/actuator/health): unlimited (load balancer)
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    @Value("${app.rate-limit.max-requests:20}")
    private int authMaxRequests;

    @Value("${app.rate-limit.window-ms:60000}")
    private long windowMs;

    // IP + tier -> request tracking
    private final Map<String, RequestTracker> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only skip health checks — everything else is rate-limited
        String path = request.getRequestURI();
        return "/actuator/health".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        String method = request.getMethod();
        long now = System.currentTimeMillis();

        // Determine rate limit tier
        int limit;
        String tier;
        if (path.startsWith("/auth")) {
            limit = authMaxRequests; // 10 in prod, 20 in dev
            tier = "auth";
        } else if ("GET".equals(method) || "OPTIONS".equals(method) || "HEAD".equals(method)) {
            limit = authMaxRequests * 6; // 60 in prod, 120 in dev
            tier = "read";
        } else {
            limit = authMaxRequests * 3; // 30 in prod, 60 in dev
            tier = "write";
        }

        String key = clientIp + ":" + tier;

        // Periodic cleanup
        if (requestCounts.size() > 3000) {
            cleanupExpiredEntries(now);
        }

        RequestTracker tracker = requestCounts.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart > windowMs) {
                return new RequestTracker(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (tracker.count.get() > limit) {
            logger.warn("[RATE-LIMIT] {} exceeded for IP {} on {} {} (tier={}, limit={})",
                    tracker.count.get(), clientIp, method, path, tier, limit);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\", \"retryAfterMs\": " + windowMs + "}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void cleanupExpiredEntries(long now) {
        requestCounts.entrySet().removeIf(entry -> now - entry.getValue().windowStart > windowMs);
    }

    private static class RequestTracker {
        final long windowStart;
        final AtomicInteger count;

        RequestTracker(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
