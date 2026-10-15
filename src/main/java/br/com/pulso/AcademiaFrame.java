package br.com.pulso;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Interface desktop: componentes Swing comuns, navegação lateral e formulários. */
public final class AcademiaFrame extends JFrame {
    private static final Color NAVY=Visual.DARK, GREEN=Visual.ORANGE, BG=Visual.BG, TEXT=Visual.INK, MUTED=Visual.MUTED, LINE=Visual.LINE;
    private final AcademiaService service;
    private Visual.Stage content;
    private final Map<String,String> portraits=new HashMap<>();
    private String current="Visão geral";
    private JTable table;
    private List<Map<String,Object>> rows=List.of();
    private final Map<String,JButton> navigation=new LinkedHashMap<>();
    private final NumberFormat currency=NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
    private record Choice(long id,String name) { @Override public String toString() { return name; } }
    @FunctionalInterface private interface Work { void run() throws Exception; }
    public AcademiaFrame(AcademiaService service) {
        super("Pulso | Academia Movimento"); this.service=service;
        setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(1440,940); setMinimumSize(new Dimension(1200,800)); setLocationRelativeTo(null);
        Visual.install();
        if(Boolean.getBoolean("pulso.skipIntro"))showLogin();else showOpening();
    }
    private static Font font(int size,boolean bold) { return new Font("Segoe UI",bold?Font.BOLD:Font.PLAIN,size); }
    private static JPanel panel(LayoutManager layout,Color bg) { JPanel p=new JPanel(layout); p.setBackground(bg); return p; }
    private static JLabel label(String s,int size,boolean bold,Color color) { JLabel l=new JLabel(s); l.setFont(font(size,bold)); l.setForeground(color); return l; }
    private static void pad(JComponent c,int top,int left,int bottom,int right) { c.setBorder(new EmptyBorder(top,left,bottom,right)); }
    private JButton button(String s,boolean primary,Work action) {
        JButton b=new Visual.SoftButton(s,primary,false); b.setBackground(primary?GREEN:Visual.SURFACE); pad(b,12,17,12,17);
        if(primary && (s.startsWith("Novo")||s.startsWith("Nova")||s.startsWith("Adicionar")||s.startsWith("Criar"))) {b.setIcon(new Visual.Glyph("mais",NAVY,17));b.setIconTextGap(8);}
        if(!primary && s.equals("Editar")){b.setIcon(new Visual.Glyph("editar",MUTED,16));b.setIconTextGap(7);}
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); b.addActionListener(e->perform(action)); return b;
    }
    private void perform(Work action) {
        try { action.run(); } catch(Exception e) {
            String message=e instanceof SQLException ? (((SQLException)e).getSQLState()!=null && ((SQLException)e).getSQLState().equals("23505")?"Este registro já existe. Confira o nome ou login informado.":"Não foi possível salvar. Confira os dados e os vínculos do registro.") : e.getMessage();
            JOptionPane.showMessageDialog(this,message,"Confira os dados",JOptionPane.WARNING_MESSAGE);
        }
    }
    private void saved(){Visual.Toast toast=new Visual.Toast("Dados salvos com sucesso");setGlassPane(toast);toast.setVisible(true);}
    private void showOpening(){
        setContentPane(new Visual.Intro(this::showLogin));revalidate();repaint();
    }
    private void showLogin() {
        JPanel root=panel(new BorderLayout(44,0),NAVY);pad(root,34,40,34,40);
        JPanel brand=new Visual.RoundPanel(new BorderLayout(0,20),BG,24,true);pad(brand,28,30,28,30);
        JPanel logo=panel(new BorderLayout(),BG);logo.setOpaque(false);JLabel mark=label("pulso",35,true,TEXT);mark.setIcon(new Visual.Glyph("marca",GREEN,32));mark.setIconTextGap(12);logo.add(mark,BorderLayout.WEST);logo.add(new Visual.Badge("ACADEMIA MOVIMENTO",MUTED,Visual.SURFACE),BorderLayout.EAST);brand.add(logo,BorderLayout.NORTH);
        JPanel center=panel(new BorderLayout(0,25),BG);center.setOpaque(false);
        Visual.PhotoPanel scene=new Visual.PhotoPanel("abertura",18,0,.5,.5);center.add(scene,BorderLayout.CENTER);
        JPanel copy=panel(new GridLayout(0,1,0,8),BG);copy.setOpaque(false);copy.add(label("O ritmo da sua academia.",34,true,TEXT));copy.add(label("Uma gestão que acompanha cada movimento.",15,false,MUTED));center.add(copy,BorderLayout.SOUTH);brand.add(center,BorderLayout.CENTER);
        JPanel brandFoot=panel(new BorderLayout(),BG);brandFoot.setOpaque(false);brandFoot.add(label("ALUNOS  /  CONTRATOS  /  ESTOQUE",10,true,MUTED),BorderLayout.WEST);brandFoot.add(new JLabel(new Visual.Glyph("marca",GREEN,23)),BorderLayout.EAST);brand.add(brandFoot,BorderLayout.SOUTH);
        JPanel right=panel(new GridBagLayout(),NAVY);right.setPreferredSize(new Dimension(365,0));
        JPanel login=panel(new GridBagLayout(),NAVY);login.setPreferredSize(new Dimension(365,490));
        GridBagConstraints g=new GridBagConstraints();g.gridx=0;g.fill=GridBagConstraints.HORIZONTAL;g.weightx=1;g.insets=new Insets(0,0,20,0);
        g.gridy=0;login.add(new Visual.Badge("SEU ESPAÇO DE GESTÃO",GREEN,Visual.SELECT),g);
        g.gridy++;login.add(label("Bem-vindo de volta.",30,true,TEXT),g);
        g.gridy++;login.add(label("A rotina da academia começa aqui.",14,false,MUTED),g);
        JTextField user=new JTextField();user.setName("loginUsuario");JPasswordField pass=new JPasswordField();pass.setName("loginSenha");styleInput(user);styleInput(pass);
        JPanel u=labeled("Usuário",user),pw=labeled("Senha",pass);u.setBackground(NAVY);pw.setBackground(NAVY);
        g.gridy++;login.add(u,g);g.gridy++;login.add(pw,g);
        JButton enter=button("Acessar minha academia",true,()->{service.login(user.getText(),new String(pass.getPassword()));pass.setText("");shell();});enter.setIcon(new Visual.Glyph("seta",NAVY,18));enter.setIconTextGap(12);
        g.gridy++;g.insets=new Insets(4,0,20,0);login.add(enter,g);
        JLabel safe=label("Acesso restrito à equipe da academia",11,false,MUTED);safe.setIcon(new Visual.Glyph("escudo",MUTED,16));safe.setIconTextGap(8);g.gridy++;login.add(safe,g);
        right.add(login);root.add(brand,BorderLayout.CENTER);root.add(right,BorderLayout.EAST);
        setContentPane(root);getRootPane().setDefaultButton(enter);revalidate();repaint();
    }

    private void shell() throws Exception {
        JPanel root=panel(new BorderLayout(),BG),side=panel(new BorderLayout(),NAVY);side.setPreferredSize(new Dimension(222,0));pad(side,30,16,22,16);
        JPanel top=panel(new BorderLayout(0,14),NAVY);JLabel logo=label("pulso",34,true,TEXT);logo.setIcon(new Visual.Glyph("marca",GREEN,30));logo.setIconTextGap(10);top.add(logo,BorderLayout.NORTH);top.add(label("ACADEMIA MOVIMENTO",10,true,MUTED),BorderLayout.CENTER);top.add(label("ESPAÇO DE GESTÃO",10,true,new Color(111,120,135)),BorderLayout.SOUTH);pad(top,0,12,32,0);side.add(top,BorderLayout.NORTH);
        JPanel links=panel(new GridLayout(0,1,0,6),NAVY);navigation.clear();
        List<String> pages=new ArrayList<>(List.of("Visão geral","Alunos","Modalidades","Planos","Matrículas","Produtos","Movimentações"));if(service.user().admin())pages.add("Usuários");
        for(String page:pages){JButton b=new Visual.SoftButton(page,false,true);b.setHorizontalAlignment(SwingConstants.LEFT);b.setFont(font(13,false));b.setForeground(MUTED);b.setBackground(NAVY);pad(b,13,12,13,10);b.setIcon(new Visual.Glyph(page,MUTED,19));b.setIconTextGap(13);b.addActionListener(e->perform(()->showPage(page)));navigation.put(page,b);links.add(b);}
        JPanel linkWrap=panel(new BorderLayout(),NAVY);linkWrap.add(links,BorderLayout.NORTH);side.add(linkWrap,BorderLayout.CENTER);
        var accountData=service.database().query("SELECT foto FROM usuarios WHERE id=?",service.user().id());
        String avatar=accountData.isEmpty()?null:(String)accountData.get(0).get("foto");
        JPanel bottom=panel(new BorderLayout(0,16),NAVY),account=panel(new BorderLayout(10,0),NAVY);account.add(new Visual.Portrait(avatar,39),BorderLayout.WEST);JPanel names=panel(new GridLayout(0,1,0,5),NAVY);names.add(label(service.user().nome(),12,true,TEXT));names.add(label(service.user().admin()?"Administrador":"Atendente",10,false,MUTED));account.add(names,BorderLayout.CENTER);bottom.add(account,BorderLayout.NORTH);
        JButton leave=new Visual.SoftButton("Encerrar sessão",false,true);leave.setBackground(NAVY);leave.setForeground(MUTED);leave.setHorizontalAlignment(SwingConstants.LEFT);leave.setIcon(new Visual.Glyph("sair",MUTED,18));leave.setIconTextGap(12);pad(leave,10,10,10,10);leave.addActionListener(e->{service.logout();showLogin();});bottom.add(leave,BorderLayout.SOUTH);side.add(bottom,BorderLayout.SOUTH);
        JPanel workspace=panel(new BorderLayout(),BG),bar=panel(new BorderLayout(),BG);pad(bar,19,28,17,28);bar.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,LINE),new EmptyBorder(19,28,17,28)));bar.add(label("Academia Movimento  /  Gestão",12,false,MUTED),BorderLayout.WEST);
        JPanel barRight=panel(new FlowLayout(FlowLayout.RIGHT,17,0),BG);barRight.add(label(LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM, yyyy",Locale.forLanguageTag("pt-BR"))),11,false,MUTED));barRight.add(new Visual.Badge("EQUIPE CONECTADA",Visual.GOOD,new Color(28,48,41)));bar.add(barRight,BorderLayout.EAST);workspace.add(bar,BorderLayout.NORTH);
        content=new Visual.Stage();pad(content,24,28,22,28);workspace.add(content,BorderLayout.CENTER);root.add(side,BorderLayout.WEST);root.add(workspace,BorderLayout.CENTER);setContentPane(root);getRootPane().setDefaultButton(null);showPage("Visão geral");
    }

    private static String initials(String name){String[] pieces=name.trim().split("\\s+");return (pieces[0].substring(0,1)+(pieces.length>1?pieces[pieces.length-1].substring(0,1):"")).toUpperCase(Locale.ROOT);}
    private void showPage(String page) throws Exception {
        content.remember(); current=page; content.removeAll(); for(var e:navigation.entrySet()){boolean active=e.getKey().equals(page);e.getValue().setBackground(active?Visual.SELECT:NAVY);e.getValue().setForeground(active?GREEN:new Color(180,185,193));e.getValue().setIcon(new Visual.Glyph(e.getKey(),active?GREEN:new Color(155,161,172),20));}
        JPanel heading=panel(new BorderLayout(),BG),titles=panel(new BorderLayout(0,8),BG); titles.add(label(page.equals("Visão geral")?"O pulso da sua academia":page,30,true,TEXT),BorderLayout.NORTH);
        Map<String,String> subtitles=Map.of("Visão geral","O que precisa da atenção da equipe hoje.","Alunos","Cadastro e consulta dos alunos da academia.","Modalidades","Atividades oferecidas aos alunos.","Planos","Valores e duração de cada contratação.","Matrículas","Acompanhe contratos, situações e pagamentos.","Produtos","Confira o saldo e os itens que precisam de reposição.","Movimentações","Entradas e saídas com motivo e responsável.","Usuários","Contas e permissões de acesso da equipe.");
        titles.add(label(subtitles.get(page),14,false,MUTED),BorderLayout.SOUTH);heading.add(titles,BorderLayout.CENTER);if(page.equals("Visão geral")){JPanel action=panel(new GridBagLayout(),BG);action.add(button("Nova matrícula",true,()->matriculaForm()));heading.add(action,BorderLayout.EAST);}content.add(heading,BorderLayout.NORTH);
        if(page.equals("Visão geral")) dashboard(); else listing(page); content.revalidate(); content.repaint();content.reveal();
    }
    private void dashboard() throws Exception {
        Database db=service.database();JPanel body=new Visual.DashboardPanel(),upper=panel(new BorderLayout(0,18),BG);
        JPanel hero=new Visual.RoundPanel(new BorderLayout(26,0),Visual.SURFACE,20,true);hero.setPreferredSize(new Dimension(0,198));pad(hero,18,24,18,18);
        JPanel copy=panel(new GridLayout(0,1,0,5),Visual.SURFACE);copy.setOpaque(false);copy.add(label("ACADEMIA MOVIMENTO",10,true,GREEN));copy.add(label("Pessoas em movimento.",27,true,TEXT));copy.add(label("Gestão no seu ritmo.",27,true,TEXT));copy.add(label("Uma visão clara do atendimento, todos os dias.",12,false,MUTED));hero.add(copy,BorderLayout.CENTER);
        Visual.PhotoPanel photograph=new Visual.PhotoPanel("abertura",12,0,.5,.5);photograph.setPreferredSize(new Dimension(315,160));hero.add(photograph,BorderLayout.EAST);upper.add(hero,BorderLayout.NORTH);
        JPanel stats=panel(new GridLayout(1,4,14,0),BG);stats.setPreferredSize(new Dimension(0,119));
        stat(stats,"Alunos ativos",db.scalar("SELECT COUNT(*) FROM alunos WHERE ativo=TRUE"),"Cadastros disponíveis");
        stat(stats,"Matrículas ativas",db.scalar("SELECT COUNT(*) FROM matriculas WHERE situacao='ATIVA'"),"Contratos em atividade");
        stat(stats,"Pagamentos pendentes",db.scalar("SELECT COUNT(*) FROM matriculas m LEFT JOIN pagamentos p ON p.matricula_id=m.id WHERE p.id IS NULL AND m.situacao<>'CANCELADA'"),"Contratações em aberto");
        stat(stats,"Itens para repor",db.scalar("SELECT COUNT(*) FROM produtos WHERE saldo<=minimo"),"Saldo no limite mínimo");upper.add(stats,BorderLayout.SOUTH);body.add(upper,BorderLayout.NORTH);
        JPanel recent=new Visual.RoundPanel(new BorderLayout(0,12),Visual.SURFACE,18,true);pad(recent,18,18,16,18);recent.add(label("Últimas matrículas",18,true,TEXT),BorderLayout.NORTH);
        var last=db.query("SELECT a.nome AS aluno,a.foto,p.nome AS plano,m.situacao FROM matriculas m JOIN alunos a ON a.id=m.aluno_id JOIN planos p ON p.id=m.plano_id ORDER BY m.id DESC LIMIT 5");
        recent.add(scroll(makeTable(last,new String[]{"aluno","plano","situacao"},new String[]{"Aluno","Plano","Situação"})),BorderLayout.CENTER);JPanel recentFoot=panel(new FlowLayout(FlowLayout.RIGHT,0,0),Visual.SURFACE);recentFoot.add(button("Consultar matrículas",false,()->showPage("Matrículas")));recent.add(recentFoot,BorderLayout.SOUTH);
        JPanel insight=new Visual.RoundPanel(new BorderLayout(0,10),Visual.SURFACE,18,true);pad(insight,18,18,16,18);insight.add(label("Situação dos contratos",18,true,TEXT),BorderLayout.NORTH);
        long active=db.scalar("SELECT COUNT(*) FROM matriculas WHERE situacao='ATIVA'"),paused=db.scalar("SELECT COUNT(*) FROM matriculas WHERE situacao='TRANCADA'"),cancelled=db.scalar("SELECT COUNT(*) FROM matriculas WHERE situacao='CANCELADA'");
        JPanel graph=panel(new BorderLayout(16,0),Visual.SURFACE);Visual.Distribution chart=new Visual.Distribution(active,paused,cancelled);chart.setPreferredSize(new Dimension(112,106));graph.add(chart,BorderLayout.WEST);
        JPanel legend=panel(new GridLayout(3,1,0,6),Visual.SURFACE);legend.add(label("Ativas   "+active,12,true,Visual.GOOD));legend.add(label("Trancadas   "+paused,12,false,GREEN));legend.add(label("Canceladas   "+cancelled,12,false,MUTED));graph.add(legend,BorderLayout.CENTER);insight.add(graph,BorderLayout.CENTER);
        var alerts=db.query("SELECT nome,saldo,minimo FROM produtos WHERE saldo<=minimo ORDER BY nome");JPanel foot=panel(new BorderLayout(10,9),Visual.SURFACE);
        String alert=alerts.isEmpty()?"Estoque em dia":alerts.size()+" "+(alerts.size()==1?"item precisa":"itens precisam")+" de reposição";JLabel alertText=label(alert,12,true,alerts.isEmpty()?Visual.GOOD:GREEN);alertText.setIcon(new Visual.Glyph(alerts.isEmpty()?"check":"Produtos",alerts.isEmpty()?Visual.GOOD:GREEN,18));alertText.setIconTextGap(8);foot.add(alertText,BorderLayout.NORTH);foot.add(button("Conferir estoque",false,()->showPage("Produtos")),BorderLayout.SOUTH);insight.add(foot,BorderLayout.SOUTH);
        JPanel lower=panel(new GridLayout(1,2,18,0),BG);lower.add(recent);lower.add(insight);body.add(lower,BorderLayout.CENTER);
        JScrollPane viewport=new JScrollPane(body,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);viewport.setBorder(BorderFactory.createEmptyBorder());viewport.getViewport().setBackground(BG);Visual.styleScroll(viewport);content.add(viewport,BorderLayout.CENTER);content.add(label("DADOS DA ACADEMIA  /  ATUALIZADOS NESTA CONSULTA",9,true,MUTED),BorderLayout.SOUTH);
    }

    private void stat(JPanel parent,String title,long value,String detail){
        JPanel card=new Visual.RoundPanel(new BorderLayout(0,5),Visual.SURFACE,16,true);pad(card,15,16,14,16);JPanel top=panel(new BorderLayout(7,0),Visual.SURFACE);top.setOpaque(false);top.add(label(title,11,false,MUTED),BorderLayout.CENTER);top.add(new JLabel(new Visual.Glyph(title.startsWith("Alunos")?"Alunos":title.startsWith("Matrículas")?"Matrículas":title.startsWith("Itens")?"Produtos":"Planos",GREEN,17)),BorderLayout.EAST);card.add(top,BorderLayout.NORTH);card.add(label(Long.toString(value),34,true,TEXT),BorderLayout.CENTER);card.add(label(detail,9,false,MUTED),BorderLayout.SOUTH);parent.add(card);
    }

    private void listing(String page) throws Exception {
        String sql; String[] keys,headers;
        switch(page) {
            case "Alunos" -> { sql="SELECT * FROM alunos ORDER BY nome"; keys=new String[]{"id","nome","cpf","telefone","email","ativo"}; headers=new String[]{"Código","Nome","CPF","Telefone","E-mail","Ativo"}; }
            case "Modalidades" -> {sql="SELECT * FROM modalidades ORDER BY nome";keys=new String[]{"id","nome","descricao"};headers=new String[]{"Código","Nome","Descrição"};}
            case "Planos" -> {sql="SELECT p.*,m.nome AS modalidade FROM planos p JOIN modalidades m ON m.id=p.modalidade_id ORDER BY p.nome";keys=new String[]{"id","nome","modalidade","valor","meses"};headers=new String[]{"Código","Plano","Modalidade","Valor total","Meses"};}
            case "Matrículas" -> {sql="SELECT m.*,a.nome AS aluno,a.foto,p.nome AS plano,CASE WHEN pg.id IS NULL THEN 'PENDENTE' ELSE 'PAGO' END AS pagamento FROM matriculas m JOIN alunos a ON a.id=m.aluno_id JOIN planos p ON p.id=m.plano_id LEFT JOIN pagamentos pg ON pg.matricula_id=m.id ORDER BY m.id DESC";keys=new String[]{"id","aluno","plano","inicio","fim","situacao","pagamento","valor"};headers=new String[]{"Código","Aluno","Plano","Início","Fim","Situação","Pagamento","Valor"};}
            case "Produtos" -> {sql="SELECT *,CASE WHEN saldo<=minimo THEN 'REPOR' ELSE 'OK' END AS alerta FROM produtos ORDER BY nome";keys=new String[]{"id","nome","unidade","preco","saldo","minimo","alerta"};headers=new String[]{"Código","Produto","Unidade","Preço","Saldo","Mínimo","Estoque"};}
            case "Movimentações" -> {sql="SELECT m.*,p.nome AS produto,u.nome AS responsavel FROM movimentacoes m JOIN produtos p ON p.id=m.produto_id JOIN usuarios u ON u.id=m.usuario_id ORDER BY m.id DESC";keys=new String[]{"id","data","produto","tipo","quantidade","motivo","responsavel"};headers=new String[]{"Código","Data","Produto","Tipo","Qtd.","Motivo","Responsável"};}
            case "Usuários" -> {sql="SELECT id,nome,login,perfil,ativo,foto FROM usuarios ORDER BY nome";keys=new String[]{"id","nome","login","perfil","ativo"};headers=new String[]{"Código","Nome","Login","Perfil","Ativo"};}
            default -> throw new IllegalArgumentException("Tela desconhecida.");
        }
        rows=service.database().query(sql); table=makeTable(rows,keys,headers);
        TableRowSorter<TableModel> sorter=new TableRowSorter<>(table.getModel()); table.setRowSorter(sorter);
        JPanel body=panel(new BorderLayout(0,18),BG); JPanel toolbar=panel(new BorderLayout(14,0),BG);
        Visual.Search searchBox=new Visual.Search(); JTextField search=searchBox.field;search.setToolTipText("Buscar por qualquer informação da lista");
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e){filter();} public void removeUpdate(javax.swing.event.DocumentEvent e){filter();} public void changedUpdate(javax.swing.event.DocumentEvent e){filter();}
            void filter(){sorter.setRowFilter(RowFilter.regexFilter("(?iu)"+java.util.regex.Pattern.quote(search.getText())));}
        });
        JPanel searchWrap=panel(new BorderLayout(),BG);searchWrap.add(searchBox,BorderLayout.CENTER);searchWrap.setPreferredSize(new Dimension(250,46));toolbar.add(searchWrap,BorderLayout.CENTER);
        JPanel actions=panel(new FlowLayout(FlowLayout.RIGHT,8,0),BG);
        if(page.equals("Matrículas")) {
            actions.add(button("Nova matrícula",true,()->matriculaForm()));
            actions.add(button("Situação",false,()->statusForm()));
            actions.add(button("Pagamento",false,()->paymentForm()));
        } else if(page.equals("Movimentações")) actions.add(button("Registrar movimento",true,()->movementForm(null)));
        else {
            boolean editable=page.equals("Alunos") || service.user().admin();
            if(editable) {String create=Map.of("Alunos","Adicionar aluno","Modalidades","Nova modalidade","Planos","Criar plano","Produtos","Novo produto","Usuários","Adicionar usuário").get(page); actions.add(button(create,true,()->editForm(null))); actions.add(button("Editar",false,()->editForm(selected()))); }
            if(page.equals("Produtos")) actions.add(button("Movimentar",true,()->movementForm(selected())));
        }
        toolbar.add(actions,BorderLayout.EAST);body.add(toolbar,BorderLayout.NORTH);
        JPanel records=new Visual.RoundPanel(new BorderLayout(0,14),Visual.SURFACE,18,true);pad(records,18,20,16,20);JPanel tableHeading=panel(new BorderLayout(),Visual.SURFACE);tableHeading.add(label(page.equals("Movimentações")?"Registro de entradas e saídas":"Lista de "+page.toLowerCase(Locale.ROOT),17,true,TEXT),BorderLayout.WEST);tableHeading.add(new Visual.Badge(rows.size()==1?"1 registro":rows.size()+" registros",MUTED,Visual.LINE),BorderLayout.EAST);records.add(tableHeading,BorderLayout.NORTH);records.add(scroll(table),BorderLayout.CENTER);
        if(page.equals("Modalidades")||page.equals("Alunos")||page.equals("Usuários")) {JPanel center=panel(new BorderLayout(0,20),BG);center.add(page.equals("Modalidades")?modalityGallery():peopleGallery(page),BorderLayout.NORTH);center.add(records,BorderLayout.CENTER);body.add(center,BorderLayout.CENTER);}else body.add(records,BorderLayout.CENTER);
        JPanel below=panel(new BorderLayout(),BG); below.add(label("Selecione uma linha para consultar ou usar as ações",12,false,MUTED),BorderLayout.WEST);
        if(page.equals("Alunos")) below.add(button("Ver ficha",false,()->studentDetails()),BorderLayout.EAST);
        if(page.equals("Matrículas")) below.add(button("Histórico",false,()->history()),BorderLayout.EAST);
        if(page.equals("Produtos")) below.add(label("REPOR: saldo menor ou igual ao mínimo",13,true,GREEN),BorderLayout.EAST);
        body.add(below,BorderLayout.SOUTH); content.add(body,BorderLayout.CENTER);
    }
    private JPanel modalityGallery() throws Exception{
        JPanel gallery=panel(new GridLayout(1,3,16,0),BG);gallery.setPreferredSize(new Dimension(0,235));
        var modalities=service.database().query("SELECT m.id,m.nome,COUNT(p.id) AS planos FROM modalidades m LEFT JOIN planos p ON p.modalidade_id=m.id GROUP BY m.id,m.nome ORDER BY m.id LIMIT 3");
        for(var modality:modalities){
            String name=modality.get("nome").toString(),key=name.toLowerCase(Locale.ROOT);long id=AcademiaService.number(modality,"id"),count=AcademiaService.number(modality,"planos");
            String asset=key.contains("muscula")?"musculacao":key.contains("funcional")?"funcional":key.contains("pilates")?"pilates":"abertura";
            JPanel tile=new Visual.RoundPanel(new BorderLayout(0,10),Visual.SURFACE,16,true);pad(tile,10,10,12,10);tile.add(new Visual.PhotoPanel(asset,10,0,.5,.5),BorderLayout.CENTER);
            JPanel caption=panel(new BorderLayout(0,5),Visual.SURFACE);pad(caption,2,6,0,6);caption.add(label(name,18,true,TEXT),BorderLayout.NORTH);caption.add(label(count==1?"1 plano disponível":count+" planos disponíveis",11,false,MUTED),BorderLayout.CENTER);tile.add(caption,BorderLayout.SOUTH);
            tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));tile.setToolTipText("Selecionar "+name);tile.addMouseListener(new java.awt.event.MouseAdapter(){@Override public void mouseClicked(java.awt.event.MouseEvent e){for(int r=0;r<table.getRowCount();r++)if(table.getValueAt(r,0).equals(Long.toString(id))){table.setRowSelectionInterval(r,r);table.scrollRectToVisible(table.getCellRect(r,1,true));return;}}});gallery.add(tile);
        }
        while(gallery.getComponentCount()<3)gallery.add(panel(new BorderLayout(),BG));return gallery;
    }
    private JPanel peopleGallery(String page){
        JPanel gallery=panel(new GridLayout(1,3,15,0),BG);gallery.setPreferredSize(new Dimension(0,132));int count=Math.min(2,rows.size());
        for(int i=0;i<count;i++){var person=rows.get(i);JPanel card=new Visual.RoundPanel(new BorderLayout(13,0),Visual.SURFACE,16,true);pad(card,18,16,18,16);card.add(new Visual.Portrait((String)person.get("foto"),64),BorderLayout.WEST);JPanel info=panel(new GridLayout(0,1,0,6),Visual.SURFACE);info.setOpaque(false);info.add(label(person.get("nome").toString(),13,true,TEXT));info.add(label(page.equals("Alunos")?"Aluno da academia":format(person.get("perfil")).equals("ADMIN")?"Administrador":"Atendente",10,false,MUTED));info.add(label(Boolean.TRUE.equals(person.get("ativo"))?"Cadastro ativo":"Cadastro inativo",10,true,Boolean.TRUE.equals(person.get("ativo"))?Visual.GOOD:MUTED));card.add(info,BorderLayout.CENTER);long id=AcademiaService.number(person,"id");card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));card.setToolTipText("Selecionar "+person.get("nome"));card.addMouseListener(new java.awt.event.MouseAdapter(){@Override public void mouseClicked(java.awt.event.MouseEvent e){for(int r=0;r<table.getRowCount();r++)if(table.getValueAt(r,0).equals(Long.toString(id))){table.setRowSelectionInterval(r,r);table.scrollRectToVisible(table.getCellRect(r,0,true));return;}}});gallery.add(card);}
        JPanel countCard=new Visual.RoundPanel(new BorderLayout(0,7),Visual.SURFACE,16,true);pad(countCard,17,18,17,18);countCard.add(label(page.equals("Alunos")?"PESSOAS NO CADASTRO":"CONTAS DA EQUIPE",9,true,MUTED),BorderLayout.NORTH);countCard.add(label(Integer.toString(rows.size()),30,true,TEXT),BorderLayout.CENTER);countCard.add(label(page.equals("Alunos")?"Cada pessoa, uma nova história.":"Acesso conforme o perfil.",10,false,MUTED),BorderLayout.SOUTH);gallery.add(countCard);while(gallery.getComponentCount()<3)gallery.add(panel(new BorderLayout(),BG));return gallery;
    }

    private JTable makeTable(List<Map<String,Object>> data,String[] keys,String[] headers) {
        DefaultTableModel model=new DefaultTableModel(headers,0) { @Override public boolean isCellEditable(int r,int c){return false;} };
        for(var row:data) {Object[] cells=new Object[keys.length]; for(int i=0;i<keys.length;i++) cells[i]=format(row.get(keys[i])); model.addRow(cells);}
        JTable t=new JTable(model); t.setRowHeight(64); t.setFont(font(13,false)); t.setForeground(TEXT); t.setSelectionBackground(Visual.SELECT); t.setSelectionForeground(TEXT); t.setGridColor(LINE);t.setShowGrid(false);t.setIntercellSpacing(new Dimension(0,0));t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); t.setFillsViewportHeight(true); t.setBackground(Visual.SURFACE);
        t.getTableHeader().setFont(font(11,true));t.getTableHeader().setForeground(MUTED);t.getTableHeader().setBackground(Visual.INPUT);t.getTableHeader().setPreferredSize(new Dimension(0,42));t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer(){@Override public Component getTableCellRendererComponent(JTable tb,Object v,boolean selected,boolean focus,int r,int col){super.getTableCellRendererComponent(tb,v,selected,focus,r,col);setText(v.toString().toUpperCase(Locale.ROOT));setFont(font(10,true));setForeground(MUTED);setBackground(Visual.INPUT);setBorder(new EmptyBorder(8,12,8,12));return this;}});
        TableCellRenderer renderer=new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb,Object v,boolean selected,boolean focus,int r,int c) {
                super.getTableCellRendererComponent(tb,v,selected,focus,r,c);setFont(font(13,false));setBorder(new MatteBorder(0,0,1,0,Visual.LINE));setToolTipText(v==null?null:v.toString());
                Color rowBg=selected?Visual.SELECT:Visual.SURFACE;setBackground(rowBg);setForeground(TEXT);String text=v==null?"":v.toString();String header=tb.getColumnName(c);
                Map<String,String> status=Map.ofEntries(Map.entry("ATIVA","Ativa"),Map.entry("TRANCADA","Trancada"),Map.entry("CANCELADA","Cancelada"),Map.entry("PAGO","Pago"),Map.entry("PENDENTE","Pendente"),Map.entry("REPOR","Repor"),Map.entry("OK","Em dia"),Map.entry("ENTRADA","Entrada"),Map.entry("SAIDA","Saída"),Map.entry("ADMIN","Admin"),Map.entry("ATENDENTE","Atendente"));
                if(status.containsKey(text)||header.equals("Ativo")){
                    boolean ok=Set.of("ATIVA","PAGO","OK","ENTRADA","Sim").contains(text),bad=Set.of("CANCELADA","SAIDA","Não").contains(text);
                    Color fg=ok?Visual.GOOD:bad?new Color(238,143,149):GREEN,bg=ok?new Color(30,55,45):bad?new Color(60,36,40):Visual.SELECT;
                    JPanel wrap=panel(new FlowLayout(FlowLayout.LEFT,12,18),rowBg);wrap.setBorder(new MatteBorder(0,0,1,0,Visual.LINE));wrap.add(new Visual.Badge(status.getOrDefault(text,text),fg,bg));wrap.setToolTipText(text);return wrap;
                }
                if((header.equals("Nome") && (current.equals("Alunos")||current.equals("Usuários")))||header.equals("Aluno")){
                    JPanel wrap=panel(new BorderLayout(11,0),rowBg);wrap.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,Visual.LINE),new EmptyBorder(10,12,10,8)));wrap.add(new Visual.Portrait((String)data.get(tb.convertRowIndexToModel(r)).get("foto"),38),BorderLayout.WEST);wrap.add(label(text,13,true,TEXT),BorderLayout.CENTER);wrap.setToolTipText(text);return wrap;
                }
                setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,Visual.LINE),new EmptyBorder(0,12,0,12)));return this;
            }
        }; t.setDefaultRenderer(Object.class,renderer);
        for(int i=0;i<headers.length;i++){String h=headers[i];TableColumn col=t.getColumnModel().getColumn(i);col.setPreferredWidth(h.equals("Código")?74:h.equals("Aluno")||h.equals("Nome")||h.equals("Produto")?210:h.equals("Motivo")?240:h.equals("Plano")?190:h.equals("E-mail")?210:h.equals("Data")?170:120);col.setMinWidth(h.equals("Código")?70:h.equals("Aluno")||h.equals("Nome")?150:h.equals("Pagamento")?96:h.equals("Situação")?85:75);}
        return t;
    }
    private String format(Object v) {
        if(v==null)return "";
        if(v instanceof BigDecimal b)return currency.format(b);
        if(v instanceof java.sql.Date d)return d.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        if(v instanceof java.sql.Timestamp d)return d.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        if(v instanceof Boolean b)return b?"Sim":"Não";
        if(v instanceof String str && str.matches("[0-9]{11}") && AcademiaService.validCpf(str))return str.substring(0,3)+"."+str.substring(3,6)+"."+str.substring(6,9)+"-"+str.substring(9);
        return v.toString();
    }
    private JScrollPane scroll(JTable t){JScrollPane sc=new JScrollPane(t);sc.setBorder(BorderFactory.createEmptyBorder());sc.getViewport().setBackground(Visual.SURFACE);Visual.styleScroll(sc);return sc;}
    private Map<String,Object> selected() {
        int row=table.getSelectedRow(); if(row<0)throw new IllegalArgumentException("Selecione um registro na lista.");return rows.get(table.convertRowIndexToModel(row));
    }
    private static void styleInput(JTextField input) {input.setFont(font(15,false));input.setBackground(Visual.INPUT);input.setForeground(TEXT);input.setCaretColor(GREEN);input.setPreferredSize(new Dimension(250,46));input.setBorder(new Visual.InputBorder());}
    private static JPanel labeled(String name,JComponent input) {JPanel p=panel(new BorderLayout(0,7),BG);JLabel caption=label(name,13,true,MUTED);caption.setLabelFor(input);if(input.getAccessibleContext()!=null)input.getAccessibleContext().setAccessibleName(name);p.add(caption,BorderLayout.NORTH);p.add(input,BorderLayout.CENTER);return p;}
    private final class Form {
        final JDialog dialog=new JDialog(AcademiaFrame.this,true);
        final JPanel fields=panel(new GridBagLayout(),BG);
        int row=0;
        Form(String title) {
            dialog.setTitle(title); dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            JPanel root=panel(new BorderLayout(0,24),BG);pad(root,24,28,24,28);JPanel heading=panel(new BorderLayout(0,8),BG);heading.add(label(title,27,true,TEXT),BorderLayout.NORTH);heading.add(label("Confira os dados antes de salvar.",13,false,MUTED),BorderLayout.SOUTH);root.add(heading,BorderLayout.NORTH);JScrollPane formScroll=new JScrollPane(fields);formScroll.setBorder(BorderFactory.createEmptyBorder());formScroll.getViewport().setBackground(BG);Visual.styleScroll(formScroll);root.add(formScroll,BorderLayout.CENTER);dialog.setContentPane(root);
        }
        void add(String name,JComponent component) {
            GridBagConstraints g=new GridBagConstraints();g.gridx=0;g.gridy=row++;g.fill=GridBagConstraints.HORIZONTAL;g.weightx=1;g.insets=new Insets(0,0,12,0);fields.add(labeled(name,component),g);
        }
        JTextField text(String name,Object value) {JTextField t=new JTextField(value==null?"":value.toString());styleInput(t);add(name,t);return t;}
        JComboBox<String> options(String name,String[] choices,String selected) {JComboBox<String> c=new JComboBox<>(choices);Visual.styleCombo(c);if(selected!=null)c.setSelectedItem(selected);add(name,c);return c;}
        JCheckBox check(String name,boolean selected){JCheckBox c=new JCheckBox("Sim",selected);c.setBackground(BG);c.setForeground(TEXT);add(name,c);return c;}
        JComboBox<Choice> choices(String name,String sql,Object selected) throws Exception {
            List<Map<String,Object>> data=service.database().query(sql);JComboBox<Choice> c=new JComboBox<>();
            for(var r:data){String title=r.get("nome").toString();if(r.containsKey("valor"))title+=" ("+format(r.get("valor"))+")";Choice choice=new Choice(AcademiaService.number(r,"id"),title);c.addItem(choice);if(selected instanceof Number n && n.longValue()==choice.id())c.setSelectedItem(choice);}
            Visual.styleCombo(c);add(name,c);return c;
        }
        void show(Work save) {
            JPanel actions=panel(new FlowLayout(FlowLayout.RIGHT,8,0),BG);actions.add(button("Cancelar",false,()->dialog.dispose()));
            JButton submit=button("Salvar",true,()->{save.run();dialog.dispose();showPage(current);saved();});actions.add(submit);dialog.getContentPane().add(actions,BorderLayout.SOUTH);dialog.getRootPane().setDefaultButton(submit);
            dialog.pack();int maxHeight=Math.max(540,getGraphicsConfiguration().getBounds().height-90);dialog.setSize(Math.max(570,dialog.getWidth()),Math.min(maxHeight,dialog.getHeight()));dialog.setLocationRelativeTo(AcademiaFrame.this);dialog.setVisible(true);
        }
    }
    private Object val(Map<String,Object> row,String key) {return row==null?null:row.get(key);}
    private static long chosen(JComboBox<Choice> c) {Choice v=(Choice)c.getSelectedItem();if(v==null)throw new IllegalArgumentException("Cadastre os dados necessários antes de continuar.");return v.id();}
    private static BigDecimal decimal(JTextField t) {try{return new BigDecimal(t.getText().trim().replace(",","."));}catch(Exception e){throw new IllegalArgumentException("Informe o valor em reais, por exemplo: 129,90.");}}
    private static int integer(JTextField t) {try{return Integer.parseInt(t.getText().trim());}catch(Exception e){throw new IllegalArgumentException("Informe uma quantidade inteira válida.");}}
    private void editForm(Map<String,Object> r) throws Exception {
        Long id=r==null?null:AcademiaService.number(r,"id");String entity=Map.of("Alunos","aluno","Modalidades","modalidade","Planos","plano","Produtos","produto","Usuários","usuário").get(current);Form f=new Form((id==null?"Cadastrar ":"Editar ")+entity);
        JTextField nome=f.text("Nome",val(r,"nome"));
        switch(current) {
            case "Alunos" -> {
                JTextField cpf=f.text("CPF",val(r,"cpf")),telefone=f.text("Telefone com DDD",val(r,"telefone")),email=f.text("E-mail",val(r,"email"));JCheckBox ativo=f.check("Aluno ativo",r==null||Boolean.TRUE.equals(val(r,"ativo")));
                PortraitPicker photo=new PortraitPicker((String)val(r,"foto"));f.add("Retrato fictício",photo);
                f.show(()->service.saveAluno(id,nome.getText(),cpf.getText(),telefone.getText(),email.getText(),ativo.isSelected(),photo.selected));
            }
            case "Modalidades" -> {JTextField descricao=f.text("Descrição",val(r,"descricao"));f.show(()->service.saveModalidade(id,nome.getText(),descricao.getText()));}
            case "Planos" -> {
                JComboBox<Choice> m=f.choices("Modalidade","SELECT id,nome FROM modalidades ORDER BY nome",val(r,"modalidade_id"));JTextField valor=f.text("Valor total do plano (R$)",val(r,"valor")),meses=f.text("Duração em meses",r==null?1:val(r,"meses"));
                f.show(()->service.savePlano(id,nome.getText(),chosen(m),decimal(valor),integer(meses)));
            }
            case "Produtos" -> {
                JTextField unidade=f.text("Unidade de medida",r==null?"unidade":val(r,"unidade")),preco=f.text("Preço unitário (R$)",val(r,"preco")),minimo=f.text("Estoque mínimo",r==null?0:val(r,"minimo"));f.add("Saldo inicial",label("Registre uma entrada após salvar o produto.",13,false,MUTED));
                f.show(()->service.saveProduto(id,nome.getText(),unidade.getText(),decimal(preco),integer(minimo)));
            }
            case "Usuários" -> {
                JTextField login=f.text("Login",val(r,"login"));JPasswordField senha=new JPasswordField();styleInput(senha);f.add(id==null?"Senha (mínimo de 8 caracteres)":"Nova senha (em branco mantém a atual)",senha);
                JComboBox<String> perfil=f.options("Perfil",new String[]{"ATENDENTE","ADMIN"},r==null?"ATENDENTE":val(r,"perfil").toString());JCheckBox ativo=f.check("Conta ativa",r==null||Boolean.TRUE.equals(val(r,"ativo")));
                PortraitPicker photo=new PortraitPicker((String)val(r,"foto"));f.add("Retrato fictício",photo);
                f.show(()->service.saveUsuario(id,nome.getText(),login.getText(),new String(senha.getPassword()),perfil.getSelectedItem().toString(),ativo.isSelected(),photo.selected));
            }
        }
    }
    private void matriculaForm() throws Exception {
        Form f=new Form("Nova matrícula");JComboBox<Choice> aluno=f.choices("Aluno ativo","SELECT id,nome FROM alunos WHERE ativo=TRUE ORDER BY nome",null);
        JComboBox<Choice> plano=f.choices("Plano","SELECT id,nome,valor FROM planos ORDER BY nome",null);
        JTextField inicio=f.text("Início (dd/mm/aaaa)",LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/uuuu")));
        f.add("Contratação",label("O pagamento corresponde ao valor total do plano.",13,false,MUTED));
        f.show(()-> {LocalDate data;try{data=LocalDate.parse(inicio.getText(),DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT));}catch(Exception e){throw new IllegalArgumentException("Informe uma data válida no formato dd/mm/aaaa.");} service.matricular(chosen(aluno),chosen(plano),data);});
    }
    private void statusForm() {
        var r=selected();Form f=new Form("Situação da matrícula #"+r.get("id"));JComboBox<String> status=f.options("Nova situação",new String[]{"ATIVA","TRANCADA","CANCELADA"},r.get("situacao").toString());JTextField motivo=f.text("Motivo da alteração",null);
        f.show(()->service.situacao(AcademiaService.number(r,"id"),status.getSelectedItem().toString(),motivo.getText()));
    }
    private void paymentForm() {
        var r=selected();Form f=new Form("Registrar pagamento");f.add("Aluno",label(r.get("aluno").toString(),16,true,TEXT));f.add("Valor da contratação",label(format(r.get("valor")),24,true,GREEN));JComboBox<String> forma=f.options("Forma de pagamento",new String[]{"PIX","DINHEIRO","CARTAO"},"PIX");
        f.show(()->service.pagar(AcademiaService.number(r,"id"),forma.getSelectedItem().toString()));
    }
    private void movementForm(Map<String,Object> r) throws Exception {
        Form f=new Form("Registrar movimentação");JComboBox<Choice> produto=f.choices("Produto e saldo atual","SELECT id,CONCAT(nome,' (saldo: ',saldo,')') AS nome FROM produtos ORDER BY nome",val(r,"id"));
        JComboBox<String> tipo=f.options("Tipo",new String[]{"ENTRADA","SAIDA"},"ENTRADA");JTextField qtd=f.text("Quantidade inteira",1),motivo=f.text("Motivo",null);
        f.show(()->service.movimentar(chosen(produto),tipo.getSelectedItem().toString(),integer(qtd),motivo.getText()));
    }
    private void history() throws Exception {
        var r=selected();long id=AcademiaService.number(r,"id");var hist=service.database().query("SELECT h.data,h.situacao,h.motivo,u.nome AS responsavel FROM historico_matriculas h JOIN usuarios u ON u.id=h.usuario_id WHERE h.matricula_id=? ORDER BY h.id",id);
        JDialog d=new JDialog(this,"Histórico da matrícula #"+id,true);JPanel p=panel(new BorderLayout(0,14),BG);pad(p,22,22,22,22);p.add(label("Alterações de situação",24,true,TEXT),BorderLayout.NORTH);p.add(scroll(makeTable(hist,new String[]{"data","situacao","motivo","responsavel"},new String[]{"Data","Situação","Motivo","Responsável"})),BorderLayout.CENTER);d.setContentPane(p);d.setSize(900,430);d.setLocationRelativeTo(this);d.setVisible(true);
    }
    private final class PortraitPicker extends JPanel{
        String selected;final Map<String,JButton> options=new LinkedHashMap<>();
        PortraitPicker(String initial){super(new FlowLayout(FlowLayout.LEFT,9,0));setBackground(BG);selected=initial;String[] assets={"retratos/ana","retratos/bruno","retratos/equipe","retratos/recepcao",""};for(int i=0;i<assets.length;i++){String asset=assets[i];JButton choice=new JButton();choice.setName("retrato-"+i);choice.setPreferredSize(new Dimension(68,68));choice.setLayout(new BorderLayout());choice.setBackground(Visual.INPUT);choice.setContentAreaFilled(false);choice.setFocusPainted(false);choice.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));choice.setToolTipText(asset.isEmpty()?"Sem retrato":"Retrato fictício "+(i+1));choice.getAccessibleContext().setAccessibleName(choice.getToolTipText());choice.add(new Visual.Portrait(asset.isEmpty()?null:asset,56),BorderLayout.CENTER);choice.addActionListener(e->{selected=asset.isEmpty()?null:asset;refresh();});options.put(asset,choice);add(choice);}refresh();}
        void refresh(){for(var entry:options.entrySet()){boolean checked=Objects.equals(entry.getKey(),selected==null?"":selected);entry.getValue().setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(checked?GREEN:LINE,checked?2:1,true),new EmptyBorder(3,3,3,3)));}}
    }
    private void studentDetails() throws Exception{
        var r=selected();long id=AcademiaService.number(r,"id");JDialog dialog=new JDialog(this,"Ficha do aluno",true);JPanel root=panel(new BorderLayout(0,23),BG);pad(root,26,28,26,28);
        JPanel header=panel(new BorderLayout(20,0),BG);header.add(new Visual.Portrait((String)r.get("foto"),110),BorderLayout.WEST);JPanel info=panel(new GridLayout(0,1,0,7),BG);info.add(label(r.get("nome").toString(),25,true,TEXT));info.add(label("CPF  "+format(r.get("cpf")),13,false,MUTED));info.add(label(r.get("email").toString(),13,false,MUTED));info.add(label(r.get("telefone").toString(),13,false,MUTED));header.add(info,BorderLayout.CENTER);root.add(header,BorderLayout.NORTH);
        var contracts=service.database().query("SELECT p.nome AS plano,m.inicio,m.fim,m.situacao,m.valor FROM matriculas m JOIN planos p ON p.id=m.plano_id WHERE aluno_id=? ORDER BY m.id DESC",id);JPanel list=new Visual.RoundPanel(new BorderLayout(0,13),Visual.SURFACE,16,true);pad(list,17,17,17,17);list.add(label("Contratações deste aluno",17,true,TEXT),BorderLayout.NORTH);list.add(scroll(makeTable(contracts,new String[]{"plano","inicio","fim","situacao","valor"},new String[]{"Plano","Início","Fim","Situação","Valor"})),BorderLayout.CENTER);root.add(list,BorderLayout.CENTER);JPanel actions=panel(new FlowLayout(FlowLayout.RIGHT,9,0),BG);actions.add(button("Fechar ficha",false,()->dialog.dispose()));actions.add(button("Editar aluno",true,()->{dialog.dispose();editForm(r);}));root.add(actions,BorderLayout.SOUTH);dialog.setContentPane(root);dialog.setSize(840,540);dialog.setLocationRelativeTo(this);dialog.setVisible(true);
    }

    /** Exporta a mesma interface do aplicativo com dados descartáveis de demonstração. */
    public void captureScreens() throws Exception {
        Files.createDirectories(Path.of("assets/telas"));showLogin(); setVisible(true); capture("01-login");
        service.login("admin","Admin@123");shell();
        String[] pages={"Visão geral","Alunos","Modalidades","Planos","Matrículas","Produtos","Movimentações","Usuários"};
        for(int i=0;i<pages.length;i++){showPage(pages[i]);capture(String.format("%02d-",i+2)+new String[]{"inicio","alunos","modalidades","planos","matriculas","produtos","movimentacoes","usuarios"}[i]);}
    }
    private void capture(String name) throws Exception {
        validate();getContentPane().doLayout();validate();Toolkit.getDefaultToolkit().sync();
        BufferedImage img=new BufferedImage(getContentPane().getWidth(),getContentPane().getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=img.createGraphics();getContentPane().printAll(g);g.dispose();ImageIO.write(img,"png",Path.of("assets/telas/"+name+".png").toFile());
    }
}
