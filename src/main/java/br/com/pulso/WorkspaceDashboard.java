package br.com.pulso;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Páginas iniciais distintas, alimentadas pelos mesmos registros reais da academia. */
final class WorkspaceDashboard extends JPanel implements Scrollable {
    interface Actions {
        void open(String page);
        void newStudent();
        void newEnrollment();
        void findStudent(String query);
        void student(Map<String,Object> record);
    }
    /** Números e listas da página inicial, lidos de uma vez fora da linha de eventos da interface. */
    record Data(String nome,boolean admin,long alunos,long ativas,long vencidas,long trancadas,long canceladas,long pendencias,long repor,
                List<Map<String,Object>> recentes,List<Map<String,Object>> pendentes,List<Map<String,Object>> pagamentos,List<Map<String,Object>> alunosRecentes) {}
    private static final String SALDO_ABERTO="m.situacao<>'CANCELADA' AND COALESCE(pg.pago,0)<m.valor";
    static Data load(AcademiaService service)throws Exception {
        service.verificarSessao();Database db=service.database();
        String from=" FROM matriculas m "+AcademiaService.PAGAMENTOS_POR_MATRICULA;
        return new Data(service.user().nome(),service.user().admin(),
            db.scalar("SELECT COUNT(*) FROM alunos WHERE ativo=TRUE"),
            db.scalar("SELECT COUNT(*) FROM matriculas WHERE situacao='ATIVA'"),
            db.scalar("SELECT COUNT(*) FROM matriculas WHERE situacao='ATIVA' AND fim<CURRENT_DATE"),
            db.scalar("SELECT COUNT(*) FROM matriculas WHERE situacao='TRANCADA'"),
            db.scalar("SELECT COUNT(*) FROM matriculas WHERE situacao='CANCELADA'"),
            db.scalar("SELECT COUNT(*)"+from+" WHERE "+SALDO_ABERTO),
            db.scalar("SELECT COUNT(*) FROM produtos WHERE ativo=TRUE AND saldo<=minimo"),
            db.query("SELECT a.nome,a.foto,p.nome AS plano,"+AcademiaService.SITUACAO_ATUAL+" AS situacao FROM matriculas m JOIN alunos a ON a.id=m.aluno_id JOIN planos p ON p.id=m.plano_id ORDER BY m.id DESC LIMIT 3"),
            db.query("SELECT a.nome,p.nome AS plano,"+AcademiaService.SITUACAO_ATUAL+" AS situacao,GREATEST(m.valor-COALESCE(pg.pago,0),0) AS saldo"+from+" JOIN alunos a ON a.id=m.aluno_id JOIN planos p ON p.id=m.plano_id WHERE "+SALDO_ABERTO+" OR (m.situacao='ATIVA' AND m.fim<CURRENT_DATE) ORDER BY m.id DESC LIMIT 3"),
            db.query("SELECT a.nome,pg.valor,pg.forma,pg.data FROM pagamentos pg JOIN matriculas m ON m.id=pg.matricula_id JOIN alunos a ON a.id=m.aluno_id ORDER BY pg.id DESC LIMIT 3"),
            db.query("SELECT * FROM alunos WHERE ativo=TRUE ORDER BY id DESC LIMIT 2"));
    }
    private final Data data;
    private final Actions actions;
    private final boolean administrator;
    private final Color accent;
    private final NumberFormat currency=NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

