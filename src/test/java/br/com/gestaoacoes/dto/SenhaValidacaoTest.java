package br.com.gestaoacoes.dto;

import br.com.gestaoacoes.model.Papel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD ciclo 3 (Aula 15, seção 18 — valores de fronteira): a senha deve ter entre 8 e 72
 * caracteres. O limite superior vem do BCrypt, que só considera os primeiros 72 bytes e, no
 * Spring Security atual, recusa senhas maiores com exceção — sem a validação, isso viraria 500.
 * Testa exatamente os quatro valores que a aula indica: 7, 8, 72 e 73.
 */
class SenhaValidacaoTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void criarValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fecharValidator() {
        factory.close();
    }

    @ParameterizedTest(name = "senha com {0} caracteres válida? {1}")
    @CsvSource({
            "7, false",
            "8, true",
            "72, true",
            "73, false"
    })
    void cadastroPeloAdministradorRespeitaLimitesDaSenha(int tamanho, boolean valida) {
        UsuarioCreateRequest request = new UsuarioCreateRequest("Ana", "ana@exemplo.com", "a".repeat(tamanho), Papel.USER);

        assertThat(violacoesDeSenha(validator.validate(request)) == 0).isEqualTo(valida);
    }

    @ParameterizedTest(name = "senha com {0} caracteres válida? {1}")
    @CsvSource({
            "7, false",
            "8, true",
            "72, true",
            "73, false"
    })
    void autocadastroRespeitaLimitesDaSenha(int tamanho, boolean valida) {
        RegistrarUsuarioRequest request = new RegistrarUsuarioRequest("Ana", "ana@exemplo.com", "a".repeat(tamanho));

        assertThat(violacoesDeSenha(validator.validate(request)) == 0).isEqualTo(valida);
    }

    @ParameterizedTest(name = "senha com {0} caracteres válida? {1}")
    @CsvSource({
            "7, false",
            "8, true",
            "72, true",
            "73, false"
    })
    void trocaDeSenhaRespeitaLimitesDaSenha(int tamanho, boolean valida) {
        AlterarSenhaRequest request = new AlterarSenhaRequest("a".repeat(tamanho));

        assertThat(violacoesDeSenha(validator.validate(request)) == 0).isEqualTo(valida);
    }

    private static long violacoesDeSenha(Set<? extends ConstraintViolation<?>> violacoes) {
        return violacoes.stream()
                .filter(v -> v.getPropertyPath().toString().equals("senha"))
                .count();
    }
}
