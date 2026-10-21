# Pulso · Academia Movimento

**Pulso** é um sistema de gestão administrativa de academia: alunos, modalidades, planos, matrículas, pagamentos, produtos, estoque e contas da equipe, com controle de acesso por perfil. Trabalho do **Grupo 05** para a disciplina de Testes de Sistemas (Técnico em Desenvolvimento de Sistemas).

> **Sistema oficial da entrega: o aplicativo Java (Swing) com banco relacional H2.** A versão web é apenas uma demonstração visual para a apresentação. Ela guarda dados fictícios no navegador, não acessa o banco H2 e acompanha somente parte das regras (validações de cadastro e remoção de registros).

## Cliente fictício

**Academia Movimento**: academia de bairro com musculação, funcional e pilates. A recepção atende alunos no balcão (cadastro, matrícula, pagamento e consulta) e a administração configura planos, produtos e equipe e acompanha contratos e estoque. Antes do sistema, essas informações ficavam separadas em fichas e planilhas, sem histórico de quem alterou o quê.

## Integrantes

| Nome | Responsabilidade principal |
| --- | --- |
| _(preencher)_ | _(ex.: requisitos e documentação)_ |
| _(preencher)_ | _(ex.: interface e demonstração)_ |
| _(preencher)_ | _(ex.: banco de dados e regras)_ |
| _(preencher)_ | _(ex.: testes e apresentação)_ |

## Stack

| Camada | Tecnologia |
| --- | --- |
| Linguagem | Java 17 (compilado com `--release 17`; testado com JDK 23) |
| Interface | Swing e AWT, com componentes e ícones desenhados em Java2D |
| Banco de dados | H2 2.3.232, relacional e persistente em arquivo, acessado por JDBC (`lib/h2-2.3.232.jar`) |
| Segurança | PBKDF2 com HMAC-SHA-256 e salt individual; `PreparedStatement` em todas as consultas |
| Testes | `SystemTest` (130 verificações de serviço e integração JDBC) e `node:test` na versão web (21 testes) |
| Demonstração web | HTML, CSS e JavaScript sem framework, IndexedDB no navegador, publicada na Vercel |
| IDE | IntelliJ IDEA (`academia-pulso.iml`, configuração `.run/`); Maven opcional (`pom.xml`) |

## Pré-requisitos

- **Aplicativo:** JDK 17 ou superior (`java` e `javac` no PATH). Nenhuma dependência para baixar: o H2 já está em `lib/`.
- **Versão web (opcional):** Node.js 22 ou superior.
- Sistema testado: Windows 11. Os scripts `.sh` existem para Linux e macOS.

## Como executar

**Aplicativo Java (oficial)**

- Windows: dois cliques em `Abrir-Academia.cmd`, ou `scripts/executar.ps1` no PowerShell.
- Linux/macOS: `sh scripts/executar.sh`.
- IntelliJ: **File > Open** na pasta `Academia`, escolher **Pulso - Academia** no seletor de execução e clicar em **Run**. Em outra máquina, selecione um JDK 17+ em **File > Project Structure**. A importação do Maven é opcional.

Na primeira execução o sistema cria `data/pulso.mv.db` a partir de `database/schema.sql` e insere os dados de demonstração. Opções da JVM: `-Dpulso.skipIntro=true` abre direto no login; `-Dpulso.reduceMotion=true` reduz as animações.

**Versão web (demonstração)**

- No ar: **https://pulso-academia-movimento.vercel.app**
- Local: `node web/scripts/build.mjs` e depois `node web/scripts/serve.mjs`; abra `http://127.0.0.1:4173`.

## Contas de demonstração

| Login | Senha | Perfil |
| --- | --- | --- |
| admin | Admin@123 | Administrador |
| recepcao | Recepcao@123 | Atendente (recepção) |

O usuário aceita maiúsculas e espaços externos; a senha diferencia maiúsculas e minúsculas. Na tela de login, **Contas da apresentação** preenche os campos. São credenciais fictícias: em uso real, crie contas individuais.

## O que o sistema faz

- **Acesso:** login por perfil (administrador ou atendente). Bloqueio de 1 minuto após 5 senhas erradas. Cada operação confere no banco se a conta continua ativa e com o mesmo perfil. Qualquer usuário troca a própria senha em **Alterar senha**.
- **Alunos:** cadastro, edição, pesquisa, retrato fictício e ficha com contratos. CPF único com 11 números (dados fictícios aceitos, sem conferir dígitos verificadores), telefone com pelo menos 8 números e e-mail com @.
- **Modalidades e planos:** atividades oferecidas e planos com valor total e duração em meses.
- **Matrículas:** a validade é calculada pelo plano, com situação (ativa, trancada, cancelada) e histórico de quem alterou e por quê. Matrícula ativa com validade encerrada aparece como **Vencida** e pode ser **renovada**.
- **Pagamentos:** à vista ou em parcelas, com total pago, saldo devedor e status Pendente, Parcial ou Pago. A soma nunca ultrapassa o valor contratado.
- **Produtos e estoque:** entradas e saídas com motivo, responsável e saldo após o movimento. Saída sem saldo é bloqueada. Produtos no mínimo ficam com a linha destacada. Filtros por produto, tipo e período, com paginação.
- **Remover registros:** sem vínculos o registro é excluído; com histórico é **inativado**; situações abertas bloqueiam. **Mostrar inativos** e **Reativar** completam o ciclo. Matrículas, pagamentos e movimentos nunca são apagados.
- **Auditoria:** criações, alterações, inativações, exclusões e trocas de senha ficam registradas com quem, quando e o valor anterior e o novo.
- **Painéis por perfil:** **Visão da gestão** (cobre) com indicadores, vencidas, pendências e reposição; **Balcão de atendimento** (verde) com busca de aluno e atalhos.

