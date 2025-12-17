package com.example.ureka02.test;

import com.example.ureka02.recruitment.Enum.RecruitApplyStatus;
import com.example.ureka02.recruitment.Enum.RecruitMemberRole;
import com.example.ureka02.recruitment.entity.Recruitment;
import com.example.ureka02.recruitment.entity.RecruitmentApply;
import com.example.ureka02.recruitment.entity.RecruitmentMember;
import com.example.ureka02.recruitment.repository.RecruitApplyRepository;
import com.example.ureka02.recruitment.repository.RecruitMemberRepository;
import com.example.ureka02.recruitment.repository.RecruitRepository;
import com.example.ureka02.settlement.service.SettlementService;
import com.example.ureka02.user.User;
import com.example.ureka02.user.UserRepository;
import com.example.ureka02.user.customUserDetails.CustomUserDetails;
import com.example.ureka02.user.enums.AuthProvider;
import com.example.ureka02.user.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class TestDataController {

    private final UserRepository userRepository;
    private final RecruitRepository recruitRepository;
    private final RecruitApplyRepository recruitApplyRepository;
    private final RecruitMemberRepository recruitMemberRepository;
    private final SettlementService settlementService;

    /**
     * 테스트 데이터 생성
     * POST /test/create-sample-data
     */
    @PostMapping("/create-sample-data")
    public ResponseEntity<Map<String, Object>> createSampleData(@AuthenticationPrincipal CustomUserDetails principal) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 방장 = 현재 로그인한 사용자
            User creator = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            log.info("✅ 방장: {}", creator.getName());
            // 2. 사용자 생성
            User user2 = createUserIfNotExists("solotache@khu.ac.kr", "김원기");
            User user3 = createUserIfNotExists("pyb2675@dankook.ac.kr", "박유빈");
            User user4 = createUserIfNotExists("arlen0201@gmail.com", "이창기");
            User user5 = createUserIfNotExists("leeemjw@gmail.com", "임지우");
            User user6 = createUserIfNotExists("gnstjrdlsla18@gmail.com", "최훈석");

            log.info("✅ 사용자 생성 완료");

            // 3. 모집글 생성
            Recruitment recruitment = Recruitment.builder()
                    .title("점심 같이 드실 분 구해요!")
                    .description("오늘 12시에 학식 같이 가실 분~")
                    .totalSpots(6)
                    .endTime(LocalDateTime.now().plusDays(1))
                    .creator(creator)
                    .build();
            recruitment = recruitRepository.save(recruitment);

            log.info("✅ 모집글 생성 완료 - ID: {}", recruitment.getId());

            // 4. 신청자 생성
            RecruitmentApply apply2 = RecruitmentApply.builder()
                    .recruitment(recruitment)
                    .applier(user2)
                    .build();
            recruitApplyRepository.save(apply2);

            RecruitmentApply apply3 = RecruitmentApply.builder()
                    .recruitment(recruitment)
                    .applier(user3)
                    .build();
            recruitApplyRepository.save(apply3);

            RecruitmentApply apply4 = RecruitmentApply.builder()
                    .recruitment(recruitment)
                    .applier(user4)
                    .build();
            recruitApplyRepository.save(apply4);

            RecruitmentApply apply5 = RecruitmentApply.builder()
                    .recruitment(recruitment)
                    .applier(user5)
                    .build();
            recruitApplyRepository.save(apply5);

            RecruitmentApply apply6 = RecruitmentApply.builder()
                    .recruitment(recruitment)
                    .applier(user6)
                    .build();
            recruitApplyRepository.save(apply6);

            recruitment.initializeSpots(5); // 신청자 5명 + 방장 1명
            recruitRepository.save(recruitment);

            log.info("✅ 신청자 생성 완료");

            // 4. 모집 완료 처리
            recruitment.complete();
            recruitRepository.save(recruitment);

            // 방장 추가
            RecruitmentMember admin = RecruitmentMember.builder()
                    .recruitment(recruitment)
                    .member(creator)
                    .role(RecruitMemberRole.ADMIN)
                    .build();
            recruitMemberRepository.save(admin);

            // 신청자들 추가
            List<RecruitmentApply> applications = recruitApplyRepository
                    .findByRecruitmentIdAndStatus(recruitment.getId(), RecruitApplyStatus.APPLIED);

            for (RecruitmentApply application : applications) {
                RecruitmentMember member = RecruitmentMember.builder()
                        .recruitment(recruitment)
                        .member(application.getApplier())
                        .role(RecruitMemberRole.MEMBER)
                        .build();
                recruitMemberRepository.save(member);
            }

            log.info("✅ 모집 완료 처리");

            // 5. 정산 자동 생성
            settlementService.createSettlement(admin, 90000L);

            log.info("✅ 정산 자동 생성 완료");

            result.put("success", true);
            result.put("message", "테스트 데이터가 생성되었습니다.");
            result.put("recruitmentId", recruitment.getId());
            result.put("users", List.of(
                    Map.of("id", user2.getId(), "name", user2.getName()),
                    Map.of("id", user3.getId(), "name", user3.getName()),
                    Map.of("id", user4.getId(), "name", user4.getName()),
                    Map.of("id", user5.getId(), "name", user5.getName()),
                    Map.of("id", user6.getId(), "name", user6.getName())
            ));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("테스트 데이터 생성 실패", e);
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    private User createUserIfNotExists(String email, String name) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .name(name)
                        .provider(AuthProvider.LOCAL)
                        .role(Role.USER)
                        .password("$2a$10$dummyPasswordHash")
                        .build()));
    }
}