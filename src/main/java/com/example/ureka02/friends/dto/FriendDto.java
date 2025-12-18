package com.example.ureka02.friends.dto;

import com.example.ureka02.friends.domain.Friendship;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FriendDto {
    public Long id;
    public Long senderId;
    public String senderName;
    public Long receiverId;
    public String receiverName;
    public LocalDateTime createdAt;

    public FriendDto(Friendship friendShip) {
        id = friendShip.getId();
        senderId = friendShip.getSender().getId();
        senderName = friendShip.getSender().getName();
        receiverId = friendShip.getReceiver().getId();
        receiverName = friendShip.getReceiver().getName();
        createdAt = friendShip.getCreatedAt();
    }
}
