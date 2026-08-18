package br.com.gestaoacoes.model;

/**
 * MVP cobre apenas compras/aportes (ver design.md, "Escopo MVP" de gestao-carteira). O valor
 * existe desde já para não exigir uma migration adicional quando venda for implementada.
 */
public enum TipoLancamento {
    COMPRA
}