package com.example.ureka02.infrastructure.redis;

import com.example.ureka02.friends.domain.Friendship;
import com.example.ureka02.friends.repository.FriendRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisFriendCacheLoader {

    private final RedisTemplate<String, Long> redisTemplate;
    private final FriendRepository friendRepository;

    @PostConstruct
    public void load() {
        List<Friendship> all = friendRepository.findAll();

        for (Friendship f : all) {
            redisTemplate.opsForSet().add("friends:" + f.getReceiver().getId(), f.getSender().getId());
            redisTemplate.opsForSet().add("friends:" + f.getSender().getId(), f.getReceiver().getId());
        }
    }
}
