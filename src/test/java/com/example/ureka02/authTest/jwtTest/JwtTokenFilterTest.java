package com.example.ureka02.authTest.jwtTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import com.example.ureka02.global.auth.jwt.JwtTokenFilter;
import com.example.ureka02.global.auth.jwt.JwtTokenUtil;
import com.example.ureka02.user.customUserDetails.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterTest {
    @Mock
    JwtTokenUtil jwtTokenUtil;

    @Mock
    CustomUserDetailsService customUserDetailsService;

    @Mock
    FilterChain filterChain;

    @InjectMocks
    JwtTokenFilter jwtTokenFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void 헤더_없으면_그냥_통과하고_인증_안함() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        jwtTokenFilter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);   // 다음 필터는 무조건 호출
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull(); // 인증 안 됨

        verifyNoInteractions(jwtTokenUtil, customUserDetailsService);
    }
    
    @Test
    void Bearer_아니면_인증_안함() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic XXXX");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        jwtTokenFilter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtTokenUtil, customUserDetailsService);
    }
    
    @Test
    void 토큰_만료되면_인증_안함() throws ServletException, IOException {
        // given
        String token = "dummy.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenUtil.isExpired(token)).thenReturn(true);

        // when
        jwtTokenFilter.doFilter(request, response, filterChain);

        // then
        verify(jwtTokenUtil).isExpired(token);
        verifyNoMoreInteractions(jwtTokenUtil);
        verifyNoInteractions(customUserDetailsService);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void 유효한_토큰이면_SecurityContext에_Authentication_세팅됨() throws ServletException, IOException {
        // given
        String token = "valid.jwt.token";
        long userId = 1L;

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenUtil.isExpired(token)).thenReturn(false);
        when(jwtTokenUtil.getId(token)).thenReturn(userId);

        // 간단한 UserDetails 더미 객체
        User userDetails = new User(
                "test@test.com",
                "encodedPw",
                List.of(new SimpleGrantedAuthority("USER"))
        );
        when(customUserDetailsService.loadUserById(userId)).thenReturn(userDetails);

        // when
        jwtTokenFilter.doFilter(request, response, filterChain);

        // then
        verify(jwtTokenUtil).isExpired(token);
        verify(jwtTokenUtil).getId(token);
        verify(customUserDetailsService).loadUserById(userId);
        verify(filterChain).doFilter(request, response);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("USER");
    }
}
