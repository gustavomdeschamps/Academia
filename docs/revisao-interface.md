# Revisão da interface Grafite

O sistema foi compilado com Java 17 como versão mínima e conferido em janelas de 1440 × 940 e 1200 × 800.

As 51 verificações do serviço e do banco passaram. O relatório está em `docs/resultados-testes.md`. Os testes incluem persistência dos retratos, edição sem perda da foto, rejeição de caminhos externos à galeria e migração do banco anterior.

A conferência da interface usou um banco temporário, separado dos dados do aplicativo:

- A abertura animada terminou e exibiu o login.
- A navegação iniciou e concluiu a transição, sem manter uma imagem por cima da tela.
- O formulário cadastrou um aluno e salvou o retrato escolhido.
- A ficha apresentou o retrato, os contatos e o contrato do aluno.
- Foram inspecionadas as telas de login, início, alunos, modalidades, planos, matrículas, produtos, movimentações e usuários.
- Foram inspecionados os formulários de aluno, usuário, plano, produto, matrícula, situação, pagamento e movimentação, além do histórico.
- As fotografias conservaram a proporção e o enquadramento completo.

Formulários maiores têm rolagem e mantêm os botões de salvar e cancelar visíveis. Campos longos nas listas podem ser consultados com a dica exibida ao passar o cursor. A apresentação acompanha a edição Grafite em 18 slides.
