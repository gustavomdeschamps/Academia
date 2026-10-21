# Requisitos e roteiro de teste

Cliente fictício: Academia Movimento. Usuários: administrador e recepção. Dados de demonstração são fictícios.

## Requisitos funcionais

| Código | Necessidade | Tela |
| --- | --- | --- |
| RF01 | Entrar com conta ativa e encerrar sessão | Acesso |
| RF02 | Cadastrar, consultar, pesquisar e editar alunos (CPF com 11 números, aceitando dados fictícios) | Alunos |
| RF03 | Definir atividades oferecidas | Modalidades |
| RF04 | Definir preço total, modalidade e duração | Planos |
| RF05 | Contratar plano para aluno ativo e calcular validade | Matrículas |
| RF06 | Trancar, retomar ou cancelar com histórico | Matrículas |
| RF07 | Registrar pagamento da contratação de uma vez ou em parcelas, com total pago, saldo devedor e status | Matrículas |
| RF08 | Cadastrar produtos com unidade e mínimo | Produtos |
| RF09 | Registrar entrada e saída com motivo, responsável e saldo resultante | Movimentações |
| RF10 | Alertar saldo menor ou igual ao mínimo | Visão geral e Produtos (linha destacada) |
| RF11 | Administrar contas e perfis | Usuários |
| RF12 | Identificar matrícula vencida e renovar no mesmo plano | Matrículas e Visão geral |
| RF13 | Remover alunos, modalidades, planos, produtos e usuários: excluir sem vínculos, inativar com histórico; reativar e mostrar inativos | Alunos, Modalidades, Planos, Produtos e Usuários |
| RF14 | Registrar e consultar auditoria (quem, quando, o que mudou) | Usuários > Auditoria |
| RF15 | Trocar a própria senha informando a senha atual | Menu lateral > Alterar senha |
| RF16 | Filtrar movimentações por produto, tipo e período, com paginação | Movimentações |

## Cadastro interno e acesso

O enunciado do Grupo 05 pede usuários e autenticação, além de cadastrar e consultar alunos, planos, modalidades e produtos. Ele não determina uma página pública de criação de contas na entrada.

Nesta solução, a administração cria as contas da equipe na área Usuários. Administrador e recepção entram com contas existentes. Alunos são cadastrados dentro do sistema pela equipe e não recebem um login automaticamente. A tela inicial não oferece criação pública de administradores. As duas contas de demonstração permitem apresentar o fluxo antes de criar outras contas.

## Requisitos não funcionais

- RNF01: execução desktop em Java 17 ou superior, com projeto para IntelliJ.
- RNF02: banco relacional persistente local, acessado por JDBC.
- RNF03: senhas com hash, salt individual e consultas parametrizadas.
- RNF04: operações de estoque, situação, pagamento, renovação e edição de usuário atômicas (commit/rollback).
- RNF05: rótulos em português, pesquisa nas listas e mensagens de validação compreensíveis.
- RNF06: nenhum serviço externo necessário para a demonstração.
- RNF07: a cada operação o sistema confere no banco se a conta continua ativa e com o mesmo perfil; se mudou, a sessão é encerrada.
- RNF08: após 5 senhas incorretas seguidas, o login fica bloqueado por 1 minuto.
- RNF09: consultas e gravações da interface rodam em segundo plano (SwingWorker), sem travar a janela.
- RNF10: listas limitadas a 500 linhas; movimentações paginadas de 50 em 50 no banco.
- RNF11: bancos criados por versões anteriores são migrados ao abrir, sem apagar registros.

## Permissões

| Operação | Administrador | Atendente |
| --- | --- | --- |
| Consultar informações | Sim | Sim |
| Cadastrar, editar, remover e reativar alunos | Sim | Sim |
| Registrar matrícula, situação, pagamento e renovação | Sim | Sim |
| Registrar movimentação de estoque | Sim | Sim |
| Trocar a própria senha | Sim | Sim |
| Alterar, remover e reativar modalidades, planos e produtos | Sim | Não |
| Gerenciar, remover e reativar usuários | Sim | Não |
| Consultar a auditoria | Sim | Não |

## Regras adotadas

**Cadastro de aluno.** O CPF precisa ter exatamente 11 números (pontos e hífen são ignorados) e ser único; os dígitos verificadores não são conferidos, para aceitar dados fictícios como 111.111.111-11. O telefone precisa de pelo menos 8 números, sem exigir DDD. O e-mail continua obrigatório (a coluna é NOT NULL), mas basta conter @.

Um aluno ativo pode ter planos diferentes, mas não duas matrículas abertas no mesmo plano. ATIVA e TRANCADA são situações abertas. CANCELADA é definitiva, permitindo nova contratação. O valor da matrícula copia o preço do plano, preservando o histórico se o plano mudar.

