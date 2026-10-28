package br.com.gestaoacoes.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cobre os experimentos obrigatórios da Aula 13 (seção 12.4) ao nível do {@link JwtDecoder}: o
 * mesmo decoder configurado em {@code SecurityConfig}, exercitado diretamente (sem subir o
 * contexto Spring inteiro) contra token adulterado, expirado e de emissor diferente.
 */
class JwtDecoderValidationTest {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-32-caracteres-1234";
    private static final String OUTRO_SEGREDO = "outro-segredo-completamente-diferente-e-longo-o-suficiente";
    private static final String EMISSOR = "gestao-acoes";

    private SecretKey chave(String segredo) {
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private JwtDecoder decoder(SecretKey chave, String emissorEsperado) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(emissorEsperado));
        return decoder;
    }

    private String token(SecretKey chave, String emissor, Instant emitidoEm, Instant expiraEm) {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(chave));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emissor)
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .subject("1")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Test
    void tokenValidoDecodificaComSucesso() {
        SecretKey chave = chave(SEGREDO);
        Instant agora = Instant.now();
        String token = token(chave, EMISSOR, agora, agora.plusSeconds(3600));

        assertThat(decoder(chave, EMISSOR).decode(token).getSubject()).isEqualTo("1");
    }

    @Test
    void tokenComAssinaturaDiferenteNaoDecodifica() {
        SecretKey chave = chave(SEGREDO);
        Instant agora = Instant.now();
        String token = token(chave, EMISSOR, agora, agora.plusSeconds(3600));

        JwtDecoder decoderComOutraChave = decoder(chave(OUTRO_SEGREDO), EMISSOR);

        assertThatThrownBy(() -> decoderComOutraChave.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenExpiradoNaoDecodifica() {
        SecretKey chave = chave(SEGREDO);
        Instant passado = Instant.now().minusSeconds(3600);
        String token = token(chave, EMISSOR, passado, passado.plusSeconds(10));

        assertThatThrownBy(() -> decoder(chave, EMISSOR).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenComEmissorDiferenteDoEsperadoNaoDecodifica() {
        SecretKey chave = chave(SEGREDO);
        Instant agora = Instant.now();
        String token = token(chave, "outro-emissor", agora, agora.plusSeconds(3600));

        assertThatThrownBy(() -> decoder(chave, EMISSOR).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenMalformadoNaoDecodifica() {
        assertThatThrownBy(() -> decoder(chave(SEGREDO), EMISSOR).decode("token-invalido"))
                .isInstanceOf(JwtException.class);
    }
}
