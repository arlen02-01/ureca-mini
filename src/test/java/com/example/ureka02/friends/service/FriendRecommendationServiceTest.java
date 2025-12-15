package com.example.ureka02.friends.service;

import com.example.ureka02.friends.dto.FriendRecommendDto;
import com.example.ureka02.user.User;
import com.example.ureka02.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.List;
import java.util.Set;

import static com.example.ureka02.user.enums.AuthProvider.KAKAO;
import static com.example.ureka02.user.enums.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.when;

class FriendRecommendServiceTest {

    private RedisTemplate<String, Long> redisTemplate;
    private SetOperations<String, Long> setOps;
    private FriendRecommendationService recommendService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(RedisTemplate.class);
        setOps = Mockito.mock(SetOperations.class);
        userRepository = Mockito.mock(UserRepository.class);

        when(redisTemplate.opsForSet()).thenReturn(setOps);

        recommendService = new FriendRecommendationService(redisTemplate, userRepository);
    }

    @Test
    void recommendFriends_success() {
        Long userId = 1L;

        // friends:1 => {2,3}
        when(setOps.members("friends:1")).thenReturn(Set.of(2L, 3L));

        // friends:2 => {1,4}
        when(setOps.members("friends:2")).thenReturn(Set.of(1L, 4L));

        // friends:3 => {1,4,5}
        when(setOps.members("friends:3")).thenReturn(Set.of(1L, 4L, 5L));

        // 공통 친구 수
        when(setOps.intersect("friends:1", "friends:4"))
                .thenReturn(Set.of(2L, 3L)); // 2명
        when(setOps.intersect("friends:1", "friends:5"))
                .thenReturn(Set.of(3L));     // 1명
      
        when(userRepository.findAllById(anyIterable()))
                .thenReturn(List.of(
                        new User(4L, KAKAO, "aaa", "user4", USER, "1234", "1234"),
                        new User(5L, KAKAO, "aaa", "user5", USER, "1234", "1234")
                ));

        // when
        List<FriendRecommendDto> result =
                recommendService.recommendFriends(userId, 10);

        // then
        assertThat(result).hasSize(2);

        assertThat(result.get(0).userId()).isEqualTo(4L);
        assertThat(result.get(0).commonCount()).isEqualTo(2);

        assertThat(result.get(1).userId()).isEqualTo(5L);
        assertThat(result.get(1).commonCount()).isEqualTo(1);
    }
}