**Vencimento.** Uma matrícula ATIVA cuja data final já passou é exibida como VENCIDA (na lista de matrículas, com a linha destacada, e na visão geral). O registro gravado continua ATIVA; a classificação é calculada na consulta, por isso não depende de nenhuma rotina agendada. Renovar encerra a matrícula vencida (CANCELADA, com motivo no histórico) e cria outra no mesmo plano a partir de hoje, com o preço atual. A renovação exige que o saldo da matrícula vencida esteja quitado. Trancar não prorroga a validade.

**Pagamentos.** O valor do plano é total para sua duração e pode ser pago de uma vez ou em parcelas, em PIX, dinheiro ou cartão. A soma das parcelas nunca ultrapassa o valor contratado. Status: PENDENTE (nada pago), PARCIAL (pago menor que o valor) e PAGO (quitado). Matrícula cancelada não recebe pagamento.

**Remoção.** O botão Remover (com confirmação) vale para alunos, modalidades, planos, produtos e usuários. Matrículas, pagamentos, movimentações e históricos nunca são apagados.

| Cadastro | Excluído de verdade quando | Inativado quando tem | Bloqueado quando |
| --- | --- | --- | --- |
| Aluno | não tem matrículas | matrículas encerradas (canceladas) | tem matrícula ATIVA ou TRANCADA |
| Plano | não tem matrículas | matrículas encerradas | tem matrícula ATIVA ou TRANCADA |
| Modalidade | não tem planos | planos (também inativados) | algum plano tem matrícula aberta |
| Produto | não tem movimentações | movimentações | tem saldo maior que zero |
| Usuário | não registrou matrícula, histórico, pagamento, movimento ou auditoria | algum desses registros | é a própria conta ou o último administrador ativo |

A inativação avisa "Possui histórico, foi inativado em vez de excluído." Cadastro já inativo com histórico não pode ser removido de novo. Alunos podem ser removidos pela recepção; os demais cadastros só pelo administrador. Tudo ocorre em uma transação, conferindo uma linha afetada, com registro na auditoria (EXCLUSAO ou INATIVACAO). As listas e as caixas de seleção mostram só cadastros ativos; "Mostrar inativos" exibe os demais e libera Reativar. Um plano só é reativado se a modalidade estiver ativa. As ações antigas de inativação do serviço (`modalidadeAtiva`, `planoAtivo`, `produtoAtivo`) continuam disponíveis e são usadas pela reativação.

**Estoque.** Quantidades inteiras. Produto novo inicia em zero e recebe saldo por entrada registrada. Quantidade deve ser positiva e saída deve respeitar o saldo. Entrada e saída exigem motivo e guardam responsável, data e o saldo após o movimento. O alerta aparece quando o saldo é menor ou igual ao mínimo.

**Contas.** O administrador não pode desativar nem rebaixar a própria conta. A edição de usuário grava dados e senha na mesma transação. Editar um código inexistente informa "registro não encontrado" em vez de aparentar sucesso. Criações, alterações, inativações e trocas de senha de alunos, modalidades, planos, produtos e usuários ficam na auditoria com o usuário, a data e cada campo no formato `valor anterior -> valor novo`.

## Testes automatizados

`scripts/testar.ps1` (Windows) ou `scripts/testar.sh` (Linux/macOS) compila com `--release 17` e executa `SystemTest`, que usa o serviço real contra um banco H2 isolado. O resultado completo, caso a caso, é gravado em `docs/resultados-testes.md`. Os casos novos desta versão:

| Casos | Regra verificada |
| --- | --- |
| CT-59 a CT-62 | Sessão revogada no banco: conta desativada e perfil alterado encerram a sessão aberta |
| CT-63 | Edição de usuário em transação única: falha no meio desfaz dados e senha |
| CT-64 a CT-67 | ID inexistente em modalidade, plano, produto e usuário: "registro não encontrado" |
| CT-68 a CT-70 | Troca da própria senha com conferência da senha atual |
| CT-71 a CT-73 | Bloqueio por 1 minuto após 5 tentativas e liberação depois do prazo |
| CT-74 a CT-78 | Vencimento (VENCIDA) e renovação no mesmo plano |
| CT-79 a CT-83 | Pagamento parcial: PENDENTE, PARCIAL, PAGO, limite do saldo |
| CT-16 | CPF: menos ou mais de 11 números bloqueado; CPF fictício de 11 números aceito |
| CT-84 a CT-91 | Inativação com bloqueios de uso e reativação |
| CT-92 a CT-96 | Auditoria: autor, data, valores anterior e novo; consulta e inativação restritas ao administrador |
| CT-97 a CT-101 | Filtros de movimentação, saldo após e paginação |
| CT-102 a CT-104 | Migração de um banco da versão anterior sem perda de dados |
| CT-105 a CT-107 | Telefone com 8 números sem DDD; e-mail exige apenas @ |
| CT-108 a CT-111 | Exclusão sem vínculos (aluno, modalidade, plano, produto, usuário) e registro na auditoria |
| CT-112 a CT-116 | Inativação com histórico (aluno, produto, usuário, modalidade com seus planos) |
| CT-117 a CT-123 | Bloqueios de remoção e garantia de que nada muda |
| CT-124 e CT-125 | Permissão do atendente: remove alunos, não remove os demais cadastros |
| CT-126 a CT-128 | Reativação |
| CT-129 e CT-130 | Rollback da remoção e código inexistente |

