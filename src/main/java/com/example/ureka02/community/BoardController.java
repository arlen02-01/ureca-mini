package com.example.ureka02.community;

import com.example.ureka02.community.dto.BoardRequest;
import com.example.ureka02.community.dto.BoardResponse;
import com.example.ureka02.global.common.ResponseDto;
import com.example.ureka02.user.customUserDetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/board")
public class BoardController {
    private final BoardService boardService;

    @PostMapping("/create")
    @Operation(summary = "게시글 작성")
    public ResponseDto<BoardResponse> createBoard(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestBody BoardRequest boardRequest
    ) {
        return ResponseDto.ok(
            boardService.createBoard(user.getId(), user.getUsername(), boardRequest)
        );
    }

    @PostMapping("/update/{boardId}")
    @Operation(summary = "게시글 수정")
    public ResponseDto<BoardResponse> updateBoard(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable Long boardId,
        @RequestBody BoardRequest boardRequest
        ){
        return ResponseDto.ok(boardService.updateBoard(boardId,user.getId(), boardRequest));
    }

    @PostMapping("/delete/{boardId}")
    @Operation(summary = "게시글 삭제")
    public void deleteBoard(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable Long boardId
    ){
        boardService.deleteBoard(boardId, user.getId());
    }

}
