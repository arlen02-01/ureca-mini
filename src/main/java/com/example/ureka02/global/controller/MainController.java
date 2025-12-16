package com.example.ureka02.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

	@GetMapping("/")
	public String main() {
		return "home";
	}
	@GetMapping("/mypage")
	public String mypage() {
		return "mypage";
	}
}
