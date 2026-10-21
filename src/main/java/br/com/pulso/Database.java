package br.com.pulso;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Conexões JDBC, criação das tabelas e consultas parametrizadas. */
public final class Database {
    private final String url;
    public Database(String url) throws Exception {
        this.url = url;
        Class.forName("org.h2.Driver");
        try (Connection c = connect(); Statement s = c.createStatement()) {
            boolean migratePortraits=query(c,"SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='ALUNOS' AND COLUMN_NAME='FOTO'").isEmpty();
            String schema = Files.readString(Path.of("database/schema.sql"), StandardCharsets.UTF_8);
            for (String sql : schema.split(";")) if (!sql.isBlank()) s.execute(sql);
            if(migratePortraits){
                update(c,"UPDATE alunos SET foto='retratos/ana' WHERE cpf='52998224725' AND email='ana@example.com'");
                update(c,"UPDATE alunos SET foto='retratos/bruno' WHERE cpf='11144477735' AND email='bruno@example.com'");
                update(c,"UPDATE usuarios SET foto='retratos/equipe' WHERE login='admin' AND nome='Administrador'");
                update(c,"UPDATE usuarios SET foto='retratos/recepcao' WHERE login='recepcao' AND nome='Recepção'");
            }
            // Bancos anteriores tinham UNIQUE em pagamentos.matricula_id (pagamento único). O nome da restrição é gerado
            // pelo H2, por isso é lido do catálogo. Remover a restrição não apaga nenhum pagamento já registrado.
            for(var unique:query(c,"SELECT tc.CONSTRAINT_NAME AS nome FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE k ON k.CONSTRAINT_NAME=tc.CONSTRAINT_NAME AND k.TABLE_NAME=tc.TABLE_NAME WHERE tc.TABLE_NAME='PAGAMENTOS' AND tc.CONSTRAINT_TYPE='UNIQUE' AND k.COLUMN_NAME='MATRICULA_ID'"))
                s.execute("ALTER TABLE pagamentos DROP CONSTRAINT \""+unique.get("nome")+"\"");
        }
    }
    public Connection connect() throws SQLException { return DriverManager.getConnection(url, "sa", ""); }
    public List<Map<String,Object>> query(String sql, Object... args) throws SQLException {
        try (Connection c = connect()) { return query(c, sql, args); }
    }
    public static List<Map<String,Object>> query(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement p = prepare(c, sql, args); ResultSet rs = p.executeQuery()) {
            List<Map<String,Object>> rows = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (int i=1;i<=meta.getColumnCount();i++) row.put(meta.getColumnLabel(i).toLowerCase(Locale.ROOT), rs.getObject(i));
                rows.add(row);
            }
            return rows;
        }
    }
    public static PreparedStatement prepare(Connection c, String sql, Object... args) throws SQLException {
        PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        for (int i=0;i<args.length;i++) p.setObject(i+1,args[i]);
        return p;
    }
    public static long insert(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement p = prepare(c,sql,args)) {
            p.executeUpdate();
            try (ResultSet rs=p.getGeneratedKeys()) { if(rs.next()) return rs.getLong(1); }
            return 0;
        }
    }
    public static int update(Connection c,String sql,Object... args) throws SQLException {
        try (PreparedStatement p=prepare(c,sql,args)) { return p.executeUpdate(); }
    }
    public long scalar(String sql,Object... args) throws SQLException {
        return ((Number)query(sql,args).get(0).values().iterator().next()).longValue();
    }
    public void seed() throws Exception {
        if (scalar("SELECT COUNT(*) FROM usuarios") > 0) return;
        try(Connection c=connect()) {
            c.setAutoCommit(false);
            try {
                LocalDate today=LocalDate.now();
                long admin=insert(c,"INSERT INTO usuarios(nome,login,senha,perfil) VALUES(?,?,?,?)","Administrador","admin",Security.hash("Admin@123"),"ADMIN");
                long recepcao=insert(c,"INSERT INTO usuarios(nome,login,senha,perfil) VALUES(?,?,?,?)","Recepção","recepcao",Security.hash("Recepcao@123"),"ATENDENTE");
                update(c,"UPDATE usuarios SET foto='retratos/equipe' WHERE login='admin'");
                update(c,"UPDATE usuarios SET foto='retratos/recepcao' WHERE login='recepcao'");
                long mus=insert(c,"INSERT INTO modalidades(nome,descricao) VALUES(?,?)","Musculação","Treino acompanhado na sala de musculação");
                long fun=insert(c,"INSERT INTO modalidades(nome,descricao) VALUES(?,?)","Funcional","Aulas em grupo com exercícios funcionais");
                long pil=insert(c,"INSERT INTO modalidades(nome,descricao) VALUES(?,?)","Pilates","Aulas com agendamento na recepção");
                long mensal=insert(c,"INSERT INTO planos(nome,modalidade_id,valor,meses) VALUES(?,?,?,?)","Mensal musculação",mus,129.90,1);
                long trimestral=insert(c,"INSERT INTO planos(nome,modalidade_id,valor,meses) VALUES(?,?,?,?)","Trimestral funcional",fun,329.90,3);
                long pilates=insert(c,"INSERT INTO planos(nome,modalidade_id,valor,meses) VALUES(?,?,?,?)","Mensal pilates",pil,219.90,1);
                long ana=insert(c,"INSERT INTO alunos(nome,cpf,telefone,email) VALUES(?,?,?,?)","Ana Martins","52998224725","(47) 99901-2040","ana@example.com");
                long bruno=insert(c,"INSERT INTO alunos(nome,cpf,telefone,email) VALUES(?,?,?,?)","Bruno Oliveira","11144477735","(47) 99902-3040","bruno@example.com");
                long carolina=insert(c,"INSERT INTO alunos(nome,cpf,telefone,email) VALUES(?,?,?,?)","Carolina Souza","22222222222","(47) 99903-4050","carolina@example.com");
                long diego=insert(c,"INSERT INTO alunos(nome,cpf,telefone,email) VALUES(?,?,?,?)","Diego Ramos","33333333333","3333-5060","diego@example.com");
                long elisa=insert(c,"INSERT INTO alunos(nome,cpf,telefone,email) VALUES(?,?,?,?)","Elisa Prado","44444444444","(47) 99905-6070","elisa@example.com");
                // Os CPFs de demonstração são fictícios: o sistema exige apenas 11 números.
                update(c,"UPDATE alunos SET foto='retratos/ana' WHERE cpf='52998224725'");
                update(c,"UPDATE alunos SET foto='retratos/bruno' WHERE cpf='11144477735'");
                // Cenários da demonstração: em dia e pendente, vencida e quitada, parcial, vencida com saldo e trancada.
                enroll(c,ana,mensal,today,1,"129.90","ATIVA",null,admin);
                long vencida=enroll(c,bruno,pilates,today.minusMonths(2),1,"219.90","ATIVA",null,admin);
                long parcial=enroll(c,carolina,trimestral,today.minusMonths(1),3,"329.90","ATIVA",null,recepcao);
                long devendo=enroll(c,diego,mensal,today.minusDays(45),1,"129.90","ATIVA",null,recepcao);
                long trancada=enroll(c,elisa,pilates,today.minusDays(10),1,"219.90","TRANCADA","Viagem de trabalho",admin);
                String pay="INSERT INTO pagamentos(matricula_id,valor,forma,usuario_id) VALUES(?,?,?,?)";
                insert(c,pay,vencida,new BigDecimal("219.90"),"PIX",admin);
                insert(c,pay,parcial,new BigDecimal("150.00"),"CARTAO",recepcao);
                insert(c,pay,devendo,new BigDecimal("60.00"),"DINHEIRO",recepcao);
                insert(c,pay,trancada,new BigDecimal("219.90"),"PIX",admin);
                long agua=insert(c,"INSERT INTO produtos(nome,unidade,preco,saldo,minimo) VALUES(?,?,?,0,?)","Água mineral 500 ml","unidade",3.50,10);
                long faixa=insert(c,"INSERT INTO produtos(nome,unidade,preco,saldo,minimo) VALUES(?,?,?,0,?)","Faixa elástica","unidade",29.90,5);
                long toalha=insert(c,"INSERT INTO produtos(nome,unidade,preco,saldo,minimo) VALUES(?,?,?,0,?)","Toalha de rosto","unidade",19.90,4);
                long garrafa=insert(c,"INSERT INTO produtos(nome,unidade,preco,saldo,minimo) VALUES(?,?,?,0,?)","Garrafa squeeze 700 ml","unidade",24.90,5);
                move(c,agua,"ENTRADA",36,"Saldo inicial de demonstração",admin);move(c,agua,"SAIDA",6,"Venda no balcão",recepcao);
                move(c,faixa,"ENTRADA",3,"Saldo inicial de demonstração",admin);
                move(c,toalha,"ENTRADA",6,"Saldo inicial de demonstração",admin);move(c,toalha,"SAIDA",4,"Reposição do vestiário",recepcao);
                move(c,garrafa,"ENTRADA",12,"Saldo inicial de demonstração",admin);
                c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        }
    }
    private static long enroll(Connection c,long aluno,long plano,LocalDate inicio,int meses,String valor,String situacao,String motivo,long usuario) throws SQLException {
        long id=insert(c,"INSERT INTO matriculas(aluno_id,plano_id,inicio,fim,valor,situacao,criado_por) VALUES(?,?,?,?,?,?,?)",aluno,plano,inicio,inicio.plusMonths(meses),new BigDecimal(valor),situacao,usuario);
        insert(c,"INSERT INTO historico_matriculas(matricula_id,situacao,motivo,usuario_id) VALUES(?,'ATIVA',?,?)",id,"Matrícula inicial de demonstração",usuario);
        if(motivo!=null) insert(c,"INSERT INTO historico_matriculas(matricula_id,situacao,motivo,usuario_id) VALUES(?,?,?,?)",id,situacao,motivo,usuario);
        return id;
    }
    private static void move(Connection c,long produto,String tipo,int quantidade,String motivo,long usuario) throws SQLException {
        update(c,"UPDATE produtos SET saldo=saldo+? WHERE id=?",tipo.equals("ENTRADA")?quantidade:-quantidade,produto);
        insert(c,"INSERT INTO movimentacoes(produto_id,tipo,quantidade,motivo,usuario_id,saldo_apos) SELECT ?,?,?,?,?,saldo FROM produtos WHERE id=?",produto,tipo,quantidade,motivo,usuario,produto);
    }
}
