# Resultados dos testes

Execução: 2026-10-09T10:44:16.380378-03:00[America/Sao_Paulo]

51 verificações passaram. Banco isolado H2. Testes de serviço e integração JDBC; a inspeção visual das telas é complementar.

| Caso | Verificação | Resultado |
| --- | --- | --- |
| CT-01 | Operação sem autenticação bloqueada | Passou |
| CT-02 | Login com senha incorreta bloqueado | Passou |
| CT-03 | Entrada SQL no login não autentica | Passou |
| CT-04 | Login válido reconhece administrador | Passou |
| CT-05 | Senha armazenada como hash com salt | Passou |
| CT-06 | Senhas iguais recebem salts diferentes | Passou |
| CT-07 | Cadastro grava e permite consultar aluno | Passou |
| CT-08 | CPF duplicado bloqueado | Passou |
| CT-09 | CPF com dígitos inválidos bloqueado | Passou |
| CT-10 | Nome vazio bloqueado | Passou |
| CT-11 | E-mail inválido bloqueado | Passou |
| CT-12 | Edição atualiza o aluno | Passou |
| CT-13 | Retrato escolhido é salvo com o aluno | Passou |
| CT-14 | Edição de dados preserva o retrato já escolhido | Passou |
| CT-15 | Caminho de imagem fora da galeria é bloqueado | Passou |
| CT-16 | Retrato inválido não altera o cadastro | Passou |
| CT-17 | Plano relaciona modalidade por chave estrangeira | Passou |
| CT-18 | Plano com preço negativo bloqueado | Passou |
| CT-19 | Plano sem duração válida bloqueado | Passou |
| CT-20 | Matrícula grava validade conforme duração | Passou |
| CT-21 | Matrícula registra histórico inicial | Passou |
| CT-22 | Matrícula aberta duplicada bloqueada | Passou |
| CT-23 | Alteração do plano preserva valor contratado | Passou |
| CT-24 | Aluno com matrícula aberta não pode ser inativado | Passou |
| CT-25 | Trancamento registra situação e histórico | Passou |
| CT-26 | Situação sem motivo bloqueada | Passou |
| CT-27 | Pagamento registra valor contratado e responsável | Passou |
| CT-28 | Pagamento duplicado bloqueado | Passou |
| CT-29 | Matrícula cancelada não pode ser reativada | Passou |
| CT-30 | Nova matrícula permitida após cancelamento | Passou |
| CT-31 | Matrícula para aluno inativo bloqueada | Passou |
| CT-32 | Produto inicia sem saldo | Passou |
| CT-33 | Entrada e saída atualizam saldo | Passou |
| CT-34 | Saldo no mínimo aparece no alerta | Passou |
| CT-35 | Saída maior que saldo bloqueada | Passou |
| CT-36 | Saída rejeitada preserva saldo e histórico | Passou |
| CT-37 | Quantidade zero bloqueada | Passou |
| CT-38 | Movimento sem motivo bloqueado | Passou |
| CT-39 | Atendente não cadastra usuários | Passou |
| CT-40 | Atendente não altera retrato de conta da equipe | Passou |
| CT-41 | Atendente não altera planos | Passou |
| CT-42 | Atendente registra movimento com responsável | Passou |
| CT-43 | Retrato de usuário mantém autenticação e perfil | Passou |
| CT-44 | Administrador não pode desativar a própria conta | Passou |
| CT-45 | Usuário cadastrado consegue autenticar | Passou |
| CT-46 | Logout encerra a sessão | Passou |
| CT-47 | Saídas simultâneas não deixam saldo negativo | Passou |
| CT-48 | Falha ao gravar histórico desfaz alteração de saldo | Passou |
| CT-49 | Dados persistem após nova abertura do banco | Passou |
| CT-50 | Remoção de retrato persiste após reabrir o banco | Passou |
| CT-51 | Banco anterior recebe retratos sem perder cadastros | Passou |
