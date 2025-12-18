package com.example.ureka02.authTest.jwtTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.Test;

import com.example.ureka02.global.auth.jwt.JwtTokenUtil;

import io.jsonwebtoken.Claims;

class JwtTokenUtilTest {

    private static final String TEST_SECRET = "01234567890123456789012345678901";

    @Test
    void createToken_에서_설정한_id를_getId로_꺼낼수있다() {
        // given
        long userId = 123L;
        long expireMs = 60 * 60 * 1000L; // 1시간
        JwtTokenUtil jwtTokenUtil = new JwtTokenUtil(TEST_SECRET, expireMs);

        // when
        String token = jwtTokenUtil.createToken(userId);

        // then
        long extractedId = jwtTokenUtil.getId(token);
        assertThat(extractedId).isEqualTo(userId);
    }

    @Test
    void createToken_발급직후에는_isExpired가_false를_리턴한다() {
        // given
        long userId = 1L;
        long expireMs = 60 * 1000L; // 1분
        JwtTokenUtil jwtTokenUtil = new JwtTokenUtil(TEST_SECRET, expireMs);

        // when
        String token = jwtTokenUtil.createToken(userId);

        // then
        boolean expired = jwtTokenUtil.isExpired(token);
        assertThat(expired).isFalse();
    }

    @Test
    void 만료시간이_과거이면_isExpired가_true를_리턴한다() {
        // given
        long userId = 1L;
        long expireMs = -1000L; // 지금 기준으로 이미 만료되도록 (과거 시간)
        JwtTokenUtil jwtTokenUtil = new JwtTokenUtil(TEST_SECRET, expireMs);

        // when
        String token = jwtTokenUtil.createToken(userId);

        // then
        boolean expired = jwtTokenUtil.isExpired(token);
        assertThat(expired).isTrue();
    }

    @Test
    void extractClaims_로_클레임과_만료시간을_읽어올_수_있다() {
        // given
        long userId = 42L;
        long expireMs = 60 * 60 * 1000L; // 1시간
        JwtTokenUtil jwtTokenUtil = new JwtTokenUtil(TEST_SECRET, expireMs);

        // when
        String token = jwtTokenUtil.createToken(userId);
        Claims claims = jwtTokenUtil.extractClaims(token);

        // then
        assertThat(claims.get("id", Object.class).toString()).isEqualTo(String.valueOf(userId));
        assertThat(claims.getExpiration()).isAfter(new Date(System.currentTimeMillis() - 1000));
        assertThat(claims.getIssuedAt()).isBeforeOrEqualTo(new Date());
    }
}
