package br.com.pulso;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Regras do negócio compartilhadas pela interface e pelos testes. */
public final class AcademiaService {
    public record User(long id,String nome,String perfil) { public boolean admin() { return perfil.equals("ADMIN"); } }
    private final Database db;
    private User user;
    public AcademiaService(Database db) { this.db=db; }
    public Database database() { return db; }
    public User user() { return user; }
    public void logout() { user=null; }
    public User login(String login,String password) throws Exception {
        user=null;
        var rows=db.query("SELECT * FROM usuarios WHERE login=? AND ativo=TRUE",login.trim());
        if(rows.isEmpty() || !Security.verify(password,(String)rows.get(0).get("senha"))) throw new IllegalArgumentException("Usuário ou senha inválidos.");
        var r=rows.get(0); user=new User(number(r,"id"),r.get("nome").toString(),r.get("perfil").toString()); return user;
    }
    private void authenticated() { if(user==null) throw new IllegalStateException("Entre no sistema para continuar."); }
    private void admin() { authenticated(); if(!user.admin()) throw new IllegalArgumentException("Apenas o administrador pode realizar esta ação."); }
    public static long number(Map<String,Object> r,String key) { return ((Number)r.get(key)).longValue(); }
    private static String required(String text,String label,int max) {
        if(text==null || text.isBlank()) throw new IllegalArgumentException("Informe "+label+".");
        if(text.trim().length()>max) throw new IllegalArgumentException(label+" excede "+max+" caracteres.");
        return text.trim();
    }
    public static boolean validCpf(String cpf) {
        if(!cpf.matches("[0-9]{11}") || cpf.chars().distinct().count()==1) return false;
        for(int length=9;length<=10;length++) {
            int sum=0; for(int i=0;i<length;i++) sum+=(cpf.charAt(i)-'0')*(length+1-i);
            int digit=(sum*10)%11; if(digit==10) digit=0;
            if(digit != cpf.charAt(length)-'0') return false;
        }
        return true;
    }
    public long saveAluno(Long id,String nome,String cpf,String telefone,String email,boolean ativo) throws Exception {
        return saveAlunoInternal(id,nome,cpf,telefone,email,ativo,null,false);
    }
    public long saveAluno(Long id,String nome,String cpf,String telefone,String email,boolean ativo,String foto) throws Exception {
        portrait(foto);return saveAlunoInternal(id,nome,cpf,telefone,email,ativo,foto,true);
    }
    private static void portrait(String foto){
        if(foto!=null&&!Set.of("retratos/ana","retratos/bruno","retratos/equipe","retratos/recepcao").contains(foto))
            throw new IllegalArgumentException("Escolha um retrato da galeria do sistema.");
    }
    private long saveAlunoInternal(Long id,String nome,String cpf,String telefone,String email,boolean ativo,String foto,boolean changePhoto) throws Exception {
        authenticated(); nome=required(nome,"o nome",100); cpf=cpf.replaceAll("[.\\-\\s]","");
        if(!validCpf(cpf)) throw new IllegalArgumentException("Informe um CPF válido com 11 dígitos.");
        telefone=required(telefone,"o telefone",20);
        if(telefone.replaceAll("\\D","").length()<10) throw new IllegalArgumentException("Informe o telefone com DDD.");
        email=required(email,"o e-mail",120);
        if(!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Informe um e-mail válido.");
        try(Connection c=db.connect()) {
            if(!Database.query(c,"SELECT id FROM alunos WHERE cpf=? AND id<>?",cpf,id==null?0:id).isEmpty()) throw new IllegalArgumentException("Este CPF já está cadastrado.");
            if(id==null) return Database.insert(c,"INSERT INTO alunos(nome,cpf,telefone,email,ativo,foto) VALUES(?,?,?,?,?,?)",nome,cpf,telefone,email,ativo,foto);
            if(!ativo && !Database.query(c,"SELECT id FROM matriculas WHERE aluno_id=? AND situacao<>'CANCELADA'",id).isEmpty()) throw new IllegalArgumentException("Cancele as matrículas abertas antes de inativar o aluno.");
            if(Database.update(c,"UPDATE alunos SET nome=?,cpf=?,telefone=?,email=?,ativo=?,foto=CASE WHEN ? THEN ? ELSE foto END WHERE id=?",nome,cpf,telefone,email,ativo,changePhoto,foto,id)!=1) throw new IllegalArgumentException("Aluno não encontrado.");
            return id;
        }
    }
    public long saveModalidade(Long id,String nome,String descricao) throws Exception {
        admin(); nome=required(nome,"o nome",80); descricao=required(descricao,"a descrição",250);
        try(Connection c=db.connect()) {
            if(id==null) return Database.insert(c,"INSERT INTO modalidades(nome,descricao) VALUES(?,?)",nome,descricao);
            Database.update(c,"UPDATE modalidades SET nome=?,descricao=? WHERE id=?",nome,descricao,id); return id;
        }
    }
    public long savePlano(Long id,String nome,long modalidade,BigDecimal valor,int meses) throws Exception {
        admin(); nome=required(nome,"o nome",80); money(valor,false);
        if(meses<1 || meses>36) throw new IllegalArgumentException("A duração deve ser de 1 a 36 meses.");
        try(Connection c=db.connect()) {
            if(id==null) return Database.insert(c,"INSERT INTO planos(nome,modalidade_id,valor,meses) VALUES(?,?,?,?)",nome,modalidade,valor,meses);
            Database.update(c,"UPDATE planos SET nome=?,modalidade_id=?,valor=?,meses=? WHERE id=?",nome,modalidade,valor,meses,id); return id;
        }
    }
    private static void money(BigDecimal amount,boolean zero) {
        if(amount==null || amount.signum()<0 || (!zero && amount.signum()==0) || amount.scale()>2 || amount.compareTo(new BigDecimal("99999999.99"))>0)
            throw new IllegalArgumentException("Informe um valor válido com até duas casas decimais.");
    }
    public long matricular(long aluno,long plano,LocalDate inicio) throws Exception {
        authenticated(); if(inicio==null) throw new IllegalArgumentException("Informe a data inicial.");
        try(Connection c=db.connect()) {
            c.setAutoCommit(false);
            try {
                var students=Database.query(c,"SELECT * FROM alunos WHERE id=? FOR UPDATE",aluno);
                if(students.isEmpty() || !Boolean.TRUE.equals(students.get(0).get("ativo"))) throw new IllegalArgumentException("Selecione um aluno ativo.");
                var plans=Database.query(c,"SELECT * FROM planos WHERE id=?",plano);
                if(plans.isEmpty()) throw new IllegalArgumentException("Plano não encontrado.");
                if(!Database.query(c,"SELECT id FROM matriculas WHERE aluno_id=? AND plano_id=? AND situacao<>'CANCELADA'",aluno,plano).isEmpty()) throw new IllegalArgumentException("O aluno já possui matrícula aberta neste plano.");
                var p=plans.get(0); LocalDate fim=inicio.plusMonths(number(p,"meses"));
                long id=Database.insert(c,"INSERT INTO matriculas(aluno_id,plano_id,inicio,fim,valor,situacao,criado_por) VALUES(?,?,?,?,?,'ATIVA',?)",aluno,plano,inicio,fim,p.get("valor"),user.id());
                Database.insert(c,"INSERT INTO historico_matriculas(matricula_id,situacao,motivo,usuario_id) VALUES(?,'ATIVA',?,?)",id,"Nova matrícula",user.id());
                c.commit(); return id;
            } catch(Exception e) { c.rollback(); throw e; }
        }
    }
    public void situacao(long id,String status,String motivo) throws Exception {
        authenticated(); motivo=required(motivo,"o motivo",250);
        if(!Set.of("ATIVA","TRANCADA","CANCELADA").contains(status)) throw new IllegalArgumentException("Situação inválida.");
        try(Connection c=db.connect()) {
            c.setAutoCommit(false);
            try {
                var rows=Database.query(c,"SELECT situacao FROM matriculas WHERE id=? FOR UPDATE",id);
                if(rows.isEmpty()) throw new IllegalArgumentException("Matrícula não encontrada.");
                String old=rows.get(0).get("situacao").toString();
                if(old.equals("CANCELADA")) throw new IllegalArgumentException("Matrícula cancelada não pode ser reativada. Registre uma nova matrícula.");
                if(old.equals(status)) throw new IllegalArgumentException("A matrícula já está nesta situação.");
                Database.update(c,"UPDATE matriculas SET situacao=? WHERE id=?",status,id);
                Database.insert(c,"INSERT INTO historico_matriculas(matricula_id,situacao,motivo,usuario_id) VALUES(?,?,?,?)",id,status,motivo,user.id()); c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        }
    }
    public void pagar(long id,String forma) throws Exception {
        authenticated(); if(!Set.of("PIX","DINHEIRO","CARTAO").contains(forma)) throw new IllegalArgumentException("Forma inválida.");
        try(Connection c=db.connect()) {
            c.setAutoCommit(false);
            try {
                var rows=Database.query(c,"SELECT * FROM matriculas WHERE id=? FOR UPDATE",id);
                if(rows.isEmpty() || rows.get(0).get("situacao").equals("CANCELADA")) throw new IllegalArgumentException("Selecione uma matrícula não cancelada.");
                if(!Database.query(c,"SELECT id FROM pagamentos WHERE matricula_id=?",id).isEmpty()) throw new IllegalArgumentException("O pagamento desta matrícula já foi registrado.");
                Database.insert(c,"INSERT INTO pagamentos(matricula_id,valor,forma,usuario_id) VALUES(?,?,?,?)",id,rows.get(0).get("valor"),forma,user.id()); c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        }
    }
    public long saveProduto(Long id,String nome,String unidade,BigDecimal preco,int minimo) throws Exception {
        admin(); nome=required(nome,"o nome",100); unidade=required(unidade,"a unidade",20); money(preco,true);
        if(minimo<0) throw new IllegalArgumentException("O estoque mínimo não pode ser negativo.");
        try(Connection c=db.connect()) {
            if(id==null) return Database.insert(c,"INSERT INTO produtos(nome,unidade,preco,minimo) VALUES(?,?,?,?)",nome,unidade,preco,minimo);
            Database.update(c,"UPDATE produtos SET nome=?,unidade=?,preco=?,minimo=? WHERE id=?",nome,unidade,preco,minimo,id); return id;
        }
    }
    public void movimentar(long produto,String tipo,int quantidade,String motivo) throws Exception {
        authenticated(); motivo=required(motivo,"o motivo",250);
        if(quantidade<=0) throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        if(!Set.of("ENTRADA","SAIDA").contains(tipo)) throw new IllegalArgumentException("Tipo de movimento inválido.");
        try(Connection c=db.connect()) {
            c.setAutoCommit(false);
            try {
                int changed;
                if(tipo.equals("ENTRADA")) changed=Database.update(c,"UPDATE produtos SET saldo=saldo+? WHERE id=?",quantidade,produto);
                else changed=Database.update(c,"UPDATE produtos SET saldo=saldo-? WHERE id=? AND saldo>=?",quantidade,produto,quantidade);
                if(changed!=1) throw new IllegalArgumentException("Produto inexistente ou saldo insuficiente para a saída.");
                Database.insert(c,"INSERT INTO movimentacoes(produto_id,tipo,quantidade,motivo,usuario_id) VALUES(?,?,?,?,?)",produto,tipo,quantidade,motivo,user.id()); c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        }
    }
    public void saveUsuario(Long id,String nome,String login,String senha,String perfil,boolean ativo) throws Exception {
        saveUsuarioInternal(id,nome,login,senha,perfil,ativo,null,false);
    }
    public void saveUsuario(Long id,String nome,String login,String senha,String perfil,boolean ativo,String foto) throws Exception {
        portrait(foto);saveUsuarioInternal(id,nome,login,senha,perfil,ativo,foto,true);
    }
    private void saveUsuarioInternal(Long id,String nome,String login,String senha,String perfil,boolean ativo,String foto,boolean changePhoto) throws Exception {
        admin(); nome=required(nome,"o nome",100); login=required(login,"o login",60);
        if(!login.matches("[a-zA-Z0-9._-]{3,60}")) throw new IllegalArgumentException("Login: use pelo menos 3 letras, números, ponto ou sublinhado.");
        if(!Set.of("ADMIN","ATENDENTE").contains(perfil)) throw new IllegalArgumentException("Perfil inválido.");
        if(id!=null && id==user.id() && (!ativo || !perfil.equals("ADMIN"))) throw new IllegalArgumentException("Você não pode desativar ou retirar seu próprio acesso de administrador.");
        String hash=null;
        if(id==null || !senha.isEmpty()) { if(senha.length()<8) throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres."); hash=Security.hash(senha); }
        try(Connection c=db.connect()) {
            if(id==null) Database.insert(c,"INSERT INTO usuarios(nome,login,senha,perfil,ativo,foto) VALUES(?,?,?,?,?,?)",nome,login,hash,perfil,ativo,foto);
            else {
                Database.update(c,"UPDATE usuarios SET nome=?,login=?,perfil=?,ativo=?,foto=CASE WHEN ? THEN ? ELSE foto END WHERE id=?",nome,login,perfil,ativo,changePhoto,foto,id);
                if(hash!=null) Database.update(c,"UPDATE usuarios SET senha=? WHERE id=?",hash,id);
            }
        }
    }
}
