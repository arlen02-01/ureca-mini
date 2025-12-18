package com.example.ureka02.friends.service;

import com.example.ureka02.friends.domain.*;
import com.example.ureka02.friends.dto.FriendDto;
import com.example.ureka02.friends.repository.*;
import com.example.ureka02.user.User;
import com.example.ureka02.user.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.example.ureka02.user.enums.AuthProvider.KAKAO;
import static com.example.ureka02.user.enums.Role.USER;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class FriendServiceTest {

    @Autowired
    FriendService friendService;

    @Autowired
    FriendRepository friendRepository;

    @Autowired
    UserRepository userRepository;

    User userA;
    User userB;

    @BeforeEach
    void setup() {
        userA = userRepository.save(new User(null, KAKAO, "aaa", "aaa", USER, "1234", "1234"));

        userB = userRepository.save(new User(null, KAKAO, "aab", "aab", USER, "1234", "1235"));
    }

    @Test
    void 친구요청_성공() {
        // when
        FriendDto response = friendService.sendFriendRequest(
                userA.getId(), userB.getName()
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.senderId).isEqualTo(userA.getId());
        assertThat(response.receiverName).isEqualTo(userB.getName());

        Friendship saved = friendRepository.findById(response.id)
                .orElseThrow();

        assertThat(saved.getStatus()).isEqualTo(FriendStatus.PENDING);
    }

    @Test
    void 친구요청_수락() {
        // given
        Friendship pending = friendRepository.save(
                Friendship.builder()
                        .sender(userA)
                        .receiver(userB)
                        .status(FriendStatus.PENDING)
                        .build()
        );

        // when
        boolean result = friendService.acceptFriendRequest(
                pending.getId(), userB.getId()
        );

        // then
        assertThat(result).isTrue();
        assertThat(pending.getStatus()).isEqualTo(FriendStatus.ACCEPTED);
    }

    @Test
    void 친구요청_거절() {
        // given
        Friendship pending = friendRepository.save(
                Friendship.builder()
                        .sender(userA)
                        .receiver(userB)
                        .status(FriendStatus.PENDING)
                        .build()
        );

        // when
        boolean result = friendService.rejectFriendRequest(
                pending.getId(), userB.getId()
        );

        // then
        assertThat(result).isTrue();
        assertThat(friendRepository.findById(pending.getId())).isEmpty();
    }

    @Test
    void 친구목록_조회() {
        // given
        friendRepository.save(
                Friendship.builder()
                        .sender(userA)
                        .receiver(userB)
                        .status(FriendStatus.ACCEPTED)
                        .build()
        );

        // when
        var list = friendService.getFriendList(userA.getId());

        // then
        assertThat(list).hasSize(1);
        assertThat(list.get(0).receiverName).isEqualTo(userB.getName());
    }

    @Test
    void 친구삭제() {
        // given
        Friendship accepted = friendRepository.save(
                Friendship.builder()
                        .sender(userA)
                        .receiver(userB)
                        .status(FriendStatus.ACCEPTED)
                        .build()
        );

        // when
        boolean result = friendService.deleteFriend(accepted.getId(), userB.getId());

        // then
        assertThat(result).isTrue();
        assertThat(friendRepository.findById(accepted.getId())).isEmpty();
    }
}