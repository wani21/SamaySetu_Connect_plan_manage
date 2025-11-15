package com.College.timetable.Filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
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
public class JWTRequestFilter extends OncePerRequestFilter{

	private final TeacherService teacher;
	

	private final JWTUtil jwtUtil;


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		final String authHeader=request.getHeader("Authorization");
		String email=null;
		String jwt=null;
		
		if(authHeader!=null && authHeader.startsWith("Bearer ")) {
			jwt=authHeader.substring(7);
			email=jwtUtil.extractUsername(jwt);
		}
		
		if(email!=null && SecurityContextHolder.getContext().getAuthentication()==null) {
			 UserDetails userdet=teacher.loadUserByUsername(email); 
			 if(jwtUtil.validateToken(jwt, userdet)) {
				 UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(userdet, null, userdet.getAuthorities());
				 authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				 SecurityContextHolder.getContext().setAuthentication(authenticationToken);
			 }
		}
		filterChain.doFilter(request, response);
	} 	
	
	
	
	
}
