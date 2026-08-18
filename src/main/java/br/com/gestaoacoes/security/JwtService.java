package br.com.gestaoacoes.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(@Value("${app.jwt.secret}") String segredo,
                       @Value("${app.jwt.expiration-minutes}") long expiracaoMinutos) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public String gerarToken(Long usuarioId, String email) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .claim("email", email)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(expiracaoMinutos * 60)))
                .signWith(chave)
                .compact();
    }

    /**
     * Retorna o id do usuário (claim {@code sub}) se o token for válido e não estiver expirado,
     * ou {@code null} caso contrário — nunca lança exceção para não vazar detalhes de validação
     * ao chamador do filtro.
     */
    public Long validarEExtrairUsuarioId(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Long.valueOf(subject);
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }
}