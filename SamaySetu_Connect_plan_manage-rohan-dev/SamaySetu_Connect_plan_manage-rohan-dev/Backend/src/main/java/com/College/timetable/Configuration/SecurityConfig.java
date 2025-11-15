package com.College.timetable.Configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.College.timetable.Service.TeacherService;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	
	
	private final TeacherService teacher;
	
	private final JWTRequestFilter jwtreqfilter;
	
	@Bean
	public SecurityFilterChain httpSecurityFilter(HttpSecurity http) throws Exception{
		http.cors(Customizer.withDefaults())
		.csrf(AbstractHttpConfigurer::disable)
		.authorizeHttpRequests(auth->auth.requestMatchers("/login").permitAll().
					requestMatchers("/api/teachers").hasAnyRole("ROLE_TEACHER")
					.requestMatchers("/admin/**").hasRole("ADMIN")
					.anyRequest().authenticated()
					
				)
		.sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		.addFilterBefore(jwtreqfilter,UsernamePasswordAuthenticationFilter.class);
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
		conf.setAllowedOrigins(List.of("http://localhost:5173"));
		conf.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
		conf.setAllowedHeaders(List.of("Authorization","Content-Type"));
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
