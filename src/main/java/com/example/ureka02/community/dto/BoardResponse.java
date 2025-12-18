package com.example.ureka02.community.dto;

import com.example.ureka02.community.domain.Board;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시글 응답 DTO
 *
 * @author 임지우
 * @since 2025-12-09(화)
 */
@Schema(description = "게시글 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponse {
    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "작성자 ID", example = "123")
    private Long userId;

    @Schema(description = "작성자 이름", example = "홍길동")
    private String username;

    @Schema(description = "제목", example = "도움이 되는 정보입니다")
    private String title;

    @Schema(description = "내용", example = "게시글 내용입니다")
    private String content;

    @Schema(description = "작성일시", example = "2025-12-09T14:30:28")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-12-09T14:30:28")
    private LocalDateTime updatedAt;

    public static BoardResponse from(Board board) {
        return new BoardResponse(
            board.getId(),
            board.getUserId(),
            board.getUsername(),
            board.getTitle(),
            board.getContent(),
            board.getCreatedAt(),
            board.getUpdatedAt()
        );
    }
}
