# Análise completa do Pulso — Academia Movimento

**Análise original:** 9 de outubro de 2026. **Atualizações:** 15 e 16/10 (interface Grafite e versão web) e 21/10/2026 (correções das seções 12 e 13, regras novas e fechamento da entrega).

**Versão analisada:** estado final do projeto na pasta Academia.
**Finalidade:** explicar o sistema inteiro (telas, regras, estrutura, qualidade e limites reais) para estudo e arguição.

O texto analisa o aplicativo Java com banco H2, que é o **sistema oficial**. A versão web (https://pulso-academia-movimento.vercel.app) é só uma demonstração com dados no navegador e está documentada em [Versão web](versao-web.md).

## 1. O que é o sistema

O **Pulso** é um aplicativo desktop de gestão administrativa de academia, desenvolvido em Java para o cenário do Grupo 05, com a Academia Movimento como cliente fictício.

Ele organiza alunos, atividades oferecidas, planos, contratações, pagamentos, produtos, movimentações de estoque e contas da equipe. Os dados ficam num banco relacional local e continuam disponíveis depois de fechar e abrir o programa.

A ferramenta é usada pela equipe da academia, com dois perfis: administrador e atendente. O aluno é uma pessoa cadastrada e titular de contratos; ele não tem login no sistema.

**Classificação:** aplicação acadêmica funcional, com dados persistentes, regras de negócio verificadas por testes automáticos e controle de acesso revalidado no banco. Uso comercial exigiria os recursos listados na seção 14.

## 2. Como esta análise foi realizada

Foram lidos o código (`src/main/java`), o esquema SQL, os testes, os scripts e a documentação. O código foi compilado com `--release 17` sem erros.

- A suíte automática registra **130 verificações aprovadas** em 21/10/2026 (`docs/resultados-testes.md`). A versão web tem 21 testes aprovados.
- As telas do administrador foram geradas pelo próprio aplicativo (modo `--screenshots`, com banco em memória) e conferidas visualmente.
- O sistema foi aberto a partir de uma cópia limpa do projeto pelo `Abrir-Academia.cmd`, criando e populando o banco.

Uma inspeção de código e uma suíte de testes não estabelecem percentual de cobertura nem garantem ausência de falhas. As telas da recepção e os diálogos foram conferidos por roteiro manual, não por clique automatizado.

## 3. Exemplo de funcionamento do começo ao fim

1. Uma pessoa chega à recepção. O atendente entra com sua conta, cadastra o aluno (CPF fictício aceito, desde que tenha 11 números) e cria uma matrícula no plano **Trimestral funcional** (3 meses, R$ 329,90). Iniciada em 21/10/2026, ela termina em 21/01/2027, e o valor fica congelado na matrícula.
2. O aluno paga R$ 150,00 no cartão. A matrícula fica **Parcial**, com saldo de R$ 179,90. Depois ele paga o restante e a matrícula fica **Paga**. O sistema não processa cartão nem PIX: registra um recebimento confirmado pela equipe.
3. Se o aluno pedir uma pausa, a matrícula é **trancada** com motivo, e o histórico guarda data e responsável. A data final não muda.
4. Quando a validade termina, a matrícula aparece como **Vencida**, com a linha destacada e contada na visão geral. Com o saldo quitado, **Renovar** encerra o contrato antigo e abre outro a partir de hoje.
5. No estoque, a equipe registra entradas e saídas com motivo. Cada movimento guarda o saldo resultante. Saldo menor ou igual ao mínimo coloca o produto em **Repor**.
6. Um cadastro que não serve mais é **removido**: sem vínculos ele é excluído; com histórico, é inativado.

## 4. Pessoas, contas e permissões

- **Aluno:** pessoa atendida, com identificação e contato. Não tem login.
- **Usuário:** integrante da equipe, com login e senha.
- **Perfil:** ADMIN ou ATENDENTE.

| Operação | Administrador | Atendente |
| --- | --- | --- |
| Consultar visão geral, alunos, modalidades, planos, produtos e movimentações | Sim | Sim |
| Cadastrar, editar, remover e reativar alunos | Sim | Sim |
| Matrícula, situação, pagamento e renovação | Sim | Sim |
| Movimentar estoque | Sim | Sim |
| Trocar a própria senha | Sim | Sim |
| Criar, editar, remover e reativar modalidades, planos e produtos | Sim | Não |
| Gerenciar, remover e reativar usuários | Sim | Não |
| Consultar a auditoria | Sim | Não |

Os botões administrativos ficam ocultos para o atendente, e o **serviço confere o perfil de novo** em cada operação protegida. A cada operação, a conta é reconsultada no banco: se foi desativada ou mudou de perfil, a sessão é encerrada. O administrador não pode desativar, rebaixar nem remover a própria conta, e o último administrador ativo não pode ser removido.

Contas de demonstração: `admin` / `Admin@123` e `recepcao` / `Recepcao@123`.

## 5. Telas e suas funções

O administrador tem oito áreas no menu; o atendente tem sete (sem Usuários). Abertura e login vêm antes do menu. Na lateral ficam **Alterar senha** e **Encerrar sessão**; no topo, **Atualizar**.

### 5.1. Abertura

Animação de 2,6 s em Java2D (fotografia preparada uma vez, abertura lateral, luz, partículas e marca), seguida de transição de 200 ms para o login. Não há barra de carregamento.

### 5.2. Login

Formulário central sobre a fotografia da academia, com opção de mostrar a senha, aviso de Caps Lock e **Contas da apresentação**. O usuário ignora maiúsculas e espaços; a senha é exata. A autenticação roda em segundo plano.

Cada senha errada soma em `usuarios.tentativas`. Na quinta, `bloqueado_ate` recebe o horário atual mais 1 minuto e a mensagem informa o bloqueio. O login correto zera o contador. A mensagem de erro comum não revela se o usuário existe.

### 5.3. Páginas iniciais por perfil

A **Visão da gestão** (cobre) mostra:

| Indicador | O que é contado |
| --- | --- |
| Alunos ativos | Alunos com `ativo = TRUE` |
| Contratos ativos | Matrículas ATIVA dentro da validade |
| Vencidas | Matrículas ATIVA com fim anterior a hoje |
| Pendências | Matrículas não canceladas com saldo a receber |
| Itens para repor | Produtos ativos com saldo ≤ mínimo |

Também mostra as últimas matrículas, a distribuição das situações (com as vencidas destacadas) e os recebimentos recentes.

O **Balcão de atendimento** (verde) traz busca de aluno, atalhos de cadastro e matrícula, recebimentos pendentes e vencidas, e fichas de alunos. Os números vêm do banco em cada consulta.

### 5.4. Alunos

A tabela mostra código, nome, CPF, telefone, e-mail e ativo, com pesquisa, **Adicionar**, **Editar**, **Remover**, **Ver ficha**, **Ampliar retrato** e **Mostrar inativos**. Regras do formulário:

- Nome obrigatório (até 100 caracteres).
- CPF com **exatamente 11 números** (pontos e hífen ignorados) e **único**. Os dígitos verificadores não são conferidos, para aceitar dados fictícios.
- Telefone com pelo menos 8 números, sem exigir DDD.
- E-mail obrigatório contendo @.
- Não é possível inativar um aluno com matrícula ativa ou trancada.

A ficha reúne foto, contatos e todos os contratos, com a situação atual (incluindo Vencida). Os retratos vêm de uma galeria de quatro imagens fictícias.

### 5.5. Modalidades

São as atividades oferecidas (musculação, funcional, pilates). Cada uma tem nome único e descrição. A galeria mostra até três modalidades ativas, com fotografia e quantidade de planos ativos. A administração cria, edita, remove e reativa; a recepção só consulta.

### 5.6. Planos

Cada plano tem nome único, modalidade ativa, valor total (maior que zero, com duas casas) e duração de 1 a 36 meses. O valor é do período inteiro: não gera mensalidades. Mudar o plano não altera matrículas já feitas, cujo valor está copiado.

### 5.7. Matrículas

A tabela mostra código, aluno, plano, início, fim, **situação atual** (Ativa, Vencida, Trancada, Cancelada), pagamento (Pendente, Parcial, Pago), valor e saldo. As ações são **Nova matrícula**, **Situação**, **Pagamento**, **Renovar** e **Histórico**.

- **Nova matrícula:** exige aluno e plano ativos. Recusa uma segunda matrícula aberta do mesmo aluno no mesmo plano. Calcula o fim, copia o preço e registra o histórico inicial. A linha do aluno fica bloqueada (`FOR UPDATE`) durante a verificação.
- **Situação:** ATIVA, TRANCADA ou CANCELADA, sempre com motivo. CANCELADA é definitiva. Trancar não prorroga a validade.
- **Pagamento:** o formulário mostra valor contratado, já pago e saldo, e sugere o saldo. Aceita um valor menor (parcela), mas nunca acima do saldo. Matrícula cancelada não recebe pagamento.
- **Vencida:** é calculada na consulta. O registro continua ATIVA, então não há rotina agendada. A linha fica destacada.
- **Renovar:** só para matrícula vencida e com saldo quitado. Cancela a antiga (com motivo no histórico) e cria outra no mesmo plano, a partir de hoje, com o preço atual.

### 5.8. Produtos

A tabela mostra nome, unidade, preço, saldo, mínimo e situação do estoque, com a **linha destacada** quando saldo ≤ mínimo. O produto nasce com saldo zero; o saldo só muda por movimentação. Ações: novo, editar, remover, movimentar e reativar (com Mostrar inativos).

### 5.9. Movimentações

A tabela mostra data, produto, tipo, quantidade, **saldo após**, motivo e responsável. Há filtros por **produto, tipo e período** e **paginação de 50 linhas**, aplicadas no banco. A entrada soma ao saldo. A saída só ocorre com saldo suficiente, numa única instrução `UPDATE ... WHERE saldo >= quantidade`, e saldo e histórico ficam na mesma transação. Produto inativo não recebe movimentação. Movimentos não são editados nem apagados; uma correção é feita com um novo movimento.

### 5.10. Usuários

Área exclusiva do administrador: cadastro, edição (com senha opcional), perfil, ativação e retrato, mais remover, reativar e **Auditoria**. A auditoria lista as últimas 200 alterações com data, responsável, cadastro, ação e "valor anterior -> valor novo".

### 5.11. Comportamentos comuns

- As consultas e gravações rodam em segundo plano (SwingWorker). Respostas atrasadas de outra tela são descartadas.
- As listas mostram até 500 linhas e só cadastros ativos (salvo **Mostrar inativos**). As combos de matrícula e movimentação oferecem só cadastros ativos.
- **Remover** só habilita com uma linha selecionada e sempre pergunta antes, dizendo se vai excluir ou inativar.
- Os erros aparecem como mensagens compreensíveis. Se a sessão for revogada, a tela volta ao login.
- A pesquisa ignora acentos e maiúsculas. Números, valores e datas são ordenados pelo tipo.

## 6. Regras de negócio

| Regra | Consequência prática |
| --- | --- |
| CPF com 11 números e único | Aceita dados fictícios e impede duplicidade |
| Aluno e plano ativos para matricular | Cadastro inativo não recebe contrato novo |
| Uma matrícula aberta por aluno e plano | Não repete o mesmo contrato enquanto ativo ou trancado |
| Valor contratado preservado | Mudar o preço do plano não altera contratos existentes |
| Pagamento em parcelas até o valor contratado | Status Pendente, Parcial ou Pago; nunca paga além do saldo |
| Vencida calculada pela data | Sinaliza renovação sem cancelar nada automaticamente |
| Renovação exige saldo quitado | Não abre contrato novo com dívida no anterior |
| Cancelamento definitivo | Para voltar, cria-se uma matrícula nova |
| Motivo para mudar situação | Histórico com justificativa e responsável |
| Trancamento não prorroga | A data final não muda |
| Produto novo com saldo zero; saída limitada ao saldo | Estoque nunca negativo |
| Alerta com saldo ≤ mínimo | A reposição é sinalizada no limite |
| Remover: excluir sem vínculos, inativar com histórico, bloquear se aberto | Nada que tenha histórico é apagado |
| Auditoria de cadastros | Quem alterou, quando e o que mudou |
| Sessão revalidada e bloqueio por tentativas | Permissões valem na hora; força bruta é retardada |

## 7. Banco de dados

O banco é o **H2 2.3.232**, acessado por JDBC, no arquivo `data/pulso.mv.db`. São **dez tabelas**: usuarios, alunos, modalidades, planos, matriculas, historico_matriculas, pagamentos, produtos, movimentacoes e auditoria. Detalhes em [Modelo do banco](modelo-banco.md).

- As chaves estrangeiras ligam a matrícula a aluno, plano e usuário; o pagamento à matrícula; o movimento ao produto; os eventos ao usuário responsável.
- `UNIQUE` em login, CPF e nomes de modalidade, plano e produto. `CHECK` em valores positivos, saldo não negativo, meses entre 1 e 36 e situações e tipos válidos.
- Valores em `DECIMAL(10,2)` no banco e `BigDecimal` no código.
- Ao abrir, `schema.sql` cria o que falta (`IF NOT EXISTS`). Um banco de versão anterior recebe as colunas novas (`ativo`, `tentativas`, `bloqueado_ate`, `saldo_apos`), a tabela de auditoria e perde o antigo `UNIQUE` de pagamento, sem perder registros.
- Os dados de demonstração só entram em banco vazio: 2 usuários, 5 alunos, 3 modalidades, 3 planos, 5 matrículas (em dia e pendente, vencida e paga, parcial, vencida com saldo, trancada), 4 pagamentos e 4 produtos (2 abaixo do mínimo).

## 8. Estrutura do código e tecnologias

| Componente | Responsabilidade |
| --- | --- |
| `Main.java` | Inicializa o banco, prepara os exemplos e abre a janela |
| `AcademiaFrame.java` | Navegação, listas, formulários, confirmações e execução em segundo plano |
| `LoginPanel.java` | Formulário de acesso |
| `OpeningPanel.java` | Abertura animada |
| `WorkspaceDashboard.java` | Painéis por perfil; `load()` lê os dados fora da linha da interface |
| `Visual.java` | Cores, componentes, ícones, fotos e animações |
| `AcademiaService.java` | Autenticação, sessão, permissões, validações, transações, remoção e auditoria |
| `Database.java` | Conexões JDBC, criação e migração das tabelas, exemplos |
| `Security.java` | Hash PBKDF2 e conferência de senhas |
| `SystemTest.java` | 130 verificações automáticas |

As regras ficam no serviço, testável sem a interface. `AcademiaFrame` ainda concentra muita coisa (montagem de telas e consultas de listagem); não é um MVC estrito.

A interface usa Swing/AWT com desenho em Java2D, sem internet nem framework externo. O código é compilado com compatibilidade Java 17 e testado com JDK 23 no Windows.

## 9. Aparência

A identidade é Grafite: fundo escuro, cobre para a administração, verde-azulado para a recepção e rosa de alerta para vencidas e reposição. Usa Segoe UI e ícones próprios em Java2D (sem emojis). As fotografias e os retratos são fictícios e estão incluídos no projeto. Detalhes em [Identidade visual](identidade-visual.md).

## 10. Segurança e integridade

- Senhas com PBKDF2-HMAC-SHA-256, 120 mil iterações e salt individual, conferidas com comparação em tempo constante.
- `PreparedStatement` em todas as consultas com dados do usuário.
- Sessão revalidada no banco a cada operação; bloqueio de 1 minuto após 5 senhas erradas; troca da própria senha exigindo a atual.
- Transações em matrícula, situação, pagamento, renovação, estoque, edição de usuário, inativação e remoção.
- Bloqueio de linhas (`FOR UPDATE`) e atualização condicional de saldo contra operações simultâneas.
- Toda gravação confere que alterou exatamente 1 linha.
- Auditoria das alterações de cadastros, gravada na mesma transação.

**Limites reais:** o banco local usa a conta `sa` sem senha e sem criptografia do arquivo, então quem tem acesso aos arquivos lê os dados fora da interface. Não há expiração de sessão por tempo, autenticação em duas etapas nem recuperação de senha por e-mail.

## 11. Testes e evidências

`SystemTest` usa o serviço real contra bancos isolados (em memória e um arquivo temporário, apagado no fim). **Resultado de 21/10/2026: 130 de 130.** A cobertura inclui:

- Login, bloqueio por tentativas, sessão revogada e permissões.
- Cadastro, CPF, telefone, e-mail e duplicidades.
- Matrícula, situações, vencimento e renovação.
- Parcelas e quitação.
- Estoque, concorrência e rollback.
- Remoção (excluir, inativar, bloquear), reativação e auditoria.
- Filtros e paginação de movimentações.
- Persistência e migração de banco antigo.

A web tem 21 testes (`node --test web/test/service.test.mjs`). O comando oficial é `scripts/testar.ps1`. O Maven não executa essa suíte com `mvn test`, porque ela não usa JUnit.

Os testes verificam cenários específicos. Não há ensaio com milhares de registros nem testes automatizados de cliques na interface.

## 12. Problemas confirmados na análise original — todos corrigidos

| Problema (09/10) | Situação | Evidência |
| --- | --- | --- |
| 12.1 Permissões antigas válidas numa sessão aberta | **Corrigido em 21/10:** a conta e o perfil são reconsultados a cada operação; se mudaram, a sessão é encerrada | CT-59 a CT-62 |
| 12.2 Edição de usuário e troca de senha fora de uma transação | **Corrigido em 21/10:** dados, senha e auditoria numa única transação | CT-63 |
| 12.3 Códigos, valores e datas ordenados como texto | **Corrigido em 15/10:** o modelo guarda os tipos e formata só ao desenhar | Conferência visual |

## 13. Pontos de atenção encontrados no código

| Ponto | Situação | Evidência |
| --- | --- | --- |
| Consultas na linha da interface | **Corrigido:** SwingWorker em telas, formulários, ficha, histórico e auditoria | Inspeção visual |
| Listas sem paginação | **Corrigido:** limite de 500 linhas; movimentações paginadas no banco | CT-100 |
| Edições sem conferir linhas alteradas | **Corrigido:** "Registro não encontrado" | CT-64 a CT-67, CT-130 |
| Inativação de aluno fora da transação | **Corrigido:** a checagem ocorre na transação que bloqueia a linha do aluno | CT-31 |
| Sem auditoria de cadastros | **Corrigido:** tabela `auditoria` | CT-92 a CT-95, CT-110 |
| Cartões de destaque não acompanham o filtro | Mantido: os dois cartões de Alunos e Usuários mostram os primeiros da lista, não o resultado da busca | — |
| Conta lateral montada ao entrar | Mantido: o nome e o retrato do menu só se atualizam no próximo login | — |
| Código concentrado em `AcademiaFrame` | Parcial: os painéis iniciais foram separados (`WorkspaceDashboard.load`), mas a janela principal continua grande | — |

## 14. Limites de escopo da versão

São recursos fora do escopo ou regras simplificadas de propósito, não falhas:

- Execução local, sem servidor e sem sincronização entre computadores; a versão web não compartilha dados com o aplicativo.
- Sem portal ou login do aluno e sem perfil de professor.
- Sem agenda de aulas, turmas, presença, catraca ou biometria.
- Sem treinos ou avaliação física.
- Sem cobrança recorrente, juros, descontos, estorno ou conciliação bancária; sem processamento real de PIX ou cartão; sem emissão fiscal.
- Sem e-mail, WhatsApp ou SMS.
- Sem upload de fotos próprias.
- Sem relatórios financeiros detalhados nem exportação das listas.
- Sem fornecedores, compras, lotes ou validade de produtos.
- Sem backup automático.
- O vencimento não cancela nada automaticamente e o trancamento não prorroga a validade (decisões de regra).

## 15. Como abrir e o que existe na entrega

No IntelliJ, abra a pasta Academia e execute **Pulso - Academia** (JDK 17 ou superior). No Windows, `Abrir-Academia.cmd` compila e abre o sistema; no Linux/macOS, `sh scripts/executar.sh`. Na primeira execução o banco é criado e populado.

| Material | Conteúdo |
| --- | --- |
| `src/main/java`, `src/test/java` | Código e testes |
| `database/schema.sql`, `database/banco-demonstracao.sql` | Estrutura e exportação com exemplos |
| `data/pulso.mv.db` | Banco local (criado na primeira execução; não vai no pacote) |
| `assets/` | Fotografias, retratos, logomarca e capturas das telas |
| `apresentacao/Pulso-Academia-Grafite.pptx` | Slides |
| `docs/` | Requisitos, banco, testes, web, roteiro, identidade, dificuldades e esta análise |
| `scripts/` | Executar, testar e exportar (`.ps1` e `.sh`) |
| `web/` | Demonstração web e seus testes |
| `pom.xml`, `academia-pulso.iml`, `.run/` | Configuração Maven/IntelliJ |

O pacote `Entrega-Pulso-Grupo05.zip` é gerado fora da pasta, sem `target`, `out`, `.build`, `.idea`, `.git` e `data`. Os arquivos removidos na organização de 21/10 estão em `_backup_removidos`, fora do projeto.

## 16. Perguntas e respostas para a apresentação

| Pergunta | Resposta baseada no código |
| --- | --- |
| Qual problema o sistema resolve? | Reúne alunos, contratos, recebimentos e estoque num só lugar, com histórico de quem fez cada operação |
| Onde ficam os dados? | No H2 persistente, em `data/pulso.mv.db`, acessado por JDBC |
| Por que dez tabelas? | Cada entidade tem uma responsabilidade; as chaves estrangeiras ligam cadastros, contratos e eventos |
| Como a senha é protegida? | Hash PBKDF2 com salt aleatório; o login recalcula e compara |
| E se o admin rebaixar alguém que está logado? | Na próxima operação dessa pessoa, o serviço reconsulta o banco e encerra a sessão |
| Como funciona o pagamento parcial? | Cada parcela é uma linha em `pagamentos`; o saldo é o valor menos a soma; o serviço recusa valor acima do saldo |
| Como o sistema sabe que venceu? | Compara `fim` com a data atual na consulta; o registro continua ATIVA |
| Por que não apagar um aluno antigo? | As chaves estrangeiras e o histórico financeiro dependem dele; por isso ele é inativado |
| O que impede estoque negativo? | `UPDATE ... WHERE saldo >= quantidade` numa transação junto com o histórico |
| O que é uma transação? | Um conjunto de alterações que é confirmado inteiro ou desfeito inteiro |
| O sistema faz PIX de verdade? | Não; registra um recebimento confirmado pela equipe |
| Os testes garantem que não há erro? | Garantem os 130 cenários verificados; não substituem testes de uso nem com grande volume |

## 17. Evoluções possíveis

1. Separar `AcademiaFrame` em telas e consultas por responsabilidade e adotar JUnit integrado ao Maven.
2. Proteger o arquivo do banco (senha e criptografia) e criar rotina de backup e restauração.
3. Expirar a sessão por tempo e permitir recuperação de senha.
4. Expandir conforme o cliente: agenda, presença, cobrança recorrente, relatórios e fotos próprias.

## 18. Avaliação final

O Pulso tem um fluxo administrativo completo e demonstrável: autenticar, cadastrar, configurar planos, matricular, receber em parcelas, trancar, renovar, movimentar estoque, remover ou inativar cadastros e auditar alterações. Há persistência, restrições no banco, transações nas operações compostas, controle de acesso revalidado e 130 verificações automáticas aprovadas.

Os pontos que continuam pedindo atenção são estruturais (concentração de código na janela principal e proteção do arquivo do banco) e de escopo (seção 14), não falhas de regra conhecidas.

## Fontes locais da análise

- [README](../README.md)
- [Regras de negócio](../src/main/java/br/com/pulso/AcademiaService.java)
- [Telas](../src/main/java/br/com/pulso/AcademiaFrame.java)
- [Persistência](../src/main/java/br/com/pulso/Database.java)
- [Esquema SQL](../database/schema.sql)
- [Testes automáticos](../src/test/java/br/com/pulso/SystemTest.java)
- [Resultados dos testes](resultados-testes.md)
- [Requisitos e testes](requisitos-e-testes.md)
- [Modelo do banco](modelo-banco.md)
- [Dificuldades e soluções](dificuldades-e-solucoes.md)
