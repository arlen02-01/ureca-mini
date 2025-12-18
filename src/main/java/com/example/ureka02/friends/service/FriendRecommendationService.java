package com.example.ureka02.friends.service;

import com.example.ureka02.friends.dto.FriendRecommendDto;
import com.example.ureka02.user.User;
import com.example.ureka02.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Member;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendRecommendationService {

    private final RedisTemplate<String, Long> redisTemplate;
    private final UserRepository userRepository;

    private String friendKey(Long userId) {
        return "friends:" + userId;
    }

    public List<FriendRecommendDto> recommendFriends(Long userId, int limit) {

        // Step 1. U의 친구 목록
        Set<Long> userFriends =
                redisTemplate.opsForSet().members(friendKey(userId));

        if (userFriends == null) userFriends = new HashSet<>();

        // Step 2. 친구들의 친구 목록 → 후보군
        Set<Long> candidates = new HashSet<>();

        for (Long f : userFriends) {
            Set<Long> friendsOfFriend = redisTemplate.opsForSet().members(friendKey(f));
            if (friendsOfFriend != null) {
                candidates.addAll(friendsOfFriend);
            }
        }

        // Step 3. 후보에서 자신 + 기존 친구 제거
        candidates.remove(userId);
        candidates.removeAll(userFriends);

        // Step 4. 각 후보에 대해 공통 친구 수 측정
        List<FriendRecommendDto> results = new ArrayList<>();

        List<User> users = userRepository.findAllById(candidates);

        for (User u : users) {
            // Redis SINTERCARD 사용 (5.x 이상 native 지원, template에서는 직접 SINTER 필요)
            long commonCount = (long) Objects.requireNonNull(redisTemplate.opsForSet()
                            .intersect(friendKey(userId), friendKey(u.getId())))
                    .size();

            if (commonCount > 0) {
                results.add(new FriendRecommendDto(u.getId(), u.getName(), Math.toIntExact(commonCount)));
            }
        }

        // Step 5. 공통 친구 많은 순 정렬
        return results.stream()
                .sorted((a, b) -> Integer.compare(b.commonCount(), a.commonCount()))
                .limit(limit)
                .toList();
    }
}
