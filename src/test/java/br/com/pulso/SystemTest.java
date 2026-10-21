package br.com.pulso;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Testes de integração: usam os serviços reais e um banco isolado. */
public final class SystemTest {
    @FunctionalInterface interface Action {void run() throws Exception;}
    static final List<String> results=new ArrayList<>();
    static int passed;
    static void check(String name,boolean condition) {
        if(!condition) throw new AssertionError(name);
        passed++;results.add("| CT-"+String.format("%02d",passed)+" | "+name+" | Passou |");System.out.println("OK "+name);
    }
    static void rejects(String name,Action action) throws Exception {
        boolean rejected=false;try{action.run();}catch(IllegalArgumentException|IllegalStateException e){rejected=true;}
        check(name,rejected);
    }
    static boolean rejected(Action action) throws Exception {
        try{action.run();return false;}catch(IllegalArgumentException|IllegalStateException e){return true;}
    }
    /** Edição de um código que não existe deve avisar, em vez de "salvar" sem alterar nada. */
    static boolean notFound(Action action) throws Exception {
        try{action.run();return false;}catch(IllegalArgumentException e){return e.getMessage().contains("não encontrado");}
    }
    public static void main(String[] args) throws Exception {
        Database db=new Database("jdbc:h2:mem:tests;DB_CLOSE_DELAY=-1");db.seed();AcademiaService s=new AcademiaService(db);
        rejects("Operação sem autenticação bloqueada",()->s.movimentar(1,"ENTRADA",1,"Teste"));
        rejects("Login com senha incorreta bloqueado",()->s.login("admin","incorreta"));
        rejects("Entrada SQL no login não autentica",()->s.login("admin' OR '1'='1","x"));
        check("Login válido reconhece administrador",s.login("admin","Admin@123").admin());
        check("Login aceita maiúsculas e espaços externos no usuário",s.login("  ADMIN  ","Admin@123").admin());
        check("Recepção autentica com usuário em maiúsculas",!s.login("RECEPCAO","Recepcao@123").admin());
        rejects("Senha mantém diferença entre maiúsculas e minúsculas",()->s.login("admin","admin@123"));
        rejects("Login sem usuário é recusado sem falha interna",()->s.login(null,"Admin@123"));
        s.login("admin","Admin@123");
        rejects("Cadastro não duplica login por diferença de maiúsculas",()->s.saveUsuario(null,"Duplicado","ADMIN","Senha@123","ATENDENTE",true));
        s.saveUsuario(null,"Conta inativa","BLOQUEADO","Senha@123","ATENDENTE",false);
        check("Novos logins são guardados em minúsculas",db.scalar("SELECT COUNT(*) FROM usuarios WHERE login='bloqueado'")==1);
        rejects("Conta inativa não entra mesmo com senha correta",()->s.login("BLOQUEADO","Senha@123"));
        s.login("admin","Admin@123");
        String hash=db.query("SELECT senha FROM usuarios WHERE login='admin'").get(0).get("senha").toString();
        check("Senha armazenada como hash com salt",!hash.contains("Admin@123") && Security.verify("Admin@123",hash));
        check("Senhas iguais recebem salts diferentes",!Security.hash("mesmaSenha").equals(Security.hash("mesmaSenha")));
        long aluno=s.saveAluno(null,"Carla Mendes","123.456.789-09","(47) 99999-1234","carla@example.com",true);
        check("Cadastro grava e permite consultar aluno",db.scalar("SELECT COUNT(*) FROM alunos WHERE id=? AND cpf=?",aluno,"12345678909")==1);
        rejects("CPF duplicado bloqueado",()->s.saveAluno(null,"Outra Carla","12345678909","(47) 99999-1234","c@example.com",true));
        check("CPF com menos ou mais de 11 números é bloqueado e CPF fictício de 11 números é aceito",rejected(()->s.saveAluno(null,"Curto","1234567890","(47) 99999-1234","c@example.com",true)) && rejected(()->s.saveAluno(null,"Longo","123456789012","(47) 99999-1234","c@example.com",true)) && rejected(()->s.saveAluno(null,"Letras","1234567890a","(47) 99999-1234","c@example.com",true)) && s.saveAluno(null,"CPF fictício","111.111.111-11","(47) 99999-1234","ficticio@example.com",true)>0 && db.scalar("SELECT COUNT(*) FROM alunos WHERE cpf='11111111111'")==1);
        rejects("Nome vazio bloqueado",()->s.saveAluno(null," ","39053344705","(47) 99999-1234","c@example.com",true));
        rejects("E-mail inválido bloqueado",()->s.saveAluno(null,"Teste","39053344705","(47) 99999-1234","semarroba",true));
        s.saveAluno(aluno,"Carla Mendes Silva","12345678909","(47) 99999-1234","carla@example.com",true);
        check("Edição atualiza o aluno",db.scalar("SELECT COUNT(*) FROM alunos WHERE nome='Carla Mendes Silva'")==1);
        s.saveAluno(aluno,"Carla Mendes Silva","12345678909","(47) 99999-1234","carla@example.com",true,"retratos/ana");
        check("Retrato escolhido é salvo com o aluno",db.scalar("SELECT COUNT(*) FROM alunos WHERE id=? AND foto='retratos/ana'",aluno)==1);
        s.saveAluno(aluno,"Carla Mendes Silva","12345678909","(47) 99999-1234","carla@example.com",true);
        check("Edição de dados preserva o retrato já escolhido",db.scalar("SELECT COUNT(*) FROM alunos WHERE id=? AND foto='retratos/ana'",aluno)==1);
        rejects("Caminho de imagem fora da galeria é bloqueado",()->s.saveAluno(aluno,"Carla","12345678909","(47) 99999-1234","carla@example.com",true,"../arquivo"));
        check("Retrato inválido não altera o cadastro",db.scalar("SELECT COUNT(*) FROM alunos WHERE id=? AND nome='Carla Mendes Silva' AND foto='retratos/ana'",aluno)==1);
        long modalidade=s.saveModalidade(null,"Teste funcional","Aula para testes");
        long plano=s.savePlano(null,"Plano teste",modalidade,new BigDecimal("120.00"),3);
        check("Plano relaciona modalidade por chave estrangeira",db.scalar("SELECT COUNT(*) FROM planos p JOIN modalidades m ON p.modalidade_id=m.id WHERE p.id=?",plano)==1);
        rejects("Plano com preço negativo bloqueado",()->s.savePlano(null,"Negativo",modalidade,new BigDecimal("-1.00"),1));
        rejects("Plano sem duração válida bloqueado",()->s.savePlano(null,"Duração inválida",modalidade,new BigDecimal("100"),0));
        LocalDate inicio=LocalDate.of(2026,10,9);long mat=s.matricular(aluno,plano,inicio);
        check("Matrícula grava validade conforme duração",db.scalar("SELECT COUNT(*) FROM matriculas WHERE id=? AND fim=?",mat,inicio.plusMonths(3))==1);
        check("Matrícula registra histórico inicial",db.scalar("SELECT COUNT(*) FROM historico_matriculas WHERE matricula_id=?",mat)==1);
        rejects("Matrícula aberta duplicada bloqueada",()->s.matricular(aluno,plano,inicio));
        s.savePlano(plano,"Plano teste",modalidade,new BigDecimal("150.00"),3);
        check("Alteração do plano preserva valor contratado",db.query("SELECT valor FROM matriculas WHERE id=?",mat).get(0).get("valor").equals(new BigDecimal("120.00")));
        rejects("Aluno com matrícula aberta não pode ser inativado",()->s.saveAluno(aluno,"Carla","12345678909","(47) 99999-1234","c@example.com",false));
        s.situacao(mat,"TRANCADA","Viagem do aluno");
        check("Trancamento registra situação e histórico",db.scalar("SELECT COUNT(*) FROM historico_matriculas WHERE matricula_id=? AND situacao='TRANCADA'",mat)==1);
        rejects("Situação sem motivo bloqueada",()->s.situacao(mat,"ATIVA"," "));
        s.situacao(mat,"ATIVA","Retorno do aluno");s.pagar(mat,"PIX");
        check("Pagamento registra valor contratado e responsável",db.scalar("SELECT COUNT(*) FROM pagamentos WHERE matricula_id=? AND valor=120 AND usuario_id=1",mat)==1);
        rejects("Pagamento duplicado bloqueado",()->s.pagar(mat,"PIX"));
        s.situacao(mat,"CANCELADA","Encerramento solicitado");
        rejects("Matrícula cancelada não pode ser reativada",()->s.situacao(mat,"ATIVA","Retorno"));
        check("Nova matrícula permitida após cancelamento",s.matricular(aluno,plano,inicio)>mat);
        long inactive=s.saveAluno(null,"Aluno inativo","39053344705","(47) 99999-1234","inativo@example.com",false);
        rejects("Matrícula para aluno inativo bloqueada",()->s.matricular(inactive,plano,inicio));
        long produto=s.saveProduto(null,"Produto teste","unidade",new BigDecimal("15.00"),2);
        check("Produto inicia sem saldo",db.scalar("SELECT saldo FROM produtos WHERE id=?",produto)==0);
        s.movimentar(produto,"ENTRADA",5,"Reposição");s.movimentar(produto,"SAIDA",3,"Consumo");
        check("Entrada e saída atualizam saldo",db.scalar("SELECT saldo FROM produtos WHERE id=?",produto)==2);
        check("Saldo no mínimo aparece no alerta",db.scalar("SELECT COUNT(*) FROM produtos WHERE id=? AND saldo<=minimo",produto)==1);
        long history=db.scalar("SELECT COUNT(*) FROM movimentacoes WHERE produto_id=?",produto);
        rejects("Saída maior que saldo bloqueada",()->s.movimentar(produto,"SAIDA",3,"Saldo insuficiente"));
        check("Saída rejeitada preserva saldo e histórico",db.scalar("SELECT saldo FROM produtos WHERE id=?",produto)==2 && db.scalar("SELECT COUNT(*) FROM movimentacoes WHERE produto_id=?",produto)==history);
        rejects("Quantidade zero bloqueada",()->s.movimentar(produto,"ENTRADA",0,"Teste"));
        rejects("Movimento sem motivo bloqueado",()->s.movimentar(produto,"ENTRADA",1,""));
        s.login("recepcao","Recepcao@123");
        rejects("Atendente não cadastra usuários",()->s.saveUsuario(null,"Teste","teste","Senha@123","ADMIN",true));
        rejects("Atendente não altera retrato de conta da equipe",()->s.saveUsuario(1L,"Administrador","admin","","ADMIN",true,"retratos/ana"));
        rejects("Atendente não altera planos",()->s.savePlano(null,"Proibido",modalidade,new BigDecimal("100"),1));
        s.movimentar(produto,"ENTRADA",1,"Recebimento pela recepção");
        check("Atendente registra movimento com responsável",db.scalar("SELECT COUNT(*) FROM movimentacoes WHERE produto_id=? AND usuario_id=2",produto)==1);
        s.login("admin","Admin@123");
        s.saveUsuario(1L,"Administrador","admin","","ADMIN",true,"retratos/equipe");
        check("Retrato de usuário mantém autenticação e perfil",db.scalar("SELECT COUNT(*) FROM usuarios WHERE id=1 AND foto='retratos/equipe' AND perfil='ADMIN'")==1 && s.login("admin","Admin@123").admin());
        rejects("Administrador não pode desativar a própria conta",()->s.saveUsuario(1L,"Admin","admin","","ADMIN",false));
        s.saveUsuario(null,"Funcionário teste","funcionario","Senha@123","ATENDENTE",true);s.login("funcionario","Senha@123");
        check("Usuário cadastrado consegue autenticar",s.user().perfil().equals("ATENDENTE"));
        s.logout();rejects("Logout encerra a sessão",()->s.movimentar(produto,"ENTRADA",1,"Teste"));
        // Duas saídas simultâneas para o mesmo saldo: apenas uma deve concluir.
        s.login("admin","Admin@123");long concurrencyProduct=s.saveProduto(null,"Concorrência","unidade",new BigDecimal("1.00"),0);s.movimentar(concurrencyProduct,"ENTRADA",1,"Saldo para concorrência");
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);List<Future<Boolean>> futures=new ArrayList<>();
        for(int i=0;i<2;i++) futures.add(pool.submit(()->{AcademiaService worker=new AcademiaService(db);worker.login("admin","Admin@123");start.await();try{worker.movimentar(concurrencyProduct,"SAIDA",1,"Saída concorrente");return true;}catch(IllegalArgumentException e){return false;}}));
        start.countDown();int completed=0;for(var f:futures)if(f.get())completed++;pool.shutdown();
        check("Saídas simultâneas não deixam saldo negativo",completed==1 && db.scalar("SELECT saldo FROM produtos WHERE id=?",concurrencyProduct)==0);
        // Uma falha na gravação do histórico precisa desfazer a mudança de saldo.
        try(Connection c=db.connect()){Database.update(c,"DELETE FROM usuarios WHERE login='funcionario'");}
        AcademiaService ghost=new AcademiaService(db);ghost.login("recepcao","Recepcao@123");
        long rollbackProduct=s.saveProduto(null,"Rollback","unidade",new BigDecimal("1.00"),0);
        // Simula falha na segunda instrução usando um CHECK temporário no banco de teste.
        try(Connection c=db.connect();Statement st=c.createStatement()){st.execute("ALTER TABLE movimentacoes ADD CONSTRAINT teste_falha CHECK(motivo <> 'FORCAR FALHA')");}
        boolean failed=false;try{s.movimentar(rollbackProduct,"ENTRADA",4,"FORCAR FALHA");}catch(SQLException e){failed=true;}
        check("Falha ao gravar histórico desfaz alteração de saldo",failed && db.scalar("SELECT saldo FROM produtos WHERE id=?",rollbackProduct)==0);
        Path testFolder=Files.createTempDirectory(Path.of(".build"),"persistencia-");String persistentUrl="jdbc:h2:file:"+testFolder.resolve("banco").toAbsolutePath().toString().replace('\\','/');
        Database first=new Database(persistentUrl);first.seed();new AcademiaService(first).login("admin","Admin@123");long seededStudents=first.scalar("SELECT COUNT(*) FROM alunos");
        Database second=new Database(persistentUrl);check("Dados persistem após nova abertura do banco",seededStudents>=2 && second.scalar("SELECT COUNT(*) FROM alunos")==seededStudents);
        AcademiaService photoService=new AcademiaService(second);photoService.login("admin","Admin@123");
        photoService.saveAluno(2L,"Bruno Oliveira","11144477735","(47) 99902-3040","bruno@example.com",true,null);
        Database third=new Database(persistentUrl);
        check("Remoção de retrato persiste após reabrir o banco",third.query("SELECT foto FROM alunos WHERE id=2").get(0).get("foto")==null);
        try(Connection c=third.connect();Statement st=c.createStatement()){st.execute("ALTER TABLE alunos DROP COLUMN foto");st.execute("ALTER TABLE usuarios DROP COLUMN foto");}
        Database migrated=new Database(persistentUrl);
        check("Banco anterior recebe retratos sem perder cadastros",migrated.scalar("SELECT COUNT(*) FROM alunos")==seededStudents &&migrated.scalar("SELECT COUNT(*) FROM alunos WHERE foto IS NOT NULL")==2 && migrated.scalar("SELECT COUNT(*) FROM usuarios")==2);
        // A partir do CT-59: sessão revalidada, transações, inativação, vencimento, parcelas, auditoria e filtros.
        s.login("admin","Admin@123");
        s.saveUsuario(null,"Sessão teste","sessao.teste","Senha@123","ATENDENTE",true);long sessaoId=db.scalar("SELECT id FROM usuarios WHERE login='sessao.teste'");
        AcademiaService aberta=new AcademiaService(db);aberta.login("sessao.teste","Senha@123");
        s.saveUsuario(sessaoId,"Sessão teste","sessao.teste","","ATENDENTE",false);
        rejects("Conta desativada tem a sessão aberta encerrada",()->aberta.movimentar(produto,"ENTRADA",1,"Depois de desativada"));
        check("Sessão revogada exige novo login",aberta.user()==null);
        s.saveUsuario(null,"Perfil teste","perfil.teste","Senha@123","ADMIN",true);long perfilId=db.scalar("SELECT id FROM usuarios WHERE login='perfil.teste'");
        AcademiaService gestor=new AcademiaService(db);gestor.login("perfil.teste","Senha@123");
        s.saveUsuario(perfilId,"Perfil teste","perfil.teste","","ATENDENTE",true);
        rejects("Mudança de perfil encerra a sessão administrativa",()->gestor.saveModalidade(null,"Proibida","Sem acesso"));
        check("Perfil rebaixado não grava cadastro administrativo",gestor.user()==null && db.scalar("SELECT COUNT(*) FROM modalidades WHERE nome='Proibida'")==0);
        s.saveUsuario(null,"Transação teste","transacao","Senha@123","ATENDENTE",true);long transacaoId=db.scalar("SELECT id FROM usuarios WHERE login='transacao'");
        // A auditoria é a última instrução da edição; um CHECK temporário a faz falhar depois que dados e senha já foram atualizados.
        try(Connection c=db.connect();Statement st=c.createStatement()){st.execute("ALTER TABLE auditoria ADD CONSTRAINT teste_auditoria CHECK(NOT(entidade='USUARIO' AND acao='ALTERACAO')) NOCHECK");}
        boolean userFailed=false;try{s.saveUsuario(transacaoId,"Nome que não deve ficar","transacao","OutraSenha@1","ATENDENTE",true);}catch(SQLException e){userFailed=true;}
        try(Connection c=db.connect();Statement st=c.createStatement()){st.execute("ALTER TABLE auditoria DROP CONSTRAINT teste_auditoria");}
        check("Falha na edição de usuário desfaz dados e senha juntos",userFailed && db.scalar("SELECT COUNT(*) FROM usuarios WHERE id=? AND nome='Transação teste'",transacaoId)==1 && new AcademiaService(db).login("transacao","Senha@123")!=null);
        check("Edição de modalidade inexistente informa registro não encontrado",notFound(()->s.saveModalidade(999999L,"Fantasma","Sem registro")));
        check("Edição de plano inexistente informa registro não encontrado",notFound(()->s.savePlano(999999L,"Plano fantasma",modalidade,new BigDecimal("10.00"),1)));
        check("Edição de produto inexistente informa registro não encontrado",notFound(()->s.saveProduto(999999L,"Produto fantasma","unidade",new BigDecimal("1.00"),0)));
        check("Edição de usuário inexistente informa registro não encontrado",notFound(()->s.saveUsuario(999999L,"Fantasma","fantasma","","ATENDENTE",true)));
        AcademiaService propria=new AcademiaService(db);propria.login("transacao","Senha@123");
        rejects("Troca de senha exige a senha atual correta",()->propria.alterarSenha("errada123","NovaSenha@9"));
        propria.alterarSenha("Senha@123","NovaSenha@9");
        check("Usuário troca a própria senha e entra com a nova",new AcademiaService(db).login("transacao","NovaSenha@9")!=null);
        rejects("Senha antiga deixa de funcionar após a troca",()->new AcademiaService(db).login("transacao","Senha@123"));
        s.saveUsuario(null,"Bloqueio teste","bloqueio","Senha@123","ATENDENTE",true);
        AcademiaService tentativa=new AcademiaService(db);String blockMessage=null;
        for(int i=0;i<5;i++){try{tentativa.login("bloqueio","incorreta");}catch(IllegalArgumentException e){blockMessage=e.getMessage();}}
        check("Quinta senha incorreta bloqueia o acesso por 1 minuto",blockMessage!=null && blockMessage.contains("bloqueado") && db.scalar("SELECT COUNT(*) FROM usuarios WHERE login='bloqueio' AND bloqueado_ate IS NOT NULL")==1);
        rejects("Senha correta é recusada durante o bloqueio",()->tentativa.login("bloqueio","Senha@123"));
        AcademiaService minutoDepois=new AcademiaService(db,Clock.offset(Clock.systemDefaultZone(),Duration.ofSeconds(61)));
        check("Acesso volta após 1 minuto e zera as tentativas",minutoDepois.login("bloqueio","Senha@123")!=null && db.scalar("SELECT tentativas FROM usuarios WHERE login='bloqueio'")==0);
        long alunoVencido=s.saveAluno(null,"Aluno vencido","11122233396","(47) 99911-0001","vencido@example.com",true);
        long planoMensal=s.savePlano(null,"Mensal vencimento",modalidade,new BigDecimal("100.00"),1);
        long vencida=s.matricular(alunoVencido,planoMensal,LocalDate.now().minusMonths(2));
        check("Matrícula ativa com fim passado aparece como vencida",s.situacaoAtual(vencida).equals("VENCIDA") && db.scalar("SELECT COUNT(*) FROM matriculas WHERE id=? AND situacao='ATIVA'",vencida)==1);
        check("Consulta das telas classifica a matrícula como VENCIDA",db.scalar("SELECT COUNT(*) FROM matriculas m WHERE m.id=? AND "+AcademiaService.SITUACAO_ATUAL+"='VENCIDA'",vencida)==1);
        rejects("Renovação exige quitar o saldo da matrícula vencida",()->s.renovar(vencida));
        s.pagar(vencida,"PIX");long renovada=s.renovar(vencida);
        check("Renovação cria nova matrícula ativa no mesmo plano a partir de hoje",db.scalar("SELECT COUNT(*) FROM matriculas WHERE id=? AND aluno_id=? AND plano_id=? AND inicio=? AND situacao='ATIVA'",renovada,alunoVencido,planoMensal,LocalDate.now())==1 && s.situacaoAtual(vencida).equals("CANCELADA"));
        rejects("Matrícula dentro da validade não pode ser renovada",()->s.renovar(renovada));
        long alunoParcial=s.saveAluno(null,"Aluno parcial","44455566619","(47) 99911-0002","parcial@example.com",true);
        long planoParcial=s.savePlano(null,"Plano parcelado",modalidade,new BigDecimal("300.00"),3);
        long parcial=s.matricular(alunoParcial,planoParcial,LocalDate.now());
        check("Matrícula sem pagamento fica PENDENTE",s.financeiro(parcial).status().equals("PENDENTE"));
        s.pagar(parcial,new BigDecimal("100.00"),"DINHEIRO");var financeiro=s.financeiro(parcial);
        check("Pagamento parcial mostra total pago, saldo e status PARCIAL",financeiro.status().equals("PARCIAL") && financeiro.pago().compareTo(new BigDecimal("100.00"))==0 && financeiro.saldo().compareTo(new BigDecimal("200.00"))==0);
        rejects("Parcela maior que o saldo devedor é recusada",()->s.pagar(parcial,new BigDecimal("250.00"),"PIX"));
        s.pagar(parcial,new BigDecimal("150.00"),"CARTAO");s.pagar(parcial,"PIX");
        check("Parcelas somadas quitam a matrícula como PAGO",s.financeiro(parcial).status().equals("PAGO") && db.scalar("SELECT COUNT(*) FROM pagamentos WHERE matricula_id=?",parcial)==3 && db.scalar("SELECT SUM(valor) FROM pagamentos WHERE matricula_id=?",parcial)==300);
        rejects("Pagamento após a quitação é recusado",()->s.pagar(parcial,new BigDecimal("1.00"),"PIX"));
        long modalidadeInativa=s.saveModalidade(null,"Modalidade inativação","Teste de inativação");
        long planoInativo=s.savePlano(null,"Plano inativação",modalidadeInativa,new BigDecimal("80.00"),1);
        long alunoInativacao=s.saveAluno(null,"Aluno inativação","77788899941","(47) 99911-0003","inativacao@example.com",true);
        long matInativacao=s.matricular(alunoInativacao,planoInativo,LocalDate.now());
        rejects("Plano com matrícula ativa não pode ser inativado",()->s.planoAtivo(planoInativo,false));
        rejects("Modalidade com plano ativo não pode ser inativada",()->s.modalidadeAtiva(modalidadeInativa,false));
        s.situacao(matInativacao,"CANCELADA","Encerramento para teste de inativação");s.planoAtivo(planoInativo,false);s.modalidadeAtiva(modalidadeInativa,false);
        check("Inativação preserva plano, modalidade e matrículas no banco",db.scalar("SELECT COUNT(*) FROM planos WHERE id=? AND ativo=FALSE",planoInativo)==1 && db.scalar("SELECT COUNT(*) FROM modalidades WHERE id=? AND ativo=FALSE",modalidadeInativa)==1 && db.scalar("SELECT COUNT(*) FROM matriculas WHERE plano_id=?",planoInativo)==1);
        rejects("Plano inativo não aceita nova matrícula",()->s.matricular(alunoInativacao,planoInativo,LocalDate.now()));
        rejects("Plano não é reativado com a modalidade inativa",()->s.planoAtivo(planoInativo,true));
        long produtoInativo=s.saveProduto(null,"Produto inativação","unidade",new BigDecimal("5.00"),1);s.movimentar(produtoInativo,"ENTRADA",2,"Saldo para inativação");
        rejects("Produto com saldo não pode ser inativado",()->s.produtoAtivo(produtoInativo,false));
        s.movimentar(produtoInativo,"SAIDA",2,"Baixa antes de inativar");s.produtoAtivo(produtoInativo,false);
        rejects("Produto inativo não recebe movimentação",()->s.movimentar(produtoInativo,"ENTRADA",1,"Depois de inativo"));
        s.produtoAtivo(produtoInativo,true);s.movimentar(produtoInativo,"ENTRADA",1,"Depois de reativado");
        check("Produto reativado volta a movimentar",db.scalar("SELECT saldo FROM produtos WHERE id=? AND ativo=TRUE",produtoInativo)==1);
        long auditado=s.saveAluno(null,"Aluno auditado","32165498791","(47) 99911-0004","auditado@example.com",true);
        s.saveAluno(auditado,"Aluno auditado editado","32165498791","(47) 99911-0004","auditado@example.com",true);
        var trilha=s.auditoria("ALUNO",auditado,10);
        check("Auditoria registra quem, quando e o valor anterior e o novo",trilha.size()==2 && trilha.get(0).get("acao").equals("ALTERACAO") && trilha.get(0).get("detalhes").toString().contains("nome: Aluno auditado -> Aluno auditado editado") && AcademiaService.number(trilha.get(0),"usuario_id")==1 && trilha.get(0).get("data")!=null);
        s.saveAluno(auditado,"Aluno auditado editado","32165498791","(47) 99911-0004","auditado@example.com",true);
        check("Gravação sem mudança não gera linha de auditoria",s.auditoria("ALUNO",auditado,10).size()==2);
        check("Auditoria cobre planos, produtos e usuários",s.auditoria("PLANO",planoInativo,10).size()==2 && s.auditoria("PRODUTO",produtoInativo,10).size()==3 && s.auditoria("USUARIO",transacaoId,10).size()==2);
        s.login("recepcao","Recepcao@123");
        rejects("Atendente não consulta a auditoria",()->s.auditoria(null,null,10));
        rejects("Atendente não inativa produtos",()->s.produtoAtivo(produtoInativo,false));
        s.login("admin","Admin@123");
        long produtoFiltro=s.saveProduto(null,"Produto filtro","unidade",new BigDecimal("2.00"),1);
        s.movimentar(produtoFiltro,"ENTRADA",10,"Compra");s.movimentar(produtoFiltro,"SAIDA",3,"Venda");s.movimentar(produtoFiltro,"SAIDA",2,"Venda");
        var saidas=s.movimentacoes(produtoFiltro,"SAIDA",null,null,50,0);
        check("Filtro por produto e tipo retorna apenas as saídas do produto",saidas.total()==2 && saidas.rows().stream().allMatch(r->r.get("tipo").equals("SAIDA") && AcademiaService.number(r,"produto_id")==produtoFiltro));
        check("Saldo após cada movimento acompanha a sequência",s.movimentacoes(produtoFiltro,null,null,null,50,0).rows().stream().map(r->((Number)r.get("saldo_apos")).intValue()).toList().equals(List.of(5,7,10)));
        check("Filtro por período inclui hoje e exclui datas futuras",s.movimentacoes(produtoFiltro,null,LocalDate.now(),LocalDate.now(),50,0).total()==3 && s.movimentacoes(produtoFiltro,null,LocalDate.now().plusDays(1),null,50,0).total()==0);
        var pagina=s.movimentacoes(produtoFiltro,null,null,null,1,1);
        check("Paginação traz uma página por vez com o total geral",pagina.rows().size()==1 && pagina.total()==3 && ((Number)pagina.rows().get(0).get("saldo_apos")).intValue()==7);
        rejects("Período com data final anterior à inicial é recusado",()->s.movimentacoes(null,null,LocalDate.now(),LocalDate.now().minusDays(1),50,0));
        // Reconstrói o esquema da versão anterior (sem as colunas novas e com pagamento único) e reabre o mesmo arquivo.
        long[] before={migrated.scalar("SELECT COUNT(*) FROM pagamentos"),migrated.scalar("SELECT COUNT(*) FROM movimentacoes"),migrated.scalar("SELECT COUNT(*) FROM matriculas")};
        try(Connection c=migrated.connect();Statement st=c.createStatement()){
            for(String sql:new String[]{"DROP TABLE auditoria","ALTER TABLE modalidades DROP COLUMN ativo","ALTER TABLE planos DROP COLUMN ativo","ALTER TABLE produtos DROP COLUMN ativo","ALTER TABLE usuarios DROP COLUMN tentativas","ALTER TABLE usuarios DROP COLUMN bloqueado_ate","ALTER TABLE movimentacoes DROP COLUMN saldo_apos","ALTER TABLE pagamentos ADD UNIQUE(matricula_id)"})st.execute(sql);
        }
        Database upgraded=new Database(persistentUrl);
        check("Banco da versão anterior migra sem perder pagamentos, movimentos e matrículas",upgraded.scalar("SELECT COUNT(*) FROM pagamentos")==before[0] && upgraded.scalar("SELECT COUNT(*) FROM movimentacoes")==before[1] && upgraded.scalar("SELECT COUNT(*) FROM matriculas")==before[2] && upgraded.scalar("SELECT COUNT(*) FROM produtos WHERE ativo=TRUE")==upgraded.scalar("SELECT COUNT(*) FROM produtos"));
        check("Migração remove o UNIQUE de pagamentos e reconstrói o saldo após",upgraded.scalar("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME='PAGAMENTOS' AND CONSTRAINT_TYPE='UNIQUE'")==0 && upgraded.scalar("SELECT COUNT(*) FROM movimentacoes WHERE saldo_apos IS NULL")==0 && upgraded.scalar("SELECT COUNT(*) FROM produtos p WHERE p.saldo<>(SELECT m.saldo_apos FROM movimentacoes m WHERE m.produto_id=p.id ORDER BY m.id DESC LIMIT 1)")==0);
        AcademiaService upgradedService=new AcademiaService(upgraded);upgradedService.login("admin","Admin@123");
        long parcialAntiga=upgraded.scalar("SELECT m.id FROM matriculas m JOIN pagamentos p ON p.matricula_id=m.id WHERE p.valor<m.valor ORDER BY m.id LIMIT 1");
        upgradedService.pagar(parcialAntiga,new BigDecimal("10.00"),"PIX");
        check("Após a migração a matrícula aceita uma nova parcela",upgraded.scalar("SELECT COUNT(*) FROM pagamentos WHERE matricula_id=?",parcialAntiga)==2);
        // Validações menos rígidas para dados fictícios (CT-105 em diante).
        long semDdd=s.saveAluno(null,"Telefone curto","55555555555","3333-4444","curto@example.com",true);
        check("Telefone com 8 números é aceito sem DDD",db.scalar("SELECT COUNT(*) FROM alunos WHERE id=? AND telefone='3333-4444'",semDdd)==1);
        rejects("Telefone com menos de 8 números é bloqueado",()->s.saveAluno(null,"Telefone inválido","66666666666","333-4444","t@example.com",true));
        check("E-mail exige apenas @",rejected(()->s.saveAluno(null,"Sem arroba","77777777777","3333-4444","semarroba.com",true)) && s.saveAluno(null,"Com arroba","77777777777","3333-4444","a@b",true)>0);
        // Remoção de cadastros (CT-108 em diante): excluir sem vínculos, inativar com histórico ou bloquear.
        long semVinculo=s.saveAluno(null,"Aluno sem vínculo","88888888888","3333-0001","sem@vinculo",true);
        check("Prévia indica exclusão para aluno sem matrículas",s.previaRemocao("ALUNO",semVinculo).equals("EXCLUIR"));
        check("Aluno sem vínculos é excluído de verdade",s.removerAluno(semVinculo).excluido() && db.scalar("SELECT COUNT(*) FROM alunos WHERE id=?",semVinculo)==0);
        check("Exclusão fica registrada na auditoria",db.scalar("SELECT COUNT(*) FROM auditoria WHERE entidade='ALUNO' AND registro_id=? AND acao='EXCLUSAO' AND detalhes LIKE '%Aluno sem vínculo%'",semVinculo)==1);
        long modalidadeLivre=s.saveModalidade(null,"Modalidade livre","Sem planos"),planoLivre=s.savePlano(null,"Plano livre",modalidade,new BigDecimal("50.00"),1),produtoLivre=s.saveProduto(null,"Produto livre","unidade",new BigDecimal("1.00"),0);
        s.saveUsuario(null,"Usuário livre","usuario.livre","Senha@123","ATENDENTE",true);long usuarioLivre=db.scalar("SELECT id FROM usuarios WHERE login='usuario.livre'");
        check("Modalidade, plano, produto e usuário sem vínculos são excluídos",s.removerModalidade(modalidadeLivre).excluido() && s.removerPlano(planoLivre).excluido() && s.removerProduto(produtoLivre).excluido() && s.removerUsuario(usuarioLivre).excluido() && db.scalar("SELECT COUNT(*) FROM modalidades WHERE id=?",modalidadeLivre)+db.scalar("SELECT COUNT(*) FROM planos WHERE id=?",planoLivre)+db.scalar("SELECT COUNT(*) FROM produtos WHERE id=?",produtoLivre)+db.scalar("SELECT COUNT(*) FROM usuarios WHERE id=?",usuarioLivre)==0);
        check("Prévia indica inativação para aluno com matrícula encerrada",s.previaRemocao("ALUNO",alunoInativacao).equals("INATIVAR"));
        var inativado=s.removerAluno(alunoInativacao);
        check("Aluno com histórico é inativado em vez de excluído",!inativado.excluido() && inativado.mensagem().equals(AcademiaService.INATIVADO) && db.scalar("SELECT COUNT(*) FROM alunos WHERE id=? AND ativo=FALSE",alunoInativacao)==1 && db.scalar("SELECT COUNT(*) FROM matriculas WHERE aluno_id=?",alunoInativacao)==1);
        long produtoHistorico=s.saveProduto(null,"Produto com histórico","unidade",new BigDecimal("3.00"),0);s.movimentar(produtoHistorico,"ENTRADA",1,"Entrada");s.movimentar(produtoHistorico,"SAIDA",1,"Saída");
        check("Produto com movimentações é inativado e o histórico continua",!s.removerProduto(produtoHistorico).excluido() && db.scalar("SELECT COUNT(*) FROM movimentacoes WHERE produto_id=?",produtoHistorico)==2 && db.scalar("SELECT COUNT(*) FROM produtos WHERE id=? AND ativo=FALSE",produtoHistorico)==1);
        check("Usuário com operações registradas é inativado",!s.removerUsuario(transacaoId).excluido() && db.scalar("SELECT COUNT(*) FROM usuarios WHERE id=? AND ativo=FALSE",transacaoId)==1);
        long modalidadeComPlano=s.saveModalidade(null,"Modalidade com plano","Teste de remoção"),planoDaModalidade=s.savePlano(null,"Plano da modalidade",modalidadeComPlano,new BigDecimal("40.00"),1);
        var cascata=s.removerModalidade(modalidadeComPlano);
        check("Modalidade com planos é inativada junto com seus planos ativos",!cascata.excluido() && cascata.mensagem().startsWith(AcademiaService.INATIVADO) && db.scalar("SELECT COUNT(*) FROM planos WHERE id=? AND ativo=FALSE",planoDaModalidade)==1);
        rejects("Aluno com matrícula ativa não pode ser removido",()->s.removerAluno(aluno));
        rejects("Plano com matrícula aberta não pode ser removido",()->s.removerPlano(plano));
        rejects("Modalidade com matrícula aberta em seus planos não pode ser removida",()->s.removerModalidade(modalidade));
        rejects("Produto com saldo não pode ser removido",()->s.removerProduto(produtoFiltro));
        rejects("Administrador não remove a própria conta",()->s.removerUsuario(1L));
        rejects("Cadastro já inativo com histórico não é excluído",()->s.removerAluno(alunoInativacao));
        check("Remoções bloqueadas não alteram os cadastros",db.scalar("SELECT COUNT(*) FROM alunos WHERE id=? AND ativo=TRUE",aluno)==1 && db.scalar("SELECT COUNT(*) FROM planos WHERE id=? AND ativo=TRUE",plano)==1 && db.scalar("SELECT COUNT(*) FROM produtos WHERE id=? AND saldo=5",produtoFiltro)==1);
        s.login("recepcao","Recepcao@123");
        long daRecepcao=s.saveAluno(null,"Aluno da recepção","99999999999","3333-0002","recepcao@aluno",true);
        check("Atendente remove aluno sem vínculos",s.removerAluno(daRecepcao).excluido());
        check("Atendente não remove modalidades, planos, produtos ou usuários",rejected(()->s.removerModalidade(modalidade)) && rejected(()->s.removerPlano(planoMensal)) && rejected(()->s.removerProduto(produtoHistorico)) && rejected(()->s.removerUsuario(perfilId)) && rejected(()->s.previaRemocao("PRODUTO",produtoFiltro)));
        s.login("admin","Admin@123");
        s.reativar("ALUNO",alunoInativacao);s.reativar("PRODUTO",produtoHistorico);s.movimentar(produtoHistorico,"ENTRADA",2,"Depois de reativado");
        check("Reativação devolve aluno e produto às operações",db.scalar("SELECT COUNT(*) FROM alunos WHERE id=? AND ativo=TRUE",alunoInativacao)==1 && db.scalar("SELECT saldo FROM produtos WHERE id=? AND ativo=TRUE",produtoHistorico)==2);
        rejects("Plano não é reativado antes da sua modalidade",()->s.reativar("PLANO",planoDaModalidade));
        s.reativar("MODALIDADE",modalidadeComPlano);s.reativar("PLANO",planoDaModalidade);
        check("Modalidade e plano reativados voltam a aceitar matrícula",s.matricular(alunoInativacao,planoDaModalidade,LocalDate.now())>0);
        long rollbackAluno=s.saveAluno(null,"Aluno rollback","10101010101","3333-0003","rollback@aluno",true);
        try(Connection c=db.connect();Statement st=c.createStatement()){st.execute("ALTER TABLE auditoria ADD CONSTRAINT teste_exclusao CHECK(acao<>'EXCLUSAO') NOCHECK");}
        boolean removeFailed=false;try{s.removerAluno(rollbackAluno);}catch(SQLException e){removeFailed=true;}
        try(Connection c=db.connect();Statement st=c.createStatement()){st.execute("ALTER TABLE auditoria DROP CONSTRAINT teste_exclusao");}
        check("Falha ao auditar a exclusão desfaz a remoção (rollback)",removeFailed && db.scalar("SELECT COUNT(*) FROM alunos WHERE id=?",rollbackAluno)==1);
        check("Remoção de código inexistente informa registro não encontrado",notFound(()->s.removerAluno(999999L)));
        // Remove o banco temporário de persistência para não acumular pastas em .build a cada execução.
        try(var files=Files.walk(testFolder)){files.sorted(Comparator.reverseOrder()).forEach(file->file.toFile().delete());}
        Files.createDirectories(Path.of("docs"));
        String when=ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
        String plan="""
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

            """;
        String report="# Plano e resultados dos testes\n\n"+plan+"Execução: **"+when+"** (horário de São Paulo). **"+passed+" de "+passed+" verificações aprovadas.**\n\n| Caso | Verificação | Resultado |\n| --- | --- | --- |\n"+String.join("\n",results)+"\n";
        Files.writeString(Path.of("docs/resultados-testes.md"),report,StandardCharsets.UTF_8);System.out.println("TOTAL: "+passed+" testes passaram.");
    }
}
