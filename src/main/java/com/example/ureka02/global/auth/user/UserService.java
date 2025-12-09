package com.example.ureka02.global.auth.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ureka02.global.auth.user.enums.AuthProvider;
import com.example.ureka02.global.auth.user.enums.Role;

import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	
	@Transactional
	public Long localSignUp(AuthProvider provider, String email, String name, Role role, String rawPassword) {
		// 이미 사용중인 아이디 인경우 
		if(userRepository.existsByName(name)) {
			throw new IllegalArgumentException("이미 사용중인 아이디 입니다.");
		}
		// 끝 
		User user = User.builder()
				.provider(provider)
				.email(email)
				.name(name)
				.role(role)
				.password(passwordEncoder.encode(rawPassword))
				.build();
		return userRepository.save(user).getId();
	}
	
	@Transactional
	public User loadByName(String name) {
		return userRepository.findByName(name).orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다."));
	}
	@Transactional
	public User loadById(long id) {
		return userRepository.findById(id).orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다."));
	}
	
}
