package com.example.ureka02.friends.service;

import com.example.ureka02.friends.domain.Friendship;
import com.example.ureka02.friends.domain.FriendStatus;
import com.example.ureka02.friends.dto.FriendDto;
import com.example.ureka02.friends.repository.FriendRepository;
import com.example.ureka02.global.error.CommonException;
import com.example.ureka02.global.error.ErrorCode;
import com.example.ureka02.user.User;
import com.example.ureka02.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendService {
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Long>  redisTemplate;

    // 1. 친구 요청 보내기
    public FriendDto sendFriendRequest(Long senderId, String receiverName) {

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CommonException(ErrorCode.USER_NOT_FOUND));

        User receiver = userRepository.findByName(receiverName)
                .orElseThrow(() -> new CommonException(ErrorCode.USER_NOT_FOUND));

        if (senderId.equals(receiver.getId())) {
            throw new CommonException(ErrorCode.FRIEND_REQUEST_SELF);
        }

        // 기존 요청 존재 확인
        Optional<Friendship> existing =
                friendRepository.findBySender_IdAndReceiver_Id(senderId, receiver.getId());

        if (existing.isPresent()) {
            throw new CommonException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
        }

        // 반대 방향 요청 또는 이미 친구 여부 확인
        Optional<Friendship> reverse =
                friendRepository.findBySender_IdAndReceiver_Id(receiver.getId(), senderId);

        if (reverse.isPresent()) {
            throw new CommonException(ErrorCode.FRIEND_REQUEST_REVERSE_EXISTS);
        }

        Friendship friendship = Friendship.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendStatus.PENDING)
                .build();

        Friendship saved = friendRepository.save(friendship);
        return new FriendDto(saved);
    }

    // 2. 친구 요청 조회 (PENDING)
    public List<FriendDto> getAllRequest(Long memberId) {
        return friendRepository.findPendingFriendships(memberId)
                .stream()
                .map(FriendDto::new)
                .toList();
    }

    // 3. 친구 요청 수락 (레디스에 friend:{memberId} 저장)
    public boolean acceptFriendRequest(Long friendshipId, Long receiverId) {

        Friendship friendship = friendRepository.findById(friendshipId)
                .orElseThrow(() -> new CommonException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!friendship.getReceiver().getId().equals(receiverId)) {
            throw new CommonException(ErrorCode.FRIEND_ACCEPT_FORBIDDEN);
        }

        friendship.setStatus(FriendStatus.ACCEPTED);

        Long userA = friendship.getSender().getId();
        Long userB = receiverId;

        redisTemplate.opsForSet().add(friendKey(userA), userB);
        redisTemplate.opsForSet().add(friendKey(userB), userA);

        return true;
    }

    private String friendKey(Long memberId) {
        return "friends:" + memberId;
    }

    // 4. 친구 요청 거절
    public boolean rejectFriendRequest(Long friendshipId, Long receiverId) {
        Friendship friendship = friendRepository.findById(friendshipId)
                .orElseThrow(() -> new CommonException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!friendship.getReceiver().getId().equals(receiverId)) {
            throw new CommonException(ErrorCode.FRIEND_REJECT_FORBIDDEN);
        }

        friendRepository.delete(friendship);
        return true;
    }

    // 5. 친구 목록 조회
    public List<FriendDto> getFriendList(Long userId) {
        return friendRepository.findAcceptedFriendships(userId)
                .stream()
                .map(FriendDto::new)
                .toList();
    }

    // 6. 친구 삭제
    public boolean deleteFriend(Long reqId, Long userId) {
        FriendDto request = friendRepository.findById(reqId)
                .map(FriendDto::new)
                .orElseThrow(() -> new CommonException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!request.getReceiverId().equals(userId)) {
            throw new CommonException(ErrorCode.FRIEND_DELETE_FORBIDDEN);
        }

        friendRepository.deleteById(reqId);

        redisTemplate.opsForSet().remove(friendKey(request.getReceiverId()), request.getSenderId());
        redisTemplate.opsForSet().remove(friendKey(request.getSenderId()), request.getReceiverId());

        return true;
    }

}