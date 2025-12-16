package com.example.ureka02.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 BAD REQUEST
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "E400001", "유효하지 않은 입력값입니다."),
    INVALID_PARAMETER_FORMAT(HttpStatus.BAD_REQUEST, "E400002", "잘못된 형식의 파라미터입니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "E400003", "필수 파라미터가 누락되었습니다."),
    BAD_REQUEST_JSON(HttpStatus.BAD_REQUEST, "E400004", "잘못된 JSON 형식입니다."),

    // 401 UNAUTHORIZED
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E401001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "E401002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "E401003", "만료된 토큰입니다."),

    // 403 FORBIDDEN
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "E403001", "접근 권한이 없습니다."),

    // 404 NOT FOUND
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404001", "요청한 리소스를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "E404002", "사용자를 찾을 수 없습니다."),
    NOT_FOUND_END_POINT(HttpStatus.NOT_FOUND, "E404003", "존재하지 않는 API 엔드포인트입니다."),

    // 409 CONFLICT
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "E409001", "이미 존재하는 리소스입니다."),

    // 500 INTERNAL SERVER ERROR
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500001", "서버 내부 오류가 발생했습니다."),

    // friends
    FRIEND_REQUEST_SELF(HttpStatus.BAD_REQUEST, "F400001", "자기 자신에게 친구 요청을 보낼 수 없습니다."),
    FRIEND_REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "F409001", "이미 친구 요청이 존재합니다."),
    FRIEND_REQUEST_REVERSE_EXISTS(HttpStatus.CONFLICT, "F409002", "이미 친구 관계이거나 요청이 반대로 존재합니다."),

    FRIEND_SENDER_NOT_FOUND(HttpStatus.NOT_FOUND, "F404001", "요청자 정보 없음"),
    FRIEND_RECEIVER_NOT_FOUND(HttpStatus.NOT_FOUND, "F404002", "수신자 정보 없음"),

    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "F404003", "요청 정보 없음"),
    FRIEND_ACCEPT_FORBIDDEN(HttpStatus.FORBIDDEN, "F403001", "해당 요청을 수락할 권한이 없습니다."),
    FRIEND_REJECT_FORBIDDEN(HttpStatus.FORBIDDEN, "F403002", "해당 요청을 거절할 권한이 없습니다."),
    FRIEND_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "F403003", "친구 삭제할 권한이 없습니다."),

    // recruitment
    RECRUITMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "R404001", "모집글이 존재하지 않습니다."),
    RECRUITMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "R403001", "모집글 수정 권한이 없습니다."),
    RECRUITMENT_EXPIRED(HttpStatus.BAD_REQUEST, "R400001", "이미 마감된 모집글입니다."),
    RECRUITMENT_ALLREADY_FULL(HttpStatus.BAD_REQUEST, "R400003",
            "선택하신 인원을 포함하면 모집 정원(${totalSpots}명)이 초과됩니다. 정원을 확인해주세요."),

    // recruitment_apply
    ALREADY_APPLIED(HttpStatus.BAD_REQUEST, "A400001", "이미 신청하셨습니다."),
    RECRUITMENT_FULL(HttpStatus.BAD_REQUEST, "A400002", "모집 정원이 마감되었습니다."),

    APPLY_NOT_FOUND(HttpStatus.NOT_FOUND, "A404001", "신청 내역을 찾을 수 없습니다."),
    INVALID_APPLY_STATUS(HttpStatus.BAD_REQUEST, "A400003", "유효하지 않은 신청 상태입니다. (취소 불가)");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
