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
import java.time.format.ResolverStyle;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;

/** Interface desktop: componentes Swing comuns, navegação lateral e formulários. */
public final class AcademiaFrame extends JFrame {
    private static final Color NAVY=Visual.DARK, BG=Visual.BG, TEXT=Visual.INK, MUTED=Visual.MUTED, LINE=Visual.LINE;
    /** Listas comuns mostram até LIMITE linhas; movimentações são paginadas no banco. */
    private static final int LIMITE=500, POR_PAGINA=50;
    private static final DateTimeFormatter DIA=DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private Color accent=Visual.ORANGE;
    private JTextField searchField;
    private final AcademiaService service;
    private Visual.Stage content;
    private String current="Visão geral";
    private JTable table;
    private List<Map<String,Object>> rows=List.of();
    private final Map<String,JButton> navigation=new LinkedHashMap<>();
    private final NumberFormat currency=NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
    /** Nas capturas de tela tudo roda em sequência; no uso normal as consultas vão para um SwingWorker. */
    private boolean synchronous;
    /** Nas capturas de tela, o próximo formulário é salvo como imagem com este nome em vez de ser aberto. */
    private String dialogCapture;
    /** Cada pedido de tela recebe um número; uma resposta atrasada de uma tela anterior é descartada. */
    private int generation;
    private Long movProduto; private String movTipo; private LocalDate movDe,movAte; private int movPagina;
    /** Telas em que "Mostrar inativos" está marcado; lido também pela consulta em segundo plano. */
    private final Set<String> showInactive=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final Map<String,String> ENTIDADES=Map.of("Alunos","ALUNO","Modalidades","MODALIDADE","Planos","PLANO","Produtos","PRODUTO","Usuários","USUARIO");
    private record Choice(long id,String name) { @Override public String toString() { return name; } }
    private record Listing(List<Map<String,Object>> rows,long total,List<Map<String,Object>> extra) {}
    @FunctionalInterface private interface Work { void run() throws Exception; }
    @FunctionalInterface private interface Load<T> { T get() throws Exception; }
    @FunctionalInterface private interface Then<T> { void accept(T value) throws Exception; }
    public AcademiaFrame(AcademiaService service) {
        super("Pulso | Academia Movimento"); this.service=service;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Rectangle available=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1440,available.width),Math.min(940,available.height));
        setMinimumSize(new Dimension(Math.min(1080,available.width),Math.min(700,available.height)));setLocationRelativeTo(null);
        Visual.install();
        if(Boolean.getBoolean("pulso.skipIntro"))showLogin();else showOpening();
    }
    private static Font font(int size,boolean bold) { return new Font("Segoe UI",bold?Font.BOLD:Font.PLAIN,size); }
    private static JPanel panel(LayoutManager layout,Color bg) { JPanel p=new JPanel(layout); p.setBackground(bg); return p; }
    private static JLabel label(String s,int size,boolean bold,Color color) { JLabel l=new JLabel(s); l.setFont(font(size,bold)); l.setForeground(color); return l; }
    private static void pad(JComponent c,int top,int left,int bottom,int right) { c.setBorder(new EmptyBorder(top,left,bottom,right)); }
    private JButton button(String s,boolean primary,Work action) {
        JButton b=new Visual.SoftButton(s,primary,false); b.setBackground(primary?accent:Visual.SURFACE); pad(b,12,17,12,17);
        if(primary && (s.startsWith("Novo")||s.startsWith("Nova")||s.startsWith("Adicionar")||s.startsWith("Criar"))) {b.setIcon(new Visual.Glyph("mais",NAVY,17));b.setIconTextGap(8);}
        if(!primary && s.equals("Editar")){b.setIcon(new Visual.Glyph("editar",MUTED,16));b.setIconTextGap(7);}
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); b.addActionListener(e->perform(action)); return b;
    }
    private void perform(Work action) {
        try { action.run(); } catch(Exception e) { failure(e); }
    }
    /** Converte qualquer falha em uma mensagem compreensível; sessão revogada no banco leva de volta ao login. */
    private void failure(Throwable e) {
        if(e instanceof ExecutionException && e.getCause()!=null) e=e.getCause();
        if(e instanceof IllegalStateException && service.user()==null) {
            for(Window owned:getOwnedWindows()) owned.dispose();
            JOptionPane.showMessageDialog(this,e.getMessage(),"Sessão encerrada",JOptionPane.INFORMATION_MESSAGE);showLogin();return;
        }
        String message;
        if(e instanceof SQLException sql) message=friendly(sql);
        else if(e instanceof IllegalArgumentException || e instanceof IllegalStateException) message=e.getMessage();
        else { e.printStackTrace(); message="Ocorreu uma falha inesperada. Tente novamente; se continuar, feche e abra o sistema."; }
        Window active=KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        JOptionPane.showMessageDialog(active!=null?active:this,message,"Confira os dados",JOptionPane.WARNING_MESSAGE);
    }
    private static String friendly(SQLException e) {
        String state=e.getSQLState()==null?"":e.getSQLState();
        if(state.equals("23505")) return "Este registro já existe. Confira o nome, CPF ou login informado.";
        if(state.equals("23503")) return "O registro está ligado a outros cadastros e não pode ser alterado dessa forma.";
        if(state.equals("23513") || state.equals("22001")) return "Algum valor está fora do permitido. Confira os campos e tente novamente.";
        if(state.startsWith("08") || state.equals("90020") || state.equals("HYT00")) return "Não foi possível acessar o banco de dados. Verifique se o sistema já está aberto em outra janela.";
        return "Não foi possível salvar. Confira os dados e os vínculos do registro.";
    }
    /** Consulta ou grava fora da linha de eventos do Swing; a tela só é montada quando o banco responde. */
    private <T> void background(Load<T> work,Then<T> then,Runnable always) {
        if(synchronous) { try { then.accept(work.get()); } catch(Exception e) { failure(e); } finally { if(always!=null) always.run(); } return; }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception { return work.get(); }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor()); if(always!=null) always.run();
                try { then.accept(get()); } catch(Exception e) { failure(e); }
            }
        }.execute();
    }
    private void saved(){saved("Dados salvos com sucesso");}
    private void saved(String text){Visual.Toast toast=new Visual.Toast(text);setGlassPane(toast);toast.setVisible(true);}
    private void showOpening(){
        setContentPane(new OpeningPanel(()->{
            BufferedImage previous=null;
            if(Visual.MOTION&&getContentPane().getWidth()>0){
                previous=new BufferedImage(getContentPane().getWidth(),getContentPane().getHeight(),BufferedImage.TYPE_INT_RGB);
                Graphics2D g=previous.createGraphics();getContentPane().printAll(g);g.dispose();
            }
            showLogin();
            if(previous!=null){Visual.ScreenFade fade=new Visual.ScreenFade(previous);setGlassPane(fade);fade.setVisible(true);}
        }));revalidate();repaint();
    }
    private void showLogin() {
        generation++;Visual.role(true);accent=Visual.ORANGE;setTitle("Pulso | Academia Movimento");
        LoginPanel login=new LoginPanel(service,this::shell);setContentPane(login);
        getRootPane().setDefaultButton(login.defaultButton());revalidate();repaint();
    }
    private void shell() throws Exception {
        boolean admin=service.user().admin();Visual.role(admin);accent=Visual.accent();movProduto=null;movTipo=null;movDe=null;movAte=null;movPagina=0;
        setTitle("Pulso | "+(admin?"Administração":"Recepção")+" | Academia Movimento");
        JPanel root=panel(new BorderLayout(),BG),side=panel(new BorderLayout(),NAVY);side.setPreferredSize(new Dimension(236,0));pad(side,24,14,18,14);
        JPanel brand=panel(new BorderLayout(0,12),NAVY);pad(brand,0,12,22,12);
        JLabel logo=label("pulso",36,true,TEXT);logo.setIcon(new Visual.Glyph("marca",accent,32));logo.setIconTextGap(11);brand.add(logo,BorderLayout.NORTH);
        JPanel identity=panel(new GridLayout(0,1,0,8),NAVY);identity.add(label("ACADEMIA MOVIMENTO",10,true,MUTED));identity.add(new Visual.Badge(admin?"ADMINISTRAÇÃO":"RECEPÇÃO",accent,Visual.selection()));brand.add(identity,BorderLayout.CENTER);side.add(brand,BorderLayout.NORTH);
        JPanel links=panel(null,NAVY);links.setLayout(new BoxLayout(links,BoxLayout.Y_AXIS));navigation.clear();
        navigationGroup(links,admin?"OPERAÇÃO":"ATENDIMENTO","Visão geral","Alunos","Matrículas");
        navigationGroup(links,admin?"CONFIGURAÇÃO":"CONSULTAS","Modalidades","Planos");
        navigationGroup(links,"MATERIAIS","Produtos","Movimentações");
        if(admin)navigationGroup(links,"ADMINISTRAÇÃO","Usuários");
        JScrollPane menu=new JScrollPane(links,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);menu.setBorder(BorderFactory.createEmptyBorder());menu.getViewport().setBackground(NAVY);Visual.styleScroll(menu);side.add(menu,BorderLayout.CENTER);
        var accountData=service.database().query("SELECT foto FROM usuarios WHERE id=?",service.user().id());String avatar=accountData.isEmpty()?null:(String)accountData.get(0).get("foto");
        JPanel bottom=panel(new BorderLayout(0,12),NAVY);pad(bottom,18,4,0,4);JPanel account=panel(new BorderLayout(11,0),NAVY);account.add(new Visual.Portrait(avatar,52),BorderLayout.WEST);
        JPanel names=panel(new GridLayout(0,1,0,6),NAVY);names.add(label(service.user().nome(),12,true,TEXT));names.add(label(admin?"Administrador":"Equipe da recepção",10,false,accent));account.add(names,BorderLayout.CENTER);bottom.add(account,BorderLayout.NORTH);
        JPanel exits=panel(new GridLayout(0,1,0,8),NAVY);
        JButton password=button("Alterar senha",false,()->passwordForm());password.setIcon(new Visual.Glyph("escudo",MUTED,18));exits.add(password);
        JButton leave=button("Encerrar sessão",false,()->{service.logout();showLogin();});leave.setIcon(new Visual.Glyph("sair",MUTED,18));exits.add(leave);bottom.add(exits,BorderLayout.SOUTH);side.add(bottom,BorderLayout.SOUTH);
        JPanel workspace=panel(new BorderLayout(),BG),bar=panel(new BorderLayout(16,0),BG);bar.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,LINE),new EmptyBorder(12,26,12,26)));
        bar.add(label("Academia Movimento",12,true,MUTED),BorderLayout.WEST);
        JPanel barRight=panel(new FlowLayout(FlowLayout.RIGHT,14,0),BG);barRight.add(label(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy",Locale.forLanguageTag("pt-BR"))),11,false,MUTED));
        JButton refresh=button("Atualizar",false,()->showPage(current));refresh.setIcon(new Visual.Glyph("atualizar",accent,17));barRight.add(refresh);barRight.add(new Visual.Badge(admin?"ACESSO ADMINISTRATIVO":"BALCÃO DE ATENDIMENTO",accent,Visual.selection()));bar.add(barRight,BorderLayout.EAST);workspace.add(bar,BorderLayout.NORTH);
        content=new Visual.Stage();pad(content,24,26,18,18);workspace.add(content,BorderLayout.CENTER);root.add(side,BorderLayout.WEST);root.add(workspace,BorderLayout.CENTER);setContentPane(root);getRootPane().setDefaultButton(null);showPage("Visão geral");revalidate();repaint();
    }
    private void navigationGroup(JPanel parent,String title,String... pages){
        JLabel heading=label(title,10,true,new Color(111,121,136));heading.setBorder(new EmptyBorder(13,12,9,0));heading.setAlignmentX(LEFT_ALIGNMENT);parent.add(heading);
        for(String page:pages){
            String name=page.equals("Visão geral")?(service.user().admin()?"Visão da gestão":"Atendimento"):page;
            JButton button=new Visual.SoftButton(name,false,true);button.setName("nav-"+page);button.setHorizontalAlignment(SwingConstants.LEFT);button.setFont(font(14,false));button.setForeground(MUTED);button.setBackground(NAVY);pad(button,14,12,14,10);button.setIcon(new Visual.Glyph(page,MUTED,19));button.setIconTextGap(12);button.setAlignmentX(LEFT_ALIGNMENT);button.setMaximumSize(new Dimension(Integer.MAX_VALUE,48));button.addActionListener(event->perform(()->showPage(page)));navigation.put(page,button);parent.add(button);parent.add(Box.createVerticalStrut(2));
        }
    }
    private void showPage(String page) { showPage(page,null); }
    /** Destaca o menu na hora, consulta o banco em segundo plano e só então troca o conteúdo (e executa {@code after}). */
    private void showPage(String page,Work after) {
        int request=++generation;
        for(var entry:navigation.entrySet()){boolean active=entry.getKey().equals(page);entry.getValue().setBackground(active?Visual.selection():NAVY);entry.getValue().setForeground(active?accent:new Color(180,185,193));entry.getValue().setIcon(new Visual.Glyph(entry.getKey(),active?accent:new Color(155,161,172),20));}
        boolean home=page.equals("Visão geral");
        this.<Object>background(()->home?WorkspaceDashboard.load(service):load(page),data->{
            if(request!=generation)return;
            content.remember();current=page;searchField=null;content.removeAll();boolean admin=service.user().admin();
            JPanel heading=panel(new BorderLayout(16,0),BG),titles=panel(new BorderLayout(0,8),BG);
            titles.add(label(home?(admin?"Visão da gestão":"Balcão de atendimento"):page,30,true,TEXT),BorderLayout.NORTH);
            Map<String,String> subtitles=Map.of("Visão geral",admin?"Contratos, recebimentos e estoque da academia.":"Consulte alunos e conduza o próximo atendimento.","Alunos","Pessoas, contatos e contratações em um só cadastro.","Modalidades","Atividades disponíveis para contratação.","Planos",admin?"Defina os valores e a duração de cada contratação.":"Consulte valores e duração para orientar o aluno.","Matrículas","Contratos, validade, situação e recebimento.","Produtos",admin?"Cadastro, disponibilidade e necessidade de reposição.":"Consulte os materiais e registre entradas ou saídas.","Movimentações","Entradas e saídas com justificativa, responsável e saldo resultante.","Usuários","Contas, perfis e acesso da equipe.");
            titles.add(label(subtitles.get(page),13,false,MUTED),BorderLayout.SOUTH);heading.add(titles,BorderLayout.CENTER);
            content.add(heading,BorderLayout.NORTH);if(home)dashboard((WorkspaceDashboard.Data)data);else listing(page,(Listing)data);content.revalidate();content.repaint();content.reveal();
            if(after!=null)after.run();
        },null);
    }
    private void dashboard(WorkspaceDashboard.Data data) {
        WorkspaceDashboard body=new WorkspaceDashboard(data,new WorkspaceDashboard.Actions(){
            public void open(String page){showPage(page);}
            public void newStudent(){showPage("Alunos",()->editForm(null));}
            public void newEnrollment(){perform(()->matriculaForm());}
            public void findStudent(String query){showPage("Alunos",()->{searchField.setText(query);searchField.requestFocusInWindow();});}
            public void student(Map<String,Object> record){studentDetails(record);}
        });
        content.add(pageScroll(body),BorderLayout.CENTER);
    }
    private JScrollPane pageScroll(JComponent body){JScrollPane scroll=new JScrollPane(body,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getViewport().setBackground(BG);Visual.styleScroll(scroll);return scroll;}
    private void selectCode(long id){for(int row=0;row<table.getRowCount();row++)if(table.getValueAt(row,0) instanceof Number number&&number.longValue()==id){table.setRowSelectionInterval(row,row);table.scrollRectToVisible(table.getCellRect(row,0,true));return;}}

    /** Por padrão as listas mostram só cadastros ativos; "Mostrar inativos" inclui os demais, depois dos ativos. */
    private String sql(String page) {
        boolean all=showInactive.contains(page);String only=all?"":" WHERE ativo=TRUE";
        return switch(page) {
            case "Alunos" -> "SELECT * FROM alunos"+only+" ORDER BY ativo DESC,nome";
            case "Modalidades" -> "SELECT * FROM modalidades"+only+" ORDER BY ativo DESC,nome";
            case "Planos" -> "SELECT p.*,m.nome AS modalidade FROM planos p JOIN modalidades m ON m.id=p.modalidade_id"+(all?"":" WHERE p.ativo=TRUE")+" ORDER BY p.ativo DESC,p.nome";
            // Situação e pagamento são calculados na consulta: VENCIDA (fim já passou) e PENDENTE/PARCIAL/PAGO pela soma das parcelas.
            case "Matrículas" -> "SELECT m.*,"+AcademiaService.SITUACAO_ATUAL+" AS situacao_atual,a.nome AS aluno,a.foto,p.nome AS plano,"+AcademiaService.STATUS_PAGAMENTO+" AS pagamento,COALESCE(pg.pago,0) AS pago,GREATEST(m.valor-COALESCE(pg.pago,0),0) AS saldo FROM matriculas m JOIN alunos a ON a.id=m.aluno_id JOIN planos p ON p.id=m.plano_id "+AcademiaService.PAGAMENTOS_POR_MATRICULA+" ORDER BY m.id DESC";
            case "Produtos" -> "SELECT *,CASE WHEN ativo=FALSE THEN 'INATIVO' WHEN saldo<=minimo THEN 'REPOR' ELSE 'OK' END AS alerta FROM produtos"+only+" ORDER BY ativo DESC,nome";
            case "Usuários" -> "SELECT id,nome,login,perfil,ativo,foto FROM usuarios"+only+" ORDER BY ativo DESC,nome";
            default -> throw new IllegalArgumentException("Tela desconhecida.");
        };
    }
    /** Executado em segundo plano: confere a sessão e lê no máximo LIMITE linhas (uma a mais indica que há outras). */
    private Listing load(String page) throws Exception {
        service.verificarSessao();Database db=service.database();
        if(page.equals("Movimentações")) {
            var result=service.movimentacoes(movProduto,movTipo,movDe,movAte,POR_PAGINA,movPagina*POR_PAGINA);
            return new Listing(result.rows(),result.total(),db.query("SELECT id,nome FROM produtos ORDER BY nome"));
        }
        List<Map<String,Object>> data=db.query(sql(page)+" LIMIT "+(LIMITE+1));long total=data.size();
        if(data.size()>LIMITE) data=new ArrayList<>(data.subList(0,LIMITE));
        var extra=page.equals("Modalidades")?db.query("SELECT m.id,m.nome,COUNT(p.id) AS planos FROM modalidades m LEFT JOIN planos p ON p.modalidade_id=m.id AND p.ativo=TRUE WHERE m.ativo=TRUE GROUP BY m.id,m.nome ORDER BY m.id LIMIT 3"):List.<Map<String,Object>>of();
        return new Listing(data,total,extra);
    }
    private void listing(String page,Listing loaded) {
        String[] keys,headers;
        switch(page) {
            case "Alunos" -> { keys=new String[]{"id","nome","cpf","telefone","email","ativo"}; headers=new String[]{"Código","Nome","CPF","Telefone","E-mail","Ativo"}; }
            case "Modalidades" -> {keys=new String[]{"id","nome","descricao","ativo"};headers=new String[]{"Código","Nome","Descrição","Ativo"};}
            case "Planos" -> {keys=new String[]{"id","nome","modalidade","valor","meses","ativo"};headers=new String[]{"Código","Plano","Modalidade","Valor total","Meses","Ativo"};}
            case "Matrículas" -> {keys=new String[]{"id","aluno","plano","inicio","fim","situacao_atual","pagamento","valor","saldo"};headers=new String[]{"Código","Aluno","Plano","Início","Fim","Situação","Pagamento","Valor","Saldo"};}
            case "Produtos" -> {keys=new String[]{"id","nome","unidade","preco","saldo","minimo","alerta"};headers=new String[]{"Código","Produto","Unidade","Preço","Saldo","Mínimo","Estoque"};}
            case "Movimentações" -> {keys=new String[]{"id","data","produto","tipo","quantidade","saldo_apos","motivo","responsavel"};headers=new String[]{"Código","Data","Produto","Tipo","Qtd.","Saldo após","Motivo","Responsável"};}
            case "Usuários" -> {keys=new String[]{"id","nome","login","perfil","ativo"};headers=new String[]{"Código","Nome","Login","Perfil","Ativo"};}
            default -> throw new IllegalArgumentException("Tela desconhecida.");
        }
        boolean movements=page.equals("Movimentações"),truncated=!movements && loaded.total()>LIMITE;
        rows=loaded.rows(); table=makeTable(rows,keys,headers);
        TableRowSorter<TableModel> sorter=new TableRowSorter<>(table.getModel()); table.setRowSorter(sorter);
        JPanel body=new Visual.PageBody(new BorderLayout(0,18)),pageShell=panel(new BorderLayout(0,14),BG); JPanel toolbar=panel(new BorderLayout(14,0),BG);
        String total=movements?loaded.total()+" registros":truncated?"Primeiros "+LIMITE+" registros":rows.size()+" registros";
        JLabel counter=new Visual.Badge(total,MUTED,LINE);
        Visual.Search searchBox=new Visual.Search(page.equals("Alunos")?"Buscar nome, CPF ou contato":"Buscar em "+page.toLowerCase(Locale.ROOT)); JTextField search=searchBox.field;searchField=search;search.setToolTipText(movements?"Buscar nesta página da lista":"Buscar por qualquer informação da lista");
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e){filter();} public void removeUpdate(javax.swing.event.DocumentEvent e){filter();} public void changedUpdate(javax.swing.event.DocumentEvent e){filter();}
            void filter(){String query=fold(search.getText());sorter.setRowFilter(new RowFilter<TableModel,Integer>(){@Override public boolean include(Entry<? extends TableModel,? extends Integer> entry){for(int column=0;column<entry.getValueCount();column++)if(fold(format(entry.getValue(column))).contains(query)||fold(cpf(entry.getValue(column))).contains(query)||(entry.getValue(column)!=null&&fold(entry.getValue(column).toString()).contains(query)))return true;return false;}});counter.setText(query.isEmpty()?total:table.getRowCount()+" de "+rows.size()+" registros");}
        });
        JPanel searchWrap=panel(new BorderLayout(),BG);searchWrap.add(searchBox,BorderLayout.CENTER);searchWrap.setPreferredSize(new Dimension(250,46));toolbar.add(searchWrap,BorderLayout.CENTER);
        JPanel actions=panel(new FlowLayout(FlowLayout.RIGHT,8,0),BG);
        if(page.equals("Matrículas")) {
            actions.add(button("Nova matrícula",true,()->matriculaForm()));
            actions.add(button("Situação",false,()->statusForm()));
            actions.add(button("Pagamento",false,()->paymentForm()));
            actions.add(button("Renovar",false,()->renewForm()));
        } else if(movements) actions.add(button("Registrar movimento",true,()->movementForm(null)));
        else {
            boolean editable=page.equals("Alunos") || service.user().admin();
            if(editable) {String create=Map.of("Alunos","Adicionar aluno","Modalidades","Nova modalidade","Planos","Criar plano","Produtos","Novo produto","Usuários","Adicionar usuário").get(page); actions.add(button(create,true,()->editForm(null))); actions.add(button("Editar",false,()->editForm(selected()))); }
            // Remover fica visível só para quem tem permissão e habilitado apenas com uma linha selecionada.
            if(editable) {
                JButton remove=button("Remover",false,()->removeSelected());remove.setEnabled(false);actions.add(remove);
                JButton reactivate=showInactive.contains(page)?button("Reativar",false,()->reactivateSelected()):null;
                if(reactivate!=null){reactivate.setEnabled(false);actions.add(reactivate);}
                table.getSelectionModel().addListSelectionListener(e->{int row=table.getSelectedRow();remove.setEnabled(row>=0);if(reactivate!=null)reactivate.setEnabled(row>=0&&!Boolean.TRUE.equals(rows.get(table.convertRowIndexToModel(row)).get("ativo")));});
            }
            if(page.equals("Produtos")) actions.add(button("Movimentar",true,()->movementForm(selected())));
        }
        toolbar.add(actions,BorderLayout.EAST);
        JPanel north=panel(new BorderLayout(0,12),BG);north.add(toolbar,BorderLayout.NORTH);if(movements)north.add(movementFilters(loaded.extra()),BorderLayout.SOUTH);pageShell.add(north,BorderLayout.NORTH);
        JPanel records=new Visual.RoundPanel(new BorderLayout(0,14),Visual.SURFACE,18,true);pad(records,18,20,16,20);JPanel tableHeading=panel(new BorderLayout(),Visual.SURFACE);tableHeading.add(label(movements?"Registro de entradas e saídas":"Lista de "+page.toLowerCase(Locale.ROOT),17,true,TEXT),BorderLayout.WEST);JPanel headingEnd=panel(new FlowLayout(FlowLayout.RIGHT,14,0),Visual.SURFACE);if(ENTIDADES.containsKey(page)){JCheckBox inactive=new JCheckBox("Mostrar inativos",showInactive.contains(page));inactive.setName("mostrar-inativos");inactive.setBackground(Visual.SURFACE);inactive.setForeground(MUTED);inactive.setFont(font(12,true));inactive.setFocusPainted(false);inactive.addActionListener(e->{if(inactive.isSelected())showInactive.add(page);else showInactive.remove(page);showPage(page);});headingEnd.add(inactive);}headingEnd.add(counter);tableHeading.add(headingEnd,BorderLayout.EAST);records.add(tableHeading,BorderLayout.NORTH);records.add(scroll(table),BorderLayout.CENTER);
        if(page.equals("Modalidades")||page.equals("Alunos")||page.equals("Usuários")) {JPanel center=panel(new BorderLayout(0,20),BG);center.add(page.equals("Modalidades")?modalityGallery(loaded.extra()):peopleGallery(page),BorderLayout.NORTH);center.add(records,BorderLayout.CENTER);body.add(center,BorderLayout.CENTER);}else body.add(records,BorderLayout.CENTER);
        JPanel below=panel(new BorderLayout(),BG); below.add(label(truncated?"Lista limitada a "+LIMITE+" linhas. Use a busca para localizar um registro.":"Selecione uma linha para consultar ou usar as ações",12,false,MUTED),BorderLayout.WEST);
        if(page.equals("Alunos")||page.equals("Usuários")){
            JPanel personActions=panel(new FlowLayout(FlowLayout.RIGHT,8,0),BG);
            personActions.add(button("Ampliar retrato",false,()->portraitPreview(selected())));
            if(page.equals("Alunos"))personActions.add(button("Ver ficha",false,()->studentDetails()));
            if(page.equals("Usuários"))personActions.add(button("Auditoria",false,()->auditLog()));
            below.add(personActions,BorderLayout.EAST);
        }
        if(page.equals("Matrículas")) {
            JPanel enrollmentActions=panel(new FlowLayout(FlowLayout.RIGHT,14,0),BG);enrollmentActions.add(label("VENCIDA: validade encerrada, aguardando renovação",12,true,Visual.ALERT));enrollmentActions.add(button("Histórico",false,()->history()));below.add(enrollmentActions,BorderLayout.EAST);
        }
        if(page.equals("Produtos")) below.add(label("Linhas destacadas (REPOR): saldo menor ou igual ao mínimo",13,true,accent),BorderLayout.EAST);
        if(movements) below.add(pagination(loaded.total()),BorderLayout.EAST);
        pageShell.add(below,BorderLayout.SOUTH);pageShell.add(pageScroll(body),BorderLayout.CENTER);content.add(pageShell,BorderLayout.CENTER);
    }
    /** Filtros por produto, tipo e período; aplicados na consulta ao banco, antes da paginação. */
    private JPanel movementFilters(List<Map<String,Object>> products) {
        JPanel bar=panel(new FlowLayout(FlowLayout.LEFT,12,0),BG);
        JComboBox<Choice> produto=new JComboBox<>();produto.addItem(new Choice(0,"Todos os produtos"));
        for(var p:products){Choice choice=new Choice(AcademiaService.number(p,"id"),p.get("nome").toString());produto.addItem(choice);if(movProduto!=null&&movProduto==choice.id())produto.setSelectedItem(choice);}
        Visual.styleCombo(produto);produto.setPreferredSize(new Dimension(240,46));
        JComboBox<String> tipo=new JComboBox<>(new String[]{"Todos","ENTRADA","SAIDA"});tipo.setSelectedItem(movTipo==null?"Todos":movTipo);Visual.styleCombo(tipo);tipo.setPreferredSize(new Dimension(140,46));
        JTextField de=new JTextField(movDe==null?"":movDe.format(DIA)),ate=new JTextField(movAte==null?"":movAte.format(DIA));styleInput(de);styleInput(ate);de.setPreferredSize(new Dimension(140,46));ate.setPreferredSize(new Dimension(140,46));
        de.setToolTipText("dd/mm/aaaa (em branco: sem limite)");ate.setToolTipText(de.getToolTipText());
        bar.add(labeled("Produto",produto));bar.add(labeled("Tipo",tipo));bar.add(labeled("De (dd/mm/aaaa)",de));bar.add(labeled("Até (dd/mm/aaaa)",ate));
        JPanel buttons=panel(new FlowLayout(FlowLayout.LEFT,8,0),BG);pad(buttons,22,0,0,0);
        buttons.add(button("Filtrar",true,()->{
            LocalDate from=day(de,"inicial"),to=day(ate,"final");
            if(from!=null&&to!=null&&to.isBefore(from))throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial.");
            Choice chosen=(Choice)produto.getSelectedItem();movProduto=chosen==null||chosen.id()==0?null:chosen.id();movTipo=tipo.getSelectedItem().equals("Todos")?null:tipo.getSelectedItem().toString();movDe=from;movAte=to;movPagina=0;showPage(current);
        }));
        buttons.add(button("Limpar filtros",false,()->{movProduto=null;movTipo=null;movDe=null;movAte=null;movPagina=0;showPage(current);}));
        bar.add(buttons);return bar;
    }
    private static LocalDate day(JTextField field,String which) {
        String text=field.getText().trim();if(text.isEmpty())return null;
        try{return LocalDate.parse(text,DIA);}catch(Exception e){throw new IllegalArgumentException("Informe a data "+which+" no formato dd/mm/aaaa ou deixe em branco.");}
    }
    private JPanel pagination(long total) {
        int pages=(int)Math.max(1,(total+POR_PAGINA-1)/POR_PAGINA);
        JPanel bar=panel(new FlowLayout(FlowLayout.RIGHT,10,0),BG);
        JButton previous=button("Anterior",false,()->{movPagina=Math.max(0,movPagina-1);showPage(current);});previous.setEnabled(movPagina>0);
        JButton next=button("Próxima",false,()->{movPagina++;showPage(current);});next.setEnabled(movPagina+1<pages);
        bar.add(previous);bar.add(label("Página "+Math.min(movPagina+1,pages)+" de "+pages,12,true,MUTED));bar.add(next);return bar;
    }
    private JPanel modalityGallery(List<Map<String,Object>> modalities){
        JPanel gallery=panel(new GridLayout(1,3,16,0),BG);gallery.setPreferredSize(new Dimension(0,235));
        for(var modality:modalities){
            String name=modality.get("nome").toString(),key=name.toLowerCase(Locale.ROOT);long id=AcademiaService.number(modality,"id"),count=AcademiaService.number(modality,"planos");
            String asset=key.contains("muscula")?"musculacao":key.contains("funcional")?"funcional":key.contains("pilates")?"pilates":"abertura";
            JPanel tile=new Visual.RoundPanel(new BorderLayout(0,10),Visual.SURFACE,16,true);pad(tile,10,10,12,10);tile.add(new Visual.PhotoPanel(asset,10,0,.5,.5),BorderLayout.CENTER);
            JPanel caption=panel(new BorderLayout(0,5),Visual.SURFACE);pad(caption,2,6,0,6);caption.add(label(name,18,true,TEXT),BorderLayout.NORTH);caption.add(label(count==1?"1 plano disponível":count+" planos disponíveis",11,false,MUTED),BorderLayout.CENTER);tile.add(caption,BorderLayout.SOUTH);
            tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));tile.setToolTipText("Selecionar "+name);tile.addMouseListener(new java.awt.event.MouseAdapter(){@Override public void mouseClicked(java.awt.event.MouseEvent e){selectCode(id);}});gallery.add(tile);
        }
        while(gallery.getComponentCount()<3)gallery.add(panel(new BorderLayout(),BG));return gallery;
    }
    private JPanel peopleGallery(String page){
        JPanel gallery=new JPanel(new GridLayout(1,2,16,0)){
            @Override public Dimension getPreferredSize(){return new Dimension(0,AcademiaFrame.this.getHeight()<850?132:164);}
        };gallery.setBackground(BG);int count=Math.min(2,rows.size());
        for(int i=0;i<count;i++){
            var person=rows.get(i);JPanel card=new Visual.RoundPanel(new BorderLayout(14,0),Visual.SURFACE,16,true);pad(card,18,16,18,16);card.add(new Visual.Portrait((String)person.get("foto"),112),BorderLayout.WEST);
            JPanel info=panel(new GridBagLayout(),Visual.SURFACE);info.setOpaque(false);GridBagConstraints line=new GridBagConstraints();line.gridx=0;line.gridy=0;line.weightx=1;line.fill=GridBagConstraints.HORIZONTAL;line.anchor=GridBagConstraints.WEST;line.insets=new Insets(0,0,10,0);
            JLabel name=label(person.get("nome").toString(),14,true,TEXT);name.setToolTipText(person.get("nome").toString());info.add(name,line);
            line.gridy++;info.add(label(page.equals("Alunos")?"Aluno da academia":format(person.get("perfil")).equals("ADMIN")?"Administrador":"Atendente",11,false,MUTED),line);
            line.gridy++;line.insets=new Insets(0,0,0,0);info.add(label(Boolean.TRUE.equals(person.get("ativo"))?"Cadastro ativo":"Cadastro inativo",10,true,Boolean.TRUE.equals(person.get("ativo"))?Visual.GOOD:MUTED),line);card.add(info,BorderLayout.CENTER);
            long id=AcademiaService.number(person,"id");card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));card.setToolTipText("Selecionar "+person.get("nome")+". Clique duas vezes para ampliar o retrato.");
            card.addMouseListener(new java.awt.event.MouseAdapter(){@Override public void mouseClicked(java.awt.event.MouseEvent e){selectCode(id);if(e.getClickCount()==2)portraitPreview(person);}});gallery.add(card);
        }
        return gallery;
    }

    private JTable makeTable(List<Map<String,Object>> data,String[] keys,String[] headers) {
        DefaultTableModel model=new DefaultTableModel(headers,0) {
            @Override public boolean isCellEditable(int row,int column){return false;}
            @Override public Class<?> getColumnClass(int column){for(var record:data){Object value=record.get(keys[column]);if(value!=null)return value.getClass();}return Object.class;}
        };
        for(var row:data) {Object[] cells=new Object[keys.length]; for(int i=0;i<keys.length;i++) cells[i]=row.get(keys[i]); model.addRow(cells);}
        boolean withPortrait=Arrays.stream(headers).anyMatch(h->h.equals("Aluno")||(h.equals("Nome")&&(current.equals("Alunos")||current.equals("Usuários"))));
        JTable t=new JTable(model){
            @Override public boolean getScrollableTracksViewportWidth(){int minimum=0;for(int column=0;column<getColumnCount();column++)minimum+=getColumnModel().getColumn(column).getMinWidth();return getParent()!=null&&getParent().getWidth()>=minimum;}
            @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);if(getRowCount()==0&&getHeight()>135){Graphics2D g=Visual.quality(graphics);g.setFont(font(15,true));g.setColor(MUTED);String message="Nenhum registro encontrado";g.drawString(message,(getWidth()-g.getFontMetrics().stringWidth(message))/2,94);g.setFont(font(12,false));message="Revise a busca ou cadastre um novo registro.";g.drawString(message,(getWidth()-g.getFontMetrics().stringWidth(message))/2,120);g.dispose();}}
        }; t.setRowHeight(withPortrait?76:64); t.setFont(font(14,false)); t.setForeground(TEXT); t.setSelectionBackground(Visual.selection()); t.setSelectionForeground(TEXT); t.setGridColor(LINE);t.setShowGrid(false);t.setIntercellSpacing(new Dimension(0,0));t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); t.setFillsViewportHeight(true); t.setBackground(Visual.SURFACE);
        t.getTableHeader().setFont(font(11,true));t.getTableHeader().setForeground(MUTED);t.getTableHeader().setBackground(Visual.INPUT);t.getTableHeader().setPreferredSize(new Dimension(0,42));t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer(){@Override public Component getTableCellRendererComponent(JTable tb,Object v,boolean selected,boolean focus,int r,int col){super.getTableCellRendererComponent(tb,v,selected,focus,r,col);setText(v.toString().toUpperCase(Locale.ROOT));setFont(font(10,true));setForeground(MUTED);setBackground(Visual.INPUT);setBorder(new EmptyBorder(8,12,8,12));return this;}});
        Color alertRow=Visual.mix(Visual.SURFACE,Visual.ALERT,.10f);
        TableCellRenderer renderer=new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb,Object v,boolean selected,boolean focus,int r,int c) {
                super.getTableCellRendererComponent(tb,v,selected,focus,r,c);setFont(font(14,false));setText(format(v));setBorder(new MatteBorder(0,0,1,0,Visual.LINE));setToolTipText(v==null?null:format(v));
                // Produto no estoque mínimo e matrícula vencida ficam com a linha inteira destacada.
                var record=data.get(tb.convertRowIndexToModel(r));boolean alert="REPOR".equals(record.get("alerta"))||"VENCIDA".equals(record.get("situacao_atual"));
                Color rowBg=selected?Visual.selection():alert?alertRow:Visual.SURFACE;setBackground(rowBg);setForeground(TEXT);String text=format(v);String header=tb.getColumnName(c);if(header.equals("CPF")){text=cpf(v);setText(text);}
                Map<String,String> status=Map.ofEntries(Map.entry("ATIVA","Ativa"),Map.entry("VENCIDA","Vencida"),Map.entry("TRANCADA","Trancada"),Map.entry("CANCELADA","Cancelada"),Map.entry("PAGO","Pago"),Map.entry("PARCIAL","Parcial"),Map.entry("PENDENTE","Pendente"),Map.entry("REPOR","Repor"),Map.entry("OK","Em dia"),Map.entry("INATIVO","Inativo"),Map.entry("ENTRADA","Entrada"),Map.entry("SAIDA","Saída"),Map.entry("ADMIN","Admin"),Map.entry("ATENDENTE","Atendente"));
                if(status.containsKey(text)||header.equals("Ativo")){
                    boolean ok=Set.of("ATIVA","PAGO","OK","ENTRADA","Sim").contains(text),bad=Set.of("CANCELADA","VENCIDA","INATIVO","SAIDA","Não").contains(text);
                    Color fg=ok?Visual.GOOD:bad?Visual.ALERT:accent,bg=ok?new Color(30,55,45):bad?new Color(60,36,40):Visual.selection();
                    JPanel wrap=panel(new FlowLayout(FlowLayout.LEFT,12,18),rowBg);wrap.setBorder(new MatteBorder(0,0,1,0,Visual.LINE));wrap.add(new Visual.Badge(status.getOrDefault(text,text),fg,bg));wrap.setToolTipText(text);return wrap;
                }
                if((header.equals("Nome") && (current.equals("Alunos")||current.equals("Usuários")))||header.equals("Aluno")){
                    JPanel wrap=panel(new BorderLayout(12,0),rowBg);wrap.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,Visual.LINE),new EmptyBorder(10,12,10,8)));wrap.add(new Visual.Portrait((String)record.get("foto"),52),BorderLayout.WEST);wrap.add(label(text,13,true,TEXT),BorderLayout.CENTER);wrap.setToolTipText(text);return wrap;
                }
                setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,Visual.LINE),new EmptyBorder(0,12,0,12)));return this;
            }
        }; t.setDefaultRenderer(Object.class,renderer);t.setDefaultRenderer(Number.class,renderer);t.setDefaultRenderer(Boolean.class,renderer);t.setDefaultRenderer(java.util.Date.class,renderer);
        for(int i=0;i<headers.length;i++){String h=headers[i];boolean money=h.contains("Valor")||h.equals("Preço")||(h.equals("Saldo")&&Arrays.asList(headers).contains("Pagamento"));TableColumn col=t.getColumnModel().getColumn(i);col.setPreferredWidth(h.equals("Código")?74:h.equals("Aluno")||h.equals("Nome")||h.equals("Produto")?210:h.equals("Motivo")?240:h.equals("Alterações")?420:h.equals("Plano")?190:h.equals("E-mail")?210:h.equals("Data")?170:120);col.setMinWidth(h.equals("Código")?70:h.equals("Aluno")||h.equals("Nome")?180:h.equals("Descrição")||h.equals("Motivo")?250:h.equals("Alterações")?320:h.equals("E-mail")?210:h.equals("CPF")||h.equals("Telefone")?145:h.equals("Plano")?160:h.equals("Início")||h.equals("Fim")?110:h.equals("Data")?165:money?125:h.equals("Pagamento")||h.equals("Situação")?105:80);}
        t.setPreferredScrollableViewportSize(new Dimension(720,Math.min(380,Math.max(190,data.size()*t.getRowHeight()))));
        return t;
    }
    private static String fold(String value){return java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);}
    /** Só a coluna CPF é formatada como 000.000.000-00; um telefone de 11 números não é confundido com CPF. */
    private static String cpf(Object v){String text=v==null?"":v.toString();return AcademiaService.validCpf(text)?text.substring(0,3)+"."+text.substring(3,6)+"."+text.substring(6,9)+"-"+text.substring(9):text;}
    private String format(Object v) {
        if(v==null)return "";
        if(v instanceof BigDecimal b)return currency.format(b);
        if(v instanceof java.sql.Date d)return d.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        if(v instanceof java.sql.Timestamp d)return d.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        if(v instanceof Boolean b)return b?"Sim":"Não";
        return v.toString();
    }
    private JScrollPane scroll(JTable t){JScrollPane sc=new JScrollPane(t);sc.setBorder(BorderFactory.createEmptyBorder());sc.getViewport().setBackground(Visual.SURFACE);Visual.styleScroll(sc);return sc;}
    private Map<String,Object> selected() {
        int row=table.getSelectedRow(); if(row<0)throw new IllegalArgumentException("Selecione um registro na lista.");return rows.get(table.convertRowIndexToModel(row));
    }
    private void styleInput(JTextField input) {input.setFont(font(15,false));input.setBackground(Visual.INPUT);input.setForeground(TEXT);input.setCaretColor(accent);input.setPreferredSize(new Dimension(250,46));input.setBorder(new Visual.InputBorder());}
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
        JPasswordField password(String name) {JPasswordField p=new JPasswordField();styleInput(p);add(name,p);return p;}
        JComboBox<String> options(String name,String[] choices,String selected) {JComboBox<String> c=new JComboBox<>(choices);Visual.styleCombo(c);if(selected!=null)c.setSelectedItem(selected);add(name,c);return c;}
        JCheckBox check(String name,boolean selected){JCheckBox c=new JCheckBox("Sim",selected);c.setBackground(BG);c.setForeground(TEXT);add(name,c);return c;}
        JComboBox<Choice> choices(String name,String sql,Object selected,Object... args) throws Exception {
            List<Map<String,Object>> data=service.database().query(sql,args);JComboBox<Choice> c=new JComboBox<>();
            for(var r:data){String title=r.get("nome").toString();if(r.containsKey("valor"))title+=" ("+format(r.get("valor"))+")";Choice choice=new Choice(AcademiaService.number(r,"id"),title);c.addItem(choice);if(selected instanceof Number n && n.longValue()==choice.id())c.setSelectedItem(choice);}
            Visual.styleCombo(c);add(name,c);return c;
        }
        /** A gravação roda em segundo plano; o botão fica desabilitado até o banco responder, evitando envio duplicado. */
        void show(Work save) {
            JPanel actions=panel(new FlowLayout(FlowLayout.RIGHT,8,0),BG);actions.add(button("Cancelar",false,()->dialog.dispose()));
            JButton[] submit=new JButton[1];
            submit[0]=button("Salvar",true,()->{
                submit[0].setEnabled(false);submit[0].setText("Salvando…");
                background(()->{save.run();return null;},done->{dialog.dispose();showPage(current);saved();},()->{submit[0].setEnabled(true);submit[0].setText("Salvar");});
            });
            actions.add(submit[0]);dialog.getContentPane().add(actions,BorderLayout.SOUTH);dialog.getRootPane().setDefaultButton(submit[0]);
            dialog.pack();int maxHeight=Math.max(540,getGraphicsConfiguration().getBounds().height-90);dialog.setSize(Math.max(570,dialog.getWidth()),Math.min(maxHeight,dialog.getHeight()));dialog.setLocationRelativeTo(AcademiaFrame.this);
            if(dialogCapture!=null){String name=dialogCapture;dialogCapture=null;try{dialog.validate();captureComponent(dialog.getContentPane(),name);}catch(Exception e){throw new IllegalStateException(e);}finally{dialog.dispose();}return;}
            dialog.setVisible(true);
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
                JTextField cpf=f.text("CPF (11 números, pode ser fictício)",val(r,"cpf")),telefone=f.text("Telefone (pelo menos 8 números)",val(r,"telefone")),email=f.text("E-mail (precisa conter @)",val(r,"email"));JCheckBox ativo=f.check("Aluno ativo",r==null||Boolean.TRUE.equals(val(r,"ativo")));
                PortraitPicker photo=new PortraitPicker((String)val(r,"foto"));f.add("Foto do cadastro",photo);
                f.show(()->service.saveAluno(id,nome.getText(),cpf.getText(),telefone.getText(),email.getText(),ativo.isSelected(),photo.selected));
            }
            case "Modalidades" -> {JTextField descricao=f.text("Descrição",val(r,"descricao"));f.show(()->service.saveModalidade(id,nome.getText(),descricao.getText()));}
            case "Planos" -> {
                JComboBox<Choice> m=f.choices("Modalidade","SELECT id,nome FROM modalidades WHERE ativo=TRUE OR id=? ORDER BY nome",val(r,"modalidade_id"),val(r,"modalidade_id"));JTextField valor=f.text("Valor total do plano (R$)",val(r,"valor")),meses=f.text("Duração em meses",r==null?1:val(r,"meses"));
                f.show(()->service.savePlano(id,nome.getText(),chosen(m),decimal(valor),integer(meses)));
            }
            case "Produtos" -> {
                JTextField unidade=f.text("Unidade de medida",r==null?"unidade":val(r,"unidade")),preco=f.text("Preço unitário (R$)",val(r,"preco")),minimo=f.text("Estoque mínimo",r==null?0:val(r,"minimo"));f.add("Saldo inicial",label("Registre uma entrada após salvar o produto.",13,false,MUTED));
                f.show(()->service.saveProduto(id,nome.getText(),unidade.getText(),decimal(preco),integer(minimo)));
            }
            case "Usuários" -> {
                JTextField login=f.text("Login",val(r,"login"));JPasswordField senha=f.password(id==null?"Senha (mínimo de 8 caracteres)":"Nova senha (em branco mantém a atual)");
                JComboBox<String> perfil=f.options("Perfil",new String[]{"ATENDENTE","ADMIN"},r==null?"ATENDENTE":val(r,"perfil").toString());JCheckBox ativo=f.check("Conta ativa",r==null||Boolean.TRUE.equals(val(r,"ativo")));
                PortraitPicker photo=new PortraitPicker((String)val(r,"foto"));f.add("Foto do cadastro",photo);
                f.show(()->service.saveUsuario(id,nome.getText(),login.getText(),new String(senha.getPassword()),perfil.getSelectedItem().toString(),ativo.isSelected(),photo.selected));
            }
        }
    }
    /**
     * Primeiro pergunta ao serviço o que acontecerá (excluir, inativar ou bloqueio), mostra isso na confirmação
     * e só então remove. Bloqueios aparecem como mensagem, sem abrir a confirmação.
     */
    private void removeSelected() {
        var r=selected();long id=AcademiaService.number(r,"id");String page=current,entity=ENTIDADES.get(page),name=r.get("nome").toString();
        background(()->service.previaRemocao(entity,id),plan->{
            boolean delete=plan.equals("EXCLUIR");
            String question=delete?"Excluir definitivamente \""+name+"\"?\nO cadastro não possui histórico e será apagado do banco.":"\""+name+"\" possui histórico e será INATIVADO em vez de excluído.\nEle deixa de aparecer nas listas e nas novas operações, mas o histórico é preservado.\nContinuar?";
            if(JOptionPane.showConfirmDialog(this,question,delete?"Excluir cadastro":"Inativar cadastro",JOptionPane.OK_CANCEL_OPTION,delete?JOptionPane.WARNING_MESSAGE:JOptionPane.QUESTION_MESSAGE)!=JOptionPane.OK_OPTION)return;
            background(()->service.remover(entity,id),result->{
                showPage(page);
                if(result.excluido())saved(result.mensagem());else JOptionPane.showMessageDialog(this,result.mensagem(),"Cadastro inativado",JOptionPane.INFORMATION_MESSAGE);
            },null);
        },null);
    }
    private void reactivateSelected() {
        var r=selected();long id=AcademiaService.number(r,"id");String page=current;
        background(()->{service.reativar(ENTIDADES.get(page),id);return null;},done->{showPage(page,()->selectCode(id));saved("Cadastro reativado");},null);
    }
    private void passwordForm() {
        Form f=new Form("Alterar minha senha");JPasswordField atual=f.password("Senha atual"),nova=f.password("Nova senha (mínimo de 8 caracteres)"),confirma=f.password("Repita a nova senha");
        f.show(()->{String n=new String(nova.getPassword());if(!n.equals(new String(confirma.getPassword())))throw new IllegalArgumentException("A confirmação não corresponde à nova senha.");service.alterarSenha(new String(atual.getPassword()),n);});
    }
    private void matriculaForm() throws Exception {
        Form f=new Form("Nova matrícula");JComboBox<Choice> aluno=f.choices("Aluno ativo","SELECT id,nome FROM alunos WHERE ativo=TRUE ORDER BY nome",null);
        JComboBox<Choice> plano=f.choices("Plano","SELECT p.id,p.nome,p.valor FROM planos p JOIN modalidades m ON m.id=p.modalidade_id WHERE p.ativo=TRUE AND m.ativo=TRUE ORDER BY p.nome",null);
        JTextField inicio=f.text("Início (dd/mm/aaaa)",LocalDate.now().format(DIA));
        f.add("Contratação",label("O valor do plano pode ser pago de uma vez ou em parcelas.",13,false,MUTED));
        f.show(()-> {LocalDate data;try{data=LocalDate.parse(inicio.getText().trim(),DIA);}catch(Exception e){throw new IllegalArgumentException("Informe uma data válida no formato dd/mm/aaaa.");} service.matricular(chosen(aluno),chosen(plano),data);});
    }
    private void statusForm() {
        var r=selected();Form f=new Form("Situação da matrícula #"+r.get("id"));JComboBox<String> status=f.options("Nova situação",new String[]{"ATIVA","TRANCADA","CANCELADA"},r.get("situacao").toString());JTextField motivo=f.text("Motivo da alteração",null);
        if("VENCIDA".equals(r.get("situacao_atual")))f.add("Validade",label("Contrato vencido: use Renovar para criar o novo período.",13,true,Visual.ALERT));
        f.show(()->service.situacao(AcademiaService.number(r,"id"),status.getSelectedItem().toString(),motivo.getText()));
    }
    private void paymentForm() {
        var r=selected();long id=AcademiaService.number(r,"id");BigDecimal saldo=new BigDecimal(r.get("saldo").toString());
        if("CANCELADA".equals(r.get("situacao")))throw new IllegalArgumentException("Matrícula cancelada não recebe pagamentos.");
        if(saldo.signum()<=0)throw new IllegalArgumentException("O pagamento desta matrícula já foi quitado.");
        Form f=new Form("Registrar pagamento");f.add("Aluno",label(r.get("aluno").toString(),16,true,TEXT));
        JPanel summary=panel(new GridLayout(1,3,18,0),BG);
        for(String[] item:new String[][]{{"Valor contratado","valor"},{"Já pago","pago"},{"Saldo devedor","saldo"}}){JPanel cell=panel(new GridLayout(0,1,0,4),BG);cell.add(label(item[0],12,false,MUTED));cell.add(label(format(new BigDecimal(r.get(item[1]).toString())),20,true,item[1].equals("saldo")?accent:TEXT));summary.add(cell);}
        f.add("Situação financeira",summary);
        JTextField valor=f.text("Valor deste pagamento (R$)",saldo.toPlainString().replace('.',','));
        f.add("Parcelas",label("Informe um valor menor que o saldo para registrar uma parcela.",13,false,MUTED));
        JComboBox<String> forma=f.options("Forma de pagamento",new String[]{"PIX","DINHEIRO","CARTAO"},"PIX");
        f.show(()->service.pagar(id,decimal(valor),forma.getSelectedItem().toString()));
    }
    /** Encerra a matrícula vencida e abre outra no mesmo plano a partir de hoje. */
    private void renewForm() {
        var r=selected();long id=AcademiaService.number(r,"id");
        if(!"VENCIDA".equals(r.get("situacao_atual")))throw new IllegalArgumentException("Somente matrículas vencidas podem ser renovadas. Selecione uma linha com a situação Vencida.");
        String question="Renovar a matrícula #"+id+" de "+r.get("aluno")+" no plano "+r.get("plano")+"?\nA matrícula atual será encerrada e uma nova começará hoje, com o valor atual do plano.";
        if(JOptionPane.showConfirmDialog(this,question,"Renovar matrícula",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE)!=JOptionPane.OK_OPTION)return;
        background(()->service.renovar(id),nova->{showPage("Matrículas",()->selectCode(nova));saved("Matrícula renovada: nova matrícula #"+nova);},null);
    }
    private void movementForm(Map<String,Object> r) throws Exception {
        Form f=new Form("Registrar movimentação");JComboBox<Choice> produto=f.choices("Produto e saldo atual","SELECT id,CONCAT(nome,' (saldo: ',saldo,')') AS nome FROM produtos WHERE ativo=TRUE ORDER BY nome",val(r,"id"));
        JComboBox<String> tipo=f.options("Tipo",new String[]{"ENTRADA","SAIDA"},"ENTRADA");JTextField qtd=f.text("Quantidade inteira",1),motivo=f.text("Motivo",null);
        f.show(()->service.movimentar(chosen(produto),tipo.getSelectedItem().toString(),integer(qtd),motivo.getText()));
    }
    private void history() {
        var r=selected();long id=AcademiaService.number(r,"id");
        background(()->{service.verificarSessao();return service.database().query("SELECT h.data,h.situacao,h.motivo,u.nome AS responsavel FROM historico_matriculas h JOIN usuarios u ON u.id=h.usuario_id WHERE h.matricula_id=? ORDER BY h.id",id);},hist->{
            JDialog d=new JDialog(this,"Histórico da matrícula #"+id,true);JPanel p=panel(new BorderLayout(0,14),BG);pad(p,22,22,22,22);p.add(label("Alterações de situação",24,true,TEXT),BorderLayout.NORTH);p.add(scroll(makeTable(hist,new String[]{"data","situacao","motivo","responsavel"},new String[]{"Data","Situação","Motivo","Responsável"})),BorderLayout.CENTER);d.setContentPane(p);d.setSize(900,430);d.setLocationRelativeTo(this);d.setVisible(true);
        },null);
    }
    /** Quem alterou, quando e o que mudou em alunos, modalidades, planos, produtos e usuários. */
    private void auditLog() {
        background(()->service.auditoria(null,null,200),log->{
            JDialog d=new JDialog(this,"Auditoria de alterações",true);JPanel p=panel(new BorderLayout(0,14),BG);pad(p,22,22,22,22);
            JPanel heading=panel(new BorderLayout(0,6),BG);heading.add(label("Auditoria de alterações",24,true,TEXT),BorderLayout.NORTH);heading.add(label("Últimas 200 alterações: valor anterior -> valor novo.",13,false,MUTED),BorderLayout.SOUTH);p.add(heading,BorderLayout.NORTH);
            p.add(scroll(makeTable(log,new String[]{"data","responsavel","entidade","registro_id","acao","detalhes"},new String[]{"Data","Responsável","Cadastro","Registro","Ação","Alterações"})),BorderLayout.CENTER);
            d.setContentPane(p);d.setSize(Math.min(1180,getWidth()-40),560);d.setLocationRelativeTo(this);d.setVisible(true);
        },null);
    }
    private final class PortraitPicker extends JPanel{
        String selected;final Map<String,JButton> options=new LinkedHashMap<>();final JPanel preview=panel(new BorderLayout(0,8),Visual.INPUT);
        PortraitPicker(String initial){
            super(new BorderLayout(18,0));setBackground(BG);selected=initial;String[] assets={"retratos/ana","retratos/bruno","retratos/equipe","retratos/recepcao",""};
            JPanel choices=panel(new GridLayout(1,5,8,0),BG);
            for(int i=0;i<assets.length;i++){
                String asset=assets[i];JButton choice=new JButton();choice.setName("retrato-"+i);choice.setPreferredSize(new Dimension(78,88));choice.setLayout(new BorderLayout());choice.setBackground(Visual.INPUT);choice.setContentAreaFilled(false);choice.setFocusPainted(false);choice.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));choice.setToolTipText(asset.isEmpty()?"Sem retrato":"Retrato "+(i+1));choice.getAccessibleContext().setAccessibleName(choice.getToolTipText());choice.add(new Visual.Portrait(asset.isEmpty()?null:asset,70),BorderLayout.CENTER);choice.addActionListener(e->{selected=asset.isEmpty()?null:asset;refresh();});options.put(asset,choice);choices.add(choice);
            }
            JPanel left=panel(new BorderLayout(0,10),BG);left.add(label("Escolha uma imagem para o cadastro",12,false,MUTED),BorderLayout.NORTH);left.add(choices,BorderLayout.CENTER);add(left,BorderLayout.CENTER);
            pad(preview,8,8,8,8);add(preview,BorderLayout.EAST);refresh();
        }
        void refresh(){for(var entry:options.entrySet()){boolean checked=Objects.equals(entry.getKey(),selected==null?"":selected);entry.getValue().setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(checked?accent:LINE,checked?2:1,true),new EmptyBorder(3,3,3,3)));}preview.removeAll();preview.add(new Visual.Portrait(selected,116),BorderLayout.CENTER);preview.revalidate();preview.repaint();}
    }
    private void portraitPreview(Map<String,Object> person){
        JDialog dialog=new JDialog(this,"Retrato do cadastro",true);dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel root=panel(new BorderLayout(0,18),BG);pad(root,24,28,24,28);
        JLabel name=label(person.get("nome").toString(),24,true,TEXT);name.setToolTipText(person.get("nome").toString());root.add(name,BorderLayout.NORTH);
        root.add(new Visual.Portrait((String)person.get("foto"),380),BorderLayout.CENTER);
        JPanel actions=panel(new FlowLayout(FlowLayout.RIGHT,0,0),BG);JButton close=button("Fechar retrato",true,()->dialog.dispose());actions.add(close);root.add(actions,BorderLayout.SOUTH);
        dialog.setContentPane(root);dialog.getRootPane().setDefaultButton(close);dialog.setSize(480,560);dialog.setLocationRelativeTo(this);dialog.setVisible(true);
    }
    private void studentDetails(){studentDetails(selected());}
    private void studentDetails(Map<String,Object> r){
        long id=AcademiaService.number(r,"id");
        background(()->{service.verificarSessao();return service.database().query("SELECT p.nome AS plano,m.inicio,m.fim,"+AcademiaService.SITUACAO_ATUAL+" AS situacao,m.valor FROM matriculas m JOIN planos p ON p.id=m.plano_id WHERE aluno_id=? ORDER BY m.id DESC",id);},contracts->{
            JDialog dialog=new JDialog(this,"Ficha do aluno",true);JPanel root=panel(new BorderLayout(0,23),BG);pad(root,26,28,26,28);
            JPanel header=panel(new BorderLayout(20,0),BG);header.add(new Visual.Portrait((String)r.get("foto"),144),BorderLayout.WEST);JPanel info=panel(new GridLayout(0,1,0,7),BG);info.add(label(r.get("nome").toString(),25,true,TEXT));info.add(label("CPF  "+cpf(r.get("cpf")),13,false,MUTED));info.add(label(r.get("email").toString(),13,false,MUTED));info.add(label(r.get("telefone").toString(),13,false,MUTED));header.add(info,BorderLayout.CENTER);root.add(header,BorderLayout.NORTH);
            JPanel list=new Visual.RoundPanel(new BorderLayout(0,13),Visual.SURFACE,16,true);pad(list,17,17,17,17);list.add(label("Contratações deste aluno",17,true,TEXT),BorderLayout.NORTH);list.add(scroll(makeTable(contracts,new String[]{"plano","inicio","fim","situacao","valor"},new String[]{"Plano","Início","Fim","Situação","Valor"})),BorderLayout.CENTER);root.add(list,BorderLayout.CENTER);JPanel actions=panel(new FlowLayout(FlowLayout.RIGHT,9,0),BG);actions.add(button("Fechar ficha",false,()->dialog.dispose()));actions.add(button("Editar aluno",true,()->{dialog.dispose();if(current.equals("Alunos"))editForm(r);else showPage("Alunos",()->editForm(r));}));root.add(actions,BorderLayout.SOUTH);dialog.setContentPane(root);dialog.setSize(840,540);dialog.setLocationRelativeTo(this);dialog.setVisible(true);
        },null);
    }

    /** Exporta a mesma interface do aplicativo com dados descartáveis de demonstração. */
    public void captureScreens() throws Exception {
        synchronous=true;Files.createDirectories(Path.of("assets/telas"));showLogin(); setVisible(true); capture("01-login");
        service.login("admin","Admin@123");shell();
        String[] pages={"Visão geral","Alunos","Modalidades","Planos","Matrículas","Produtos","Movimentações","Usuários"};
        for(int i=0;i<pages.length;i++){showPage(pages[i]);capture(String.format("%02d-",i+2)+new String[]{"inicio","alunos","modalidades","planos","matriculas","produtos","movimentacoes","usuarios"}[i]);}
        // Formulário de pagamento da matrícula parcial de demonstração (código 3) e as telas da recepção.
        showPage("Matrículas");selectCode(3);dialogCapture="10-pagamento-parcial";paymentForm();
        service.login("recepcao","Recepcao@123");shell();capture("11-recepcao");showPage("Alunos");capture("12-recepcao-alunos");
    }
    private void capture(String name) throws Exception {
        validate();getContentPane().doLayout();validate();Toolkit.getDefaultToolkit().sync();captureComponent(getContentPane(),name);
    }
    private static void captureComponent(Container area,String name) throws Exception {
        BufferedImage img=new BufferedImage(area.getWidth(),area.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=img.createGraphics();area.printAll(g);g.dispose();ImageIO.write(img,"png",Path.of("assets/telas/"+name+".png").toFile());
    }
}
