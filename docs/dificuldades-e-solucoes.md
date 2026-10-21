# Dificuldades e soluções

Problemas reais encontrados durante o desenvolvimento e os testes do Pulso, com a solução adotada e onde ela pode ser conferida. Os números CT referem-se a `docs/resultados-testes.md`.

| # | Dificuldade | Como apareceu | Solução adotada | Evidência |
| --- | --- | --- | --- | --- |
| 1 | Permissões antigas continuavam valendo numa sessão aberta | Um administrador rebaixado a atendente (ou desativado) ainda conseguia criar planos até sair do sistema | `authenticated()` reconsulta no banco, a cada operação, se a conta está ativa e com o mesmo perfil; se mudou, a sessão é encerrada e a tela volta ao login com aviso | CT-59 a CT-62 |
| 2 | Edição de usuário podia ficar pela metade | Com uma falha provocada na troca de senha, o novo nome já tinha sido salvo | Dados, senha e auditoria gravados em **uma transação** (commit no fim, rollback em qualquer erro) | CT-63 |
| 3 | Testar falhas raras (rollback) | Não há como "derrubar" o banco no meio de uma operação real | O teste cria uma restrição `CHECK ... NOCHECK` temporária que só recusa a última instrução, confere que nada ficou gravado e depois remove a restrição | CT-55, CT-63, CT-129 |
| 4 | Estoque negativo com duas saídas ao mesmo tempo | Duas recepções podiam vender a última unidade simultaneamente | A saída é um único `UPDATE ... WHERE saldo >= quantidade`; se nenhuma linha muda, a operação é recusada. Saldo e histórico ficam na mesma transação | CT-54 (duas threads), CT-42 e CT-43 |
| 5 | Pagamento em parcelas era impossível | O banco tinha `UNIQUE` em `pagamentos.matricula_id` | Migração em `Database`: o nome da restrição (gerado pelo H2) é lido do catálogo e removido, sem apagar pagamentos; o saldo devedor passa a ser `valor − soma das parcelas` | CT-79 a CT-83, CT-102 a CT-104 |
| 6 | Bancos já usados não tinham as colunas novas | Quem abrisse uma versão nova com um banco antigo teria erro | `schema.sql` usa `CREATE TABLE IF NOT EXISTS` e `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`; o `saldo_apos` antigo é reconstruído pela soma acumulada | CT-102 a CT-104, conferido numa cópia do banco real |
| 7 | Vencimento sem rotina agendada | Não há servidor para "rodar à meia-noite" e marcar contratos vencidos | A situação VENCIDA é **calculada na consulta** (`ATIVA` com `fim < hoje`); o registro não muda e nada precisa ser agendado | CT-74 a CT-78 |
| 8 | Excluir um cadastro quebraria o histórico | Apagar um aluno com matrícula violaria as chaves estrangeiras e perderia o histórico financeiro | **Remover** decide: sem vínculos exclui; com histórico inativa (`ativo = FALSE`); com situação aberta bloqueia. Matrículas, pagamentos e movimentos nunca são apagados | CT-108 a CT-130 |
| 9 | CPFs fictícios eram recusados | A validação dos dígitos verificadores impedia dados de teste como 111.111.111-11 | A regra passou a exigir apenas 11 números e unicidade; telefone com 8 números e e-mail com @ | CT-16, CT-105 a CT-107 |
| 10 | Telefone exibido como se fosse CPF | Depois de aceitar qualquer 11 números, um telefone sem máscara apareceria formatado como CPF | A máscara de CPF passou a ser aplicada só na coluna CPF | Inspeção das telas |
| 11 | A janela travava durante consultas | Consultas e gravações rodavam na linha de eventos do Swing | Telas e formulários usam `SwingWorker`; cada pedido de tela recebe um número e respostas atrasadas de telas anteriores são descartadas | Inspeção visual |
| 12 | Listas cresciam sem limite | Todas as linhas eram carregadas na tabela | Listas limitadas a 500 linhas; movimentações paginadas no banco (50 por página) com filtros | CT-97 a CT-101 |
| 13 | Testar o bloqueio de 1 minuto sem esperar 1 minuto | Um teste que dorme 60 s seria lento e frágil | O serviço recebe um `Clock`; o teste usa um relógio adiantado em 61 s | CT-71 a CT-73 |
| 14 | Códigos ordenados como texto (1, 10, 2) | A tabela recebia textos já formatados | O modelo guarda números, valores e datas com seus tipos; a formatação ocorre só ao desenhar a célula | Conferido em 15/10 |
| 15 | Abertura pesada | A primeira versão usava um vídeo AVI decodificado em tempo real | Animação curta em Java2D com a fotografia preparada uma vez em memória | Inspeção visual |
| 16 | Testes deixavam lixo no disco | Cada execução criava uma pasta `persistencia-*` em `.build` | O `SystemTest` apaga o banco temporário no fim | `.build` vazio após os testes |
| 17 | Edição de um código inexistente parecia ter dado certo | O `UPDATE` não alterava nenhuma linha e não avisava | Toda gravação confere que exatamente 1 linha foi alterada; senão informa "Registro não encontrado" | CT-64 a CT-67, CT-130 |
| 18 | Versão web e aplicativo divergindo | A web foi criada para a apresentação e não acompanha todas as regras | A documentação deixa explícito que a web é só demonstração; ela recebeu as mesmas validações e a mesma lógica de remoção, com testes próprios (21) | `docs/versao-web.md` |

## Lições

- Regras importantes ficam **no serviço**, não só na tela: esconder um botão não impede uma chamada indevida.
- O banco é a última linha de defesa: chaves estrangeiras, `UNIQUE` e `CHECK` impedem dados inconsistentes mesmo se o código falhar.
- Uma operação com várias etapas deve ser uma **transação**: tudo ou nada.
- Testes de falha (rollback, concorrência, sessão revogada) valem tanto quanto os testes do caminho feliz.
