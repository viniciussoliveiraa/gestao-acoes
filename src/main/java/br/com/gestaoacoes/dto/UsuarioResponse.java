package br.com.gestaoacoes.dto;

import br.com.gestaoacoes.model.Papel;

import java.time.OffsetDateTime;

public record UsuarioResponse(Long id, String nome, String email, Papel papel, Boolean ativo, OffsetDateTime criadoEm) {
}
