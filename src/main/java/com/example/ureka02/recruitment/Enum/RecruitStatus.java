package com.example.ureka02.recruitment.Enum;

public enum RecruitStatus {
    OPEN, // 모집중
    CLOSED, // 모집 마감 -> 자동으로 서버에서 처리해줌(신청 불가)
    COMPLETED // 모집 완료 -> 신청 취소 및 모집 상태 변경 불가
}