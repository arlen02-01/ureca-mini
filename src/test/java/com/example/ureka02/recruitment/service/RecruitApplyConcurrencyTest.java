package com.example.ureka02.recruitment.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

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
class RecruitApplyConcurrencyTest {
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

    @Test
    void 동시에_신청시_정원만큼만_원자적_성공() throws Exception {
        int totalSpots = 10;
        int threads = 50; // 동시에 50명이 신청하면 정원 10명만 성공

        User creator = userRepository.save(User.builder()
                .provider(AuthProvider.LOCAL)
                .email("creator_" + System.nanoTime() + "@test.com")
                .name("creator_" + System.nanoTime())
                .role(Role.USER)
                .password("pw")
                .socialId(null) // LOCAL이면 null 가능
                .build());

        Recruitment recruitment = recruitRepository.save(Recruitment.builder()
                .title("동시성 테스트")
                .description("정원 제한 테스트")
                .totalSpots(totalSpots)
                .endTime(LocalDateTime.now().plusMinutes(10))
                .creator(creator)
                .build());

        List<User> users = new ArrayList<>();
        long base = System.nanoTime();

        for (int i = 0; i < threads; i++) {
            users.add(userRepository.save(User.builder()
                    .provider(AuthProvider.LOCAL)
                    .email("user_" + base + "_" + i + "@test.com")
                    .name("user_" + base + "_" + i) // name 유니크 제약 피함
                    .role(Role.USER)
                    .password("pw")
                    .socialId(null)
                    .build()));
        }

        // 테스트 간 간섭 방지 : Redis 카운티 키 초기화
        String countKey = "recruit:count:" + recruitment.getId();
        stringRedisTemplate.delete(countKey);

        ExecutorService pool = Executors.newFixedThreadPool(threads);

        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fullFail = new AtomicInteger();
        AtomicInteger otherFail = new AtomicInteger();

        // when
        for (User u : users) {
            pool.submit(() -> {
                ready.countDown(); // 준비 완료 표시
                try {
                    start.await(); // 출발 신호 기다렸다가 동시에 시작

                    recruitApplyService.applyRecruitment(recruitment.getId(), u.getId());
                    success.incrementAndGet();

                } catch (CommonException e) {
                    if (e.getErrorCode() == ErrorCode.RECRUITMENT_FULL) {
                        fullFail.incrementAndGet();
                    } else {
                        otherFail.incrementAndGet();
                    }
                } catch (Exception e) {
                    otherFail.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        // 모두 준비될 때까지 기다렸다가
        ready.await();
        // 동시에 출발
        start.countDown();
        // 전부 끝날 때까지 대기
        done.await();

        pool.shutdown();

        // then
        assertThat(success.get()).isEqualTo(totalSpots);
        assertThat(fullFail.get()).isEqualTo(threads - totalSpots);
        assertThat(otherFail.get()).isEqualTo(0);

        Recruitment refreshed = recruitRepository.findById(recruitment.getId()).orElseThrow();
        assertThat(refreshed.getCurrentSpots()).isEqualTo(totalSpots);

        log.error("=====================================");
        log.error("✅ 동시성 테스트 성공 결과");
        log.error("총 시도 스레드 수: {}", threads);
        log.error("총 정원 (기대값): {}", totalSpots);
        log.error("-------------------------------------");
        log.error("최종 성공 카운트 (success): {}", success.get());
        log.error("정원 초과 실패 (fullFail): {}", fullFail.get());
        log.error("기타 실패 (otherFail): {}", otherFail.get());
        log.error("DB 최종 현재 정원 (dbCurrentSpots): {}", refreshed.getCurrentSpots());
        log.error("=====================================");

    }

}
