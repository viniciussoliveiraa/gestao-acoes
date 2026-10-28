package br.com.gestaoacoes.config;

import br.com.gestaoacoes.model.Papel;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Cria o administrador inicial (Aula 14), já que o autocadastro público em
 * {@code /auth/registrar} sempre cria usuários {@code USER} — sem isso, não haveria como obter o
 * primeiro {@code ADMIN} para promover os demais via {@code /usuarios}.
 *
 * <p>Roda apenas nos perfis {@code dev} e {@code mysql} (nunca em {@code test}, para não exigir
 * {@code APP_ADMIN_PASSWORD} na suíte automatizada) e só insere o usuário se o email configurado
 * ainda não existir — alterar {@code APP_ADMIN_PASSWORD} depois da primeira inicialização não
 * muda o hash já salvo no banco. A senha existe em texto legível somente aqui, durante a
 * inicialização; antes de ser persistida, ela passa pelo {@link PasswordEncoder}.
 */
@Component
@Profile({"dev", "mysql"})
public class AdminInitializer {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String nome;
    private final String email;
    private final String senha;

    public AdminInitializer(UsuarioRepository usuarioRepository,
                             PasswordEncoder passwordEncoder,
                             @Value("${app.bootstrap.admin.nome}") String nome,
                             @Value("${app.bootstrap.admin.email}") String email,
                             @Value("${app.bootstrap.admin.senha}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    @PostConstruct
    public void criarAdministradorInicial() {
        String emailNormalizado = email.trim().toLowerCase();
        if (usuarioRepository.findByEmail(emailNormalizado).isPresent()) {
            return;
        }

        Usuario admin = new Usuario(
                nome,
                emailNormalizado,
                passwordEncoder.encode(senha),
                Papel.ADMIN,
                true,
                OffsetDateTime.now()
        );
        usuarioRepository.save(admin);
    }
}
