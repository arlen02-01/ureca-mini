package com.example.ureka02.user.dto;

import com.example.ureka02.user.enums.AuthProvider;
import com.example.ureka02.user.enums.Role;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupForm {
    private String email;
    private String password;
    private String name;
    private AuthProvider provider;
    private Role role;
}