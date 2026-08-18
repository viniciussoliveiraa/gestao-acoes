package br.com.gestaoacoes.mapper;

import br.com.gestaoacoes.dto.UsuarioResponse;
import br.com.gestaoacoes.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getCriadoEm());
    }
}