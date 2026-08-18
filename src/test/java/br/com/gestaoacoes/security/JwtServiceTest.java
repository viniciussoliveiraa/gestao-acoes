package br.com.gestaoacoes.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-32-caracteres-1234";

    private final JwtService jwtService = new JwtService(SEGREDO, 120);

    @Test
    void geraTokenEValidaExtraindoUsuarioId() {
        String token = jwtService.gerarToken(42L, "ana@exemplo.com");

        Long usuarioId = jwtService.validarEExtrairUsuarioId(token);

        assertThat(usuarioId).isEqualTo(42L);
    }

    @Test
    void tokenComAssinaturaDiferenteNaoValida() {
        String token = jwtService.gerarToken(1L, "ana@exemplo.com");
        JwtService outroServico = new JwtService("outro-segredo-completamente-diferente-e-longo-o-suficiente", 120);

        assertThat(outroServico.validarEExtrairUsuarioId(token)).isNull();
    }

    @Test
    void tokenMalformadoNaoValida() {
        assertThat(jwtService.validarEExtrairUsuarioId("token-invalido")).isNull();
    }

    @Test
    void tokenExpiradoNaoValida() {
        JwtService servicoComExpiracaoPassada = new JwtService(SEGREDO, -1);
        String token = servicoComExpiracaoPassada.gerarToken(1L, "ana@exemplo.com");

        assertThat(servicoComExpiracaoPassada.validarEExtrairUsuarioId(token)).isNull();
    }
}