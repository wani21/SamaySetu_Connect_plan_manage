package com.College.timetable.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Structured audit logging for security-sensitive admin actions.
 * Logs to SLF4J with [AUDIT] prefix — can be filtered and shipped to SIEM.
 *
 * Usage: auditLogService.log("APPROVE_TEACHER", "teacherId=21, email=rohit@mitaoe.ac.in");
 */
@Service
public class AuditLogService {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    /**
     * Log an admin action with the current authenticated user.
     */
    public void log(String action, String details) {
        String admin = getCurrentUser();
        audit.info("[AUDIT] admin={} action={} {}", admin, action, details);
    }

    /**
     * Log an action with explicit actor (for cases where SecurityContext isn't set).
     */
    public void log(String actor, String action, String details) {
        audit.info("[AUDIT] admin={} action={} {}", actor, action, details);
    }

    private String getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "SYSTEM";
    }
}
