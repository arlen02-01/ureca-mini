package com.example.ureka02.friends.controller;

import com.example.ureka02.friends.dto.FriendRecommendDto;
import com.example.ureka02.friends.service.FriendRecommendationService;
import com.example.ureka02.global.common.ResponseDto;
import com.example.ureka02.user.customUserDetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/friends/recommend")
public class FriendRecommendationController {

    private final FriendRecommendationService friendRecommendationService;

    @Operation(summary = "친구 추천", description = "친구 추천 기능입니다.")
    @GetMapping
    public ResponseDto<List<FriendRecommendDto>> recommend(@RequestParam(name="limit", defaultValue = "10") int limit,
                                                           @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseDto.ok(
                friendRecommendationService.recommendFriends(user.getId(), limit)
        );
    }
}
