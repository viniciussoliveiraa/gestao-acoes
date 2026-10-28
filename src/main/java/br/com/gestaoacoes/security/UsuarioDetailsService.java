package br.com.gestaoacoes.security;

import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carrega o {@link Usuario} do banco para o {@code DaoAuthenticationProvider} (configurado
 * automaticamente pelo Spring a partir deste bean + do {@code PasswordEncoder}), usado somente em
 * {@code POST /auth/login} (Aula 13). Requisições subsequentes são autenticadas pelo JWT já
 * emitido, decodificado por {@link org.springframework.security.oauth2.jwt.JwtDecoder} — este
 * serviço não entra nesse caminho.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        String emailNormalizado = email.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        return User.withUsername(usuario.getEmail())
                .password(usuario.getSenhaHash())
                .roles(usuario.getPapel().name())
                .disabled(!usuario.getAtivo())
                .build();
    }
}
