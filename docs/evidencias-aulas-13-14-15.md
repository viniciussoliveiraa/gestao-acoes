# Evidências das Aulas 13, 14 e 15

Este documento reúne o que as **atividades avaliativas** das aulas
[`AULA-13-LOGIN-COM-JWT`](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-13-LOGIN-COM-JWT.md),
[`AULA-14-CONTROLE-DE-ACESSO`](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-14-CONTROLE-DE-ACESSO.md) e
[`AULA-15-TESTES-E-COBERTURA`](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-15-TESTES-E-COBERTURA.md)
pedem como entrega escrita (justificativas, explicações e evidências de execução). A
implementação está descrita no [README](../README.md#aderência-às-aulas-de-login-jwt-controle-de-acesso-e-testescobertura).

---

## Aula 13 — Login com JWT

### Testes de integração de segurança (atividade, item 4: "pelo menos quatro")

`AuthFluxoIntegrationTest` é o equivalente ao `AuthResourceIntegrationTest` do material: contexto
Spring completo (H2 + Liquibase + cadeia de segurança real), sem mocks.

| Cenário do material (seção 13) | Teste | Resultado |
|---|---|---|
| Emissão do JWT com credenciais válidas | `loginComCredenciaisValidasEmiteJwtAssinado` | `200` + token `header.payload.assinatura` |
| Rejeição da senha inválida | `loginComSenhaIncorretaRetorna401` | `401` |
| Rejeição de acesso sem token | `recursoProtegidoSemTokenRetorna401` | `401` |
| Acesso a recurso protegido com o token emitido | `recursoProtegidoComTokenEmitidoRetorna200` | `200` |
| Bloqueio de escrita quando o papel não é `ADMIN` | `escritaComPapelUserRetorna403` | `403` |
| Leitura do usuário autenticado em `/auth/me` | `meDevolveOUsuarioDoTokenSemExporSenha` | `200`, sem `senha`/hash |

Também há `CorretoraFluxoIntegrationTest` e `AcaoFluxoIntegrationTest` (escrita sem token → `401`,
com `USER` → `403`, com `ADMIN` → `201`).

### Experimentos obrigatórios (seção 12.4), automatizados

| # | Experimento | Onde | Resultado |
|---|---|---|---|
| 1 | Recurso protegido sem cabeçalho | `recursoProtegidoSemTokenRetorna401` | `401` ✅ |
| 2 | Login com senha incorreta | `loginComSenhaIncorretaRetorna401` | `401` ✅ |
| 3 | Alterar um caractere do token | `tokenAdulteradoRetorna401` | `401` ✅ |
| 4 | Usar o token correto | `recursoProtegidoComTokenEmitidoRetorna200` | `200` ✅ |
| 5 | `USER` tentando operação restrita | `escritaComPapelUserRetorna403` | `403` ✅ |
| 6 | Token expirado | `tokenExpiradoRetorna401` | `401` ✅ |

O experimento 6 não precisa esperar o relógio: o teste assina, com o mesmo segredo e emissor do
servidor, um token cujo `exp` já passou. Assim ele mostra que a **assinatura válida não basta**,
porque a validade temporal também é verificada. A coleção do Postman (`postman/`) traz os experimentos
1 a 5 para execução manual, incluindo "Token adulterado (-> 401)". O 6 depende de esperar a
expiração, então fica coberto pelo teste automatizado.

Detalhe encontrado ao escrever o teste 5: o Spring valida o corpo (`@Valid`) **antes** de avaliar
o `@PreAuthorize`, que só atua quando o método do controller é chamado. Um `USER` que envia um corpo
inválido recebe `400`, não `403`. O teste usa um corpo válido para isolar a regra de papel.

### Por que o JWT não deve conter dados sigilosos (atividade, item 6)

O JWT deste projeto é **assinado (JWS), não criptografado**. O payload é apenas Base64URL: qualquer
pessoa que tenha o token, como uma extensão do navegador, um log de proxy ou quem olhar o
`localStorage` do frontend, consegue decodificá-lo sem conhecer o segredo. A assinatura HS256
garante **integridade e autenticidade**, ou seja, que o conteúdo não foi alterado e que foi emitido
por quem conhece o segredo. Ela **não garante confidencialidade**. Por isso o token carrega só o
necessário para autorizar: `sub` (id do usuário), `email`, `authorities`, `iss`, `iat` e `exp`.
Nunca leva senha, hash, CPF ou saldo. Além disso, o token é válido até `exp` mesmo que os dados
mudem, então colocar nele qualquer coisa sensível ou volátil espalha uma cópia não revogável dessa
informação.

### Diferença observada entre `401` e `403` (atividade, item 7)

- **`401 Unauthorized`**: a API **não sabe quem você é**. Pode faltar o cabeçalho
  `Authorization`, a assinatura pode não conferir (token adulterado), o token pode estar expirado
  ou com emissor errado, ou o login pode ter falhado. Quem responde é o `ProblemDetailAuthEntryPoint`,
  e a requisição nem chega ao controller. A ação correta do cliente é autenticar de novo.
- **`403 Forbidden`**: a API **sabe quem você é, mas você não pode fazer isso**. O token é válido,
  porém o papel (`ROLE_USER`) não satisfaz o `@PreAuthorize("hasRole('ADMIN')")`. Repetir o login
  não resolve, porque o mesmo usuário continuará sem permissão.

Observado nos testes: o mesmo `POST /corretoras` devolve `401` sem token, `403` com token de `USER`
e `201` com token de `ADMIN`.

---

## Aula 14 — Controle de acesso (RBAC)

### Testes de integração de usuários (seção 11)

`UsuarioFluxoIntegrationTest` é o equivalente ao `UsuarioResourceIntegrationTest` do material:
serviço, repositório, banco e segurança reais. Ele verifica no banco o que o
`UsuarioControllerTest` (serviço simulado) não consegue ver.

| Cenário do material | Teste |
|---|---|
| Criação de usuário por administrador | `administradorCriaUsuarioComHashBcryptSemExporSenhaNaResposta` |
| Armazenamento do hash BCrypt | mesmo teste: o hash gravado começa com `$2`, é diferente do texto e `matches()` confere |
| Ausência de senha e hash nas respostas | mesmo teste + `listagemAdministrativaNaoExpoeSenhaNemHash` |
| Listagem administrativa | `listagemAdministrativaNaoExpoeSenhaNemHash` |
| Alteração de papel e situação | `alteracaoDePapelESituacaoEhPersistidaEDesativadoNaoAutentica` (inclui: desativado não consegue logar) |
| Troca segura de senha | `trocaDeSenhaInvalidaASenhaAntigaEAceitaANova` (senha antiga → `401`, nova → `200`) |
| `403` quando `USER` acessa a administração | `usuarioComumComTokenRealNaoGerenciaUsuarios` (JWT real) e `userNaoDeveGerenciarUsuarios` (`@WithMockUser`, o exemplo da aula) |
| `401` sem token | `semTokenRetorna401` |
| Regra da seção 12.1 (último administrador) | `naoPermiteRebaixarOUltimoAdministradorAtivo` → `422` |

**Correção de infraestrutura encontrada aqui:** `@WithMockUser` não funcionava no projeto. No
Spring Boot 4, a ligação entre o Spring Security e o MockMvc auto-configurado fica no
`spring-boot-starter-security-test`, mas o `pom.xml` declarava só a biblioteca
`spring-security-test`. Com a anotação, toda requisição "autenticada" chegava sem usuário e virava
`401`. A dependência foi trocada pelo starter.

### Matriz de acesso (atividade, itens 1 e 2)

Dois papéis (`ADMIN`, `USER`). A matriz está no
[README](../README.md#aderência-às-aulas-de-login-jwt-controle-de-acesso-e-testescobertura).

### Regra de autorização horizontal no domínio (atividade, item 6)

RBAC é uma regra **vertical**: decide *que tipo de operação* um papel pode fazer. A regra
**horizontal** decide *sobre quais registros* um usuário do mesmo papel pode agir. No gestao-acoes,
**cada usuário só enxerga e altera a própria carteira**. Dois `USER`s têm o mesmo papel, mas um não
pode listar, nem apagar, os lançamentos ou proventos do outro.

Implementação: o id do usuário vem **do token** (`@AuthenticationPrincipal Long usuarioId`, extraído
do claim `sub` pelo `JwtPrincipalConverter`), nunca de um parâmetro que o cliente controla. Todas as
consultas filtram por ele (`findByUsuarioId`, `findByUsuarioIdAndAcaoId`). Na exclusão,
`CarteiraService.excluirLancamento` só remove se `lancamento.getUsuarioId()` for o do token. Caso
contrário, responde `404`, e não `403`, para não revelar que o id existe para outra pessoa. Testes:
`CarteiraServiceTest.excluirLancamentoDeOutroUsuarioLancaExcecaoENaoRemove` e
`ProventoRepositoryTest.naoRetornaProventosDeOutroUsuario`.

### Onde está cada `@PreAuthorize` e por quê (atividade, item 7)

| Local | Por quê |
|---|---|
| `UsuarioController`, **na classe** | Todo o recurso `/usuarios` é administrativo. Anotar a classe garante que um método novo já nasce protegido, sem depender de lembrar de anotá-lo. |
| `CorretoraController` / `AcaoController`, **só nos métodos de escrita** (`POST`, `PUT`) | Nesses controllers a leitura é pública por requisito do enunciado (RF01-RF12). Anotar a classe bloquearia o `GET`, então a restrição fica método a método. |
| **Nenhum** em `CarteiraController` / `ProventoController` | Ali a regra é horizontal (dono do dado), não de papel. Basta estar autenticado (`anyRequest().authenticated()` na `SecurityFilterChain`), e o filtro por dono fica no serviço, onde o `usuarioId` do token é aplicado à consulta. |
| `SecurityFilterChain` (URL), e não `@PreAuthorize` | Só para o que é por rota e não por regra de negócio: o que é público (`/auth/login`, `/auth/registrar`, `GET` do catálogo, Swagger) e o "exige autenticação" geral. |

O `@PreAuthorize` fica no **controller** porque o papel é uma regra da **API**. Os serviços são
chamados também por código interno (por exemplo, o `AdminInitializer`), que não tem um usuário
HTTP no contexto.

---

## Aula 15 — Testes, TDD e cobertura

### 1. Relatório JaCoCo inicial (atividade, item 1)

Medido com `./mvnw clean verify` antes desta etapa (commit `ef6d0ed`):

| Métrica | Coberto / Total | Percentual |
|---|---:|---:|
| Testes | 170 | — |
| Linhas | 818 / 849 | 96,35% |
| Ramos | 107 / 138 | **77,54%** (abaixo da meta de 80%) |

Portão versionado na época: linhas `0.85`, ramos `0.70`.

### 2. Classe escolhida e lista de comportamentos (atividade, itens 2 e 3)

Classe: **`UsuarioService`**, a sugerida pela atividade guiada da seção 16.1. Lista escrita antes
de codificar:

```text
[x] criação com e-mail disponível                 (já existia)
[x] rejeição de e-mail duplicado                  (já existia)
[x] atualização mantendo o próprio e-mail         (já existia)
[x] rejeição ao usar e-mail de outro usuário      (já existia)
[x] consulta de ID inexistente                    (já existia)
[x] troca de senha gera hash diferente do texto   (já existia)
[ ] atualização altera o nome                     -> ciclo 1
[ ] não rebaixar o último ADMIN ativo             -> ciclo 2
[ ] não desativar o último ADMIN ativo            -> ciclo 2
[ ] rebaixar ADMIN quando há outro ADMIN ativo    -> ciclo 2
[ ] senha: 7 (inválida), 8 (válida), 72 (válida), 73 (inválida) -> ciclo 3
```

Valores de fronteira: contagem de administradores ativos `1` (bloqueia) e `2` (permite); tamanho
da senha `7`/`8`/`72`/`73`; tamanho do segredo JWT `31`/`32` bytes.

### 3. Ciclos Red → Green → Refactor (atividade, itens 4 e 5)

Cada ciclo foi executado com o teste escrito **antes** da implementação. Abaixo está a saída real do
Maven em cada fase vermelha.

**Ciclo 1. Atualização altera o nome (defeito real encontrado).** `UsuarioUpdateRequest` recebia
`nome`, mas `UsuarioService.atualizar` nunca o gravava, e `Usuario` nem tinha `setNome`. O teste
antigo `atualizarComSucessoAlteraEmailPapelEAtivo` não percebia porque não verificava o nome.

```text
RED  ./mvnw test -Dtest=UsuarioServiceTest
[ERROR] UsuarioServiceTest.atualizarAlteraONome <<< FAILURE!
expected: "Ana Souza"
 but was: "Nome"
[ERROR] Tests run: 9, Failures: 1, Errors: 0, Skipped: 0

GREEN  (Usuario.setNome + usuario.setNome(request.nome().trim()))
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
```

**Ciclo 2. Último administrador ativo (Aula 14, seção 12.1).** Antes do teste foram criados só
`UltimoAdministradorException` e a consulta `countByPapelAndAtivoTrue`, sem a regra, para o teste
compilar e falhar pelo motivo certo.

```text
RED
[ERROR] UsuarioServiceTest.desativarOUltimoAdministradorAtivoLancaExcecao <<< FAILURE!
Expecting code to raise a throwable.
[ERROR] UsuarioServiceTest.rebaixarOUltimoAdministradorAtivoLancaExcecao <<< FAILURE!
Expecting code to raise a throwable.
[ERROR] Tests run: 14, Failures: 2, Errors: 1, Skipped: 0
        (o erro restante: stub de contagem nunca consultado — também esperado, a regra não existia)

GREEN  (UsuarioService.validarQueNaoRemoveOUltimoAdministrador)
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
```

Refactor: a condição foi extraída para um método com nome de regra de negócio e as variáveis
`eraAdministradorAtivo`/`continuaAdministradorAtivo`. A contagem só é consultada quando a alteração
de fato tira um administrador ativo dessa condição, o que é verificado por
`manterOUltimoAdministradorComoAdminAtivoNaoConsultaContagem` e
`reativarAdministradorDesativadoNaoConsultaContagem`.

**Ciclo 3. Fronteiras da senha (`@ParameterizedTest`, seção 18).** O BCrypt só considera os
primeiros 72 bytes, e o Spring Security atual recusa senhas maiores com exceção (viraria `500`).
`SenhaValidacaoTest` testa os três contratos que recebem senha.

```text
RED
[ERROR] SenhaValidacaoTest.cadastroPeloAdministradorRespeitaLimitesDaSenha(int, boolean)[4] <<< FAILURE!
expected: false
[ERROR] SenhaValidacaoTest.autocadastroRespeitaLimitesDaSenha(int, boolean)[4] <<< FAILURE!
[ERROR] SenhaValidacaoTest.trocaDeSenhaRespeitaLimitesDaSenha(int, boolean)[4] <<< FAILURE!
[ERROR] Tests run: 12, Failures: 3, Errors: 0, Skipped: 0
        (exatamente o caso [4] = 73 caracteres; 7, 8 e 72 já se comportavam como esperado)

GREEN  (@Size(min = 8, max = 72) nos três DTOs; formulário de cadastro do frontend com maxLength(72))
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
```

### 4. Evolução da cobertura de ramos (atividade, itens 6 e 9)

| Etapa | Testes | Ramos |
|---|---:|---:|
| Inicial | 170 | 77,54% (107/138) |
| + ciclos TDD + integração das Aulas 13/14 | 205 | 80,26% (122/152) |
| + `JwtPropertiesTest`, `CorrelationIdFilterTest`, `GlobalExceptionHandlerTest` | **222** | **84,87% (129/152)** |

Linhas finais: **96,52% (831/861)**. Nenhum limite foi reduzido, nenhuma exclusão foi adicionada
ao JaCoCo e nenhum teste foi removido ou desativado.

Os testes extras seguiram a ordem de risco da seção 16, começando por autenticação:
`JwtProperties`, a validação do segredo, da expiração e do emissor, com a fronteira 31/32 bytes e
o caso de caracteres multibyte. Depois `CorrelationIdFilter` (cabeçalho ausente, em branco ou
reaproveitado, e o MDC limpo ao fim) e `GlobalExceptionHandler` (uma exceção inesperada não vaza a
mensagem interna; exceções do Spring com status próprio não viram `500`).

O portão de ramos subiu de `0.70` para **`0.80`** (a meta acadêmica) **depois** de a suíte
sustentar 84,87%, deixando margem para flutuação. O de linhas continua em `0.85`.

### 5. Demonstração do portão (seção 14)

```text
./mvnw verify -Djacoco.minimum.branch.coverage=0.90
[INFO] Tests run: 222, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
[ERROR] ... jacoco-maven-plugin:0.8.12:check (check-coverage) ... Coverage checks have not been met.

./mvnw clean verify
[INFO] Tests run: 222, Failures: 0, Errors: 0, Skipped: 0
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

Testes aprovados ≠ política de qualidade aprovada.

### 6. Justificativa de cada dublê (atividade, item 7)

| Teste | Dublê | Tipo | Por quê |
|---|---|---|---|
| `UsuarioServiceTest` | `UsuarioRepository` (`@Mock`) | stub + mock | Isola a regra do serviço do banco. Os stubs programam "e-mail livre/ocupado" e "quantos ADMIN ativos". `verify(..., never()).save(...)` comprova que nada é gravado quando a regra recusa, e `never().countByPapelAndAtivoTrue` que a contagem só é consultada quando necessária. |
| `UsuarioServiceTest` | `PasswordEncoder` | **real** (`BCryptPasswordEncoder`) | Não foi mockado de propósito: o comportamento observável é "o hash confere com a senha" (`matches`). Um mock só provaria que `encode` foi chamado. |
| `UsuarioServiceTest` | `UsuarioCreateRequest`/`UsuarioUpdateRequest` | **reais** | A aula orienta não mockar DTOs nem a classe testada. |
| `CorrelationIdFilterTest` | `MockHttpServletRequest`/`Response`/`MockFilterChain` | fake | Implementações leves do spring-test. São reais o bastante para ler o cabeçalho devolvido e o MDC, sem subir servidor. |
| `GlobalExceptionHandlerTest` | `MockHttpServletRequest` | fake | O handler só lê a URI da requisição. |
| `SenhaValidacaoTest`, `JwtPropertiesTest` | nenhum | — | Testam regras puras (Bean Validation e o construtor do record). |
| `UsuarioFluxoIntegrationTest`, `AuthFluxoIntegrationTest` | nenhum | — | Integração: banco H2, Liquibase, filtros de segurança, serialização e JWT reais, como a seção 9.3 recomenda. |
| `UsuarioFluxoIntegrationTest` | `@WithMockUser` | dummy de autenticação | Usado só nos dois testes cujo foco é a **regra de autorização**, como na seção 11. Os demais usam login e JWT reais. |

### 7. Teste que detecta uma alteração proposital (atividade, item 8)

Dois mutantes aplicados à mão (o que o PIT faria automaticamente), cada um executado e depois
revertido:

**Mutante 1:** em `UsuarioService`, `countByPapelAndAtivoTrue(Papel.ADMIN) <= 1` virou `< 1`
(fronteira trocada).

```text
./mvnw test -Dtest=UsuarioServiceTest
[ERROR] UsuarioServiceTest.desativarOUltimoAdministradorAtivoLancaExcecao <<< FAILURE!
[ERROR] UsuarioServiceTest.rebaixarOUltimoAdministradorAtivoLancaExcecao <<< FAILURE!
Expecting code to raise a throwable.
[ERROR] Tests run: 14, Failures: 2, Errors: 0, Skipped: 0      -> mutante morto
```

**Mutante 2:** em `JwtProperties`, `length < 32` virou `length < 31` (aceitaria chave HS256 de
248 bits).

```text
./mvnw test -Dtest=JwtPropertiesTest
[ERROR] JwtPropertiesTest.segredoCom31BytesEhRecusado <<< FAILURE!
Expecting code to raise a throwable.
[ERROR] Tests run: 10, Failures: 1, Errors: 0, Skipped: 0      -> mutante morto
```

Sem os testes de fronteira (`1` vs `2` administradores, `31` vs `32` bytes), os dois mutantes
sobreviveriam: a cobertura de linha seria a mesma, mas a regra não estaria verificada.

### 8. Integração contínua (seção 20)

`.github/workflows/testes.yml` executa `./mvnw --batch-mode clean verify` (o mesmo comando local) a
cada `push` e `pull_request` e publica `target/site/jacoco/` como artefato.
