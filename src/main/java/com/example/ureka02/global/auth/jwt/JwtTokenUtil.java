package com.example.ureka02.global.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenUtil {
	private final Key secretKey;
	private final long expireTimeMs;

    @Autowired
    public JwtTokenUtil(@Value("${jwt.secret}") String key, @Value("${jwt.expiration}") long expireTimeMs) {
        this.secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
        this.expireTimeMs=expireTimeMs;
    }
    
	public String createToken(long id) {
		Claims claims = Jwts.claims();
		//claims 여기가 pay로드로 원하는 값을 저장하면 된다.
		claims.put("id", id);
		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis()+expireTimeMs))
				.signWith(secretKey,SignatureAlgorithm.HS256)
				.compact();
	}
	
	public Claims extractClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(secretKey)
				.build()
				.parseClaimsJws(token)
				.getBody();
	}
	
	public boolean isExpired(String token) {
		try {
			Date expiration = extractClaims(token).getExpiration();
			return expiration.before(new Date());
		} catch (Exception e) {
			return true;
		}
	}
	
	public long getId(String token) {
		return Long.parseLong(extractClaims(token).get("id").toString());
	}
}
