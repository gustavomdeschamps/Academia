# Requisitos e roteiro de teste

Cliente fictício: Academia Movimento. Usuários: administrador e recepção. Dados de demonstração são fictícios.

## Requisitos funcionais

| Código | Necessidade | Tela |
| --- | --- | --- |
| RF01 | Entrar com conta ativa e encerrar sessão | Acesso |
| RF02 | Cadastrar, consultar, pesquisar e editar alunos | Alunos |
| RF03 | Definir atividades oferecidas | Modalidades |
| RF04 | Definir preço total, modalidade e duração | Planos |
| RF05 | Contratar plano para aluno ativo e calcular validade | Matrículas |
| RF06 | Trancar, retomar ou cancelar com histórico | Matrículas |
| RF07 | Registrar pagamento único da contratação | Matrículas |
| RF08 | Cadastrar produtos com unidade e mínimo | Produtos |
| RF09 | Registrar entrada e saída com motivo e responsável | Movimentações |
| RF10 | Alertar saldo menor ou igual ao mínimo | Visão geral e Produtos |
| RF11 | Administrar contas e perfis | Usuários |

## Requisitos não funcionais

- RNF01: execução desktop em Java 17 ou superior, com projeto para IntelliJ.
- RNF02: banco relacional persistente local, acessado por JDBC.
- RNF03: senhas com hash, salt individual e consultas parametrizadas.
- RNF04: operações de estoque e alteração de situação atômicas.
- RNF05: rótulos em português, pesquisa nas listas e mensagens de validação.
- RNF06: nenhum serviço externo necessário para a demonstração.

## Permissões

| Operação | Administrador | Atendente |
| --- | --- | --- |
| Consultar informações | Sim | Sim |
| Cadastrar e editar alunos | Sim | Sim |
| Registrar matrícula, situação e pagamento | Sim | Sim |
| Registrar movimentação de estoque | Sim | Sim |
| Alterar modalidades, planos e produtos | Sim | Não |
| Gerenciar usuários | Sim | Não |

## Regras adotadas

CPF válido e único. Um aluno ativo pode ter planos diferentes, mas não duas matrículas abertas no mesmo plano. ATIVA e TRANCADA são situações abertas. CANCELADA é definitiva, permitindo nova contratação. O valor da matrícula copia o preço do plano, preservando o histórico se o plano mudar.

O valor do plano é total para sua duração. O pagamento é único por matrícula, em PIX, dinheiro ou cartão. Trancar não prorroga a validade. A data final aparece para conferência e não altera a situação automaticamente.

O estoque usa quantidades inteiras. Produto novo inicia em zero e recebe saldo por entrada registrada. Quantidade deve ser positiva e saída deve respeitar o saldo. Entrada e saída exigem motivo e guardam responsável e data. O alerta aparece quando saldo é menor ou igual ao mínimo. O administrador não pode desativar nem rebaixar sua própria conta.

## Casos manuais para a apresentação

| Caso | Pré-condição e passos | Resultado esperado |
| --- | --- | --- |
| TM01 | Na tela de acesso, digitar admin e senha incorreta | Mensagem de credenciais inválidas, sem acesso |
| TM02 | Entrar como admin com Admin@123 | Menu completo e visão geral |
| TM03 | Buscar Ana na tela Alunos | Lista mostra somente registros que contêm Ana |
| TM04 | Novo aluno com CPF 12345678909, nome Carla Mendes, telefone (47) 99999-1234 e carla@example.com | Cadastro aparece na lista |
| TM05 | Repetir o CPF usado no caso anterior | Mensagem de duplicidade, sem novo registro |
| TM06 | Selecionar Carla e editar seu telefone | Alteração aparece após salvar e reiniciar |
| TM07 | Criar matrícula de Carla no plano Mensal musculação | Situação ATIVA, pagamento PENDENTE, validade de um mês |
| TM08 | Tentar criar a mesma matrícula novamente | Duplicidade bloqueada |
| TM09 | Selecionar matrícula, registrar pagamento em PIX | Pagamento PAGO, valor preservado |
| TM10 | Tentar registrar um segundo pagamento | Operação bloqueada |
| TM11 | Trancar a matrícula com motivo Viagem e abrir Histórico | Alteração mostra situação, motivo, data e responsável |
| TM12 | Produto Faixa elástica com saldo inicial 3 e mínimo 5: registrar entrada 4 | Saldo 7, alerta desaparece |
| TM13 | Registrar saída 3 da Faixa elástica | Saldo 4, alerta REPOR aparece |
| TM14 | Tentar saída maior que o saldo atual | Mensagem de saldo insuficiente, sem mudança no saldo ou histórico |
| TM15 | Consultar Movimentações e buscar Faixa | Históricos de entrada e saída aparecem |
| TM16 | Sair e entrar como recepcao com Recepcao@123 | Tela Usuários ausente, edição de planos e produtos indisponível |
| TM17 | Fechar e abrir novamente o sistema | Dados cadastrados continuam disponíveis |

Antes de repetir os casos, confira os dados existentes. O teste manual não apaga registros nem restaura o saldo inicial automaticamente. Não marque estes casos como executados até realizá-los no aplicativo. O relatório automático registra somente verificações realmente executadas pelo programa de testes.
