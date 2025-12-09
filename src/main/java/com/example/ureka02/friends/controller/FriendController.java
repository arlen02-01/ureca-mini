package com.example.ureka02.friends.controller;

import com.example.ureka02.friends.dto.response.FriendResponse;
import com.example.ureka02.friends.service.FriendService;
import com.example.ureka02.global.common.ResponseDto;
import com.example.ureka02.user.customUserDetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

     //1. 친구 요청 보내기
    @PostMapping("/request")
    public ResponseDto<FriendResponse> sendFriendRequest(@RequestParam Long receiverId) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        FriendResponse response = friendService.sendFriendRequest(userDetails.getId(), receiverId);
        return ResponseDto.ok(response);
    }

    //2. 내가 받은 친구 요청 조회
    @GetMapping("/requests")
    public ResponseDto<List<FriendResponse>> getAllRequests() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseDto.ok(friendService.getAllRequest(userDetails.getId()));
    }

     //3. 친구 요청 수락
    @PostMapping("/{friendshipId}/accept")
    public ResponseDto<Boolean> acceptFriendRequest(@PathVariable Long friendshipId) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseDto.ok(friendService.acceptFriendRequest(friendshipId, userDetails.getId()));
    }

    //4. 친구 요청 거절
    @PostMapping("/{friendshipId}/reject")
    public ResponseDto<Boolean> rejectFriendRequest(@PathVariable Long friendshipId) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseDto.ok(friendService.rejectFriendRequest(friendshipId, userDetails.getId()));
    }

     //5. 친구 목록 조회
    @GetMapping("/list")
    public ResponseDto<List<FriendResponse>> getFriendList() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseDto.ok(friendService.getFriendList(userDetails.getId()));
    }

     //6. 친구 삭제
    @DeleteMapping("/{friendshipId}")
    public ResponseDto<Boolean> deleteFriend(
            @PathVariable Long friendshipId
    ) {
        return ResponseDto.ok(friendService.deleteFriend(friendshipId));
    }
}

