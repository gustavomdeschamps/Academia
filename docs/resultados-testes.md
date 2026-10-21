# Plano e resultados dos testes

## Plano de testes

| Item | Definição |
| --- | --- |
| Objetivo | Verificar regras de negócio, permissões, integridade transacional e persistência do aplicativo Java |
| Tipo | Testes automatizados de serviço e integração com banco (JDBC + H2), caixa-preta pelos métodos públicos de `AcademiaService` |
| Ambiente | Banco H2 em memória e um arquivo temporário (apagado no fim); `data/pulso.mv.db` nunca é usado |
| Execução | `scripts/testar.ps1` (Windows) ou `sh scripts/testar.sh` (Linux/macOS), compilando com `--release 17` |
| Critério de aprovação | Todas as verificações passam; a primeira falha interrompe a execução e o relatório não é gravado |
| Técnicas | Valores válidos e inválidos (CPF, telefone, e-mail, valores), limites (saldo, parcelas, 5 tentativas), permissões por perfil, concorrência (duas threads), falha provocada para rollback, migração de banco antigo |
| Complementos | Versão web: `node --test web/test/service.test.mjs` (21 testes); casos manuais TM01 a TM27 em `docs/requisitos-e-testes.md` |

| Casos | Área |
| --- | --- |
| CT-01 a CT-13 | Autenticação, senhas e contas |
| CT-14 a CT-23 | Cadastro de alunos, modalidades e planos |
| CT-24 a CT-38 | Matrículas, situações e pagamento |
| CT-39 a CT-58 | Estoque, permissões do atendente, concorrência, rollback e persistência |
| CT-59 a CT-73 | Sessão revogada, transação de usuário, ID inexistente, troca de senha e bloqueio |
| CT-74 a CT-91 | Vencimento e renovação, parcelas, inativação |
| CT-92 a CT-104 | Auditoria, filtros de movimentação e migração |
| CT-105 a CT-130 | Validações de cadastro, remoção (excluir, inativar, bloquear), reativação e rollback |

## Resultado

Execução: **21/10/2026 às 11:37** (horário de São Paulo). **130 de 130 verificações aprovadas.**

