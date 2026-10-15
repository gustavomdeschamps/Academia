package br.com.pulso;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Linguagem visual do Pulso. Ícones vetoriais e animações locais, sem bibliotecas externas. */
final class Visual {
    static final Color DARK=new Color(15,17,20), ORANGE=new Color(255,139,99),
        INK=new Color(242,243,245), MUTED=new Color(157,164,176), BG=new Color(20,23,28),
        SURFACE=new Color(28,32,39), INPUT=new Color(22,26,32), LINE=new Color(47,53,63),
        SELECT=new Color(62,45,39), GOOD=new Color(130,216,178);
    static final boolean MOTION=!Boolean.getBoolean("pulso.reduceMotion");
    private static final Map<String,BufferedImage> IMAGES=new HashMap<>();
    static Font font(int size,boolean bold){return new Font("Segoe UI",bold?Font.BOLD:Font.PLAIN,size);}
    static Graphics2D quality(Graphics g){Graphics2D x=(Graphics2D)g.create();x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);x.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);x.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);return x;}
    static Color mix(Color a,Color b,float f){f=Math.max(0,Math.min(1,f));return new Color((int)(a.getRed()+(b.getRed()-a.getRed())*f),(int)(a.getGreen()+(b.getGreen()-a.getGreen())*f),(int)(a.getBlue()+(b.getBlue()-a.getBlue())*f));}
    static BufferedImage image(String asset){
        if(asset==null||asset.isBlank())return null;
        if(IMAGES.containsKey(asset))return IMAGES.get(asset);
        BufferedImage loaded=null;
        try{loaded=ImageIO.read(Path.of("assets",asset.contains("/")?asset+".png":"imagens/"+asset+".png").toFile());}catch(Exception ignored){}
        IMAGES.put(asset,loaded);return loaded;
    }
    static void install(){
        ColorUIResource surface=new ColorUIResource(SURFACE),ink=new ColorUIResource(INK),bg=new ColorUIResource(BG),line=new ColorUIResource(LINE);
        for(String key:new String[]{"Panel.background","OptionPane.background","Viewport.background","ScrollPane.background","List.background","ComboBox.background","Menu.background","MenuItem.background","PopupMenu.background","ToolTip.background"})UIManager.put(key,surface);
        for(String key:new String[]{"Label.foreground","Button.foreground","TextField.foreground","PasswordField.foreground","ComboBox.foreground","List.foreground","CheckBox.foreground","OptionPane.messageForeground","Menu.foreground","MenuItem.foreground","ToolTip.foreground"})UIManager.put(key,ink);
        for(String key:new String[]{"TextField.background","PasswordField.background","CheckBox.background","Table.background"})UIManager.put(key,bg);
        UIManager.put("List.selectionBackground",new ColorUIResource(SELECT));UIManager.put("List.selectionForeground",ink);
        UIManager.put("ComboBox.selectionBackground",new ColorUIResource(SELECT));UIManager.put("ComboBox.selectionForeground",ink);
        UIManager.put("ToolTip.border",BorderFactory.createLineBorder(LINE));UIManager.put("PopupMenu.border",BorderFactory.createLineBorder(LINE));
        UIManager.put("ScrollBar.width",10);UIManager.put("Button.background",surface);UIManager.put("Button.focus",line);
        for(String key:new String[]{"Label.font","Button.font","TextField.font","PasswordField.font","ComboBox.font","CheckBox.font","OptionPane.messageFont","ToolTip.font"})UIManager.put(key,font(14,false));
        for(String key:new String[]{"OptionPane.warningIcon","OptionPane.errorIcon","OptionPane.informationIcon","OptionPane.questionIcon"})UIManager.put(key,new Glyph("alerta",ORANGE,32));
        UIManager.put("CheckBox.icon",new CheckIcon());
    }
    static void styleCombo(JComboBox<?> combo){
        combo.setFont(font(14,false));combo.setBackground(INPUT);combo.setForeground(INK);combo.setBorder(new InputBorder());combo.setPreferredSize(new Dimension(300,46));
        combo.setUI(new BasicComboBoxUI(){
            @Override protected JButton createArrowButton(){JButton b=new JButton(new Glyph("baixo",MUTED,16));b.setBorder(BorderFactory.createEmptyBorder());b.setOpaque(false);b.setContentAreaFilled(false);b.setFocusable(false);return b;}
        });
        combo.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){super.getListCellRendererComponent(list,value,index,selected,focus);setFont(font(14,false));setBorder(BorderFactory.createEmptyBorder(7,8,7,8));setBackground(selected?SELECT:INPUT);setForeground(INK);if(value instanceof String key)setText(switch(key){case "ATIVA"->"Ativa";case "TRANCADA"->"Trancada";case "CANCELADA"->"Cancelada";case "ADMIN"->"Administrador";case "ATENDENTE"->"Atendente";case "ENTRADA"->"Entrada";case "SAIDA"->"Saída";case "DINHEIRO"->"Dinheiro";case "CARTAO"->"Cartão";default->key;});return this;}
        });
    }
    static void styleScroll(JScrollPane pane){
        for(JScrollBar bar:new JScrollBar[]{pane.getVerticalScrollBar(),pane.getHorizontalScrollBar()}){
            bar.setPreferredSize(new Dimension(9,9));bar.setUnitIncrement(24);
            bar.setUI(new BasicScrollBarUI(){
                @Override protected JButton createDecreaseButton(int orientation){return empty();}
                @Override protected JButton createIncreaseButton(int orientation){return empty();}
                private JButton empty(){JButton b=new JButton();b.setPreferredSize(new Dimension(0,0));return b;}
                @Override protected void paintTrack(Graphics g,JComponent c,Rectangle r){g.setColor(pane.getViewport().getBackground());g.fillRect(r.x,r.y,r.width,r.height);}
                @Override protected void paintThumb(Graphics graphics,JComponent c,Rectangle r){if(r.isEmpty()||!scrollbar.isEnabled())return;Graphics2D g=quality(graphics);g.setColor(new Color(75,82,94));g.fillRoundRect(r.x+2,r.y+2,Math.max(3,r.width-4),Math.max(3,r.height-4),5,5);g.dispose();}
            });
        }
    }
    static class RoundPanel extends JPanel {
        final int radius;final boolean outlined;
        RoundPanel(LayoutManager layout,Color color,int radius,boolean outlined){super(layout);this.radius=radius;this.outlined=outlined;setBackground(color);setOpaque(false);}
        @Override protected void paintComponent(Graphics graphics){Graphics2D g=quality(graphics);g.setPaint(new GradientPaint(0,0,mix(getBackground(),INK,.017f),0,getHeight(),getBackground()));g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius);if(outlined){g.setColor(LINE);g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius);}g.dispose();super.paintComponent(graphics);}
    }
    static final class Stage extends JPanel {
        private BufferedImage old;private float progress=1;private Timer timer;
        Stage(){super(new BorderLayout(0,22));setBackground(BG);}
        void remember(){stop();if(MOTION&&isShowing()&&getWidth()>0){old=new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=old.createGraphics();printAll(g);g.dispose();}}
        void reveal(){if(old==null)return;progress=0;long start=System.nanoTime();timer=new Timer(16,e->{progress=Math.min(1,(System.nanoTime()-start)/260_000_000f);if(progress>=1){stop();old=null;}repaint();});timer.start();}
        void stop(){if(timer!=null){timer.stop();timer=null;}}
        @Override protected void paintChildren(Graphics graphics){super.paintChildren(graphics);if(old!=null&&progress<1){Graphics2D g=quality(graphics);float ease=1-(1-progress)*(1-progress);g.setComposite(AlphaComposite.SrcOver.derive(1-ease));g.drawImage(old,0,0,null);g.dispose();}}
        @Override public void removeNotify(){stop();old=null;super.removeNotify();}
    }
    static final class DashboardPanel extends JPanel implements Scrollable {
        DashboardPanel(){super(new BorderLayout(0,18));setBackground(BG);setPreferredSize(new Dimension(0,626));}
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle r,int o,int d){return 24;}
        public int getScrollableBlockIncrement(Rectangle r,int o,int d){return Math.max(24,r.height-24);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return getParent()!=null&&getParent().getHeight()>=getPreferredSize().height;}
    }
    /** Exibe toda a fotografia com proporção preservada. A imagem nunca é ampliada para cortar o sujeito. */
    static final class PhotoPanel extends JPanel {
        private final BufferedImage photo;private final int radius;private final float shade;
        PhotoPanel(String asset,int radius,float shade,double focalX,double focalY){photo=image(asset);this.radius=radius;this.shade=shade;setOpaque(false);}
        @Override protected void paintComponent(Graphics graphics){
            Graphics2D g=quality(graphics);g.clip(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),radius,radius));g.setColor(DARK);g.fillRect(0,0,getWidth(),getHeight());
            if(photo!=null){double scale=Math.min((double)getWidth()/photo.getWidth(),(double)getHeight()/photo.getHeight());int w=(int)Math.round(photo.getWidth()*scale),h=(int)Math.round(photo.getHeight()*scale);g.drawImage(photo,(getWidth()-w)/2,(getHeight()-h)/2,w,h,null);}
            if(shade>0){g.setPaint(new GradientPaint(0,0,new Color(15,17,20,(int)(255*shade)),getWidth(),0,new Color(15,17,20,0)));g.fillRect(0,0,getWidth(),getHeight());}
            g.dispose();super.paintComponent(graphics);
        }
    }
    static final class SoftButton extends JButton {
        private final boolean primary,nav;private float hover;private Timer timer;
        SoftButton(String title,boolean primary,boolean nav){super(title);this.primary=primary;this.nav=nav;setOpaque(false);setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);setFont(font(13,true));setForeground(primary?DARK:INK);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));setRolloverEnabled(true);addMouseListener(new MouseAdapter(){@Override public void mouseEntered(MouseEvent e){animate(1);}@Override public void mouseExited(MouseEvent e){animate(0);}});}
        private void animate(float target){if(timer!=null)timer.stop();if(!MOTION){hover=target;repaint();return;}float from=hover;long start=System.nanoTime();timer=new Timer(16,e->{float t=Math.min(1,(System.nanoTime()-start)/160_000_000f);hover=from+(target-from)*(1-(1-t)*(1-t));repaint();if(t>=1)timer.stop();});timer.start();}
        @Override public void removeNotify(){if(timer!=null)timer.stop();super.removeNotify();}
        @Override protected void paintComponent(Graphics graphics){Graphics2D g=quality(graphics);Color base=nav?getBackground():primary?ORANGE:SURFACE;Color bg=mix(base,primary?new Color(255,174,139):new Color(54,61,72),hover*.55f);if(getModel().isPressed())bg=mix(bg,DARK,.18f);g.setPaint(new GradientPaint(0,0,mix(bg,INK,.025f),0,getHeight(),bg));g.fillRoundRect(0,0,getWidth(),getHeight(),12,12);if(!primary&&!nav){g.setColor(mix(LINE,ORANGE,hover*.3f));g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);}if(hasFocus()){g.setColor(ORANGE);g.drawRoundRect(2,2,getWidth()-5,getHeight()-5,9,9);}g.dispose();super.paintComponent(graphics);}
    }
    static final class InputBorder extends AbstractBorder {
        @Override public Insets getBorderInsets(Component c){return new Insets(11,13,11,13);}
        @Override public Insets getBorderInsets(Component c,Insets i){i.top=11;i.left=13;i.bottom=11;i.right=13;return i;}
        @Override public void paintBorder(Component c,Graphics graphics,int x,int y,int w,int h){Graphics2D g=quality(graphics);g.setColor(c.hasFocus()?ORANGE:LINE);g.drawRoundRect(x,y,w-1,h-1,10,10);g.dispose();}
    }
    static final class Glyph implements Icon {
        final String name;final Color color;final int size;
        Glyph(String name,Color color,int size){this.name=name;this.color=color;this.size=size;}
        public int getIconWidth(){return size;}public int getIconHeight(){return size;}
        public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=quality(graphics);g.translate(x,y);g.scale(size/24.0,size/24.0);g.setColor(color);g.setStroke(new BasicStroke(1.6f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            switch(name){
                case "Visão geral" -> {for(int a=0;a<2;a++)for(int b=0;b<2;b++)g.drawRoundRect(3+a*11,3+b*11,7,7,2,2);}
                case "Alunos","Usuários" -> {g.drawOval(8,3,8,8);g.draw(new Arc2D.Double(3,12,18,15,0,180,Arc2D.OPEN));}
                case "Modalidades" -> {g.drawLine(3,12,21,12);g.drawRoundRect(5,6,3,12,1,1);g.drawRoundRect(16,6,3,12,1,1);g.drawLine(3,9,3,15);g.drawLine(21,9,21,15);}
                case "Planos" -> {g.drawRoundRect(4,4,16,17,3,3);g.drawLine(8,9,16,9);g.drawLine(8,13,16,13);g.drawLine(8,17,13,17);}
                case "Matrículas" -> {g.drawRoundRect(3,5,18,16,2,2);g.drawLine(7,3,7,7);g.drawLine(17,3,17,7);g.drawLine(3,10,21,10);g.drawLine(8,15,11,18);g.drawLine(11,18,17,13);}
                case "Produtos" -> {g.drawRect(4,7,16,14);g.drawLine(4,7,12,3);g.drawLine(12,3,20,7);g.drawLine(4,7,12,11);g.drawLine(12,11,20,7);g.drawLine(12,11,12,21);}
                case "Movimentações" -> {g.drawLine(3,8,20,8);g.drawLine(16,4,20,8);g.drawLine(20,8,16,12);g.drawLine(21,17,4,17);g.drawLine(8,13,4,17);g.drawLine(4,17,8,21);}
                case "buscar" -> {g.drawOval(3,3,12,12);g.drawLine(13,13,21,21);}
                case "mais" -> {g.drawLine(12,4,12,20);g.drawLine(4,12,20,12);}
                case "sair","seta" -> {if(name.equals("sair")){g.drawLine(10,4,4,4);g.drawLine(4,4,4,20);g.drawLine(4,20,10,20);}g.drawLine(10,12,21,12);g.drawLine(17,8,21,12);g.drawLine(21,12,17,16);}
                case "baixo" -> {g.drawLine(6,9,12,15);g.drawLine(12,15,18,9);}
                case "editar" -> {g.draw(new Line2D.Double(5,17,17,5));g.drawLine(15,3,21,9);g.drawLine(5,17,3,21);g.drawLine(3,21,7,19);g.drawLine(10,21,21,21);}
                case "olho" -> {g.draw(new Ellipse2D.Double(2,6,20,12));g.drawOval(9,9,6,6);}
                case "fechar" -> {g.drawLine(6,6,18,18);g.drawLine(18,6,6,18);}
                case "check" -> {g.drawLine(4,12,9,17);g.drawLine(9,17,20,6);}
                case "escudo" -> {Path2D p=new Path2D.Double();p.moveTo(12,2);p.lineTo(21,6);p.lineTo(19,16);p.lineTo(12,22);p.lineTo(5,16);p.lineTo(3,6);p.closePath();g.draw(p);g.drawLine(8,12,11,15);g.drawLine(11,15,16,9);}
                case "marca" -> {Path2D line=new Path2D.Double();line.moveTo(1,14);line.lineTo(6,14);line.lineTo(10,4);line.lineTo(14,21);line.lineTo(18,10);line.lineTo(22,10);g.setStroke(new BasicStroke(2.6f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(line);}
                default -> {g.drawOval(3,3,18,18);g.drawLine(12,7,12,13);g.fillOval(11,16,2,2);}
            }g.dispose();
        }
    }
    static final class Badge extends JLabel {
        final Color bg;
        Badge(String text,Color fg,Color bg){super(text);this.bg=bg;setFont(font(11,true));setForeground(fg);setBorder(BorderFactory.createEmptyBorder(6,10,6,10));setOpaque(false);}
        @Override protected void paintComponent(Graphics graphics){Graphics2D g=quality(graphics);g.setColor(bg);g.fillRoundRect(0,0,getWidth(),getHeight(),10,10);g.dispose();super.paintComponent(graphics);}
    }
    static final class Avatar extends JLabel {
        final Color bg;
        Avatar(String initials,Color fg,Color bg,int size){super(initials,SwingConstants.CENTER);this.bg=bg;setForeground(fg);setFont(font(size/3,true));setPreferredSize(new Dimension(size,size));}
        @Override protected void paintComponent(Graphics graphics){Graphics2D g=quality(graphics);g.setColor(bg);g.fillOval(0,0,getWidth()-1,getHeight()-1);g.dispose();super.paintComponent(graphics);}
    }
    static final class Portrait extends JComponent {
        final String asset;final int size;
        Portrait(String asset,int size){this.asset=asset;this.size=size;setPreferredSize(new Dimension(size,size));setMinimumSize(new Dimension(size,size));setMaximumSize(new Dimension(size,size));getAccessibleContext();}
        @Override protected void paintComponent(Graphics graphics){Graphics2D g=quality(graphics);int d=Math.min(getWidth(),getHeight()),x=(getWidth()-d)/2,y=(getHeight()-d)/2;g.setColor(INPUT);g.fillRoundRect(x,y,d,d,14,14);BufferedImage photo=image(asset);if(photo!=null){Shape clip=g.getClip();g.clip(new RoundRectangle2D.Double(x,y,d,d,14,14));g.drawImage(photo,x,y,d,d,null);g.setClip(clip);}else new Glyph("Alunos",MUTED,d/2).paintIcon(this,g,x+d/4,y+d/4);g.setColor(LINE);g.drawRoundRect(x,y,d-1,d-1,14,14);g.dispose();}
    }
    static final class Search extends JPanel {
        final JTextField field=new JTextField(){
            @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);if(getText().isEmpty()&&!hasFocus()){Graphics2D g=quality(graphics);g.setColor(MUTED);g.setFont(getFont());FontMetrics fm=g.getFontMetrics();g.drawString("Buscar nos registros...",0,(getHeight()-fm.getHeight())/2+fm.getAscent());g.dispose();}}
        };
        Search(){super(new BorderLayout(10,0));setBackground(INPUT);setBorder(new InputBorder());add(new JLabel(new Glyph("buscar",MUTED,18)),BorderLayout.WEST);field.setBorder(BorderFactory.createEmptyBorder());field.setFont(font(13,false));field.setBackground(INPUT);field.setForeground(INK);field.setCaretColor(ORANGE);add(field,BorderLayout.CENTER);setPreferredSize(new Dimension(260,46));}
    }
    static final class CheckIcon implements Icon {
        public int getIconWidth(){return 21;}public int getIconHeight(){return 21;}
        public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=quality(graphics);boolean selected=c instanceof AbstractButton b&&b.isSelected();g.setColor(selected?ORANGE:INPUT);g.fillRoundRect(x+1,y+1,18,18,5,5);g.setColor(selected?ORANGE:LINE);g.drawRoundRect(x+1,y+1,18,18,5,5);if(selected)new Glyph("check",DARK,16).paintIcon(c,g,x+2,y+2);g.dispose();}
    }
    static final class Intro extends JPanel {
        private float progress;private Timer timer;private final Runnable finish;private boolean completed;
        Intro(Runnable finish){this.finish=finish;setLayout(new BorderLayout());setBackground(DARK);JPanel bottom=new JPanel(new FlowLayout(FlowLayout.RIGHT,32,24));bottom.setOpaque(false);JButton skip=new SoftButton("Pular abertura",false,false);skip.setBorder(BorderFactory.createEmptyBorder(12,18,12,18));skip.addActionListener(e->complete());bottom.add(skip);add(bottom,BorderLayout.SOUTH);}
        @Override public void addNotify(){super.addNotify();if(!MOTION){SwingUtilities.invokeLater(this::complete);return;}long start=System.nanoTime();timer=new Timer(16,e->{progress=Math.min(1,(System.nanoTime()-start)/2_200_000_000f);repaint();if(progress>=1)complete();});timer.start();}
        private void complete(){if(completed)return;completed=true;if(timer!=null)timer.stop();finish.run();}
        @Override public void removeNotify(){if(timer!=null)timer.stop();super.removeNotify();}
        @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=quality(graphics);int w=getWidth(),h=getHeight();g.setPaint(new RadialGradientPaint(w*.5f,h*.43f,Math.max(w,h)*.52f,new float[]{0,.55f,1},new Color[]{new Color(58,38,31),new Color(23,22,25),DARK}));g.fillRect(0,0,w,h);float ease=1-(1-progress)*(1-progress);int cy=(int)(h*.41);g.setColor(new Color(255,139,99,18));g.setStroke(new BasicStroke(1));for(int i=0;i<3;i++){double d=170+i*108+ease*30;g.draw(new Ellipse2D.Double(w/2.0-d/2,cy-d/2,d,d));}
            Graphics2D logo=(Graphics2D)g.create();logo.translate(w/2.0-59,cy-59);logo.clip(new Rectangle2D.Double(0,0,118*Math.min(1,progress*2.5),118));new Glyph("marca",ORANGE,118).paintIcon(this,logo,0,0);logo.dispose();
            float opacity=Math.min(1,Math.max(0,(progress-.22f)*3));g.setComposite(AlphaComposite.SrcOver.derive(opacity));g.setColor(INK);g.setFont(font(64,true));String title="pulso";g.drawString(title,(w-g.getFontMetrics().stringWidth(title))/2,cy+133);g.setFont(font(12,true));g.setColor(MUTED);String sub="ACADEMIA MOVIMENTO";g.drawString(sub,(w-g.getFontMetrics().stringWidth(sub))/2,cy+172);g.setComposite(AlphaComposite.SrcOver);g.setColor(LINE);g.fillRoundRect(w/2-100,h-126,200,3,3,3);g.setColor(ORANGE);g.fillRoundRect(w/2-100,h-126,(int)(200*ease),3,3,3);g.dispose();}
    }
    /** Distribuição baseada exclusivamente nos contratos registrados no banco. */
    static final class Distribution extends JComponent {
        final long active,paused,cancelled;
        Distribution(long a,long p,long c){active=a;paused=p;cancelled=c;setPreferredSize(new Dimension(190,112));}
        @Override protected void paintComponent(Graphics graphics){Graphics2D g=quality(graphics);long total=active+paused+cancelled;int d=Math.min(94,getHeight()-12),x=8,y=(getHeight()-d)/2;g.setStroke(new BasicStroke(8,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(LINE);g.drawOval(x+5,y+5,d-10,d-10);if(total>0){double start=90;long[] values={active,paused,cancelled};Color[] colors={GOOD,ORANGE,new Color(226,121,128)};for(int i=0;i<3;i++){double extent=360.0*values[i]/total;g.setColor(colors[i]);g.draw(new Arc2D.Double(x+5,y+5,d-10,d-10,start,-extent,Arc2D.OPEN));start-=extent;}}g.setFont(font(25,true));g.setColor(INK);String value=Long.toString(total);g.drawString(value,x+d/2-g.getFontMetrics().stringWidth(value)/2,y+d/2+5);g.setFont(font(9,false));g.setColor(MUTED);g.drawString("CONTRATOS",x+d/2-g.getFontMetrics().stringWidth("CONTRATOS")/2,y+d/2+22);g.dispose();}
    }
    static final class Toast extends JComponent {
        final String text;private Timer timer;private float opacity;private long started;
        Toast(String text){this.text=text;setOpaque(false);setEnabled(false);getAccessibleContext();}
        @Override public void addNotify(){super.addNotify();started=System.nanoTime();timer=new Timer(16,e->{long ms=(System.nanoTime()-started)/1_000_000;opacity=MOTION?Math.min(1,ms/180f):1;if(ms>2600)opacity=Math.max(0,1-(ms-2600)/250f);repaint();if(ms>=2850){timer.stop();setVisible(false);}});timer.start();}
        @Override public void removeNotify(){if(timer!=null)timer.stop();super.removeNotify();}
        @Override public boolean contains(int x,int y){return false;}
        @Override protected void paintComponent(Graphics graphics){Graphics2D g=quality(graphics);g.setComposite(AlphaComposite.SrcOver.derive(opacity));g.setFont(font(13,true));int w=Math.min(getWidth()-44,g.getFontMetrics().stringWidth(text)+70),x=getWidth()-w-24,y=getHeight()-74;g.setColor(new Color(25,47,38));g.fillRoundRect(x,y,w,48,13,13);g.setColor(new Color(59,97,76));g.drawRoundRect(x,y,w,48,13,13);new Glyph("check",GOOD,19).paintIcon(this,g,x+14,y+14);g.setColor(INK);g.drawString(text,x+43,y+29);g.dispose();}
    }
}
