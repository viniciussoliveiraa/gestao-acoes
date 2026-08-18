package br.com.gestaoacoes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

// UserDetailsServiceAutoConfiguration excluída: a autenticação é JWT stateless própria
// (JwtService/JwtAuthenticationFilter), sem UserDetailsService/AuthenticationManager do Spring
// Security — sem essa exclusão, o Boot gera um usuário em memória com senha aleatória a cada
// subida (ruído no log, nunca usado).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableFeignClients
public class GestaoAcoesApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestaoAcoesApplication.class, args);
    }

}
