package com.example.ureka02.community;

import com.example.ureka02.community.domain.Board;
import com.example.ureka02.community.dto.BoardRequest;
import com.example.ureka02.community.dto.BoardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;

    @Transactional
    public BoardResponse createBoard(Long userId, BoardRequest request) {
        Board board = Board.builder()
            .userId(1L)
            .username("temp")
            .title(request.getTitle())
            .content(request.getContent())
            .build();

        Board savedBoard = boardRepository.save(board);

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