As regras completas estão em `docs/requisitos-e-testes.md`.

## Estrutura de pastas

```
Academia/
├─ src/main/java/br/com/pulso/   código do aplicativo (telas, serviço, banco, segurança)
├─ src/test/java/br/com/pulso/   SystemTest: 130 verificações automáticas
├─ database/                     schema.sql (estrutura) e banco-demonstracao.sql (exportação com exemplos)
├─ data/                         pulso.mv.db, criado na primeira execução (fora do git)
├─ lib/                          h2-2.3.232.jar
├─ web/                          demonstração web (src, testes e scripts de build)
├─ docs/                         requisitos, banco, testes, web, roteiro, identidade, dificuldades, análise
├─ assets/                       fotografias, retratos, logomarca e capturas das telas
├─ apresentacao/                 Pulso-Academia-Grafite.pptx
├─ scripts/                      executar, testar e exportar banco (.ps1 e .sh)
├─ Abrir-Academia.cmd            atalho de execução no Windows
├─ pom.xml, academia-pulso.iml, .run/   configuração Maven/IntelliJ
└─ vercel.json                   publicação da versão web
```

## Testes

| Suíte | Comando | Resultado atual |
| --- | --- | --- |
| Java (serviço + JDBC) | `scripts/testar.ps1` ou `sh scripts/testar.sh` | 130 de 130 aprovadas; relatório em `docs/resultados-testes.md` |
| Web (regras) | `node --test web/test/service.test.mjs` | 21 de 21 aprovados |
| Build web | `node web/scripts/build.mjs` | "Site preparado: 15 arquivos" |
| Web publicada | fluxos no navegador em https://pulso-academia-movimento.vercel.app | 17 de 17 (21/10/2026; detalhes em `docs/versao-web.md`) |

Os testes Java compilam com `--release 17`, usam bancos isolados (em memória e temporário) e não alteram `data/pulso.mv.db`. O roteiro de testes manuais está em `docs/requisitos-e-testes.md`.

## Banco de dados

O H2 é relacional e persistente. A estrutura (dez tabelas, chaves estrangeiras, restrições e índices) está em `database/schema.sql`, e o modelo explicado, em `docs/modelo-banco.md`. Ao abrir um banco de versão anterior, o sistema acrescenta o que falta sem apagar registros.

- Os dados de demonstração só são inseridos em banco vazio. Para apresentá-los sem perder o banco atual, feche o aplicativo e **renomeie** `data/pulso.mv.db`.
- Para restaurar a exportação num banco novo: `java -cp lib/h2-2.3.232.jar org.h2.tools.RunScript -url jdbc:h2:file:./data/pulso-restaurado -user sa -script database/banco-demonstracao.sql`.
- Para exportar o banco atual: feche o aplicativo e rode `scripts/exportar-banco.ps1`.
- Para consultar: `java -cp lib/h2-2.3.232.jar org.h2.tools.Shell -url jdbc:h2:file:./data/pulso -user sa` (senha vazia, só para esta demonstração local).
- Não abra duas instâncias sobre o mesmo arquivo. Para backup, feche o sistema e copie `data/pulso.mv.db`.

## Documentação e apresentação

| Arquivo | Conteúdo |
| --- | --- |
| `docs/requisitos-e-testes.md` | Requisitos funcionais e não funcionais, permissões, regras de negócio, mapa dos testes e casos manuais |
| `docs/modelo-banco.md` | Tabelas, relacionamentos, valores calculados, remoção e migração |
| `docs/resultados-testes.md` | Plano de testes e resultado da última execução (gerado pela suíte) |
| `docs/versao-web.md` | Versão web: finalidade, limites e verificação |
| `docs/roteiro-apresentacao.md` | Roteiro da demonstração e guia de estudo para a arguição |
| `docs/identidade-visual.md` | Cores, tipografia, marca e padrões de interface |
| `docs/dificuldades-e-solucoes.md` | Problemas encontrados no desenvolvimento e como foram resolvidos |
| `docs/analise-completa-sistema.md` | Análise detalhada de telas, regras, segurança e limites |
| `docs/imagens-ficticias.md` | Origem das fotografias e retratos fictícios |
| `apresentacao/Pulso-Academia-Grafite.pptx` | Slides da apresentação |

O pacote de entrega (`Entrega-Pulso-Grupo05.zip`) é gerado **fora** desta pasta. Ele não inclui `target`, `out`, `.build`, `.idea`, `.git` nem `data`, que são recriados ou locais.

## Limites

Execução local, sem servidor nem sincronização entre computadores. Não há cobrança recorrente, integração bancária, emissão fiscal, agenda de aulas, catraca nem envio de mensagens. Os registros financeiros são controles internos: o sistema registra um pagamento confirmado pela equipe, não processa PIX ou cartão. Não há backup automático.
