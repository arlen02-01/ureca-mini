package com.example.ureka02.recruitment.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles; // 필요한 경우 프로필 설정

import com.example.ureka02.global.error.CommonException;
import com.example.ureka02.global.error.ErrorCode;
import com.example.ureka02.recruitment.entity.Recruitment;
import com.example.ureka02.recruitment.repository.RecruitApplyRepository;
import com.example.ureka02.recruitment.repository.RecruitRepository;
import com.example.ureka02.user.User;
import com.example.ureka02.user.UserRepository;
import com.example.ureka02.user.enums.AuthProvider;
import com.example.ureka02.user.enums.Role;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
class RecruitApplyConcurrencyFinalTest {
    @Autowired
    RecruitApplyService recruitApplyService;
    @Autowired
    RecruitApplyRepository recruitApplyRepository;
    @Autowired
    RecruitRepository recruitRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private static final int TOTAL_SPOTS = 10;
    private static final int THREADS = 50; // 동시에 50명이 신청
    private Long recruitmentId;
    private List<Long> userIds = new ArrayList<>();
    private String countKey;

    @BeforeEach
    void setup() {
        // 1. 모집글 생성자 생성
        User creator = userRepository.save(User.builder()
                .provider(AuthProvider.LOCAL)
                .email("creator_concurrency@test.com")
                .name("creator_concurrency")
                .role(Role.USER)
                .password("pw").build());

        // 2. 모집글 생성 (정원 10명)
        Recruitment recruitment = recruitRepository.save(Recruitment.builder()
                .title("동시성 테스트")
                .description("정원 제한 테스트")
                .totalSpots(TOTAL_SPOTS)
                .endTime(LocalDateTime.now().plusMinutes(10))
                .creator(creator)
                .build());
        recruitmentId = recruitment.getId();
        countKey = "recruit:count:" + recruitmentId;

        // 3. 테스트 유저 생성 (50명)
        for (int i = 0; i < THREADS; i++) {
            User user = userRepository.save(User.builder()
                    .provider(AuthProvider.LOCAL)
                    .email("user_conc_" + i + "@test.com")
                    .name("user_conc_" + i)
                    .role(Role.USER)
                    .password("pw").build());
            userIds.add(user.getId());
        }

        // 4. Redis 카운트 초기화
        // DB의 currentSpots이 0이므로, Redis도 0으로 명시적으로 초기화합니다.
        stringRedisTemplate.delete(countKey);
        stringRedisTemplate.opsForValue().set(countKey, "0");
    }

    @AfterEach
    void cleanup() {
        // 테스트 후 데이터 정리
        recruitApplyRepository.deleteAll();
        recruitRepository.deleteAll();
        userRepository.deleteAll();
        stringRedisTemplate.delete(countKey);
    }

    @Test
    void 동시에_신청시_정원만큼만_원자적_성공() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        // 스레드 시작 동기화 도구
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);

        // 결과 카운터
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fullFail = new AtomicInteger();
        AtomicInteger otherFail = new AtomicInteger();

        // when
        for (Long userId : userIds) {
            pool.submit(() -> {
                ready.countDown(); // 준비 완료 표시
                try {
                    start.await(); // 출발 신호 대기

                    // 핵심 로직 실행
                    recruitApplyService.applyRecruitment(recruitmentId, userId);
                    success.incrementAndGet();

                } catch (CommonException e) {
                    if (e.getErrorCode() == ErrorCode.RECRUITMENT_FULL) {
                        fullFail.incrementAndGet();
                    } else {
                        otherFail.incrementAndGet();
                        log.error("Other failure for user {}: {}", userId, e.getMessage());
                    }
                } catch (Exception e) {
                    otherFail.incrementAndGet();
                    log.error("Unexpected exception for user {}: {}", userId, e.getMessage(), e);
                } finally {
                    done.countDown();
                }
            });
        }

        // 모든 스레드가 준비될 때까지 기다렸다가
        ready.await();
        // 동시에 출발 신호
        start.countDown();
        // 모든 스레드가 끝날 때까지 대기
        done.await();

        pool.shutdown();

        // then
        // 1. 성공 횟수 검증: 정확히 정원 수만큼 성공해야 함
        assertThat(success.get()).isEqualTo(TOTAL_SPOTS);
        // 2. 정원 초과 실패 횟수 검증: 전체 스레드 수에서 성공 횟수를 뺀 만큼 실패해야 함
        assertThat(fullFail.get()).isEqualTo(THREADS - TOTAL_SPOTS);
        // 3. 다른 종류의 실패는 없어야 함
        assertThat(otherFail.get()).isEqualTo(0);

        // 4. DB 최종 상태 검증
        Recruitment refreshed = recruitRepository.findById(recruitmentId).orElseThrow();
        // DB에 저장된 최종 인원수도 정원 수와 같아야 함 (Lost Update 방지 확인)
        assertThat(refreshed.getCurrentSpots()).isEqualTo(TOTAL_SPOTS);

        // 5. 신청 기록 검증
        long totalAppliesInDB = recruitApplyRepository.countByRecruitmentId(recruitmentId);
        assertThat(totalAppliesInDB).isEqualTo(TOTAL_SPOTS);

        // 6. Redis 카운터 검증 (실패 스레드가 DECR을 호출했으므로, 최종값은 정원 수와 같아야 함)
        Long finalRedisCount = stringRedisTemplate.opsForValue().increment(countKey, 0); // 현재 값 조회
        // 최종적으로 성공한 신청 수만큼 Redis 카운터가 남아있어야 합니다.
        // 첫 번째 단계 (Redis INCR)에서 50번 실행 -> 50.
        // 실패한 40번의 DECR 실행 -> 50 - 40 = 10.
        assertThat(finalRedisCount).isEqualTo((long) TOTAL_SPOTS);

        log.info("--- Concurrency Test Results ---");
        log.info("Total Threads: {}", THREADS);
        log.info("Total Spots: {}", TOTAL_SPOTS);
        log.info("Success Count: {}", success.get());
        log.info("RECRUITMENT_FULL Failure Count: {}", fullFail.get());
        log.info("Other Failure Count: {}", otherFail.get());
        log.info("DB Final CurrentSpots: {}", refreshed.getCurrentSpots());
        log.info("Redis Final Count: {}", finalRedisCount);
    }
}