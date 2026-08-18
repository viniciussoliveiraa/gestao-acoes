package br.com.gestaoacoes.dto;

import java.time.OffsetDateTime;

public record UsuarioResponse(Long id, String nome, String email, OffsetDateTime criadoEm) {
}