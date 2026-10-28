package br.com.gestaoacoes.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Validação do JWT (assinatura, emissor, expiração) é responsabilidade do JwtDecoder, exercitada
// via MockMvc com a stack de segurança real (ver AuthFluxoIntegrationTest/SegurancaIntegrationTest)
// — este teste cobre só a emissão (encode), responsabilidade deste serviço.
class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-32-caracteres-1234";

    private JwtService jwtService(JwtProperties properties) {
        SecretKey chave = new SecretKeySpec(SEGREDO.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(chave));
        return new JwtService(encoder, properties);
    }

    @Test
    void geraTokenComSubjectEAuthorities() {
        JwtService service = jwtService(new JwtProperties(SEGREDO, 120, "gestao-acoes"));

        String token = service.gerarToken(42L, "ana@exemplo.com", List.of("ROLE_ADMIN"));

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void jwtPropertiesRejeitaSegredoCurto() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new JwtProperties("curto-demais", 120, "gestao-acoes"));
    }

    @Test
    void jwtPropertiesRejeitaExpiracaoNaoPositiva() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new JwtProperties(SEGREDO, 0, "gestao-acoes"));
    }

    @Test
    void jwtPropertiesRejeitaEmissorEmBranco() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new JwtProperties(SEGREDO, 120, " "));
    }
}
