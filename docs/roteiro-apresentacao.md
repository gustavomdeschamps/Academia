# Roteiro de apresentação e estudo

## Abertura

“Nosso cliente é a Academia Movimento. A proposta do Pulso é reunir o cadastro dos alunos, as matrículas e o estoque em um sistema que a recepção consiga usar durante o atendimento.”

Apresente os nomes dos integrantes e as responsabilidades reais de cada um. Os slides identificam o Grupo 05, porque os nomes ainda não foram informados.

## Sequência sugerida, de 8 a 12 minutos

1. Apresentar o cliente e o problema de manter informações separadas.
2. Mostrar o que o sistema cobre e explicar as regras de contratação.
3. Explicar as telas com apoio dos slides, sem ler todo o texto.
4. Abrir o aplicativo e executar a demonstração abaixo.
5. Apresentar a estrutura do banco, as tecnologias e o resultado dos testes.
6. Encerrar com as limitações e responder às perguntas.

## Demonstração ao vivo

**Preparação.** Os cenários abaixo (Faixa elástica abaixo do mínimo, Bruno e Diego com matrícula vencida) vêm da semente de um banco novo. Se `data/pulso.mv.db` foi criado por uma versão anterior, feche o aplicativo e renomeie esse arquivo (por exemplo, `pulso-anterior.mv.db`) antes do ensaio; nada é apagado e basta desfazer a troca de nomes depois. Para abrir: **Run** no IntelliJ, `scripts/executar.ps1` ou `sh scripts/executar.sh`.

| Passo | O que fazer | O que mostrar e explicar |
| --- | --- | --- |
| 1. Login | Digite `admin` com uma senha errada, depois `Admin@123` | `LoginPanel` chama `AcademiaService.login` em segundo plano (SwingWorker). Login procurado sem diferenciar maiúsculas, com parâmetro SQL (sem injeção); senha conferida com PBKDF2 e salt (`Security`). Cada erro soma em `usuarios.tentativas`; no 5º, `bloqueado_ate` = agora + 1 minuto. Daí em diante, cada operação reconfere no banco se a conta segue ativa e com o mesmo perfil. |
| 2. Cadastrar aluno com CPF fictício | Alunos > **Adicionar aluno**: Carla Mendes, CPF 111.111.111-11, telefone 3333-1234, e-mail carla@exemplo | `saveAluno` remove pontos e hífen e exige só 11 números (sem dígitos verificadores, para aceitar dados fictícios); telefone com pelo menos 8 números; e-mail com @. Na transação confere CPF único (também UNIQUE no banco) e grava `CRIACAO` na auditoria. Repita o CPF para mostrar a duplicidade e digite 10 números para mostrar "Informe o CPF com 11 números." |
| 3. Matricular | Matrículas > **Nova matrícula**: Carla, Mensal musculação, hoje | `matricular` bloqueia a linha do aluno (`FOR UPDATE`), exige aluno e plano ativos, recusa matrícula aberta repetida, calcula `fim = início + meses` e copia o preço do plano. Linha: Ativa, Pendente, saldo R$ 129,90. |
| 4. Pagar parcial | Selecione Carla > **Pagamento** > 50,00, PIX | Cada pagamento é uma linha em `pagamentos` (o antigo UNIQUE foi removido por migração). `pagar` bloqueia a matrícula, soma o já pago e recusa valor acima do saldo. Status calculado: **Parcial**, saldo R$ 79,90. Opcional: pagar o restante para ver **Pago**. |
| 5. Trancar | Carla > **Situação** > TRANCADA, motivo "Viagem" > **Histórico** | `situacao` grava mudança e histórico (data, motivo, responsável) na mesma transação. Trancar não prorroga a validade; CANCELADA é definitiva. |
| 6. Remover aluno sem vínculo | **Adicionar aluno** "Teste Remoção", CPF 222.333.444-55, telefone 3333-0000, e-mail t@t. Selecione e **Remover** | Remover só habilita com linha selecionada. Antes de confirmar, `previaRemocao` diz o que acontecerá: "Excluir definitivamente" (sem matrículas). `remover` faz o DELETE na transação, confere 1 linha e grava `EXCLUSAO` na auditoria com os dados apagados. |
| 7. Tentar remover aluno com matrícula | Selecione Carla (matrícula trancada) > **Remover** | Bloqueado: "O aluno possui matrícula ativa ou trancada. Cancele a matrícula antes de remover o aluno." Se a matrícula estivesse cancelada, o aluno seria **inativado** ("Possui histórico, foi inativado em vez de excluído.") e apareceria só com **Mostrar inativos**, com **Reativar**. Matrículas e pagamentos nunca são apagados. |
| 8. Movimentar estoque | Produtos > Faixa elástica (saldo 3, mínimo 5, linha destacada) > **Movimentar**: ENTRADA 4 "Compra"; depois SAÍDA 3 "Venda"; tente SAÍDA 10 | A saída só altera a linha se `saldo >= quantidade`; senão é recusada e o histórico não é gravado. O movimento guarda `saldo_apos`. Em Movimentações, filtre Produto e Tipo para mostrar filtros, saldo após e paginação. |
| 9. Ver alerta | Produtos e **Visão da gestão** | Com saldo 7 o alerta some; com 4 (≤ 5) a linha volta destacada como Repor. Na visão geral, os cartões "Itens para repor", "Vencidas" e "Pendências" saem das mesmas consultas. Opcional: Bruno (Vencida) > **Renovar**. |

