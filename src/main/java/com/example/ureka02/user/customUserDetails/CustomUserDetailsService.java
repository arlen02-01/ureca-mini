package com.example.ureka02.user.customUserDetails;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.ureka02.user.User;
import com.example.ureka02.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService{
    private final UserRepository userRepository;

    // 로컬 로그인 (id / password) 할 때 사용하는 메서드
    @Override
    public UserDetails loadUserByUsername(String localId) {
        User user = userRepository.findByEmail(localId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. localId=" + localId));
        return new CustomUserDetails(user);
    }

    // ✅ JWT 필터에서 PK 기반으로 유저 찾고 싶을 때 쓰는 메서드
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. id=" + id));
        return new CustomUserDetails(user);
    }
}