| Caso | Verificação | Resultado |
| --- | --- | --- |
| CT-01 | Operação sem autenticação bloqueada | Passou |
| CT-02 | Login com senha incorreta bloqueado | Passou |
| CT-03 | Entrada SQL no login não autentica | Passou |
| CT-04 | Login válido reconhece administrador | Passou |
| CT-05 | Login aceita maiúsculas e espaços externos no usuário | Passou |
| CT-06 | Recepção autentica com usuário em maiúsculas | Passou |
| CT-07 | Senha mantém diferença entre maiúsculas e minúsculas | Passou |
| CT-08 | Login sem usuário é recusado sem falha interna | Passou |
| CT-09 | Cadastro não duplica login por diferença de maiúsculas | Passou |
| CT-10 | Novos logins são guardados em minúsculas | Passou |
| CT-11 | Conta inativa não entra mesmo com senha correta | Passou |
| CT-12 | Senha armazenada como hash com salt | Passou |
| CT-13 | Senhas iguais recebem salts diferentes | Passou |
| CT-14 | Cadastro grava e permite consultar aluno | Passou |
| CT-15 | CPF duplicado bloqueado | Passou |
| CT-16 | CPF com menos ou mais de 11 números é bloqueado e CPF fictício de 11 números é aceito | Passou |
| CT-17 | Nome vazio bloqueado | Passou |
| CT-18 | E-mail inválido bloqueado | Passou |
| CT-19 | Edição atualiza o aluno | Passou |
| CT-20 | Retrato escolhido é salvo com o aluno | Passou |
| CT-21 | Edição de dados preserva o retrato já escolhido | Passou |
| CT-22 | Caminho de imagem fora da galeria é bloqueado | Passou |
| CT-23 | Retrato inválido não altera o cadastro | Passou |
| CT-24 | Plano relaciona modalidade por chave estrangeira | Passou |
| CT-25 | Plano com preço negativo bloqueado | Passou |
| CT-26 | Plano sem duração válida bloqueado | Passou |
| CT-27 | Matrícula grava validade conforme duração | Passou |
| CT-28 | Matrícula registra histórico inicial | Passou |
| CT-29 | Matrícula aberta duplicada bloqueada | Passou |
| CT-30 | Alteração do plano preserva valor contratado | Passou |
| CT-31 | Aluno com matrícula aberta não pode ser inativado | Passou |
| CT-32 | Trancamento registra situação e histórico | Passou |
| CT-33 | Situação sem motivo bloqueada | Passou |
| CT-34 | Pagamento registra valor contratado e responsável | Passou |
| CT-35 | Pagamento duplicado bloqueado | Passou |
| CT-36 | Matrícula cancelada não pode ser reativada | Passou |
| CT-37 | Nova matrícula permitida após cancelamento | Passou |
| CT-38 | Matrícula para aluno inativo bloqueada | Passou |
| CT-39 | Produto inicia sem saldo | Passou |
| CT-40 | Entrada e saída atualizam saldo | Passou |
| CT-41 | Saldo no mínimo aparece no alerta | Passou |
| CT-42 | Saída maior que saldo bloqueada | Passou |
| CT-43 | Saída rejeitada preserva saldo e histórico | Passou |
| CT-44 | Quantidade zero bloqueada | Passou |
| CT-45 | Movimento sem motivo bloqueado | Passou |
| CT-46 | Atendente não cadastra usuários | Passou |
| CT-47 | Atendente não altera retrato de conta da equipe | Passou |
| CT-48 | Atendente não altera planos | Passou |
| CT-49 | Atendente registra movimento com responsável | Passou |
| CT-50 | Retrato de usuário mantém autenticação e perfil | Passou |
| CT-51 | Administrador não pode desativar a própria conta | Passou |
| CT-52 | Usuário cadastrado consegue autenticar | Passou |
| CT-53 | Logout encerra a sessão | Passou |
| CT-54 | Saídas simultâneas não deixam saldo negativo | Passou |
| CT-55 | Falha ao gravar histórico desfaz alteração de saldo | Passou |
| CT-56 | Dados persistem após nova abertura do banco | Passou |
| CT-57 | Remoção de retrato persiste após reabrir o banco | Passou |
| CT-58 | Banco anterior recebe retratos sem perder cadastros | Passou |
| CT-59 | Conta desativada tem a sessão aberta encerrada | Passou |
| CT-60 | Sessão revogada exige novo login | Passou |
| CT-61 | Mudança de perfil encerra a sessão administrativa | Passou |
| CT-62 | Perfil rebaixado não grava cadastro administrativo | Passou |
| CT-63 | Falha na edição de usuário desfaz dados e senha juntos | Passou |
| CT-64 | Edição de modalidade inexistente informa registro não encontrado | Passou |
| CT-65 | Edição de plano inexistente informa registro não encontrado | Passou |
| CT-66 | Edição de produto inexistente informa registro não encontrado | Passou |
| CT-67 | Edição de usuário inexistente informa registro não encontrado | Passou |
| CT-68 | Troca de senha exige a senha atual correta | Passou |
| CT-69 | Usuário troca a própria senha e entra com a nova | Passou |
| CT-70 | Senha antiga deixa de funcionar após a troca | Passou |
| CT-71 | Quinta senha incorreta bloqueia o acesso por 1 minuto | Passou |
| CT-72 | Senha correta é recusada durante o bloqueio | Passou |
| CT-73 | Acesso volta após 1 minuto e zera as tentativas | Passou |
| CT-74 | Matrícula ativa com fim passado aparece como vencida | Passou |
| CT-75 | Consulta das telas classifica a matrícula como VENCIDA | Passou |
| CT-76 | Renovação exige quitar o saldo da matrícula vencida | Passou |
| CT-77 | Renovação cria nova matrícula ativa no mesmo plano a partir de hoje | Passou |
| CT-78 | Matrícula dentro da validade não pode ser renovada | Passou |
| CT-79 | Matrícula sem pagamento fica PENDENTE | Passou |
| CT-80 | Pagamento parcial mostra total pago, saldo e status PARCIAL | Passou |
| CT-81 | Parcela maior que o saldo devedor é recusada | Passou |
| CT-82 | Parcelas somadas quitam a matrícula como PAGO | Passou |
| CT-83 | Pagamento após a quitação é recusado | Passou |
| CT-84 | Plano com matrícula ativa não pode ser inativado | Passou |
| CT-85 | Modalidade com plano ativo não pode ser inativada | Passou |
| CT-86 | Inativação preserva plano, modalidade e matrículas no banco | Passou |
| CT-87 | Plano inativo não aceita nova matrícula | Passou |
| CT-88 | Plano não é reativado com a modalidade inativa | Passou |
| CT-89 | Produto com saldo não pode ser inativado | Passou |
| CT-90 | Produto inativo não recebe movimentação | Passou |
| CT-91 | Produto reativado volta a movimentar | Passou |
| CT-92 | Auditoria registra quem, quando e o valor anterior e o novo | Passou |
| CT-93 | Gravação sem mudança não gera linha de auditoria | Passou |
| CT-94 | Auditoria cobre planos, produtos e usuários | Passou |
| CT-95 | Atendente não consulta a auditoria | Passou |
| CT-96 | Atendente não inativa produtos | Passou |
| CT-97 | Filtro por produto e tipo retorna apenas as saídas do produto | Passou |
| CT-98 | Saldo após cada movimento acompanha a sequência | Passou |
| CT-99 | Filtro por período inclui hoje e exclui datas futuras | Passou |
| CT-100 | Paginação traz uma página por vez com o total geral | Passou |
| CT-101 | Período com data final anterior à inicial é recusado | Passou |
| CT-102 | Banco da versão anterior migra sem perder pagamentos, movimentos e matrículas | Passou |
| CT-103 | Migração remove o UNIQUE de pagamentos e reconstrói o saldo após | Passou |
| CT-104 | Após a migração a matrícula aceita uma nova parcela | Passou |
| CT-105 | Telefone com 8 números é aceito sem DDD | Passou |
| CT-106 | Telefone com menos de 8 números é bloqueado | Passou |
| CT-107 | E-mail exige apenas @ | Passou |
| CT-108 | Prévia indica exclusão para aluno sem matrículas | Passou |
| CT-109 | Aluno sem vínculos é excluído de verdade | Passou |
| CT-110 | Exclusão fica registrada na auditoria | Passou |
| CT-111 | Modalidade, plano, produto e usuário sem vínculos são excluídos | Passou |
| CT-112 | Prévia indica inativação para aluno com matrícula encerrada | Passou |
| CT-113 | Aluno com histórico é inativado em vez de excluído | Passou |
| CT-114 | Produto com movimentações é inativado e o histórico continua | Passou |
| CT-115 | Usuário com operações registradas é inativado | Passou |
| CT-116 | Modalidade com planos é inativada junto com seus planos ativos | Passou |
| CT-117 | Aluno com matrícula ativa não pode ser removido | Passou |
| CT-118 | Plano com matrícula aberta não pode ser removido | Passou |
| CT-119 | Modalidade com matrícula aberta em seus planos não pode ser removida | Passou |
| CT-120 | Produto com saldo não pode ser removido | Passou |
| CT-121 | Administrador não remove a própria conta | Passou |
| CT-122 | Cadastro já inativo com histórico não é excluído | Passou |
| CT-123 | Remoções bloqueadas não alteram os cadastros | Passou |
| CT-124 | Atendente remove aluno sem vínculos | Passou |
| CT-125 | Atendente não remove modalidades, planos, produtos ou usuários | Passou |
| CT-126 | Reativação devolve aluno e produto às operações | Passou |
| CT-127 | Plano não é reativado antes da sua modalidade | Passou |
| CT-128 | Modalidade e plano reativados voltam a aceitar matrícula | Passou |
| CT-129 | Falha ao auditar a exclusão desfaz a remoção (rollback) | Passou |
| CT-130 | Remoção de código inexistente informa registro não encontrado | Passou |
