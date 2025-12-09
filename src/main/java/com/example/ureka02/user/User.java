package com.example.ureka02.user;

import com.example.ureka02.user.enums.AuthProvider;
import com.example.ureka02.user.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor 
@Builder
public class User{
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                     // 내부 PK

    // 로그인 타입: LOCAL(이메일/비밀번호), KAKAO(OAuth)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;       // LOCAL, KAKAO

    // ===== 공통 프로필 영역 =====
    @Column(unique = true, length = 100)
    private String email;                // 카카오도 이메일 받아서 매핑하는 용도

    @Column(nullable = false, length = 50)
    private String name;                 // 닉네임/이름

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;					// USER, ADMIN 등

    // ===== LOCAL 로그인용 =====
    @Column(length = 255)
    private String password;				// LOCAL일 때만 사용, KAKAO는 null

    // ===== 카카오 OAuth용 =====
    @Column(unique = true)
    private String socialId;					// 카카오에서 내려주는 id(정수형)
    
    public User update(String nickname, String email) {
        this.name = nickname;
        if (email != null && !email.isEmpty()) {
            this.email = email;
        }
        return this;
    }
}