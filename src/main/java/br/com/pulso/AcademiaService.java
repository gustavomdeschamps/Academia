package br.com.pulso;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Regras do negócio compartilhadas pela interface e pelos testes. */
public final class AcademiaService {
    public record User(long id,String nome,String perfil) { public boolean admin() { return perfil.equals("ADMIN"); } }
    public record Financeiro(BigDecimal valor,BigDecimal pago,BigDecimal saldo,String status) {}
    public record Pagina(List<Map<String,Object>> rows,long total) {}
    /** Situação exibida: uma matrícula ATIVA com fim anterior a hoje aparece como VENCIDA, sem alterar o registro gravado. */
    public static final String SITUACAO_ATUAL="CASE WHEN m.situacao='ATIVA' AND m.fim<CURRENT_DATE THEN 'VENCIDA' ELSE m.situacao END";
    /** Junta o total pago de cada matrícula (alias pg.pago) a uma consulta cuja matrícula usa o alias m. */
    public static final String PAGAMENTOS_POR_MATRICULA="LEFT JOIN (SELECT matricula_id,SUM(valor) AS pago FROM pagamentos GROUP BY matricula_id) pg ON pg.matricula_id=m.id";
    public static final String STATUS_PAGAMENTO="CASE WHEN COALESCE(pg.pago,0)=0 THEN 'PENDENTE' WHEN pg.pago<m.valor THEN 'PARCIAL' ELSE 'PAGO' END";
    static final int MAX_TENTATIVAS=5;
    static final Duration BLOQUEIO=Duration.ofMinutes(1);
    static final String INVALIDO="Usuário ou senha inválidos.", BLOQUEADO="Acesso bloqueado por excesso de tentativas. Aguarde 1 minuto e tente novamente.";
    static final String NAO_ENCONTRADO="Registro não encontrado. Atualize a lista e tente novamente.";
    @FunctionalInterface private interface Tx<T> { T run(Connection c) throws Exception; }
    private final Database db;
    private final Clock clock;
    private volatile User user;
    public AcademiaService(Database db) { this(db,Clock.systemDefaultZone()); }
    /** O relógio pode ser substituído nos testes para conferir o fim do bloqueio de acesso. */
    AcademiaService(Database db,Clock clock) { this.db=db; this.clock=clock; }
    public Database database() { return db; }
    public User user() { return user; }
    public void logout() { user=null; }
    public User login(String login,String password) throws Exception {
        user=null;
        if(login==null||password==null)throw new IllegalArgumentException(INVALIDO);
        var rows=db.query("SELECT * FROM usuarios WHERE LOWER(login)=?",login.strip().toLowerCase(Locale.ROOT));
        if(rows.size()!=1) throw new IllegalArgumentException(INVALIDO);
        var r=rows.get(0); long id=number(r,"id"); LocalDateTime now=LocalDateTime.now(clock);
        if(r.get("bloqueado_ate") instanceof Timestamp until && now.isBefore(until.toLocalDateTime())) throw new IllegalArgumentException(BLOQUEADO);
        if(!Boolean.TRUE.equals(r.get("ativo"))) throw new IllegalArgumentException(INVALIDO);
        try(Connection c=db.connect()) {
            if(!Security.verify(password,(String)r.get("senha"))) {
                // O incremento é feito no banco para que tentativas simultâneas também sejam contadas.
                Database.update(c,"UPDATE usuarios SET tentativas=tentativas+1 WHERE id=?",id);
                if(Database.update(c,"UPDATE usuarios SET tentativas=0,bloqueado_ate=? WHERE id=? AND tentativas>=?",Timestamp.valueOf(now.plus(BLOQUEIO)),id,MAX_TENTATIVAS)==1)
                    throw new IllegalArgumentException("Muitas tentativas incorretas. O acesso foi bloqueado por 1 minuto.");
                throw new IllegalArgumentException(INVALIDO);
            }
            Database.update(c,"UPDATE usuarios SET tentativas=0,bloqueado_ate=NULL WHERE id=?",id);
        }
        user=new User(id,r.get("nome").toString(),r.get("perfil").toString()); return user;
    }
    /** Confere no banco, a cada operação, se a conta continua ativa e com o mesmo perfil. */
    private void authenticated() throws SQLException {
        User current=user;
        if(current==null) throw new IllegalStateException("Entre no sistema para continuar.");
        var rows=db.query("SELECT nome,perfil,ativo FROM usuarios WHERE id=?",current.id());
        if(rows.isEmpty() || !Boolean.TRUE.equals(rows.get(0).get("ativo"))) { user=null; throw new IllegalStateException("Sua conta foi desativada. A sessão foi encerrada."); }
        String perfil=rows.get(0).get("perfil").toString();
        if(!perfil.equals(current.perfil())) { user=null; throw new IllegalStateException("Seu perfil de acesso foi alterado. A sessão foi encerrada; entre novamente."); }
        user=new User(current.id(),rows.get(0).get("nome").toString(),perfil);
    }
    private void admin() throws SQLException { authenticated(); if(!user.admin()) throw new IllegalArgumentException("Apenas o administrador pode realizar esta ação."); }
    public void verificarSessao() throws SQLException { authenticated(); }
    public static long number(Map<String,Object> r,String key) { return ((Number)r.get(key)).longValue(); }
    private static String required(String text,String label,int max) {
        if(text==null || text.isBlank()) throw new IllegalArgumentException("Informe "+label+".");
        if(text.trim().length()>max) throw new IllegalArgumentException(label+" excede "+max+" caracteres.");
        return text.trim();
    }
    /** Dados fictícios são aceitos: basta ter exatamente 11 números (pontos e hífen são removidos antes). */
    public static boolean validCpf(String cpf) {
        return cpf!=null && cpf.matches("[0-9]{11}");
    }
    private <T> T transaction(Tx<T> work) throws Exception {
        try(Connection c=db.connect()) {
            c.setAutoCommit(false);
            try { T result=work.run(c); c.commit(); return result; } catch(Exception e) { c.rollback(); throw e; }
        }
    }
    /** Lê e bloqueia a linha que será alterada; os nomes de tabela vêm apenas do próprio código. */
    private static Map<String,Object> locked(Connection c,String table,long id) throws SQLException {
        var rows=Database.query(c,"SELECT * FROM "+table+" WHERE id=? FOR UPDATE",id);
        if(rows.isEmpty()) throw new IllegalArgumentException(NAO_ENCONTRADO);
        return rows.get(0);
    }
    private static Map<String,Object> fields(Object... pairs) { Map<String,Object> m=new LinkedHashMap<>(); for(int i=0;i<pairs.length;i+=2) m.put((String)pairs[i],pairs[i+1]); return m; }
    private static boolean same(Object a,Object b) {
        if(a instanceof BigDecimal x && b instanceof BigDecimal y) return x.compareTo(y)==0;
        if(a instanceof Number x && b instanceof Number y) return x.longValue()==y.longValue();
        return Objects.equals(a,b);
    }
    private static String shown(Object v) { return v==null?"vazio":v instanceof Boolean b?(b?"sim":"não"):v instanceof BigDecimal d?d.toPlainString():v.toString(); }
    /** Auditoria: registra quem alterou, quando e cada campo com o valor anterior e o novo. Alteração sem diferença não gera linha. */
    private void audit(Connection c,String entidade,long id,String acao,Map<String,Object> antes,Map<String,Object> depois) throws SQLException {
        StringBuilder text=new StringBuilder();
        for(var e:depois.entrySet()) {
            Object old=antes==null?null:antes.get(e.getKey());
            if(antes!=null && same(old,e.getValue())) continue;
            if(text.length()>0) text.append("; ");
            text.append(e.getKey()).append(": ").append(antes==null?shown(e.getValue()):shown(old)+" -> "+shown(e.getValue()));
        }
        if(text.length()==0) return;
        record(c,entidade,id,acao,text.length()>1000?text.substring(0,997)+"...":text.toString());
    }
    private void record(Connection c,String entidade,long id,String acao,String detalhes) throws SQLException {
        Database.insert(c,"INSERT INTO auditoria(usuario_id,entidade,registro_id,acao,detalhes) VALUES(?,?,?,?,?)",user.id(),entidade,id,acao,detalhes);
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
        authenticated(); String n=required(nome,"o nome",100), doc=cpf==null?"":cpf.replaceAll("[.\\-\\s]","");
        if(!validCpf(doc)) throw new IllegalArgumentException("Informe o CPF com 11 números.");
        String phone=required(telefone,"o telefone",20);
        if(phone.replaceAll("\\D","").length()<8) throw new IllegalArgumentException("Informe um telefone com pelo menos 8 números.");
        // O e-mail continua obrigatório (coluna NOT NULL), mas basta conter @.
        String mail=required(email,"o e-mail",120);
        if(!mail.contains("@")) throw new IllegalArgumentException("Informe um e-mail com @.");
        var values=fields("nome",n,"cpf",doc,"telefone",phone,"email",mail,"ativo",ativo); if(changePhoto) values.put("foto",foto);
        return transaction(c->{
            if(!Database.query(c,"SELECT id FROM alunos WHERE cpf=? AND id<>?",doc,id==null?0:id).isEmpty()) throw new IllegalArgumentException("Este CPF já está cadastrado.");
            if(id==null) { long created=Database.insert(c,"INSERT INTO alunos(nome,cpf,telefone,email,ativo,foto) VALUES(?,?,?,?,?,?)",n,doc,phone,mail,ativo,foto); audit(c,"ALUNO",created,"CRIACAO",null,values); return created; }
            var old=Database.query(c,"SELECT * FROM alunos WHERE id=? FOR UPDATE",id);
            if(old.isEmpty()) throw new IllegalArgumentException("Aluno não encontrado.");
            if(!ativo && !Database.query(c,"SELECT id FROM matriculas WHERE aluno_id=? AND situacao<>'CANCELADA'",id).isEmpty()) throw new IllegalArgumentException("Cancele as matrículas abertas antes de inativar o aluno.");
            if(Database.update(c,"UPDATE alunos SET nome=?,cpf=?,telefone=?,email=?,ativo=?,foto=CASE WHEN ? THEN ? ELSE foto END WHERE id=?",n,doc,phone,mail,ativo,changePhoto,foto,id)!=1) throw new IllegalArgumentException("Aluno não encontrado.");
            audit(c,"ALUNO",id,"ALTERACAO",old.get(0),values); return id;
        });
    }
    public long saveModalidade(Long id,String nome,String descricao) throws Exception {
        admin(); String n=required(nome,"o nome",80), d=required(descricao,"a descrição",250); var values=fields("nome",n,"descricao",d);
        return transaction(c->{
            if(id==null) { long created=Database.insert(c,"INSERT INTO modalidades(nome,descricao) VALUES(?,?)",n,d); audit(c,"MODALIDADE",created,"CRIACAO",null,values); return created; }
            var old=Database.query(c,"SELECT * FROM modalidades WHERE id=? FOR UPDATE",id);
            if(Database.update(c,"UPDATE modalidades SET nome=?,descricao=? WHERE id=?",n,d,id)!=1) throw new IllegalArgumentException(NAO_ENCONTRADO);
            audit(c,"MODALIDADE",id,"ALTERACAO",old.get(0),values); return id;
        });
    }
    public long savePlano(Long id,String nome,long modalidade,BigDecimal valor,int meses) throws Exception {
        admin(); String n=required(nome,"o nome",80); money(valor,false);
        if(meses<1 || meses>36) throw new IllegalArgumentException("A duração deve ser de 1 a 36 meses.");
        var values=fields("nome",n,"modalidade_id",modalidade,"valor",valor,"meses",meses);
        return transaction(c->{
            var old=id==null?List.<Map<String,Object>>of():Database.query(c,"SELECT * FROM planos WHERE id=? FOR UPDATE",id);
            var modality=Database.query(c,"SELECT ativo FROM modalidades WHERE id=?",modalidade);
            boolean sameModality=!old.isEmpty() && number(old.get(0),"modalidade_id")==modalidade;
            if(modality.isEmpty() || (!sameModality && !Boolean.TRUE.equals(modality.get(0).get("ativo")))) throw new IllegalArgumentException("Selecione uma modalidade ativa.");
            if(id==null) { long created=Database.insert(c,"INSERT INTO planos(nome,modalidade_id,valor,meses) VALUES(?,?,?,?)",n,modalidade,valor,meses); audit(c,"PLANO",created,"CRIACAO",null,values); return created; }
            if(Database.update(c,"UPDATE planos SET nome=?,modalidade_id=?,valor=?,meses=? WHERE id=?",n,modalidade,valor,meses,id)!=1) throw new IllegalArgumentException(NAO_ENCONTRADO);
            audit(c,"PLANO",id,"ALTERACAO",old.get(0),values); return id;
        });
    }
    private static void money(BigDecimal amount,boolean zero) {
        if(amount==null || amount.signum()<0 || (!zero && amount.signum()==0) || amount.scale()>2 || amount.compareTo(new BigDecimal("99999999.99"))>0)
            throw new IllegalArgumentException("Informe um valor válido com até duas casas decimais.");
    }
    private static String brl(BigDecimal value) { return "R$ "+value.setScale(2,java.math.RoundingMode.HALF_UP).toPlainString().replace('.',','); }
    public long matricular(long aluno,long plano,LocalDate inicio) throws Exception {
        authenticated(); if(inicio==null) throw new IllegalArgumentException("Informe a data inicial.");
        return transaction(c->matricular(c,aluno,plano,inicio,"Nova matrícula"));
    }
    private long matricular(Connection c,long aluno,long plano,LocalDate inicio,String motivo) throws SQLException {
        var students=Database.query(c,"SELECT * FROM alunos WHERE id=? FOR UPDATE",aluno);
        if(students.isEmpty() || !Boolean.TRUE.equals(students.get(0).get("ativo"))) throw new IllegalArgumentException("Selecione um aluno ativo.");
        var plans=Database.query(c,"SELECT * FROM planos WHERE id=?",plano);
        if(plans.isEmpty() || !Boolean.TRUE.equals(plans.get(0).get("ativo"))) throw new IllegalArgumentException("Selecione um plano ativo.");
        if(!Database.query(c,"SELECT id FROM matriculas WHERE aluno_id=? AND plano_id=? AND situacao<>'CANCELADA'",aluno,plano).isEmpty()) throw new IllegalArgumentException("O aluno já possui matrícula aberta neste plano.");
        var p=plans.get(0); LocalDate fim=inicio.plusMonths(number(p,"meses"));
        long id=Database.insert(c,"INSERT INTO matriculas(aluno_id,plano_id,inicio,fim,valor,situacao,criado_por) VALUES(?,?,?,?,?,'ATIVA',?)",aluno,plano,inicio,fim,p.get("valor"),user.id());
        Database.insert(c,"INSERT INTO historico_matriculas(matricula_id,situacao,motivo,usuario_id) VALUES(?,'ATIVA',?,?)",id,motivo,user.id());
        return id;
    }
    private String situacaoAtual(Map<String,Object> m) {
        String gravada=m.get("situacao").toString();
        return gravada.equals("ATIVA") && ((java.sql.Date)m.get("fim")).toLocalDate().isBefore(LocalDate.now(clock)) ? "VENCIDA" : gravada;
    }
    /** ATIVA, TRANCADA, CANCELADA ou VENCIDA (ativa cujo fim já passou). */
    public String situacaoAtual(long matricula) throws Exception {
        authenticated(); var rows=db.query("SELECT situacao,fim FROM matriculas WHERE id=?",matricula);
        if(rows.isEmpty()) throw new IllegalArgumentException("Matrícula não encontrada.");
        return situacaoAtual(rows.get(0));
    }
    /** Encerra a matrícula vencida e cria outra no mesmo plano, a partir de hoje, com o preço atual do plano. */
    public long renovar(long matricula) throws Exception {
        authenticated();
        return transaction(c->{
            var rows=Database.query(c,"SELECT * FROM matriculas WHERE id=? FOR UPDATE",matricula);
            if(rows.isEmpty()) throw new IllegalArgumentException("Matrícula não encontrada.");
            var m=rows.get(0);
            if(!situacaoAtual(m).equals("VENCIDA")) throw new IllegalArgumentException("Somente matrículas vencidas podem ser renovadas.");
            if(((BigDecimal)m.get("valor")).compareTo(pago(c,matricula))>0) throw new IllegalArgumentException("Registre o pagamento restante desta matrícula antes de renovar.");
            Database.update(c,"UPDATE matriculas SET situacao='CANCELADA' WHERE id=?",matricula);
            long nova=matricular(c,number(m,"aluno_id"),number(m,"plano_id"),LocalDate.now(clock),"Renovação da matrícula #"+matricula);
            Database.insert(c,"INSERT INTO historico_matriculas(matricula_id,situacao,motivo,usuario_id) VALUES(?,'CANCELADA',?,?)",matricula,"Contrato vencido encerrado por renovação (nova matrícula #"+nova+")",user.id());
            return nova;
        });
    }
    public void situacao(long id,String status,String motivo) throws Exception {
        authenticated(); String reason=required(motivo,"o motivo",250);
        if(!Set.of("ATIVA","TRANCADA","CANCELADA").contains(status)) throw new IllegalArgumentException("Situação inválida.");
        transaction(c->{
            var rows=Database.query(c,"SELECT situacao FROM matriculas WHERE id=? FOR UPDATE",id);
            if(rows.isEmpty()) throw new IllegalArgumentException("Matrícula não encontrada.");
            String old=rows.get(0).get("situacao").toString();
            if(old.equals("CANCELADA")) throw new IllegalArgumentException("Matrícula cancelada não pode ser reativada. Registre uma nova matrícula.");
            if(old.equals(status)) throw new IllegalArgumentException("A matrícula já está nesta situação.");
            Database.update(c,"UPDATE matriculas SET situacao=? WHERE id=?",status,id);
            Database.insert(c,"INSERT INTO historico_matriculas(matricula_id,situacao,motivo,usuario_id) VALUES(?,?,?,?)",id,status,reason,user.id()); return null;
        });
    }
    private static BigDecimal pago(Connection c,long matricula) throws SQLException {
        return new BigDecimal(Database.query(c,"SELECT COALESCE(SUM(valor),0) AS pago FROM pagamentos WHERE matricula_id=?",matricula).get(0).get("pago").toString());
    }
    /** Registra o saldo restante de uma vez. */
    public void pagar(long id,String forma) throws Exception { pagar(id,null,forma); }
    /** Registra um pagamento parcial ou o restante; a soma dos pagamentos nunca ultrapassa o valor contratado. */
    public void pagar(long id,BigDecimal valor,String forma) throws Exception {
        authenticated(); if(!Set.of("PIX","DINHEIRO","CARTAO").contains(forma)) throw new IllegalArgumentException("Forma inválida.");
        if(valor!=null) money(valor,false);
        transaction(c->{
            var rows=Database.query(c,"SELECT * FROM matriculas WHERE id=? FOR UPDATE",id);
            if(rows.isEmpty() || rows.get(0).get("situacao").equals("CANCELADA")) throw new IllegalArgumentException("Selecione uma matrícula não cancelada.");
            BigDecimal saldo=((BigDecimal)rows.get(0).get("valor")).subtract(pago(c,id));
            if(saldo.signum()<=0) throw new IllegalArgumentException("O pagamento desta matrícula já foi quitado.");
            BigDecimal amount=valor==null?saldo:valor;
            if(amount.compareTo(saldo)>0) throw new IllegalArgumentException("O valor excede o saldo devedor de "+brl(saldo)+".");
            Database.insert(c,"INSERT INTO pagamentos(matricula_id,valor,forma,usuario_id) VALUES(?,?,?,?)",id,amount,forma,user.id()); return null;
        });
    }
    public Financeiro financeiro(long matricula) throws Exception {
        authenticated();
        try(Connection c=db.connect()) {
            var rows=Database.query(c,"SELECT valor FROM matriculas WHERE id=?",matricula);
            if(rows.isEmpty()) throw new IllegalArgumentException("Matrícula não encontrada.");
            BigDecimal valor=(BigDecimal)rows.get(0).get("valor"), pago=pago(c,matricula), saldo=valor.subtract(pago).max(BigDecimal.ZERO);
            return new Financeiro(valor,pago,saldo,pago.signum()==0?"PENDENTE":saldo.signum()>0?"PARCIAL":"PAGO");
        }
    }
    public long saveProduto(Long id,String nome,String unidade,BigDecimal preco,int minimo) throws Exception {
        admin(); String n=required(nome,"o nome",100), u=required(unidade,"a unidade",20); money(preco,true);
        if(minimo<0) throw new IllegalArgumentException("O estoque mínimo não pode ser negativo.");
        var values=fields("nome",n,"unidade",u,"preco",preco,"minimo",minimo);
        return transaction(c->{
            if(id==null) { long created=Database.insert(c,"INSERT INTO produtos(nome,unidade,preco,minimo) VALUES(?,?,?,?)",n,u,preco,minimo); audit(c,"PRODUTO",created,"CRIACAO",null,values); return created; }
            var old=Database.query(c,"SELECT * FROM produtos WHERE id=? FOR UPDATE",id);
            if(Database.update(c,"UPDATE produtos SET nome=?,unidade=?,preco=?,minimo=? WHERE id=?",n,u,preco,minimo,id)!=1) throw new IllegalArgumentException(NAO_ENCONTRADO);
            audit(c,"PRODUTO",id,"ALTERACAO",old.get(0),values); return id;
        });
    }
    private void active(Connection c,String table,String entidade,Map<String,Object> old,long id,boolean ativo) throws SQLException {
        if(Boolean.valueOf(ativo).equals(old.get("ativo"))) throw new IllegalArgumentException(ativo?"O cadastro já está ativo.":"O cadastro já está inativo.");
        if(Database.update(c,"UPDATE "+table+" SET ativo=? WHERE id=?",ativo,id)!=1) throw new IllegalArgumentException(NAO_ENCONTRADO);
        audit(c,entidade,id,ativo?"REATIVACAO":"INATIVACAO",old,fields("ativo",ativo));
    }
    /** Inativar preserva o histórico: o registro deixa de ser oferecido, mas continua ligado aos contratos e movimentos. */
    public void modalidadeAtiva(long id,boolean ativo) throws Exception {
        admin();
        transaction(c->{
            var old=locked(c,"modalidades",id);
            if(!ativo && !Database.query(c,"SELECT id FROM planos WHERE modalidade_id=? AND ativo=TRUE",id).isEmpty()) throw new IllegalArgumentException("Inative os planos desta modalidade antes de inativá-la.");
            active(c,"modalidades","MODALIDADE",old,id,ativo); return null;
        });
    }
    public void planoAtivo(long id,boolean ativo) throws Exception {
        admin();
        transaction(c->{
            var old=locked(c,"planos",id);
            if(!ativo && !Database.query(c,"SELECT id FROM matriculas WHERE plano_id=? AND situacao IN ('ATIVA','TRANCADA')",id).isEmpty()) throw new IllegalArgumentException("Este plano possui matrículas ativas ou trancadas. Encerre-as antes de inativar o plano.");
            if(ativo && Database.query(c,"SELECT id FROM modalidades WHERE id=? AND ativo=TRUE",old.get("modalidade_id")).isEmpty()) throw new IllegalArgumentException("Reative a modalidade deste plano primeiro.");
            active(c,"planos","PLANO",old,id,ativo); return null;
        });
    }
    public void produtoAtivo(long id,boolean ativo) throws Exception {
        admin();
        transaction(c->{
            var old=locked(c,"produtos",id);
            if(!ativo && number(old,"saldo")>0) throw new IllegalArgumentException("Registre a saída do saldo restante antes de inativar o produto.");
            active(c,"produtos","PRODUTO",old,id,ativo); return null;
        });
    }
    public void movimentar(long produto,String tipo,int quantidade,String motivo) throws Exception {
        authenticated(); String reason=required(motivo,"o motivo",250);
        if(quantidade<=0) throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        if(!Set.of("ENTRADA","SAIDA").contains(tipo)) throw new IllegalArgumentException("Tipo de movimento inválido.");
        transaction(c->{
            int changed;
            if(tipo.equals("ENTRADA")) changed=Database.update(c,"UPDATE produtos SET saldo=saldo+? WHERE id=? AND ativo=TRUE",quantidade,produto);
            else changed=Database.update(c,"UPDATE produtos SET saldo=saldo-? WHERE id=? AND ativo=TRUE AND saldo>=?",quantidade,produto,quantidade);
            if(changed!=1) throw new IllegalArgumentException("Produto inexistente, inativo ou com saldo insuficiente para a saída.");
            // A linha do produto já está bloqueada pela atualização; o saldo lido é o resultado deste movimento.
            Database.insert(c,"INSERT INTO movimentacoes(produto_id,tipo,quantidade,motivo,usuario_id,saldo_apos) SELECT ?,?,?,?,?,saldo FROM produtos WHERE id=?",produto,tipo,quantidade,reason,user.id(),produto); return null;
        });
    }
    /** Movimentações mais recentes primeiro, com filtros opcionais (nulo = todos) e paginação. */
    public Pagina movimentacoes(Long produto,String tipo,LocalDate de,LocalDate ate,int limite,int deslocamento) throws Exception {
        authenticated();
        if(tipo!=null && !Set.of("ENTRADA","SAIDA").contains(tipo)) throw new IllegalArgumentException("Tipo de movimento inválido.");
        if(de!=null && ate!=null && ate.isBefore(de)) throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial.");
        StringBuilder where=new StringBuilder(" WHERE 1=1"); List<Object> args=new ArrayList<>();
        if(produto!=null) { where.append(" AND m.produto_id=?"); args.add(produto); }
        if(tipo!=null) { where.append(" AND m.tipo=?"); args.add(tipo); }
        if(de!=null) { where.append(" AND m.data>=?"); args.add(Timestamp.valueOf(de.atStartOfDay())); }
        if(ate!=null) { where.append(" AND m.data<?"); args.add(Timestamp.valueOf(ate.plusDays(1).atStartOfDay())); }
        long total=db.scalar("SELECT COUNT(*) FROM movimentacoes m"+where,args.toArray());
        args.add(limite); args.add(deslocamento);
        return new Pagina(db.query("SELECT m.*,p.nome AS produto,u.nome AS responsavel FROM movimentacoes m JOIN produtos p ON p.id=m.produto_id JOIN usuarios u ON u.id=m.usuario_id"+where+" ORDER BY m.id DESC LIMIT ? OFFSET ?",args.toArray()),total);
    }
    public void saveUsuario(Long id,String nome,String login,String senha,String perfil,boolean ativo) throws Exception {
        saveUsuarioInternal(id,nome,login,senha,perfil,ativo,null,false);
    }
    public void saveUsuario(Long id,String nome,String login,String senha,String perfil,boolean ativo,String foto) throws Exception {
        portrait(foto);saveUsuarioInternal(id,nome,login,senha,perfil,ativo,foto,true);
    }
    private void saveUsuarioInternal(Long id,String nome,String login,String senha,String perfil,boolean ativo,String foto,boolean changePhoto) throws Exception {
        admin(); String n=required(nome,"o nome",100), account=required(login,"o login",60).toLowerCase(Locale.ROOT);
        if(!account.matches("[a-zA-Z0-9._-]{3,60}")) throw new IllegalArgumentException("Login: use pelo menos 3 letras, números, ponto ou sublinhado.");
        if(!Set.of("ADMIN","ATENDENTE").contains(perfil)) throw new IllegalArgumentException("Perfil inválido.");
        if(id!=null && id==user.id() && (!ativo || !perfil.equals("ADMIN"))) throw new IllegalArgumentException("Você não pode desativar ou retirar seu próprio acesso de administrador.");
        String password=senha==null?"":senha, hash=null;
        if(id==null || !password.isEmpty()) { if(password.length()<8) throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres."); hash=Security.hash(password); }
        String newHash=hash; var values=fields("nome",n,"login",account,"perfil",perfil,"ativo",ativo); if(changePhoto) values.put("foto",foto);
        // Dados e senha são gravados juntos: se qualquer instrução falhar, nada da edição fica salvo.
        transaction(c->{
            if(!Database.query(c,"SELECT id FROM usuarios WHERE LOWER(login)=? AND (? IS NULL OR id<>?)",account,id,id).isEmpty())throw new IllegalArgumentException("Este login já está cadastrado.");
            if(id==null) { long created=Database.insert(c,"INSERT INTO usuarios(nome,login,senha,perfil,ativo,foto) VALUES(?,?,?,?,?,?)",n,account,newHash,perfil,ativo,foto); audit(c,"USUARIO",created,"CRIACAO",null,values); return null; }
            var old=Database.query(c,"SELECT * FROM usuarios WHERE id=? FOR UPDATE",id);
            if(Database.update(c,"UPDATE usuarios SET nome=?,login=?,perfil=?,ativo=?,foto=CASE WHEN ? THEN ? ELSE foto END WHERE id=?",n,account,perfil,ativo,changePhoto,foto,id)!=1) throw new IllegalArgumentException(NAO_ENCONTRADO);
            if(newHash!=null) Database.update(c,"UPDATE usuarios SET senha=?,tentativas=0,bloqueado_ate=NULL WHERE id=?",newHash,id);
            audit(c,"USUARIO",id,"ALTERACAO",old.get(0),values);
            if(newHash!=null) record(c,"USUARIO",id,"SENHA","Senha redefinida pelo administrador");
            return null;
        });
    }
    /** Resultado de uma remoção: excluído do banco ou apenas inativado por possuir histórico. */
    public record Remocao(boolean excluido,String mensagem) {}
    public static final String INATIVADO="Possui histórico, foi inativado em vez de excluído.";
    private static final Map<String,String> TABELAS=Map.of("ALUNO","alunos","MODALIDADE","modalidades","PLANO","planos","PRODUTO","produtos","USUARIO","usuarios");
    private static final String ABERTA="situacao IN ('ATIVA','TRANCADA')";
    private static boolean exists(Connection c,String sql,Object... args) throws SQLException { return !Database.query(c,sql,args).isEmpty(); }
    /** Alunos podem ser removidos pela recepção; modalidades, planos, produtos e usuários só pelo administrador. */
    private String permitirRemocao(String entidade) throws SQLException {
        String tabela=TABELAS.get(entidade); if(tabela==null) throw new IllegalArgumentException("Cadastro inválido.");
        if(entidade.equals("ALUNO")) authenticated(); else admin();
        return tabela;
    }
    /**
     * Decide o que a remoção fará: true = excluir (não há registros dependentes), false = inativar (há histórico).
     * Situações que não podem ser desfeitas por inativação são bloqueadas com uma mensagem explicando o motivo.
     */
    private boolean excluivel(Connection c,String entidade,Map<String,Object> r) throws SQLException {
        long id=number(r,"id"); boolean excluir;
        switch(entidade) {
            case "ALUNO" -> {
                if(exists(c,"SELECT id FROM matriculas WHERE aluno_id=? AND "+ABERTA,id)) throw new IllegalArgumentException("O aluno possui matrícula ativa ou trancada. Cancele a matrícula antes de remover o aluno.");
                excluir=!exists(c,"SELECT id FROM matriculas WHERE aluno_id=?",id);
            }
            case "PLANO" -> {
                if(exists(c,"SELECT id FROM matriculas WHERE plano_id=? AND "+ABERTA,id)) throw new IllegalArgumentException("O plano possui matrículas ativas ou trancadas. Encerre-as antes de remover o plano.");
                excluir=!exists(c,"SELECT id FROM matriculas WHERE plano_id=?",id);
            }
            case "MODALIDADE" -> {
                if(exists(c,"SELECT m.id FROM matriculas m JOIN planos p ON p.id=m.plano_id WHERE p.modalidade_id=? AND m."+ABERTA,id)) throw new IllegalArgumentException("A modalidade possui planos com matrículas ativas ou trancadas. Encerre-as antes de remover a modalidade.");
                excluir=!exists(c,"SELECT id FROM planos WHERE modalidade_id=?",id);
            }
            case "PRODUTO" -> {
                if(number(r,"saldo")>0) throw new IllegalArgumentException("O produto ainda possui saldo em estoque. Registre a saída do saldo antes de removê-lo.");
                excluir=!exists(c,"SELECT id FROM movimentacoes WHERE produto_id=?",id);
            }
            default -> {
                if(id==user.id()) throw new IllegalArgumentException("Você não pode remover a própria conta.");
                if("ADMIN".equals(r.get("perfil")) && Boolean.TRUE.equals(r.get("ativo")) && Database.query(c,"SELECT id FROM usuarios WHERE perfil='ADMIN' AND ativo=TRUE").size()<=1) throw new IllegalArgumentException("Não é possível remover o último administrador ativo.");
                excluir=!exists(c,"SELECT 1 FROM matriculas WHERE criado_por=? UNION ALL SELECT 1 FROM historico_matriculas WHERE usuario_id=? UNION ALL SELECT 1 FROM pagamentos WHERE usuario_id=? UNION ALL SELECT 1 FROM movimentacoes WHERE usuario_id=? UNION ALL SELECT 1 FROM auditoria WHERE usuario_id=?",id,id,id,id,id);
            }
        }
        if(!excluir && !Boolean.TRUE.equals(r.get("ativo"))) throw new IllegalArgumentException("O cadastro já está inativo e possui histórico; por isso não pode ser excluído.");
        return excluir;
    }
    /** Usado pela confirmação da tela: "EXCLUIR" ou "INATIVAR"; lança exceção se a remoção for bloqueada. Não altera nada. */
    public String previaRemocao(String entidade,long id) throws Exception {
        String tabela=permitirRemocao(entidade);
        try(Connection c=db.connect()) {
            var rows=Database.query(c,"SELECT * FROM "+tabela+" WHERE id=?",id);
            if(rows.isEmpty()) throw new IllegalArgumentException(NAO_ENCONTRADO);
            return excluivel(c,entidade,rows.get(0))?"EXCLUIR":"INATIVAR";
        }
    }
    /**
     * Remove um cadastro: sem dependentes é excluído (DELETE); com histórico é inativado. Matrículas, pagamentos,
     * movimentações e históricos nunca são apagados. Tudo ocorre em uma transação, com registro na auditoria.
     */
    public Remocao remover(String entidade,long id) throws Exception {
        String tabela=permitirRemocao(entidade);
        return transaction(c->{
            var r=locked(c,tabela,id);
            if(excluivel(c,entidade,r)) {
                if(Database.update(c,"DELETE FROM "+tabela+" WHERE id=?",id)!=1) throw new IllegalArgumentException(NAO_ENCONTRADO);
                StringBuilder resumo=new StringBuilder("Registro excluído: ");
                r.forEach((k,v)->{ if(!Set.of("id","senha","tentativas","bloqueado_ate","foto").contains(k)) resumo.append(k).append('=').append(shown(v)).append("; "); });
                record(c,entidade,id,"EXCLUSAO",resumo.length()>1000?resumo.substring(0,997)+"...":resumo.toString());
                return new Remocao(true,"Registro excluído definitivamente.");
            }
            if(Database.update(c,"UPDATE "+tabela+" SET ativo=FALSE WHERE id=?",id)!=1) throw new IllegalArgumentException(NAO_ENCONTRADO);
            audit(c,entidade,id,"INATIVACAO",r,fields("ativo",false));
            String mensagem=INATIVADO;
            if(entidade.equals("MODALIDADE")) {
                // Plano ativo de modalidade inativa não deve continuar sendo oferecido; nenhum deles tem matrícula aberta (conferido acima).
                var planos=Database.query(c,"SELECT * FROM planos WHERE modalidade_id=? AND ativo=TRUE FOR UPDATE",id);
                for(var p:planos) { Database.update(c,"UPDATE planos SET ativo=FALSE WHERE id=?",p.get("id")); audit(c,"PLANO",number(p,"id"),"INATIVACAO",p,fields("ativo",false)); }
                if(!planos.isEmpty()) mensagem+=" Os planos ativos desta modalidade também foram inativados.";
            }
            return new Remocao(false,mensagem);
        });
    }
    public Remocao removerAluno(long id) throws Exception { return remover("ALUNO",id); }
    public Remocao removerModalidade(long id) throws Exception { return remover("MODALIDADE",id); }
    public Remocao removerPlano(long id) throws Exception { return remover("PLANO",id); }
    public Remocao removerProduto(long id) throws Exception { return remover("PRODUTO",id); }
    public Remocao removerUsuario(long id) throws Exception { return remover("USUARIO",id); }
    /** Ação "Reativar" das telas; planos exigem a modalidade ativa. */
    public void reativar(String entidade,long id) throws Exception {
        switch(entidade) {
            case "MODALIDADE" -> modalidadeAtiva(id,true);
            case "PLANO" -> planoAtivo(id,true);
            case "PRODUTO" -> produtoAtivo(id,true);
            default -> {
                String tabela=permitirRemocao(entidade);
                transaction(c->{ active(c,tabela,entidade,locked(c,tabela,id),id,true); return null; });
            }
        }
    }
    /** Trilha de auditoria mais recente primeiro; restrita ao administrador. Entidade e registro nulos listam tudo. */
    public List<Map<String,Object>> auditoria(String entidade,Long registro,int limite) throws Exception {
        admin();
        return db.query("SELECT a.*,u.nome AS responsavel FROM auditoria a JOIN usuarios u ON u.id=a.usuario_id WHERE (? IS NULL OR a.entidade=?) AND (? IS NULL OR a.registro_id=?) ORDER BY a.id DESC LIMIT ?",entidade,entidade,registro,registro,limite);
    }
    /** Qualquer usuário autenticado troca a própria senha, desde que confirme a senha atual. */
    public void alterarSenha(String atual,String nova) throws Exception {
        authenticated();
        if(atual==null || atual.isEmpty()) throw new IllegalArgumentException("Informe a senha atual.");
        if(nova==null || nova.length()<8) throw new IllegalArgumentException("A nova senha deve ter pelo menos 8 caracteres.");
        if(nova.equals(atual)) throw new IllegalArgumentException("A nova senha deve ser diferente da atual.");
        long id=user.id(); String hash=Security.hash(nova);
        transaction(c->{
            var rows=Database.query(c,"SELECT senha FROM usuarios WHERE id=? FOR UPDATE",id);
            if(rows.isEmpty() || !Security.verify(atual,(String)rows.get(0).get("senha"))) throw new IllegalArgumentException("A senha atual não confere.");
            if(Database.update(c,"UPDATE usuarios SET senha=? WHERE id=?",hash,id)!=1) throw new IllegalArgumentException(NAO_ENCONTRADO);
            record(c,"USUARIO",id,"SENHA","Senha alterada pelo próprio usuário"); return null;
        });
    }
}