    WorkspaceDashboard(Data data,Actions actions) {
        this.data=data;this.actions=actions;administrator=data.admin();accent=administrator?Visual.ORANGE:Visual.TEAL;
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));setBackground(Visual.BG);setBorder(new EmptyBorder(0,0,12,10));
        append(welcome(data.nome()),224);
        if(administrator)append(metrics(),122);else append(studentLookup(),116);
        JPanel main=panel(new GridLayout(1,2,18,0),Visual.BG);main.add(recentContracts());main.add(administrator?contractDistribution():pendingContracts());append(main,380);
        if(administrator)append(recentPayments(),330);else append(recentStudents(),230);
        JPanel footer=panel(new BorderLayout(),Visual.BG);footer.add(text("Informações do banco local, atualizadas nesta consulta.",11,false,Visual.MUTED),BorderLayout.WEST);append(footer,26);
    }
    private static JPanel panel(LayoutManager layout,Color color){JPanel panel=new JPanel(layout);panel.setBackground(color);return panel;}
    private static JLabel text(String value,int size,boolean bold,Color color){JLabel label=new JLabel(value);label.setFont(Visual.font(size,bold));label.setForeground(color);label.setToolTipText(value);return label;}
    private JPanel section(String title,String subtitle){
        JPanel card=new Visual.RoundPanel(new BorderLayout(0,16),Visual.SURFACE,18,true);card.setBorder(new EmptyBorder(20,22,18,22));
        JPanel heading=panel(new BorderLayout(0,6),Visual.SURFACE);heading.setOpaque(false);heading.add(text(title,18,true,Visual.INK),BorderLayout.NORTH);heading.add(text(subtitle,12,false,Visual.MUTED),BorderLayout.SOUTH);card.add(heading,BorderLayout.NORTH);return card;
    }
    private JButton action(String title,String glyph,boolean primary,Runnable callback){
        JButton button=new Visual.SoftButton(title,primary,false);button.setBorder(new EmptyBorder(14,15,14,15));button.setIcon(new Visual.Glyph(glyph,primary?Visual.DARK:accent,18));button.setIconTextGap(9);button.addActionListener(event->callback.run());return button;
    }
    private void append(JPanel component,int height){component.setAlignmentX(LEFT_ALIGNMENT);component.setPreferredSize(new Dimension(900,height));component.setMaximumSize(new Dimension(Integer.MAX_VALUE,height));component.setMinimumSize(new Dimension(0,height));add(component);add(Box.createVerticalStrut(18));}
    private JPanel welcome(String name){
        JPanel hero=new Visual.RoundPanel(new BorderLayout(24,0),Visual.SURFACE,22,true);hero.setBorder(new EmptyBorder(22,26,22,20));
        JPanel copy=panel(new BorderLayout(0,12),Visual.SURFACE);copy.setOpaque(false);
        JPanel words=panel(new GridLayout(0,1,0,8),Visual.SURFACE);words.setOpaque(false);
        words.add(text(administrator?"ADMINISTRAÇÃO":"RECEPÇÃO",10,true,accent));
        int hour=ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).getHour();String greeting=hour<12?"Bom dia":hour<18?"Boa tarde":"Boa noite";
        words.add(text(greeting+", "+name.split(" ")[0]+".",28,true,Visual.INK));
        words.add(text(administrator?"Contratos, recebimentos e materiais da academia.":"Alunos e matrículas, a um passo do atendimento.",13,false,Visual.MUTED));copy.add(words,BorderLayout.CENTER);
        JPanel buttons=panel(new FlowLayout(FlowLayout.LEFT,8,0),Visual.SURFACE);buttons.setOpaque(false);
        buttons.add(action(administrator?"Ver matrículas":"Nova matrícula", "Matrículas",true,administrator?()->actions.open("Matrículas"):actions::newEnrollment));
        buttons.add(action(administrator?"Gerenciar equipe":"Cadastrar aluno",administrator?"Usuários":"Alunos",false,administrator?()->actions.open("Usuários"):actions::newStudent));copy.add(buttons,BorderLayout.SOUTH);hero.add(copy,BorderLayout.CENTER);
        JPanel photo=new JPanel(new BorderLayout()){@Override public Dimension getPreferredSize(){return new Dimension(WorkspaceDashboard.this.getWidth()<950?245:270,180);}};photo.setBackground(Visual.SURFACE);photo.setOpaque(false);photo.add(new Visual.PhotoPanel("abertura",14,0,.5,.5));hero.add(photo,BorderLayout.EAST);return hero;
    }
    private JPanel metrics(){
        JPanel row=panel(new GridLayout(1,5,14,0),Visual.BG);
        metric(row,"Alunos ativos",data.alunos(),"Pessoas no cadastro","Alunos");
        metric(row,"Contratos ativos",data.ativas()-data.vencidas(),"Dentro da validade","Matrículas");
        metric(row,"Vencidas",data.vencidas(),"Aguardam renovação","Matrículas");
        metric(row,"Pendências",data.pendencias(),"Saldo a receber","Planos");
        metric(row,"Itens para repor",data.repor(),"Estoque no limite","Produtos");return row;
    }
    private void metric(JPanel row,String name,long value,String detail,String glyph){
        JPanel card=new Visual.RoundPanel(new BorderLayout(0,6),Visual.SURFACE,16,true);card.setBorder(new EmptyBorder(16,18,14,18));
        JPanel top=panel(new BorderLayout(),Visual.SURFACE);top.setOpaque(false);top.add(text(name,12,true,Visual.MUTED),BorderLayout.CENTER);top.add(new JLabel(new Visual.Glyph(glyph,accent,18)),BorderLayout.EAST);card.add(top,BorderLayout.NORTH);card.add(text(Long.toString(value),34,true,Visual.INK),BorderLayout.CENTER);card.add(text(detail,11,false,Visual.MUTED),BorderLayout.SOUTH);row.add(card);
    }
    private JPanel studentLookup(){
        JPanel card=new Visual.RoundPanel(new BorderLayout(14,10),Visual.SURFACE,16,true);card.setBorder(new EmptyBorder(16,20,14,20));card.add(text("Localizar aluno",15,true,Visual.INK),BorderLayout.NORTH);
        Visual.Search search=new Visual.Search("Digite nome, CPF ou telefone");card.add(search,BorderLayout.CENTER);JPanel buttons=panel(new FlowLayout(FlowLayout.RIGHT,8,0),Visual.SURFACE);
        buttons.add(action("Buscar","buscar",true,()->actions.findStudent(search.field.getText())));buttons.add(action("Novo aluno","mais",false,actions::newStudent));card.add(buttons,BorderLayout.EAST);search.field.addActionListener(event->actions.findStudent(search.field.getText()));return card;
    }
    private static String situation(String state){return state.equals("ATIVA")?"Ativa":state.equals("VENCIDA")?"Vencida":state.equals("TRANCADA")?"Trancada":"Cancelada";}
    private JPanel recentContracts(){
        JPanel card=section("Últimas matrículas","Contratações mais recentes do cadastro.");
        List<Map<String,Object>> recent=data.recentes();
        JPanel list=vertical();for(var record:recent){
            JPanel row=panel(new BorderLayout(12,0),Visual.SURFACE);row.setBorder(new EmptyBorder(8,0,8,0));row.add(new Visual.Portrait((String)record.get("foto"),48),BorderLayout.WEST);
            JPanel names=panel(new GridLayout(0,1,0,5),Visual.SURFACE);names.add(text(record.get("nome").toString(),14,true,Visual.INK));names.add(text(record.get("plano").toString(),12,false,Visual.MUTED));row.add(names,BorderLayout.CENTER);
            String state=record.get("situacao").toString();JPanel badge=panel(new GridBagLayout(),Visual.SURFACE);badge.add(new Visual.Badge(situation(state),state.equals("ATIVA")?Visual.GOOD:state.equals("VENCIDA")?Visual.ALERT:accent,Visual.INPUT));row.add(badge,BorderLayout.EAST);row.setPreferredSize(new Dimension(0,72));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,72));row.setAlignmentX(LEFT_ALIGNMENT);list.add(row);
        }
        if(recent.isEmpty())list.add(empty("Nenhuma matrícula registrada.","As novas contratações aparecerão aqui."));card.add(list,BorderLayout.CENTER);card.add(action("Consultar matrículas","seta",false,()->actions.open("Matrículas")),BorderLayout.SOUTH);return card;
    }
    private JPanel contractDistribution(){
        JPanel card=section("Situação dos contratos","Distribuição de todos os contratos registrados.");
        long active=data.ativas(),paused=data.trancadas(),cancelled=data.canceladas();
        JPanel chart=panel(new BorderLayout(20,0),Visual.SURFACE);Visual.Distribution ring=new Visual.Distribution(active,paused,cancelled);ring.setPreferredSize(new Dimension(120,112));chart.add(ring,BorderLayout.WEST);
        JPanel legend=panel(new GridLayout(4,1,0,6),Visual.SURFACE);legend.add(text("Ativas     "+active,13,true,Visual.GOOD));legend.add(text("    vencidas entre as ativas     "+data.vencidas(),12,false,data.vencidas()>0?Visual.ALERT:Visual.MUTED));legend.add(text("Trancadas     "+paused,13,false,accent));legend.add(text("Canceladas     "+cancelled,13,false,Visual.MUTED));chart.add(legend,BorderLayout.CENTER);card.add(chart,BorderLayout.CENTER);
        long low=data.repor();card.add(action(low==0?"Conferir estoque":low+" "+(low==1?"item para reposição":"itens para reposição"),"Produtos",false,()->actions.open("Produtos")),BorderLayout.SOUTH);return card;
    }
    private JPanel pendingContracts(){
        JPanel card=section("Recebimentos e renovações","Saldo em aberto ou contrato vencido.");
        var pending=data.pendentes();
        JPanel list=vertical();for(var record:pending){
            JPanel row=panel(new BorderLayout(10,0),Visual.SURFACE);row.setBorder(new EmptyBorder(10,0,10,0));JPanel names=panel(new GridLayout(0,1,0,5),Visual.SURFACE);names.add(text(record.get("nome").toString(),14,true,Visual.INK));
            boolean expired=record.get("situacao").equals("VENCIDA");names.add(text(record.get("plano")+(expired?"  /  vencida":""),12,false,expired?Visual.ALERT:Visual.MUTED));row.add(names,BorderLayout.CENTER);
            BigDecimal saldo=(BigDecimal)record.get("saldo");row.add(text(saldo.signum()>0?currency.format(saldo):"Renovar",15,true,saldo.signum()>0?accent:Visual.ALERT),BorderLayout.EAST);row.setPreferredSize(new Dimension(0,72));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,72));row.setAlignmentX(LEFT_ALIGNMENT);list.add(row);
        }
        if(pending.isEmpty())list.add(empty("Nenhuma pendência registrada.","Os recebimentos estão conferidos."));card.add(list,BorderLayout.CENTER);card.add(action("Consultar pagamentos","Planos",false,()->actions.open("Matrículas")),BorderLayout.SOUTH);return card;
    }
    private JPanel recentPayments(){
        JPanel card=section("Recebimentos recentes","Valores confirmados pela equipe, sem processamento bancário.");
        var payments=data.pagamentos();
        JPanel list=vertical();for(var record:payments){JPanel row=panel(new BorderLayout(14,0),Visual.SURFACE);row.setBorder(new EmptyBorder(8,0,8,0));JPanel names=panel(new GridLayout(0,1,0,4),Visual.SURFACE);names.add(text(record.get("nome").toString(),14,true,Visual.INK));String date=((java.sql.Timestamp)record.get("data")).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));names.add(text(record.get("forma")+"  /  "+date,11,false,Visual.MUTED));row.add(names,BorderLayout.CENTER);row.add(text(currency.format((BigDecimal)record.get("valor")),18,true,Visual.GOOD),BorderLayout.EAST);row.setPreferredSize(new Dimension(0,72));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,72));row.setAlignmentX(LEFT_ALIGNMENT);list.add(row);}
        if(payments.isEmpty())list.add(empty("Ainda não há recebimentos registrados.","Ao confirmar um pagamento, o registro aparecerá aqui."));card.add(list,BorderLayout.CENTER);return card;
    }
    private JPanel recentStudents(){
        JPanel card=section("Alunos do cadastro","Abra a ficha para consultar contatos e contratos.");
        var students=data.alunosRecentes();JPanel people=panel(new GridLayout(1,Math.max(1,students.size()),18,0),Visual.SURFACE);
        for(var person:students){JPanel row=panel(new BorderLayout(14,0),Visual.SURFACE);row.add(new Visual.Portrait((String)person.get("foto"),78),BorderLayout.WEST);JPanel names=panel(new GridLayout(0,1,0,6),Visual.SURFACE);names.add(text(person.get("nome").toString(),15,true,Visual.INK));names.add(text(person.get("telefone").toString(),12,false,Visual.MUTED));row.add(names,BorderLayout.CENTER);row.add(action("Ficha","olho",false,()->actions.student(person)),BorderLayout.SOUTH);people.add(row);}
        if(students.isEmpty())people.add(empty("Cadastre o primeiro aluno.","Nome e contatos organizados para o atendimento."));card.add(people,BorderLayout.CENTER);return card;
    }
    private JPanel vertical(){JPanel list=panel(null,Visual.SURFACE);list.setLayout(new BoxLayout(list,BoxLayout.Y_AXIS));return list;}
    private JPanel empty(String title,String detail){JPanel empty=panel(new GridLayout(0,1,0,8),Visual.SURFACE);empty.setMaximumSize(new Dimension(Integer.MAX_VALUE,80));empty.add(text(title,14,true,Visual.INK));empty.add(text(detail,12,false,Visual.MUTED));return empty;}
    public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
    public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 28;}
    public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return Math.max(28,r.height-28);}
    public boolean getScrollableTracksViewportWidth(){return true;}
    public boolean getScrollableTracksViewportHeight(){return false;}
}
