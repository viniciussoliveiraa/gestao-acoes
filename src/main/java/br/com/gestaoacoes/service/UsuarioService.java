package br.com.gestaoacoes.service;

import br.com.gestaoacoes.dto.AlterarSenhaRequest;
import br.com.gestaoacoes.dto.UsuarioCreateRequest;
import br.com.gestaoacoes.dto.UsuarioUpdateRequest;
import br.com.gestaoacoes.exception.EmailJaCadastradoException;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Gerenciamento administrativo de usuários (Aula 14): só {@code ADMIN} acessa estas operações
 * (reforçado por {@code @PreAuthorize} em {@link br.com.gestaoacoes.controller.UsuarioController}).
 * Nunca recebe nem devolve a entidade {@link Usuario} diretamente nos controllers — ela carrega o
 * hash da senha e é um modelo de persistência, não o contrato público da API.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<Usuario> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Usuario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    @Transactional
    public Usuario criar(UsuarioCreateRequest request) {
        String email = normalizarEmail(request.email());
        validarEmailDisponivel(email, null);

        Usuario usuario = new Usuario(
                request.nome().trim(),
                email,
                passwordEncoder.encode(request.senha()),
                request.papel(),
                true,
                OffsetDateTime.now()
        );
        return repository.save(usuario);
    }

    @Transactional
    public Usuario atualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = buscarPorId(id);
        String email = normalizarEmail(request.email());
        validarEmailDisponivel(email, id);

        usuario.setEmail(email);
        usuario.setPapel(request.papel());
        usuario.setAtivo(request.ativo());
        return repository.save(usuario);
    }

    @Transactional
    public void alterarSenha(Long id, AlterarSenhaRequest request) {
        Usuario usuario = buscarPorId(id);
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        repository.save(usuario);
    }

    private void validarEmailDisponivel(String email, Long idAtual) {
        repository.findByEmail(email)
                .filter(usuario -> !usuario.getId().equals(idAtual))
                .ifPresent(usuario -> {
                    throw new EmailJaCadastradoException("Já existe um usuário cadastrado com este email");
                });
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }
}
