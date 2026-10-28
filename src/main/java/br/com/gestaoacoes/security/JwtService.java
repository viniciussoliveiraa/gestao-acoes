package br.com.gestaoacoes.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Emite o JWT de acesso (Aula 13) assinado com HS256 pelo {@link JwtEncoder} do Spring Security.
 * A validação (assinatura, emissor, expiração) é responsabilidade do
 * {@link org.springframework.security.oauth2.jwt.JwtDecoder}, configurado em
 * {@link br.com.gestaoacoes.config.SecurityConfig} e acionado automaticamente pelo
 * {@code oauth2ResourceServer} em toda requisição — este serviço não valida tokens.
 */
@Component
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    /**
     * @param usuarioId  vira a claim {@code sub} (subject) — é o que {@link JwtPrincipalConverter}
     *                   extrai de volta como o principal autenticado em cada requisição.
     * @param email      claim informativa, não usada para autorização.
     * @param authorities ex.: {@code ["ROLE_ADMIN"]} — é o que {@code @PreAuthorize} avalia.
     */
    public String gerarToken(Long usuarioId, String email, List<String> authorities) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plusSeconds(jwtProperties.expirationMinutes() * 60);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .subject(String.valueOf(usuarioId))
                .claim("email", email)
                .claim("authorities", authorities)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
