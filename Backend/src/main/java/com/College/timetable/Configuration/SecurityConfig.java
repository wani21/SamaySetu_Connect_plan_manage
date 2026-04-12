package com.College.timetable.Configuration;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.College.timetable.Filter.JWTRequestFilter;
import com.College.timetable.Filter.RateLimitFilter;
import com.College.timetable.Service.TeacherService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private static final Logger securityLogger = LoggerFactory.getLogger(SecurityConfig.class);

	private final TeacherService teacher;

	private final JWTRequestFilter jwtreqfilter;

	private final RateLimitFilter rateLimitFilter;

	@Value("${app.cors.allowed-origins:http://localhost:5173}")
	private String allowedOrigins;
	
	@Bean
	public SecurityFilterChain httpSecurityFilter(HttpSecurity http) throws Exception{
		http.cors(Customizer.withDefaults())
		// CSRF disabled for stateless JWT-based API. CSRF tokens are designed for form-based sessions.
		// Since we use JWTs which are not vulnerable to CSRF in the same way, disabling is appropriate.
		.csrf(AbstractHttpConfigurer::disable)
		.authorizeHttpRequests(auth->auth
					// Public endpoints (no authentication required)
					.requestMatchers("/auth/**", "/api/timetable/manual", "/actuator/health").permitAll()
					// Public read-only for any authenticated user
					.requestMatchers("/api/academic-years/**").authenticated()
					.requestMatchers("/api/time-slots/**").authenticated()
					.requestMatchers("/api/timetable/**").authenticated()
					// Staff self-service (any authenticated role)
					.requestMatchers("/api/staff/**").authenticated()
					// Teacher endpoints (TEACHER + all higher roles)
					.requestMatchers("/api/teachers/profile").hasAnyRole("TEACHER", "ADMIN", "HOD", "TIMETABLE_COORDINATOR")
					.requestMatchers("/api/teachers/pending-approvals").hasAnyRole("ADMIN", "HOD")
					.requestMatchers("/api/teachers/*/approve", "/api/teachers/*/reject").hasAnyRole("ADMIN", "HOD")
					.requestMatchers("/api/teachers/**").hasAnyRole("TEACHER", "ADMIN", "HOD", "TIMETABLE_COORDINATOR")
					// Admin panel — ADMIN has full access, HOD and COORDINATOR have partial
					.requestMatchers("/admin/upload-staff", "/admin/create-staff", "/admin/update-staff/**", "/admin/download-staff-template").hasAnyRole("ADMIN", "HOD")
					.requestMatchers("/admin/upload-courses", "/admin/download-courses-template").hasAnyRole("ADMIN", "HOD")
					.requestMatchers("/admin/api/**").hasAnyRole("ADMIN", "HOD", "TIMETABLE_COORDINATOR")
					.requestMatchers("/admin/**").hasAnyRole("ADMIN", "HOD", "TIMETABLE_COORDINATOR")
					.anyRequest().authenticated()
				)
		.sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		// Add security headers to prevent common attacks
		.headers(headers -> headers
			.frameOptions(frame -> frame.deny())
			.contentTypeOptions(Customizer.withDefaults())
			.httpStrictTransportSecurity(hsts -> hsts
				.includeSubDomains(true)
				.maxAgeInSeconds(31536000) // 1 year
			)
			// Cache-Control — prevent browsers from caching API responses with sensitive data
			.cacheControl(Customizer.withDefaults())
			// Content Security Policy — restricts which resources the browser may load
			.contentSecurityPolicy(csp -> csp
				.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'")
			)
		)
		// Rate limiting runs first to block brute-force before any auth processing
		.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
		.addFilterBefore(jwtreqfilter, UsernamePasswordAuthenticationFilter.class)
		// Diagnostic handlers to expose WHY a 401/403 was returned.
		// IMPORTANT: response body must NOT contain the words "token",
		// "unauthorized" or "forbidden" because the frontend axios interceptor
		// (api.ts) does substring matching on those words and would trigger a
		// logout-redirect loop. All real diagnostics go to the SERVER terminal.
		.exceptionHandling(ex -> ex
			.authenticationEntryPoint((req, res, authEx) -> {
				securityLogger.warn("[SEC] 401 on {} {} - reason: {}",
						req.getMethod(), req.getRequestURI(), authEx.getMessage());
				res.setStatus(401);
				res.setContentType("application/json");
				res.getWriter().write("{\"status\":401,\"error\":\"AuthRequired\"}");
			})
			.accessDeniedHandler((req, res, deniedEx) -> {
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				String principal = auth != null ? String.valueOf(auth.getPrincipal()) : "null";
				String authorities = auth != null ? String.valueOf(auth.getAuthorities()) : "null";
				securityLogger.warn("[SEC] 403 on {} {} - principal={} - authorities={} - reason: {}",
						req.getMethod(), req.getRequestURI(), principal, authorities, deniedEx.getMessage());
				res.setStatus(403);
				res.setContentType("application/json");
				res.getWriter().write("{\"status\":403,\"error\":\"AccessDenied\"}");
			})
		);

		securityLogger.info("Security configuration initialized with allowed origins: {}", allowedOrigins);
		return http.build();
	}

	@Bean
	public CorsFilter corsFilter() {
		return new CorsFilter(corsConfigurationSource());
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	private UrlBasedCorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration conf=new CorsConfiguration();
		// Use configurable origins from application.properties instead of wildcard
		List<String> origins = Arrays.asList(allowedOrigins.split(","));
		conf.setAllowedOrigins(origins.stream().map(String::trim).toList());
		// Restrict allowed methods to what's actually needed
		conf.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
		// Restrict headers instead of allowing all
		conf.setAllowedHeaders(List.of("Content-Type", "Authorization", "Accept", "X-Requested-With"));
		conf.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource src=new UrlBasedCorsConfigurationSource();
		src.registerCorsConfiguration("/**", conf);
		return src;
	}
	
	@Bean
	public AuthenticationManager authManager() {
	    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
	    authProvider.setUserDetailsService(teacher);
	    authProvider.setPasswordEncoder(passwordEncoder());
	    return new ProviderManager(authProvider);
	}
	
}
