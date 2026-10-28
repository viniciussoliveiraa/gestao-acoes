package br.com.gestaoacoes.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPrincipalConverterTest {

    private final JwtPrincipalConverter converter = new JwtPrincipalConverter();

    private Jwt jwt(String subject, List<String> authorities) {
        Map<String, Object> claims = authorities == null
                ? Map.of("sub", subject)
                : Map.of("sub", subject, "authorities", authorities);
        return new Jwt("token-fake", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "HS256"), claims);
    }

    @Test
    void converteSubjectParaUsuarioIdELeAuthorities() {
        var authentication = (UsernamePasswordAuthenticationToken) converter.convert(jwt("42", List.of("ROLE_ADMIN")));

        assertThat(authentication.getPrincipal()).isEqualTo(42L);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void semClaimAuthoritiesResultaEmListaVazia() {
        var authentication = (UsernamePasswordAuthenticationToken) converter.convert(jwt("1", null));

        assertThat(authentication.getAuthorities()).isEmpty();
    }
}
