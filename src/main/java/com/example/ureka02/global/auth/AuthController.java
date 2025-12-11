package com.example.ureka02.global.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.ureka02.user.UserService;
import com.example.ureka02.user.dto.LoginForm;
import com.example.ureka02.user.dto.SignupForm;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
	private final AuthService authService;
	private final UserService userService;

	@GetMapping("/success-login")
	public String test() {
		return "redirect:/";
	}

	@GetMapping("/login")
	public String loginPage() {
		return "auth/login";
	}

	@GetMapping("/signup")
	public String signupPage() {
		return "/auth/signup";
	}
	
    @GetMapping("/logout")
    public String logoutGet(HttpServletResponse response) {

        ResponseCookie dcookie = ResponseCookie.from("accessToken", "")
                .path("/")
                .maxAge(0)        // 쿠키 삭제
                .httpOnly(true)  // JWT 쿠키가 HttpOnly였으면 동일 설정 유지
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, dcookie.toString());

        return "redirect:/";
    }

	@PostMapping("/login/local")
	public String localLogin(LoginForm form, HttpServletResponse res) {
		ResponseCookie jwtCookie = authService.localLoginAndCreateCookie(form.getEmail(), form.getPassword());
		res.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
		return "redirect:/";
	}

	@PostMapping("/signup")
	public String postSignup(SignupForm form, HttpServletResponse res) {
		userService.localSignUp(form.getProvider(), form.getEmail(), form.getName(), form.getRole(),
				form.getPassword());
		return "auth/login";
	}
}
