package com.example.ureka02.friends.dto.response;

import com.example.ureka02.friends.domain.Friendship;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FriendResponse {
    public Long id;
    public Long senderId;
    public Long receiverId;
    public LocalDateTime createdAt;

    public FriendResponse(Friendship friendShip) {
        id = friendShip.getId();
        senderId = friendShip.getSender().getId();
        receiverId = friendShip.getReceiver().getId();
        createdAt = friendShip.getCreatedAt();
    }
}
