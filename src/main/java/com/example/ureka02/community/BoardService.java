package com.example.ureka02.community;

import com.example.ureka02.community.domain.Board;
import com.example.ureka02.community.dto.BoardRequest;
import com.example.ureka02.community.dto.BoardResponse;
import com.example.ureka02.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardService {
    private final BoardRepository boardRepository;

    @Transactional
    public BoardResponse createBoard(Long userId,String username, BoardRequest request) {
        log.info("[BoardService.createBoard] 요청 시작 - userId={}, username={}, title={}",
            userId, username, request.getTitle());

        Board board = Board.builder()
            .userId(userId)
            .username(username)
            .title(request.getTitle())
            .content(request.getContent())
            .build();
        log.info("[BoardService.createBoard] Board 엔티티 생성 완료 - board={}", board);

        Board savedBoard = boardRepository.save(board);
        log.info("[BoardService.createBoard] 게시글 저장 완료 - boardId={}, userId={}",
            savedBoard.getId(), savedBoard.getUserId());

        return BoardResponse.from(savedBoard);
    }

    @Transactional
    public BoardResponse updateBoard(Long boardId, Long userId, BoardRequest request) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다: " + boardId));

        // 작성자 확인
        if (!board.getUserId().equals(userId)) {
            throw new IllegalArgumentException("게시글을 수정할 권한이 없습니다");
        }

        Board updated=board.updatePost(request.getTitle(), request.getContent());

        return BoardResponse.from(updated);
    }

    @Transactional
    public void deleteBoard(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다: " + boardId));

        // 작성자 확인
        if (!board.getUserId().equals(userId)) {
            throw new IllegalArgumentException("게시글을 삭제할 권한이 없습니다");
        }
        // 게시글 삭제
        board.delete();
    }

}
