package com.example.ureka02.authTest.userTest.costomUserDetailsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.ureka02.global.auth.user.User;
import com.example.ureka02.global.auth.user.UserRepository;
import com.example.ureka02.global.auth.user.customUserDetails.CustomUserDetailsService;
import com.example.ureka02.global.auth.user.enums.AuthProvider;
import com.example.ureka02.global.auth.user.enums.Role;

import jakarta.transaction.Transactional;

@SpringBootTest
@Transactional
public class CustomUserDetailsServiceTest {

//    // ✅ JWT 필터에서 PK 기반으로 유저 찾고 싶을 때 쓰는 메서드
//    public UserDetails loadUserById(Long id) {
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. id=" + id));
//        return new CustomUserDetails(user);
//    }
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private UserRepository userRepository;
    
    private final PasswordEncoder passwordEncoder = passwordEncoder();

    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
	long saveUser() {		
	    AuthProvider provider = AuthProvider.LOCAL;
	    String email = "test@test.com";
	    String name = "nameTest";
	    Role role = Role.USER;
	    String rawPassword = "rawpasswordTest";
	    
		User user = User.builder()
				.provider(provider)
				.email(email)
				.name(name)
				.role(role)
				.password(passwordEncoder.encode(rawPassword))
				.build();
		
		return userRepository.save(user).getId();		
	}
    

    @Test
    void loadUserByUsername_테스트() {
    	//given
    	saveUser();
    	String email = "test@test.com";
    	//when
    	UserDetails saved = customUserDetailsService.loadUserByUsername(email);
    	//then
    	assertThat(saved.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly(Role.USER.toString());
		assertThat(saved.getUsername()).isEqualTo("test@test.com");
		//user에 담길때 raw비번은 암호화 되어 달라야 정상
		assertThat(saved.getPassword()).isNotEqualTo("rawpasswordTest");
    }
    
    @Test
    void loadUserByUsername_실패_테스트() {
    	//given
    	//saveUser();
    	String email = "test@test.com";
    	//when
        UsernameNotFoundException exception =
                assertThrows(
                        UsernameNotFoundException.class,
                        () -> customUserDetailsService.loadUserByUsername(email)
                );    	
    	//then
        assertThat(exception.getMessage()).isEqualTo("사용자를 찾을 수 없습니다. localId=" + email);
    }
    
    @Test
    void loadUserById_테스트() {
    	//given
    	long id = saveUser();
    	//when
    	UserDetails saved = customUserDetailsService.loadUserById(id);
    	//then
    	assertThat(saved.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly(Role.USER.toString());
		assertThat(saved.getUsername()).isEqualTo("test@test.com");
		//user에 담길때 raw비번은 암호화 되어 달라야 정상
		assertThat(saved.getPassword()).isNotEqualTo("rawpasswordTest");
    }
    
    @Test
    void loadUserById_실패_테스트() {
    	//given
    	//saveUser();
    	//when
        UsernameNotFoundException exception =
                assertThrows(
                        UsernameNotFoundException.class,
                        () -> customUserDetailsService.loadUserById((long)1)
                );    	
    	//then
        assertThat(exception.getMessage()).isEqualTo("사용자를 찾을 수 없습니다. id=" + (long)1);
    }
}