Para encerrar: em Usuários > **Auditoria**, mostre a criação de Carla e a exclusão do aluno de teste, com responsável e data. Saia e entre como `recepcao` / `Recepcao@123`: painel verde, sem o menu Usuários, sem edição nem inativação de planos e produtos. Feche e reabra o aplicativo para mostrar a persistência.

## Guia para estudar o projeto

### Como a tela chega ao banco?

O formulário coleta os dados em `AcademiaFrame`. Ele chama um método de `AcademiaService`, que valida os campos e o perfil. O serviço usa `Database` para executar instruções SQL com JDBC. A interface recarrega a consulta após salvar. Para explicar, acompanhe o botão Nova matrícula e o método `matricular`.

### Para que servem as chaves estrangeiras?

Elas ligam registros existentes. `matriculas.aluno_id` aponta para alunos e `matriculas.plano_id` aponta para planos. Assim, o banco não aceita uma matrícula com aluno ou plano inexistente. Mostre essas referências no SQL.

### Por que separar plano e matrícula?

O plano representa a oferta da academia. A matrícula representa a contratação de um aluno. O sistema copia preço e calcula validade no momento da contratação. Alterar o preço do plano depois não modifica contratos antigos.

### O que acontece se a saída exceder o estoque?

A instrução SQL só atualiza um produto se houver saldo suficiente. Quando nenhuma linha muda, o serviço rejeita a operação. A atualização e o histórico pertencem à mesma transação. Uma falha desfaz os dois passos. Estude `movimentar` e os testes de concorrência e rollback.

### Onde os dados ficam guardados?

Em um arquivo H2 na pasta data. H2 é um banco relacional acessado por JDBC. O SQL declara dez tabelas, incluindo a auditoria. Usuários, alunos, planos, produtos e operações permanecem depois de fechar a janela. Os testes usam bancos separados.

### Como funciona a autenticação?

O serviço procura uma conta ativa pelo login, usando parâmetro SQL e ignorando maiúsculas no nome de usuário. A senha continua respeitando maiúsculas e minúsculas. Depois confere a senha com PBKDF2, salt e comparação segura. O perfil controla ações no serviço, além de ocultar botões na interface. A senha original não fica gravada na tabela.

### Por que não apagar tudo?

Excluir um aluno que já teve matrícula quebraria o histórico financeiro e as chaves estrangeiras. Por isso a remoção exclui só o que não tem vínculos e, nos demais casos, inativa (`ativo = FALSE`): o cadastro some das listas e das novas operações, mas os registros antigos continuam consultáveis. Estude `AcademiaService.excluivel` e `remover`.

### O que foi testado?

O relatório automático registra 130 verificações de regras e integração com JDBC. Elas incluem login, bloqueio por tentativas, sessão revogada, cadastro, retratos, duplicidades, permissões, histórico, parcelas, vencimento e renovação, inativação, auditoria, filtros de movimentação, validações de cadastro, exclusão, inativação, bloqueios e reativação de cadastros, persistência, migração de banco antigo, concorrência e rollback. A versão web tem 21 testes de regras (`node --test web/test/service.test.mjs`); no endereço publicado, 17 fluxos foram conferidos com navegador em 21/10 (login dos dois perfis, CPF fictício, matrícula, pagamento, remoção, estoque e celular). A conferência complementar da interface está em `docs/revisao-interface.md`. A interface deve ser ensaiada pelos casos manuais antes da apresentação. Não afirme que todos os cliques foram automatizados.

## Alternativa para demonstrar pela internet (somente demonstração)

Abra https://pulso-academia-movimento.vercel.app. Mostre a abertura sem barra, o login centralizado e as contas da demonstração. Entre como `recepcao` / `Recepcao@123`, consulte uma ficha e amplie o retrato. Compare com `admin` / `Admin@123`, destacando indicadores, configurações e gestão da equipe. Demonstre matrícula, pagamento e o bloqueio de saída acima do saldo.

Explique que os dados web são fictícios e pertencem somente àquele navegador. **O sistema oficial é o aplicativo Java com banco relacional H2**; a versão web não tem parcelas, vencimento, inativação, auditoria nem bloqueio de login. O site não realiza cobranças e não compartilha o banco do computador. Antes do ensaio, a administração pode restaurar os exemplos pela opção **Sobre a demonstração**.

### O que esta versão ainda não cobre?

Não há cobranças recorrentes, emissão fiscal, catraca, envio de mensagens nem backup automático. O vencimento é sinalizado e a renovação é manual; nada é cancelado automaticamente. Esses pontos são limites do escopo e devem ser apresentados com clareza.

## Divisão da fala

Distribua cliente e requisitos, interface e demonstração, banco e regras, testes e limites entre os integrantes. Ajuste essa divisão ao trabalho que cada pessoa realmente realizou. Todos devem conseguir explicar o fluxo completo. Evite decorar respostas sem acompanhar os métodos e as consultas citados acima.