A versão web tem testes próprios: `node --test web/test/service.test.mjs` (21 testes, incluindo validações, exclusão, inativação, bloqueios, permissão da recepção, reativação e rollback).

## Casos manuais para a apresentação

| Caso | Pré-condição e passos | Resultado esperado |
| --- | --- | --- |
| TM01 | Na tela de acesso, digitar admin e senha incorreta | Mensagem de credenciais inválidas, sem acesso |
| TM02 | Entrar como ADMIN com Admin@123 | Menu completo e Visão da gestão; maiúsculas no usuário são aceitas |
| TM03 | Buscar Ana na tela Alunos | Lista mostra somente registros que contêm Ana |
| TM04 | Novo aluno com CPF fictício 111.111.111-11, nome Carla Mendes, telefone 3333-1234 e carla@exemplo | Cadastro aparece na lista |
| TM05 | Repetir o CPF usado no caso anterior | Mensagem de duplicidade, sem novo registro |
| TM06 | Selecionar Carla e editar seu telefone | Alteração aparece após salvar e reiniciar |
| TM07 | Criar matrícula de Carla no plano Mensal musculação | Situação Ativa, pagamento Pendente, validade de um mês |
| TM08 | Tentar criar a mesma matrícula novamente | Duplicidade bloqueada |
| TM09 | Selecionar a matrícula, Pagamento, informar 50,00 em PIX | Pagamento Parcial, saldo R$ 79,90 |
| TM10 | Pagamento novamente com o valor sugerido (saldo) | Pagamento Pago; uma terceira tentativa é bloqueada |
| TM11 | Trancar a matrícula com motivo Viagem e abrir Histórico | Alteração mostra situação, motivo, data e responsável |
| TM12 | Produto Faixa elástica com saldo inicial 3 e mínimo 5: registrar entrada 4 | Saldo 7, alerta desaparece |
| TM13 | Registrar saída 3 da Faixa elástica | Saldo 4, linha destacada com alerta Repor |
| TM14 | Tentar saída maior que o saldo atual | Mensagem de saldo insuficiente, sem mudança no saldo ou histórico |
| TM15 | Em Movimentações, filtrar Produto Faixa elástica e Tipo SAIDA | Somente saídas da faixa, com a coluna Saldo após |
| TM16 | Sair e entrar como recepcao com Recepcao@123 | Tela Usuários ausente, edição de planos e produtos indisponível |
| TM17 | Fechar e abrir novamente o sistema | Dados cadastrados continuam disponíveis |
| TM18 | Em Matrículas, selecionar Bruno Oliveira (Vencida) e clicar Renovar | Matrícula antiga cancelada e nova Ativa a partir de hoje |
| TM19 | Selecionar Diego Ramos (Vencida, Parcial) e clicar Renovar | Pedido para quitar o saldo antes de renovar |
| TM20 | Em Planos, selecionar Mensal musculação e Remover | Bloqueado: há matrículas abertas |
| TM21 | Errar a senha da recepção 5 vezes | Acesso bloqueado por 1 minuto |
| TM22 | Alterar senha pelo menu lateral, sair e entrar com a nova | Login com a nova senha; a antiga é recusada |
| TM23 | Com o admin, desativar a recepção enquanto ela está conectada em outra janela | Na próxima ação a recepção volta ao login com aviso |
| TM24 | Em Usuários, clicar Auditoria | Lista com data, responsável, cadastro e "anterior -> novo" |
| TM25 | Cadastrar um aluno sem matrícula, selecionar e Remover | Confirmação diz "excluir definitivamente"; o aluno some da lista |
| TM26 | Selecionar Ana Martins (matrícula ativa) e Remover | Mensagem de bloqueio, nada muda |
| TM27 | Em Produtos, zerar o saldo da Toalha de rosto (saída 2) e Remover | Confirmação de inativação; marcar Mostrar inativos e usar Reativar |

Antes de repetir os casos, confira os dados existentes. O teste manual não apaga registros nem restaura o saldo inicial automaticamente. Não marque estes casos como executados até realizá-los no aplicativo. O relatório automático registra somente verificações realmente executadas pelo programa de testes.

## Demonstração web (apenas demonstração)

**O sistema oficial desta entrega é o aplicativo Java com banco relacional H2.** A versão web em https://pulso-academia-movimento.vercel.app é somente uma vitrine para a apresentação: guarda dados no IndexedDB de cada navegador, não usa o banco H2 e não sincroniza com o aplicativo. Ela acompanha as validações de cadastro e a remoção/inativação/reativação, mas não implementa parcelas, vencimento, auditoria, troca da própria senha nem bloqueio de login. Detalhes e limites em `docs/versao-web.md`. O site não deve receber dados pessoais ou senhas reais.
