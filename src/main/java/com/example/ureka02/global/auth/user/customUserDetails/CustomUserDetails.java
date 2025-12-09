package com.example.ureka02.global.auth.user.customUserDetails;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.ureka02.global.auth.user.User;

public class CustomUserDetails implements UserDetails{
	private final User user;
	
	public CustomUserDetails(User user){
		this.user = user;
	}
	
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

    	return List.of(new SimpleGrantedAuthority(user.getRole().toString()));
    }
	
    // ===== 로컬 로그인 시 =====
    @Override
    public String getPassword() {
        return user.getPassword();   // 로컬 로그인 시에만 필요
    }
    
    @Override
    public String getUsername() {
        return user.getEmail();   // 로컬 로그인 시에만 필요
    }
    
    public long getId() {
        return user.getId();   // 로컬 로그인 시에만 필요
    }
}
