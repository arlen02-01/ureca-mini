package com.example.ureka02.friends.controller;

import com.example.ureka02.friends.dto.FriendDto;
import com.example.ureka02.friends.service.FriendService;
import com.example.ureka02.global.common.ResponseDto;
import com.example.ureka02.user.customUserDetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/friends")
public class FriendController {

    private final FriendService friendService;

    // 1. 친구 요청 보내기
    @Operation(summary = "친구 요청 전송", description = "친구 요청을 보냅니다.")
    @PostMapping("/request")
    public ResponseDto<FriendDto> sendFriendRequest(@RequestParam("receiverName") String receiverName,
            @AuthenticationPrincipal CustomUserDetails user) {
        FriendDto response = friendService.sendFriendRequest(user.getId(), receiverName);
        return ResponseDto.ok(response);
    }

    // 2. 내가 받은 친구 요청 조회
    @Operation(summary = "모든 친구 요청 조회", description = "모든 친구 요청을 조회합니다.")
    @GetMapping("/requests")
    public ResponseDto<List<FriendDto>> getAllRequests(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseDto.ok(friendService.getAllRequest(user.getId()));
    }

    // 3. 친구 요청 수락
    @Operation(summary = "받은 친구 요청 수락", description = "친구 요청을 수락합니다.")
    @PostMapping("/{friendshipId}/accept")
    public ResponseDto<Boolean> acceptFriendRequest(@PathVariable("friendshipId") Long friendshipId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseDto.ok(friendService.acceptFriendRequest(friendshipId, user.getId()));
    }

    // 4. 친구 요청 거절
    @Operation(summary = "받은 친구 요청 거절", description = "친구 요청을 거절합니다.")
    @PostMapping("/{friendshipId}/reject")
    public ResponseDto<Boolean> rejectFriendRequest(@PathVariable("friendshipId") Long friendshipId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseDto.ok(friendService.rejectFriendRequest(friendshipId, user.getId()));
    }

    // 5. 친구 목록 조회
    @Operation(summary = "친구 목록 조회", description = "친구 목록을 조회합니다.")
    @GetMapping("/list")
    public ResponseDto<List<FriendDto>> getFriendList(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseDto.ok(friendService.getFriendList(user.getId()));
    }

    // 6. 친구 삭제
    @Operation(summary = "친구 삭제", description = "친구 목록에서 친구를 삭제합니다.")
    @DeleteMapping("/{friendshipId}")
    public ResponseDto<Boolean> deleteFriend(@PathVariable("friendshipId") Long friendshipId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseDto.ok(friendService.deleteFriend(friendshipId, user.getId()));
    }
}
