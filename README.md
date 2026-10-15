# Pulso · Academia Movimento

Sistema desktop para o cenário do Grupo 05. Cliente fictício: Academia Movimento.

## Abrir no IntelliJ

1. Use **File > Open** e escolha esta pasta `Academia`.
   Se aparecer **Trust and Open Project**, revise a confirmação. Não é necessário adicionar a pasta às exclusões do Microsoft Defender para usar o projeto.
2. O módulo `academia-pulso` inclui a biblioteca H2 em `lib` para execução local. A configuração de execução usa o mesmo nome do módulo criado ao importar o Maven.
3. O JDK 23 instalado nesta máquina já está configurado. Em outra máquina, escolha um JDK 17 ou superior em **File > Project Structure > Project SDK**.
4. Escolha **Pulso - Academia** no seletor de execução e clique em **Run**. Outra opção: abra `Main.java` e execute seu método `main`.
5. Se a IDE perguntar se deseja importar o Maven, a importação é opcional. O módulo local já funciona com o arquivo JAR incluído.

O módulo e o Maven usam a mesma pasta de compilação: `target/classes` para a aplicação e `target/test-classes` para os testes. Se uma versão anterior apresentar `ClassNotFoundException: br.com.pulso.Main`, feche e reabra o projeto para carregar a configuração corrigida e execute **Pulso - Academia** novamente.

Alternativa: dê dois cliques em **Abrir-Academia.cmd**. Requer Java/JDK 17 ou superior disponível no computador.

## Contas de demonstração

| Login | Senha | Perfil |
| --- | --- | --- |
| admin | Admin@123 | Administrador |
| recepcao | Recepcao@123 | Atendente |

Estas são credenciais fictícias para apresentação. As senhas são armazenadas com PBKDF2 e salt. Em uma implantação real, crie contas individuais e altere as senhas de demonstração.

## Funções

- Acesso com conta ativa e perfil de administrador ou atendente.
- Alunos: cadastro, edição, pesquisa, retrato fictício, ficha com contratos, CPF único e validação dos dígitos.
- Modalidades e planos: modalidade, valor total e duração em meses.
- Matrículas: aluno, plano, validade calculada, situação, histórico e pagamento único do valor contratado.
- Produtos: preço, unidade, saldo e estoque mínimo.
- Movimentações: entrada ou saída inteira, motivo, data e responsável. Saída sem saldo é bloqueada.
- Visão geral: alunos ativos, matrículas ativas, pagamentos pendentes e itens para repor.
- Usuários: cadastro, retrato fictício, troca de senha, perfil e ativação, disponíveis ao administrador.

A edição Grafite inclui abertura animada, transições entre telas, ícones vetoriais próprios e confirmação ao salvar. Escolha um retrato nos formulários de alunos e usuários. As fotografias mantêm a proporção e o enquadramento completo. A interface e suas imagens funcionam sem internet.

**Regra adotada:** o pagamento é único por contratação e corresponde ao valor total do plano. O sistema não gera cobranças mensais recorrentes. Trancamento altera a situação, sem estender a validade. O vencimento fica visível, mas não cancela o contrato automaticamente.

## Banco de dados

H2 2.3.232, relacional e persistente. O aplicativo cria o banco na primeira execução em `data/pulso.mv.db`. O arquivo `database/schema.sql` contém tabelas, chaves, restrições e índices. `Database.seed()` insere registros fictícios apenas quando não há usuários.

`database/banco-demonstracao.sql` inclui a estrutura e os dados iniciais, exportados do H2. Para restaurar esse script, use um banco novo e vazio: `java -cp lib/h2-2.3.232.jar org.h2.tools.RunScript -url jdbc:h2:file:./data/pulso-restaurado -user sa -script database/banco-demonstracao.sql`. O aplicativo continua usando `data/pulso.mv.db`; esta restauração é uma cópia para consulta e teste. Para exportar o estado atual, feche a aplicação e execute `scripts/exportar-banco.ps1`.

Para examinar o banco, feche o aplicativo e execute a ferramenta H2: `java -cp lib/h2-2.3.232.jar org.h2.tools.Shell -url jdbc:h2:file:./data/pulso -user sa`. A senha do banco local está vazia. Use apenas nesta demonstração local. O login da aplicação é independente da conta do banco.

Não remova o banco para tentar resolver problemas de acesso. Para uma cópia de segurança, feche o sistema e copie `data/pulso.mv.db`. Não execute duas instâncias do aplicativo sobre o mesmo arquivo.

## Testes e apresentação

Execute `scripts/testar.ps1` no PowerShell. Os testes usam bancos separados e não alteram os dados do aplicativo. O resultado fica em `docs/resultados-testes.md`.

- `apresentacao/Pulso-Academia-Grafite.pptx`: 18 slides editáveis com a interface escura e notas de apresentação.
- `docs/roteiro-apresentacao.md`: sequência para demonstrar a ferramenta e estudar seu funcionamento.
- `docs/requisitos-e-testes.md`: requisitos, permissões, regras e roteiro de testes manuais.
- `docs/modelo-banco.md`: entidades e relacionamentos.
- `assets/telas`: imagens da interface com dados fictícios.
- `assets/imagens`: fotografias fictícias de musculação, funcional e pilates, incluídas no aplicativo.
- `docs/imagens-ficticias.md`: origem das fotografias e prompts utilizados.
- `assets/retratos`: quatro retratos de pessoas fictícias disponíveis na galeria.
- `docs/novas-imagens.md`: prompts da abertura e dos novos retratos.

Para reduzir movimento, adicione `-Dpulso.reduceMotion=true` às opções da JVM na configuração de execução. Para abrir direto no login, use `-Dpulso.skipIntro=true`.

A pasta `.build` contém arquivos de preparação e não faz parte da entrega. O arquivo `pom.xml` permite usar Maven se esse for o fluxo escolhido em aula. A execução local acima usa a biblioteca já incluída.

## Organização do código

| Arquivo | Responsabilidade |
| --- | --- |
| Main.java | Inicialização e abertura da janela |
| AcademiaFrame.java | Telas e formulários Swing |
| Visual.java | Componentes visuais, fotografias, ícones e estilos |
| AcademiaService.java | Validações, permissões e transações |
| Database.java | JDBC, criação das tabelas e dados de demonstração |
| Security.java | Hash e conferência de senhas |
| SystemTest.java | Testes automáticos do serviço e integração com banco |

Limites da versão: execução local, sem envio de e-mails, catraca ou emissão fiscal. Os registros financeiros são controles internos da demonstração. A aplicação não possui auditoria de edição de cadastros nem rotina automática de backup.
