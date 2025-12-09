package com.example.ureka02.friends.dto.response;

import com.example.ureka02.friends.domain.FriendShip;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FriendResponse {
    public Long requestId;
    public Long senderId;
    public Long receiverId;
    public LocalDateTime createdAt;

    public FriendResponse(FriendShip friendShip) {
        requestId = friendShip.getId();
        senderId = friendShip.getSender().getId();
        receiverId = friendShip.getReceiver().getId();
        createdAt = friendShip.getCreatedAt();
    }
}
