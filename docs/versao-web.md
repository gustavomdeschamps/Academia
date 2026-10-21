# Pulso na Vercel

> **Apenas demonstração.** O sistema oficial desta entrega é o aplicativo Java com banco relacional H2 (`src/main/java`, `database/schema.sql`). Esta versão web serve somente para mostrar a interface na apresentação: guarda dados fictícios no navegador, não lê nem grava o banco H2 e não sincroniza com o aplicativo. Em 21/10/2026 a web recebeu as mesmas validações de cadastro (CPF com 11 números, telefone com 8 números, e-mail com @) e a mesma lógica de remoção (excluir sem vínculos, inativar com histórico, bloqueios, Mostrar inativos e Reativar, com os mesmos textos). Pagamento em parcelas, matrícula vencida e renovação, auditoria, troca da própria senha, bloqueio após tentativas e filtros de movimentação **não** existem nesta versão. Para avaliar o sistema, use o aplicativo Java.

Publicado em 16/10/2026 e **atualizado em 21/10/2026**: **https://pulso-academia-movimento.vercel.app**

O endereço foi aberto sem autenticação da Vercel e conferido com navegador isolado. O aplicativo mantém sua própria tela de acesso de demonstração.

## Finalidade e dados

Esta versão atende à escolha de uma **demonstração pública com dados fictícios**. Cada navegador tem seus próprios exemplos. Não é uma implantação para operar uma academia com dados pessoais reais.

| Versão | Interface | Armazenamento | Uso |
| --- | --- | --- | --- |
| IntelliJ | Java, Swing e Java2D | Banco relacional H2 local | **Sistema oficial** do trabalho |
| Vercel | HTML, CSS e JavaScript | IndexedDB neste navegador | Somente demonstração pública responsiva |

O arquivo `data/pulso.mv.db`, scripts SQL, documentos, slides, capturas e arquivos de desenvolvimento não foram enviados ao site. A publicação contém a página, os três arquivos de interface/serviço, o ícone e nove imagens fictícias. Os cadastros do computador não são sincronizados com a web.

O armazenamento do navegador persiste após atualizar a página e fechar uma aba. Apagar os dados do site ou utilizar outro navegador cria outra demonstração. Não existe um banco compartilhado entre visitantes. A administração pode restaurar os exemplos em **Sobre a demonstração > Restaurar demonstração**.

## Contas e telas

| Login | Senha fictícia | Perfil |
| --- | --- | --- |
| admin | Admin@123 | Administração |
| recepcao | Recepcao@123 | Recepção |

O usuário aceita maiúsculas e espaços externos. A senha mantém distinção entre maiúsculas e minúsculas. O botão de contas preenche o formulário; a entrada continua conferindo a senha.

- **Administração:** painel cobre, alunos e contratos ativos, recebimentos, reposição, situações dos contratos e últimos movimentos. Configura modalidades, planos, produtos e equipe.
- **Recepção:** painel verde, busca de aluno, fichas, cadastro e matrícula. Consulta configurações e registra pagamentos e movimentos; não gerencia usuários nem altera planos ou produtos.
- **Alunos:** cadastro, edição, pesquisa por nome/CPF/contato, CPF com 11 números (fictício aceito) e único, telefone com pelo menos 8 números, e-mail com @, situação e galeria fictícia. A ficha reúne contratos e permite ampliar o retrato.
- **Modalidades e planos:** imagens inteiras, descrição, duração e valor total. A edição de um plano não modifica o valor de uma contratação anterior.
- **Matrículas:** aluno ativo, plano, data, validade calculada, situação, histórico e pagamento único (somente na web; o aplicativo Java aceita parcelas). Cancelamento definitivo; trancamento sem prorrogação automática.
- **Produtos e movimentos:** preço, unidade, mínimo e saldo. Entradas e saídas inteiras, motivo, data e responsável. Saída sem saldo é bloqueada.
- **Equipe:** conta, senha de teste, perfil, situação e retrato. O administrador mantém sua própria conta ativa com perfil de administração.

As operações salvam o estado e o histórico juntos em uma transação do IndexedDB. Revisões impedem sobrescritas silenciosas entre abas; navegadores com Web Locks também coordenam a gravação. As permissões são conferidas novamente nas operações do serviço.

## Abertura e imagens

A sequência web termina em até 2,85 segundos. Usa abertura lateral, traçado vetorial da marca, luz diagonal, ondas e partículas. Não há indicador de progresso, vídeo ou áudio. A abertura aparece uma vez por sessão da aba e é dispensada com movimento reduzido. Animações podem ser reduzidas também na própria interface.

