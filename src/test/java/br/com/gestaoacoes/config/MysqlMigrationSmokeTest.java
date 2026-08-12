package br.com.gestaoacoes.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Não há servidor MySQL real disponível no ambiente de desenvolvimento/CI, então este teste
 * sobe o contexto com H2 em {@code MODE=MySQL} apontando para as migrations de
 * {@code db/migration/mysql}. Isso valida que a sintaxe SQL e o mapeamento das entidades
 * (via {@code ddl-auto=validate}) são compatíveis com o dialeto MySQL — mas não é garantia
 * absoluta de compatibilidade com um MySQL real, já que a emulação do H2 é aproximada.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:mysql_smoke;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration/mysql",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class MysqlMigrationSmokeTest {

    @Test
    void contextCarregaComMigrationsMysqlEValidacaoDeSchema() {
        // Se o contexto subir, Flyway aplicou db/migration/mysql com sucesso e o
        // Hibernate validou o schema resultante contra as entidades Corretora/Acao.
    }
}
