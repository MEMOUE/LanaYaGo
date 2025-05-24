package com.lanayago.security;

import com.lanayago.enums.TypeUtilisateur;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

	private final SecretKey key;
	private final long tokenValidityInMilliseconds;
	private final long refreshTokenValidityInMilliseconds;

	public JwtTokenProvider(@Value("${jwt.secret}") String secret,
	                        @Value("${jwt.expiration:86400000}") long tokenValidityInMilliseconds) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes());
		this.tokenValidityInMilliseconds = tokenValidityInMilliseconds;
		this.refreshTokenValidityInMilliseconds = tokenValidityInMilliseconds * 7; // 7 jours
	}

	public String createToken(String email, TypeUtilisateur typeUtilisateur) {
		Claims claims = Jwts.claims().setSubject(email);
		claims.put("role", "ROLE_" + typeUtilisateur.name());

		Date now = new Date();
		Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(now)
				.setExpiration(validity)
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	public String createRefreshToken(String email) {
		Claims claims = Jwts.claims().setSubject(email);
		claims.put("type", "refresh");

		Date now = new Date();
		Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(now)
				.setExpiration(validity)
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	public String getEmailFromToken(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody()
				.getSubject();
	}

	public String getRoleFromToken(String token) {
		return (String) Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody()
				.get("role");
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			log.error("Token JWT invalide: {}", e.getMessage());
			return false;
		}
	}

	public long getTokenValidityInMilliseconds() {
		return tokenValidityInMilliseconds;
	}
}