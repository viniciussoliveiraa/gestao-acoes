package br.com.gestaoacoes.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

/**
 * Propriedades tipadas e validadas do JWT (Aula 13, seção "Propriedades tipadas"): a aplicação
 * falha cedo, na inicialização, se o segredo for curto demais ou a expiração/emissor estiverem
 * ausentes, em vez de descobrir isso só quando o primeiro login falhar silenciosamente.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMinutes, String issuer) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.jwt.secret deve possuir pelo menos 32 bytes");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException("app.jwt.expiration-minutes deve ser maior que zero");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("app.jwt.issuer deve ser informado");
        }
    }
}
