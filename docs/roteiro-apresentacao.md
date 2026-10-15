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

Entre como admin / Admin@123. Na visão geral, mostre o alerta da Faixa elástica. Em Alunos, pesquise Ana, depois cadastre Carla Mendes usando CPF 12345678909, telefone (47) 99999-1234 e carla@example.com. Se Carla já existir por causa de um ensaio, use seu cadastro atual.

Em Matrículas, contrate Mensal musculação para Carla com a data do dia. Se já houver matrícula aberta igual, use a existente. Mostre início, validade, valor, situação e pagamento pendente. Selecione a linha, registre o pagamento em PIX e explique que o pagamento corresponde ao total contratado. Tranque com motivo Viagem e consulte o histórico.

Em Produtos, selecione Faixa elástica. Registre uma entrada e uma saída com motivos. Consulte os movimentos. Tente uma saída maior que o saldo e mostre a validação. Volte à visão geral para explicar como o alerta acompanha o saldo.

Saia e entre como recepcao / Recepcao@123. Mostre a diferença de permissões. Termine fechando e reabrindo a aplicação para confirmar a persistência.

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

Em um arquivo H2 na pasta data. H2 é um banco relacional acessado por JDBC. O SQL declara nove tabelas. Usuários, alunos, planos, produtos e operações permanecem depois de fechar a janela. Os testes usam bancos separados.

### Como funciona a autenticação?

O serviço procura uma conta ativa pelo login, usando parâmetro SQL. Depois confere a senha com PBKDF2, salt e comparação segura. O perfil controla ações no serviço, além de ocultar botões na interface. A senha original não fica gravada na tabela.

### O que foi testado?

O relatório automático registra 51 verificações de regras e integração com JDBC. Elas incluem login, cadastro, retratos, duplicidades, permissões, histórico, pagamento, persistência, concorrência e rollback. A conferência complementar da interface está em `docs/revisao-interface.md`. A interface deve ser ensaiada pelos casos manuais antes da apresentação. Não afirme que todos os cliques foram automatizados.

### O que esta versão ainda não cobre?

Pagamento é único por contratação. Não há cobranças recorrentes, emissão fiscal, catraca, envio de mensagens nem backup automático. A validade é consultável, sem cancelamento automático no vencimento. Esses pontos são limites do escopo e devem ser apresentados com clareza.

## Divisão da fala

Distribua cliente e requisitos, interface e demonstração, banco e regras, testes e limites entre os integrantes. Ajuste essa divisão ao trabalho que cada pessoa realmente realizou. Todos devem conseguir explicar o fluxo completo. Evite decorar respostas sem acompanhar os métodos e as consultas citados acima.
