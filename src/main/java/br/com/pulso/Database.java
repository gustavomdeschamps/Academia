package br.com.pulso;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
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
                long admin=insert(c,"INSERT INTO usuarios(nome,login,senha,perfil) VALUES(?,?,?,?)","Administrador","admin",Security.hash("Admin@123"),"ADMIN");
                insert(c,"INSERT INTO usuarios(nome,login,senha,perfil) VALUES(?,?,?,?)","Recepção","recepcao",Security.hash("Recepcao@123"),"ATENDENTE");
                update(c,"UPDATE usuarios SET foto='retratos/equipe' WHERE login='admin'");
                update(c,"UPDATE usuarios SET foto='retratos/recepcao' WHERE login='recepcao'");
                long mus=insert(c,"INSERT INTO modalidades(nome,descricao) VALUES(?,?)","Musculação","Treino acompanhado na sala de musculação");
                long fun=insert(c,"INSERT INTO modalidades(nome,descricao) VALUES(?,?)","Funcional","Aulas em grupo com exercícios funcionais");
                long pil=insert(c,"INSERT INTO modalidades(nome,descricao) VALUES(?,?)","Pilates","Aulas com agendamento na recepção");
                long plano=insert(c,"INSERT INTO planos(nome,modalidade_id,valor,meses) VALUES(?,?,?,?)","Mensal musculação",mus,129.90,1);
                insert(c,"INSERT INTO planos(nome,modalidade_id,valor,meses) VALUES(?,?,?,?)","Trimestral funcional",fun,329.90,3);
                insert(c,"INSERT INTO planos(nome,modalidade_id,valor,meses) VALUES(?,?,?,?)","Mensal pilates",pil,219.90,1);
                long ana=insert(c,"INSERT INTO alunos(nome,cpf,telefone,email) VALUES(?,?,?,?)","Ana Martins","52998224725","(47) 99901-2040","ana@example.com");
                insert(c,"INSERT INTO alunos(nome,cpf,telefone,email) VALUES(?,?,?,?)","Bruno Oliveira","11144477735","(47) 99902-3040","bruno@example.com");
                update(c,"UPDATE alunos SET foto='retratos/ana' WHERE cpf='52998224725'");
                update(c,"UPDATE alunos SET foto='retratos/bruno' WHERE cpf='11144477735'");
                long mat=insert(c,"INSERT INTO matriculas(aluno_id,plano_id,inicio,fim,valor,situacao,criado_por) VALUES(?,?,CURRENT_DATE,DATEADD('MONTH',1,CURRENT_DATE),?,'ATIVA',?)",ana,plano,129.90,admin);
                insert(c,"INSERT INTO historico_matriculas(matricula_id,situacao,motivo,usuario_id) VALUES(?,'ATIVA',?,?)",mat,"Matrícula inicial de demonstração",admin);
                long agua=insert(c,"INSERT INTO produtos(nome,unidade,preco,saldo,minimo) VALUES(?,?,?,0,?)","Água mineral 500 ml","unidade",3.50,10);
                long faixa=insert(c,"INSERT INTO produtos(nome,unidade,preco,saldo,minimo) VALUES(?,?,?,0,?)","Faixa elástica","unidade",29.90,5);
                insert(c,"INSERT INTO movimentacoes(produto_id,tipo,quantidade,motivo,usuario_id) VALUES(?,'ENTRADA',30,?,?)",agua,"Saldo inicial de demonstração",admin);
                update(c,"UPDATE produtos SET saldo=30 WHERE id=?",agua);
                insert(c,"INSERT INTO movimentacoes(produto_id,tipo,quantidade,motivo,usuario_id) VALUES(?,'ENTRADA',3,?,?)",faixa,"Saldo inicial de demonstração",admin);
                update(c,"UPDATE produtos SET saldo=3 WHERE id=?",faixa);
                c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        }
    }
}
