package com.example.ureka02.global.auth;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.ureka02.global.auth.jwt.JwtTokenUtil;
import com.example.ureka02.global.auth.user.customUserDetails.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final JwtTokenUtil jwtTokenUtil;
	private final AuthenticationManager authenticationManager;
	
    @Value("${jwt.cookie-name:accessToken}")
    private String jwtCookieName;
    
    @Value("${jwt.expiration}")
    private long jwtExpireMs;
    
    public ResponseCookie localLoginAndCreateCookie(String email, String rawPassword) {
    	UsernamePasswordAuthenticationToken authToken =
    			new UsernamePasswordAuthenticationToken(email, rawPassword);
    	
    	Authentication authentication = authenticationManager.authenticate(authToken);
    	
    	CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
    	
    	String token = jwtTokenUtil.createToken(principal.getId());
    	
    	return buildJwtCookie(token);
    }
    
    private ResponseCookie buildJwtCookie(String token) {
    	return ResponseCookie.from(jwtCookieName, token)
    			.httpOnly(true)
    			.secure(false)
    			.path("/")
    			.maxAge(Duration.ofMillis(jwtExpireMs))
    			.sameSite("Lax")
    			.build();
    }
}
