package br.com.gestaoacoes.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converte o {@link Jwt} já validado (assinatura, emissor, expiração — tudo isso é
 * responsabilidade do {@link org.springframework.security.oauth2.jwt.JwtDecoder}, não deste
 * conversor) em uma {@link org.springframework.security.core.Authentication} do Spring Security.
 *
 * <p>Diferença deliberada do conversor padrão do Spring ({@code JwtAuthenticationConverter}):
 * o principal aqui é o {@code Long usuarioId} (claim {@code sub}), não o objeto {@link Jwt}
 * inteiro nem uma string — isso preserva {@code @AuthenticationPrincipal Long usuarioId} já usado
 * em {@code CarteiraController}/{@code ProventoController} antes desta aula, sem precisar tocar
 * nesses controllers. As authorities vêm da claim {@code authorities} (ex.: {@code ["ROLE_ADMIN"]})
 * emitida por {@link JwtService}, e são o que {@code @PreAuthorize("hasRole('ADMIN')")} (Aula 14)
 * avalia.
 */
@Component
public class JwtPrincipalConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String CLAIM_AUTHORITIES = "authorities";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        List<GrantedAuthority> authorities = jwt.getClaimAsStringList(CLAIM_AUTHORITIES) == null
                ? List.of()
                : jwt.getClaimAsStringList(CLAIM_AUTHORITIES).stream()
                        .map(SimpleGrantedAuthority::new)
                        .map(GrantedAuthority.class::cast)
                        .toList();

        return new UsernamePasswordAuthenticationToken(usuarioId, jwt, authorities);
    }
}
