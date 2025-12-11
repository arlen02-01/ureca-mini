package com.example.ureka02.friends.service;

import com.example.ureka02.friends.dto.FriendRecommendDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class FriendRecommendServiceTest {

    private RedisTemplate<String, Long> redisTemplate;
    private SetOperations<String, Long> setOps;
    private FriendRecommendationService recommendService;

    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(RedisTemplate.class);
        setOps = Mockito.mock(SetOperations.class);

        when(redisTemplate.opsForSet()).thenReturn(setOps);

        recommendService = new FriendRecommendationService(redisTemplate);
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

        // 공통 친구 계산:
        // intersect(friends:1, friends:candidate)
        when(setOps.intersect("friends:1", "friends:4")).thenReturn(Set.of(2L, 3L)); // commonCount=2
        when(setOps.intersect("friends:1", "friends:5")).thenReturn(Set.of(3L));     // commonCount=1

        List<FriendRecommendDto> result = recommendService.recommendFriends(userId, 10);

        assertThat(result).hasSize(2);

        // 4 → 5 순서 확인
        assertThat(result.get(0).userId()).isEqualTo(4L);
        assertThat(result.get(0).commonCount()).isEqualTo(2);

        assertThat(result.get(1).userId()).isEqualTo(5L);
        assertThat(result.get(1).commonCount()).isEqualTo(1);
    }
}