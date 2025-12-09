package com.example.ureka02.friends.repository;

import com.example.ureka02.friends.domain.FriendShip;
import com.example.ureka02.friends.domain.FriendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<FriendShip, Long> {
    Optional<FriendShip> findBySenderId(Long senderId);
    Optional<FriendShip> findByReceiverId(Long receiverId);
    Optional<FriendShip> findBySenderIdAndReceiverId(Long senderId, Long receiverId);

    List<FriendShip> findAllByReceiverIdAndStatus(Long receiverId, FriendStatus status);

    // 친구 요청 조회용
    @Query("""
        SELECT f FROM friendship f
        WHERE (f.sender.id = :userId OR f.receiver.id = :userId)
          AND f.status = 'PENDING'
    """)
    List<FriendShip> findPendingFriendships(Long userId);


    // 친구 목록 조회용
    @Query("""
        SELECT f FROM friendship f
        WHERE (f.sender.id = :userId OR f.receiver.id = :userId)
          AND f.status = 'ACCEPTED'
    """)
    List<FriendShip> findAcceptedFriendships(Long userId);


}
