package com.example.ureka02.global.auth.Oauth;

import com.example.ureka02.global.auth.jwt.JwtTokenUtil;
import com.example.ureka02.global.error.CommonException;
import com.example.ureka02.global.error.ErrorCode;
import com.example.ureka02.user.User;
import com.example.ureka02.user.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    
    OAuth2LoginSuccessHandler(JwtTokenUtil jwtTokenUtil, UserRepository userRepository) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userRepository = userRepository;
    }
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
	    String socialId = oAuth2User.getName();
	    
	    User user = userRepository.findBySocialId(socialId).orElseThrow(()->new CommonException(ErrorCode.USER_NOT_FOUND));
	    Long id =user.getId();
	    
	    String accessToken = jwtTokenUtil.createToken(id);
	    
        // ✅ JWT를 HttpOnly 쿠키로 내려줌
        ResponseCookie jwtCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)    // JS에서 못 읽게
                .secure(false)     // HTTPS면 true로 변경
                .path("/")
                .maxAge(60 * 60)   // 유효시간(sec)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
	    		
	

        String targetURL = "/";

        response.sendRedirect(targetURL);
    }
}

