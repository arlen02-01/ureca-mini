package com.example.ureka02.friends.repository;

import com.example.ureka02.friends.domain.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findBySender_IdAndReceiver_Id(Long senderId, Long receiverId);


    // 친구 요청 조회
    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.sender.id = :userId OR f.receiver.id = :userId)
          AND f.status = com.example.ureka02.friends.domain.FriendStatus.PENDING
    """)
    List<Friendship> findPendingFriendships(@Param("userId") Long userId);


    // 친구 목록 조회
    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.sender.id = :userId OR f.receiver.id = :userId)
          AND f.status = com.example.ureka02.friends.domain.FriendStatus.ACCEPTED
    """)
    List<Friendship> findAcceptedFriendships(@Param("userId") Long userId);
}

