package com.example.ureka02.global.auth.jwt;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.ureka02.user.customUserDetails.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
	private final CustomUserDetailsService customUserDetailsService;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
	        throws ServletException, IOException {

        String token = resolveToken(request);

        // 1) 토큰 없으면 패스
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2) 이미 인증 있으면 패스
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3) 만료면 패스 (원하면 여기서 accessToken 쿠키 삭제도 가능)
        if (jwtTokenUtil.isExpired(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        long id = jwtTokenUtil.getId(token);
        UserDetails userDetails = customUserDetailsService.loadUserById(id);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);

	}

	
	private String resolveToken(HttpServletRequest request) {
        // 1) Authorization 헤더 (Bearer xxx)
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            // "Bearer " 잘라내고 순수 토큰만 반환
            return authorizationHeader.substring(7);
        }

        // 2) accessToken 쿠키 (순수 토큰이 들어있다고 가정)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

	
}
