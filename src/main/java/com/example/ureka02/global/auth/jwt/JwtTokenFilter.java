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

	    try {
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

	    } catch (Exception ex) {
	        // ✅ 여기서 예외를 밖으로 던지면 “카카오로 튐/리다이렉트”가 발생할 수 있음
	        SecurityContextHolder.clearContext();

	         //토큰이 이상하면 accessToken 쿠키 제거해버리기
	         ResponseCookie dcookie = ResponseCookie.from("accessToken", "").path("/").maxAge(0).httpOnly(true).build();
	         response.addHeader(HttpHeaders.SET_COOKIE, dcookie.toString());

	        filterChain.doFilter(request, response);
	    }
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
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
	    String uri = request.getRequestURI();

	    return uri.startsWith("/auth/")
	        || uri.startsWith("/oauth2/")
	        || uri.startsWith("/login/oauth2/")
	        || uri.startsWith("/css/")
	        || uri.startsWith("/js/")
	        || uri.startsWith("/images/")
	        || uri.startsWith("/v3/api-docs/")
	        || uri.startsWith("/swagger-ui/");
	}

	
}
