package com.example.ureka02.recruitment.service;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ureka02.recruitment.Enum.RecruitApplyStatus;
import com.example.ureka02.recruitment.Enum.RecruitMemberRole;
import com.example.ureka02.recruitment.Enum.RecruitStatus;
import com.example.ureka02.recruitment.dto.response.RecruitCompletedResponse;
//import com.example.ureka02.recruitment.dto.response.CreateMemberResponse;
import com.example.ureka02.recruitment.dto.response.RecruitDetailResponse;
import com.example.ureka02.recruitment.dto.response.RecruitMemberResponse;
import com.example.ureka02.recruitment.entity.Recruitment;
import com.example.ureka02.recruitment.entity.RecruitmentApply;
import com.example.ureka02.recruitment.entity.RecruitmentMember;
import com.example.ureka02.recruitment.repository.RecruitApplyRepository;
import com.example.ureka02.recruitment.repository.RecruitMemberRepository;
import com.example.ureka02.recruitment.repository.RecruitRepository;
import com.example.ureka02.user.User;
import com.example.ureka02.user.UserRepository;

import jakarta.transaction.TransactionScoped;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecruitMemberService {

        private final RecruitRepository recruitRepository;
        private final RecruitApplyRepository recruitApplyRepository;
        private final RecruitMemberRepository recruitMemberRepository;
        private final UserRepository userRepository;

        @Transactional
        public RecruitCompletedResponse completeRecruitment(Long recruitId, Long creatorId) {
                /*
                 * 모집글 상태를 COMPLETED 로 변경하고 멤버 생성
                 */

                User creator = userRepository.findById(creatorId)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

                Recruitment recruitment = recruitRepository.findById(recruitId)
                                .orElseThrow(() -> new IllegalArgumentException("모집글이 존재하지 않습니다."));
                // + RuntimeException 대신 CommonException + ErrorCode 로 변경

                if (!recruitment.getCreator().getId().equals(creatorId)) {
                        throw new IllegalStateException("변경 권한이 없습니다.");
                }

                // 모집 상태를 COMPLETED 로 변경
                recruitment.complete(); // 영속 상태라 @Transactional 안에서 flush 시 DB에 반영

                RecruitmentMember admin = RecruitmentMember.builder()
                                .recruitment(recruitment)
                                .member(creator)
                                .role(RecruitMemberRole.ADMIN)
                                .build();

                recruitMemberRepository.save(admin);

                // 해당 모집글에 APPLIED 상태로 신청한 팀원 조회
                List<RecruitmentApply> applications = recruitApplyRepository.findByRecruitmentIdAndStatus(recruitId,
                                RecruitApplyStatus.APPLIED);

                // 멤버 Entity 에 팀원 추가(admin 제외)
                for (RecruitmentApply application : applications) {
                        User applier = application.getApplier();

                        // 작성자와 동일한 유저면 ADMIN으로 이미 들어갔으므로 스킵
                        if (applier.getId().equals(creatorId)) {
                                continue;
                        }

                        RecruitmentMember member = RecruitmentMember.builder()
                                        .recruitment(recruitment).member(applier).role(RecruitMemberRole.MEMBER)
                                        .build();
                        recruitMemberRepository.save(member);
                }

                // 이 모집글의 모든 멤버 다시 조회
                List<RecruitmentMember> memberEntities = recruitMemberRepository.findByRecruitmentId(recruitId);
                List<RecruitMemberResponse> memberResponses = toMemberResponse(memberEntities);

                return RecruitCompletedResponse.builder()
                                .recruitId(recruitment.getId())
                                .status(recruitment.getStatus().name()) // "COMPLETED"
                                .members(memberResponses)
                                .build();

        }

        // 컨버터 메소드 작성
        private List<RecruitMemberResponse> toMemberResponse(List<RecruitmentMember> memberEntities) {
                List<RecruitMemberResponse> list = new ArrayList<>();

                for (RecruitmentMember m : memberEntities) {
                        RecruitMemberResponse dto = RecruitMemberResponse.builder()
                                        .memberId(m.getId())
                                        .userId(m.getMember().getId())
                                        .name(m.getMember().getName())
                                        .role(m.getRole())
                                        .build();

                        list.add(dto);
                }

                return list;
        }
}