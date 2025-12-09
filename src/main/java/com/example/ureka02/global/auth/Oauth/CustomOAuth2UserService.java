package com.example.ureka02.global.auth.Oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.example.ureka02.user.User;
import com.example.ureka02.user.UserRepository;
import com.example.ureka02.user.enums.AuthProvider;
import com.example.ureka02.user.enums.Role;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;



import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// 카카오가 인증을 완료한 사용자 정보를 받아, DB에 저장한 후, 우리 서비스의 고유 ID(PK)를 다음 단계 (JWT 발행)로 전달
// CustomOAuth2UserService 메서드는 카카오 로그인에 성공했을 때 SpringSecurity에 의해 자동으로 호출
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 카카오 리소스 서버에 접근하여 사용자 정보 획득
        // SpringSecurity의 기본 사용자 정보 로드 구현체(DefaultOAuth2UserService)를 생성, 이 객체가 실제 카카오 API 통신을 담당
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        // delegate에게 userRequest(카카오 Access Token 포함)를 넘겨주면, 카카오 서버와 통신하여 사용자 정보를 JSON으로 받아 OAuth2User 객체로 변환하여 반환
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 소셜 타입 및 식별자 추출
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 카카오 사용자 정보(Attributes) 추출
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String socialId = String.valueOf(attributes.get(userNameAttributeName));

        System.out.println("attributes = " + attributes);

        // 닉네임, 이메일 추출
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) attributes.get("properties");
        String nickname = (String) profile.get("nickname");

        // 이메일 동의 여부에 따라 조건부로 추출
        String email = (kakaoAccount.containsKey("email_needs_agreement") && (boolean) kakaoAccount.get("email_needs_agreement") == false) ? (String) kakaoAccount.get("email") : null;

        AuthProvider socialType = AuthProvider.valueOf(registrationId.toUpperCase());

        User user = saveOrUpdate(socialType, socialId, nickname, email);

        // JWT 발행 핸들러에게 전달할 속성 Map 준비
        Map<String, Object> customAttributes = new LinkedHashMap<>();
        customAttributes.put(userNameAttributeName, oAuth2User.getName());
        customAttributes.put("userId", user.getId());

        // 새로운 DefaultOAuth2User 객체를 생성하여 반환
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().toString())),
                customAttributes,
                userNameAttributeName
        );
    }

    private User saveOrUpdate(AuthProvider socialType, String socialId, String nickname, String email) {
        User user = userRepository.findBySocialId(socialId)
                .map(entity -> entity.update(nickname, email))
                .orElseGet(() -> User.builder()
                        .socialId(socialId)
                        .provider(socialType)
                        .email(email)
                        .name(nickname)
                        .role(Role.USER)
                        .password(null)
                        .build());

        return userRepository.save(user);
    }
}
