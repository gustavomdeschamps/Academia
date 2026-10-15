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
    public static void main(String[] args) throws Exception {
        Database db=new Database("jdbc:h2:mem:tests;DB_CLOSE_DELAY=-1");db.seed();AcademiaService s=new AcademiaService(db);
        rejects("Operação sem autenticação bloqueada",()->s.movimentar(1,"ENTRADA",1,"Teste"));
        rejects("Login com senha incorreta bloqueado",()->s.login("admin","incorreta"));
        rejects("Entrada SQL no login não autentica",()->s.login("admin' OR '1'='1","x"));
        check("Login válido reconhece administrador",s.login("admin","Admin@123").admin());
        String hash=db.query("SELECT senha FROM usuarios WHERE login='admin'").get(0).get("senha").toString();
        check("Senha armazenada como hash com salt",!hash.contains("Admin@123") && Security.verify("Admin@123",hash));
        check("Senhas iguais recebem salts diferentes",!Security.hash("mesmaSenha").equals(Security.hash("mesmaSenha")));
        long aluno=s.saveAluno(null,"Carla Mendes","123.456.789-09","(47) 99999-1234","carla@example.com",true);
        check("Cadastro grava e permite consultar aluno",db.scalar("SELECT COUNT(*) FROM alunos WHERE id=? AND cpf=?",aluno,"12345678909")==1);
        rejects("CPF duplicado bloqueado",()->s.saveAluno(null,"Outra Carla","12345678909","(47) 99999-1234","c@example.com",true));
        rejects("CPF com dígitos inválidos bloqueado",()->s.saveAluno(null,"Inválido","12345678900","(47) 99999-1234","c@example.com",true));
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
        Database first=new Database(persistentUrl);first.seed();new AcademiaService(first).login("admin","Admin@123");
        Database second=new Database(persistentUrl);check("Dados persistem após nova abertura do banco",second.scalar("SELECT COUNT(*) FROM alunos")==2);
        AcademiaService photoService=new AcademiaService(second);photoService.login("admin","Admin@123");
        photoService.saveAluno(2L,"Bruno Oliveira","11144477735","(47) 99902-3040","bruno@example.com",true,null);
        Database third=new Database(persistentUrl);
        check("Remoção de retrato persiste após reabrir o banco",third.query("SELECT foto FROM alunos WHERE id=2").get(0).get("foto")==null);
        try(Connection c=third.connect();Statement st=c.createStatement()){st.execute("ALTER TABLE alunos DROP COLUMN foto");st.execute("ALTER TABLE usuarios DROP COLUMN foto");}
        Database migrated=new Database(persistentUrl);
        check("Banco anterior recebe retratos sem perder cadastros",migrated.scalar("SELECT COUNT(*) FROM alunos")==2 && migrated.scalar("SELECT COUNT(*) FROM alunos WHERE foto IS NOT NULL")==2 && migrated.scalar("SELECT COUNT(*) FROM usuarios")==2);
        Files.createDirectories(Path.of("docs"));
        String report="# Resultados dos testes\n\nExecução: "+ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"))+"\n\n"+passed+" verificações passaram. Banco isolado H2. Testes de serviço e integração JDBC; a inspeção visual das telas é complementar.\n\n| Caso | Verificação | Resultado |\n| --- | --- | --- |\n"+String.join("\n",results)+"\n";
        Files.writeString(Path.of("docs/resultados-testes.md"),report,StandardCharsets.UTF_8);System.out.println("TOTAL: "+passed+" testes passaram.");
    }
}
