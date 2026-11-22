package br.com.gestaoacoes.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Aula 13, seção 5 + Aula 15, seção 18: a configuração do JWT falha na inicialização quando é
 * insegura ou incompleta. Fronteira do segredo: HS256 exige chave de pelo menos 256 bits, ou seja,
 * 32 bytes — 31 é recusado, 32 é aceito.
 */
class JwtPropertiesTest {

    private static final String SEGREDO_32_BYTES = "a".repeat(32);

    @Test
    void segredoComExatamente32BytesEhAceito() {
        assertThatCode(() -> new JwtProperties(SEGREDO_32_BYTES, 60, "gestao-acoes")).doesNotThrowAnyException();
    }

    @Test
    void segredoCom31BytesEhRecusado() {
        assertThatThrownBy(() -> new JwtProperties("a".repeat(31), 60, "gestao-acoes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void segredoAusenteEhRecusado() {
        assertThatThrownBy(() -> new JwtProperties(null, 60, "gestao-acoes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secret");
    }

    @Test
    void tamanhoDoSegredoEhMedidoEmBytesUtf8ENaoEmCaracteres() {
        // 16 caracteres acentuados ocupam 32 bytes em UTF-8: atinge o mínimo.
        JwtProperties propriedades = new JwtProperties("é".repeat(16), 60, "gestao-acoes");

        assertThat(propriedades.secret()).hasSize(16);
    }

    @ParameterizedTest(name = "expiração de {0} minuto(s) é recusada")
    @ValueSource(longs = {0, -1})
    void expiracaoNaoPositivaEhRecusada(long minutos) {
        assertThatThrownBy(() -> new JwtProperties(SEGREDO_32_BYTES, minutos, "gestao-acoes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiration-minutes");
    }

    @Test
    void expiracaoDeUmMinutoEhAceita() {
        assertThat(new JwtProperties(SEGREDO_32_BYTES, 1, "gestao-acoes").expirationMinutes()).isEqualTo(1);
    }

    @ParameterizedTest(name = "emissor \"{0}\" é recusado")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void emissorAusenteOuEmBrancoEhRecusado(String emissor) {
        assertThatThrownBy(() -> new JwtProperties(SEGREDO_32_BYTES, 60, emissor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issuer");
    }
}
