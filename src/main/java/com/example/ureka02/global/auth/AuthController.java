package com.example.ureka02.global.auth;


import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.ureka02.global.auth.dto.LoginForm;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
	private final AuthService authService;
	
	@GetMapping("/login")
	public String loginPage() {
		return "auth/login";
	}
	
	@PostMapping("/login/local")
	public String localLogin(LoginForm form, HttpServletResponse res) {
		ResponseCookie jwtCookie = authService.localLoginAndCreateCookie(form.getEmail(), form.getPassword());
		
		res.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
		
		return "redirect:/";
	}
}