As nove imagens publicadas preservam resolução e enquadramento completos. A conversão WebP usa modo sem perda e foi conferida comparando todos os pixels com os PNG originais. O conjunto caiu de 20.471.302 para 13.757.338 bytes, aproximadamente 33% menor, sem redimensionamento. Os originais têm entre 1254 e 1672 pixels de largura; a fotografia de abertura tem 1536 × 1024. A ampliação mostra a imagem inteira, respeitando o tamanho da tela.

## Remoção de cadastros (21/10/2026)

Alunos, modalidades, planos, produtos e usuários têm a ação **Remover**, que primeiro mostra se o registro será excluído (sem vínculos) ou inativado (com histórico) e pede confirmação. Bloqueios e permissões seguem o aplicativo Java: a recepção remove apenas alunos. As listas mostram somente cadastros ativos; **Mostrar inativos** exibe os demais com **Reativar**. Matrículas, pagamentos, movimentos e históricos nunca são apagados. `node --test web/test/service.test.mjs` executa 21 testes (13 anteriores ajustados às validações novas e 8 de remoção). Esta versão foi publicada em produção em 21/10/2026 (veja a verificação abaixo).

## Verificação em 21/10/2026 (versão atual)

Publicação com `vercel --prod` a partir da raiz do projeto: a Vercel executa `node web/scripts/build.mjs` e publica `.build/site`. A primeira tentativa falhou porque o `.vercelignore` tinha regras sem `/` inicial (`src`, `scripts`) que também excluíam `web/src` e `web/scripts`; as regras foram ancoradas na raiz e a publicação seguinte ficou **Ready**.

| Verificação | Resultado |
| --- | --- |
| Testes de regras (`node --test web/test/service.test.mjs`) | 21 de 21 |
| Fluxos no endereço público com navegador (Chrome headless, perfil temporário) | 17 de 17 |

Os 17 fluxos: página de login, senha errada recusada, login `admin`, menu Usuários visível ao administrador, cadastro com CPF fictício 123.123.123-12, CPF com 10 números recusado com "Informe o CPF com 11 números.", matrícula, pagamento (status Pago), confirmação "excluído definitivamente", aluno sem vínculo excluído, remoção bloqueada para aluno com matrícula ativa, entrada de estoque somando ao saldo, saída acima do saldo recusada, login `recepcao`, recepção sem menu Usuários e sem Remover em planos, e tela de celular (390 × 844). As capturas `assets/telas/13-web-login.png` a `16-web-celular.png` foram geradas nessa execução.

Arquivos que **não** são servidos (resposta 404 conferida): `.env.local`, `README.md`, `docs/`, `src/`, `database/`. O build grava no site uma cópia de `vercel.json` contendo apenas os cabeçalhos de segurança, sem dados sensíveis.

## Verificação em 16/10/2026 (versão anterior)

Foram aprovados **13 testes de regras** (`node --test web/test/service.test.mjs`) e **27 verificações no navegador**, executadas tanto localmente quanto no endereço público. Cobriram login inválido e válido, os dois perfis, sete páginas, cadastro, persistência após atualizar, busca por CPF, matrícula, pagamento, saída sem saldo, conta nova, consulta da recepção, nove imagens, ampliação, menu e tabela no celular, movimento reduzido e ausência de erros JavaScript ou recursos ausentes.

O serviço também foi testado para CPF duplicado, aluno com contrato aberto, datas de fim de mês, valor contratado preservado, pagamento duplicado, histórico, cancelamento definitivo, disputa de saldo, desativação de conta e restauração por outro administrador. Os 14 arquivos servidos publicamente tiveram seus conteúdos comparados por SHA-1 com os arquivos preparados. 

Esses resultados não representam auditoria de segurança ou teste com milhares de usuários. O acesso desta demonstração é implementado no navegador e pode ser inspecionado ou alterado por quem controla esse navegador. Não use dados pessoais, senhas reais ou cobranças reais. Uma operação real requer autenticação e autorização no servidor, banco online e políticas de backup e privacidade.

## Manutenção

O código fica em `web/src`. `data.js` reúne armazenamento, validação e regras; `app.js` contém telas e interações; `styles.css` contém a apresentação e os efeitos. Não há dependência de frameworks em tempo de execução, API externa ou rastreamento.

Para publicar alterações pelo projeto da Vercel, a raiz do repositório é a pasta `Academia`. A configuração executa `node web/scripts/build.mjs` e publica `.build/site`. A instalação opcional do `sharp` habilita WebP sem perda; sua ausência usa os PNG originais. `.vercelignore` exclui os arquivos locais que não pertencem ao site. As imagens têm uma única origem em `assets/imagens` e `assets/retratos`; as cópias de publicação são temporárias.

Em 16/10 a publicação foi feita por envio explícito dos arquivos preparados; em 21/10, pela CLI da Vercel a partir da raiz, com o build executado na plataforma. Não foi criada integração com um repositório remoto nem uma rotina automática de implantação. O banco H2 local continua independente.
