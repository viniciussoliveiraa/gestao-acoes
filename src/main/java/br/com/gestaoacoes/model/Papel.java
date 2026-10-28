package br.com.gestaoacoes.model;

/**
 * Papel (role) atribuído a um {@link Usuario}, base do controle de acesso (RBAC): {@code ADMIN}
 * administra o catálogo de corretoras/ações e os demais usuários; {@code USER} só opera a própria
 * carteira e proventos.
 */
public enum Papel {
    ADMIN,
    USER
}
