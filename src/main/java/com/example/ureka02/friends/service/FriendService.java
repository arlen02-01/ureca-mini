package com.example.ureka02.friends.service;

import com.example.ureka02.friends.domain.FriendShip;
import com.example.ureka02.friends.domain.FriendStatus;
import com.example.ureka02.friends.dto.response.FriendResponse;
import com.example.ureka02.friends.repository.FriendRepository;
import com.example.ureka02.user.User;
import com.example.ureka02.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendService {
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    // 1. 친구 요청 보내기
    public FriendResponse sendFriendRequest(Long senderId, Long receiverId) {

        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("자기 자신에게 친구 요청을 보낼 수 없습니다.");
        }

        // 기존 요청 존재 여부 확인
        Optional<FriendShip> existing =
                friendRepository.findBySenderIdAndReceiverId(senderId, receiverId);

        if (existing.isPresent()) {
            throw new IllegalStateException("이미 친구 요청이 존재합니다.");
        }

        // 반대 방향 요청 또는 친구 여부 확인
        Optional<FriendShip> reverse =
                friendRepository.findBySenderIdAndReceiverId(receiverId, senderId);

        if (reverse.isPresent()) {
            throw new IllegalStateException("이미 친구 관계이거나 요청이 반대로 존재합니다.");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("요청자 정보 없음"));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("수신자 정보 없음"));

        FriendShip friendship = FriendShip.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendStatus.PENDING)
                .build();

        FriendShip saved = friendRepository.save(friendship);
        return new FriendResponse(saved);
    }

    // 2. 친구 요청 조회 (PENDING)
    public List<FriendResponse> getAllRequest(Long memberId) {
        return friendRepository.findPendingFriendships(memberId)
                .stream()
                .map(FriendResponse::new)
                .toList();
    }

    // 3. 친구 요청 수락
    public boolean acceptFriendRequest(Long friendshipId, Long receiverId) {
        FriendShip friendship = friendRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("요청 정보 없음"));

        if (!friendship.getReceiver().getId().equals(receiverId)) {
            throw new IllegalStateException("해당 요청을 수락할 권한이 없습니다.");
        }

        friendship.setStatus(FriendStatus.ACCEPTED);
        return true;
    }

    // 4. 친구 요청 거절 (삭제)
    public boolean rejectFriendRequest(Long friendshipId, Long receiverId) {

        FriendShip friendship = friendRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("요청 정보 없음"));

        if (!friendship.getReceiver().getId().equals(receiverId)) {
            throw new IllegalStateException("해당 요청을 거절할 권한이 없습니다.");
        }

        friendRepository.delete(friendship);
        return true;
    }

    // 5. 친구 목록 조회
    public List<FriendResponse> getFriendList(Long userId) {
        return friendRepository.findAcceptedFriendships(userId)
                .stream()
                .map(FriendResponse::new)
                .toList();
    }

    // 6. 친구 삭제 (양방향 삭제)
    public boolean deleteFriend(Long reqId) {
        friendRepository.deleteById(reqId);
        return true;
    }

}