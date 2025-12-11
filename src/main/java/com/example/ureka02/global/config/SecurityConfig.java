package com.example.ureka02.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.ureka02.global.auth.Oauth.CustomOAuth2UserService;
import com.example.ureka02.global.auth.Oauth.OAuth2LoginSuccessHandler;
import com.example.ureka02.global.auth.jwt.JwtTokenFilter;
import com.example.ureka02.global.auth.jwt.JwtTokenUtil;
import com.example.ureka02.user.customUserDetails.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtTokenUtil jwtTokenUtil;
        private final CustomUserDetailsService customUserDetailsService;
        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
                return configuration.getAuthenticationManager();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                // JWT 필터 생성
                JwtTokenFilter jwtTokenFilter = new JwtTokenFilter(jwtTokenUtil, customUserDetailsService);

                http
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())
                                // 🔑 URL별 권한 설정
                                .authorizeHttpRequests(auth -> auth
                                                // swagger 관련
                                                .requestMatchers(
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/authSwagger/**",
                                                                "/swagger-ui.html")
                                                .permitAll()

                                                // 토큰 없이 접근 허용할 URL들
                                                .requestMatchers(
                                                				"/home", "/",
                                                                "/auth/login",
                                                                "/auth/login/local",
                                                                "/auth/signup",
                                                                "/auth/kakao/**",
                                                                "/oauth2/**",
                                                                "/login/oauth2/**",
                                                                "/recruitments/**",
                                                                 "/friends/**"
                                                )
                                                .permitAll()
                                                // 나머지는 인증 필요
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth2 -> oauth2
                                                .successHandler(oAuth2LoginSuccessHandler)
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService)))
                                // 🔥 JwtTokenFilter를 UsernamePasswordAuthenticationFilter 앞에 끼워넣기
                                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        // 비밀번호 해싱용 (로컬 로그인 있을 때 거의 필수)
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
