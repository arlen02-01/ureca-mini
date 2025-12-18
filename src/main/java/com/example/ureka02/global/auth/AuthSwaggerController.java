package com.example.ureka02.global.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.example.ureka02.global.common.ResponseDto;
import com.example.ureka02.user.User;
import com.example.ureka02.user.UserService;
import com.example.ureka02.user.dto.LoginForm;
import com.example.ureka02.user.dto.SignupForm;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/authSwagger")
public class AuthSwaggerController {
	
	private final AuthService authService;
	private final UserService userService;
	@Operation(
			summary = "로컬 로그인",
			description = "이메일/비밀번호로 로그인 후 JWT 토큰을 쿠키에 저장합니다."
	)
	@PostMapping("/login/local")
	public ResponseDto<String> localLogin(
			@RequestBody LoginForm form,
	        HttpServletResponse res) {
		
	    ResponseCookie jwtCookie =
	    		authService.localLoginAndCreateCookie(form.getEmail(), form.getPassword());
	
	    res.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
	    
	    return ResponseDto.ok("accessToken : "+jwtCookie.getValue());
	}
	
	@Operation(
            summary = "회원가입",
            description = "이메일/비밀번호 등 정보로 회원가입을 수행합니다."
    )
    @PostMapping("/signup")
    public ResponseDto<User> signup(
            @RequestBody SignupForm form
    ) {
        long id = userService.localSignUp(
                form.getProvider(),
                form.getEmail(),
                form.getName(),
                form.getRole(),
                form.getPassword()
        );

        return ResponseDto.ok(userService.loadById(id));
    }

    @Operation(
            summary = "로그아웃",
            description = "accessToken 쿠키를 삭제하여 로그아웃합니다."
    )
    @PostMapping("/logout")
    public ResponseDto<String> logout(HttpServletResponse res) {	
        // accessToken 쿠키 삭제 (maxAge = 0)
        ResponseCookie deleteCookie = ResponseCookie.from("accessToken", "")
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .build();

        res.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseDto.ok("로그아웃이 완료되었습니다.");
    }
}
