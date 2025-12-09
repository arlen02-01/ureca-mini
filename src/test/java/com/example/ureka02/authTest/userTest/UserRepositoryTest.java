package com.example.ureka02.authTest.userTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.example.ureka02.global.auth.user.User;
import com.example.ureka02.global.auth.user.UserRepository;
import com.example.ureka02.global.auth.user.enums.AuthProvider;
import com.example.ureka02.global.auth.user.enums.Role;



@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {
	
    @Autowired
    private UserRepository userRepository;
	
	@Test
	void findByName_테스트() {
		//given
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
                .password(rawPassword+"encoded") // 여기서는 암호화 여부까지 신경 안 써도 됨
                .build();
        
        //when
        userRepository.save(user);        
        //then        
		User saved = userRepository.findByName(name).orElseThrow();
		assertThat(saved.getProvider()).isEqualTo(provider);
		assertThat(saved.getEmail()).isEqualTo(email);
		assertThat(saved.getName()).isEqualTo(name);
		assertThat(saved.getRole()).isEqualTo(role);
		//user에 담길때 raw비번은 암호화 되어 달라야 정상
		assertThat(saved.getPassword()).isNotEqualTo(rawPassword);
		
	    Optional<User> result = userRepository.findByName("없는사람");
	    assertThat(result).isEmpty();
	}
	@Test
	void findById_테스트() {
		//given
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
                .password(rawPassword+"encoded") // 여기서는 암호화 여부까지 신경 안 써도 됨
                .build();
        
        //when
        long id = userRepository.save(user).getId();        
        //then        
		User saved = userRepository.findById(id).orElseThrow();
		assertThat(saved.getProvider()).isEqualTo(provider);
		assertThat(saved.getEmail()).isEqualTo(email);
		assertThat(saved.getName()).isEqualTo(name);
		assertThat(saved.getRole()).isEqualTo(role);
		//user에 담길때 raw비번은 암호화 되어 달라야 정상
		assertThat(saved.getPassword()).isNotEqualTo(rawPassword);
		
	    Optional<User> result = userRepository.findById(-1);
	    assertThat(result).isEmpty();
	}
	@Test
	void findByEmail_테스트() {
		//given
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
                .password(rawPassword+"encoded") // 여기서는 암호화 여부까지 신경 안 써도 됨
                .build();
        
        //when
        userRepository.save(user);        
        //then        
		User saved = userRepository.findByEmail(email).orElseThrow();
		assertThat(saved.getProvider()).isEqualTo(provider);
		assertThat(saved.getEmail()).isEqualTo(email);
		assertThat(saved.getName()).isEqualTo(name);
		assertThat(saved.getRole()).isEqualTo(role);
		//user에 담길때 raw비번은 암호화 되어 달라야 정상
		assertThat(saved.getPassword()).isNotEqualTo(rawPassword);
		
	    Optional<User> result = userRepository.findByEmail("없는사람@없어");
	    assertThat(result).isEmpty();
	}
	@Test
	void existsByName_테스트() {
		//given
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
                .password(rawPassword+"encoded") // 여기서는 암호화 여부까지 신경 안 써도 됨
                .build();
        
        //when
        userRepository.save(user);        
        //then        
        assertThat(userRepository.existsByName(name)).isTrue();
        assertThat(userRepository.existsByName("없는사람")).isFalse();
		
	}
}
