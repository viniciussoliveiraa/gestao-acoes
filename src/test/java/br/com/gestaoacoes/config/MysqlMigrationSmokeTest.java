package br.com.gestaoacoes.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Não há servidor MySQL real disponível no ambiente de desenvolvimento/CI, então este teste
 * sobe o contexto com H2 em {@code MODE=MySQL} e força o Liquibase a rodar os changesets do
 * context {@code mysql} (ver changes/*.xml em db/changelog) via {@code spring.liquibase.contexts}
 * — não dá para usar detecção automática de banco (atributo {@code dbms} do Liquibase) aqui,
 * porque o Liquibase identifica o produto pela conexão JDBC real (sempre "h2"), ignorando o MODE
 * de compatibilidade. Isso valida que a sintaxe SQL gerada para MySQL e o mapeamento das
 * entidades (via {@code ddl-auto=validate}) são compatíveis com o dialeto MySQL — mas não é
 * garantia absoluta de compatibilidade com um MySQL real, já que a emulação do H2 é aproximada.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:mysql_smoke;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.liquibase.contexts=mysql",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class MysqlMigrationSmokeTest {

    @Test
    void contextCarregaComMigrationsMysqlEValidacaoDeSchema() {
        // Se o contexto subir, o Liquibase aplicou os changesets do context "mysql" com sucesso
        // e o Hibernate validou o schema resultante contra as entidades do domínio.
    }
}