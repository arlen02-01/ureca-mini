package com.example.ureka02.authTest.userTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.ureka02.global.auth.user.User;
import com.example.ureka02.global.auth.user.UserRepository;
import com.example.ureka02.global.auth.user.UserService;
import com.example.ureka02.global.auth.user.enums.AuthProvider;
import com.example.ureka02.global.auth.user.enums.Role;

import jakarta.transaction.Transactional;

@SpringBootTest
@Transactional
public class UserServiceTest {
	
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;
    
    @Test
	void localSignUp_가입성공테스트() {
		//given
        AuthProvider provider = AuthProvider.LOCAL;
        String email = "test@test.com";
        String name = "nameTest";
        Role role = Role.USER;
        String rawPassword = "rawpasswordTest";
        
        //when
        long loginId = userService.localSignUp(provider, email, name, role, rawPassword);
        
        //then
		User saved = userRepository.findById(loginId).orElseThrow();
		assertThat(saved.getProvider()).isEqualTo(provider);
		assertThat(saved.getEmail()).isEqualTo(email);
		assertThat(saved.getName()).isEqualTo(name);
		assertThat(saved.getRole()).isEqualTo(role);
		//user에 담길때 raw비번은 암호화 되어 달라야 정상
		assertThat(saved.getPassword()).isNotEqualTo(rawPassword);
	}
    
    @Test
    void localSignUp_이미사용유저테스트() {
    	//given
        AuthProvider provider = AuthProvider.LOCAL;
        String email = "test@test.com";
        String name = "nameTest";
        Role role = Role.USER;
        String rawPassword = "rawpasswordTest";
        
        //when
        userService.localSignUp(provider, email, name, role, rawPassword);
        
    	//then
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.localSignUp(provider, email, name, role, rawPassword)
                );

        assertThat(exception.getMessage()).isEqualTo("이미 사용중인 아이디 입니다.");
    }

    @Test
    void loadByName_사용자있음(){
    	//given
        AuthProvider provider = AuthProvider.LOCAL;
        String email = "test@test.com";
        String name = "nameTest";
        Role role = Role.USER;
        String rawPassword = "rawpasswordTest";
        
        //when
        userService.localSignUp(provider, email, name, role, rawPassword);
    	User loaduser = userService.loadByName(name);
    	
        //then
		assertThat(loaduser.getProvider()).isEqualTo(provider);
		assertThat(loaduser.getEmail()).isEqualTo(email);
		assertThat(loaduser.getName()).isEqualTo(name);
		assertThat(loaduser.getRole()).isEqualTo(role);
		//user에 담길때 raw비번은 암호화 되어 달라야 정상
		assertThat(loaduser.getPassword()).isNotEqualTo(rawPassword);
    }
    
    @Test
    void loadByName_사용자없음(){
    	//given
        //AuthProvider provider = AuthProvider.LOCAL;
        //String email = "test@test.com";
        String name = "nameTest";
        //Role role = Role.USER;
        //String rawPassword = "rawpasswordTest";
        
        //when
        //userService.localSignUp(provider, email, name, role, rawPassword);(저장안함)

    	//then
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.loadByName(name)
                );

        assertThat(exception.getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
    }
    
    @Test
    void loadById_사용자있음(){
    	//given
        AuthProvider provider = AuthProvider.LOCAL;
        String email = "test@test.com";
        String name = "nameTest";
        Role role = Role.USER;
        String rawPassword = "rawpasswordTest";
        
        //when
        long id = userService.localSignUp(provider, email, name, role, rawPassword);
    	User loaduser = userService.loadById(id);
    	
        //then
		assertThat(loaduser.getProvider()).isEqualTo(provider);
		assertThat(loaduser.getEmail()).isEqualTo(email);
		assertThat(loaduser.getName()).isEqualTo(name);
		assertThat(loaduser.getRole()).isEqualTo(role);
		//user에 담길때 raw비번은 암호화 되어 달라야 정상
		assertThat(loaduser.getPassword()).isNotEqualTo(rawPassword);
    }
    
    @Test
    void loadById_사용자없음(){
    	//given
        AuthProvider provider = AuthProvider.LOCAL;
        String email = "test@test.com";
        String name = "nameTest";
        Role role = Role.USER;
        String rawPassword = "rawpasswordTest";
        
        //when
        userService.localSignUp(provider, email, name, role, rawPassword);

    	//then
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.loadById(0)
                );
        assertThat(exception.getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
    }
}
