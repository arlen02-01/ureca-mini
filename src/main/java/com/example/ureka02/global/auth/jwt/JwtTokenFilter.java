package com.example.ureka02.global.auth.jwt;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.ureka02.global.auth.user.customUserDetails.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
	private final CustomUserDetailsService customUserDetailsService;
	
	@Override
	protected void doFilterInternal(
			HttpServletRequest request
			, HttpServletResponse response
			, FilterChain filterChain
			) throws ServletException, IOException {
		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		
		//헤더에 토큰이 있는지 검사 -> 없으면 리턴
		if(authorizationHeader == null) {
			filterChain.doFilter(request, response); // 다음 필터로 넘기는 코드
			return;
		}
		//헤더에 토큰이 Bearer로 시작 하지 않으면 -> 잘못된 토큰
		if(!authorizationHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response); // 다음 필터로 넘기는 코드
			return;
		}
		
		//헤더에서 토큰 분리 ("Bearer asdkf.asdf.asdf" -> "asdkf.asdf.asdf")
		String token = authorizationHeader.split(" ")[1];
		
		//시간상 만료되었는지 확인
		if(jwtTokenUtil.isExpired(token)) {
			filterChain.doFilter(request, response); // 다음 필터로 넘기는 코드
			return;
		}
		
		//토큰에서 loginId 추출
		long id = jwtTokenUtil.getId(token);
		
		//유저 객체를 authentication에 필요한 규격에 맞춰 생성
		UserDetails userDetails = customUserDetailsService.loadUserById(id);
		
		//spring sequrity에서 관리해주는 UsernamePasswordAuthenticationToken 발급
		UsernamePasswordAuthenticationToken authentication
			= new UsernamePasswordAuthenticationToken(
					userDetails,null,userDetails.getAuthorities()
			);
		// authentication에 추가 정보 세팅 (현재 기능상 필요없지만 Spring Security 관례상 넣는다.)
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		// Spring Security에서 사용자를 기억하기 위한 빈에 authentication을 주입한다. 이걸로 인증이 된다.
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);

		
	}
	
}
