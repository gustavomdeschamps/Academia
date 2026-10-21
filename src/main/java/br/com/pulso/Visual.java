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
        SELECT=new Color(62,45,39), GOOD=new Color(130,216,178), TEAL=new Color(108,222,200), ALERT=new Color(238,143,149);
    private static Color accent=ORANGE,selection=SELECT;
    static Color accent(){return accent;}
    static Color selection(){return selection;}
    static void role(boolean administrator){accent=administrator?ORANGE:TEAL;selection=administrator?SELECT:new Color(27,58,53);UIManager.put("List.selectionBackground",new ColorUIResource(selection));UIManager.put("ComboBox.selectionBackground",new ColorUIResource(selection));}
    static final boolean MOTION=!Boolean.getBoolean("pulso.reduceMotion");
    private static final Map<String,BufferedImage> IMAGES=new HashMap<>();
    static Font font(int size,boolean bold){return new Font("Segoe UI",bold?Font.BOLD:Font.PLAIN,size);}
    static void brand(Graphics2D graphics,int center,int baseline,int size){
        Graphics2D g=(Graphics2D)graphics.create();g.setFont(font(size,true));int word=g.getFontMetrics().stringWidth("pulso"),icon=(int)(size*.7),gap=(int)(size*.22),x=center-(word+icon+gap)/2;
        new Glyph("marca",ORANGE,icon).paintIcon(null,g,x,baseline-icon);g.setColor(INK);g.drawString("pulso",x+icon+gap,baseline);g.dispose();
    }
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
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){super.getListCellRendererComponent(list,value,index,selected,focus);setFont(font(14,false));setBorder(BorderFactory.createEmptyBorder(7,8,7,8));setBackground(selected?selection():INPUT);setForeground(INK);if(value instanceof String key)setText(switch(key){case "ATIVA"->"Ativa";case "TRANCADA"->"Trancada";case "CANCELADA"->"Cancelada";case "ADMIN"->"Administrador";case "ATENDENTE"->"Atendente";case "ENTRADA"->"Entrada";case "SAIDA"->"Saída";case "DINHEIRO"->"Dinheiro";case "CARTAO"->"Cartão";default->key;});return this;}
        });
    }
    static void styleScroll(JScrollPane pane){
        if(Boolean.TRUE.equals(pane.getClientProperty("pulso.scroll")))return;
        pane.putClientProperty("pulso.scroll",true);pane.setWheelScrollingEnabled(false);
        for(JScrollBar bar:new JScrollBar[]{pane.getVerticalScrollBar(),pane.getHorizontalScrollBar()}){
            bar.setPreferredSize(new Dimension(13,13));bar.setUnitIncrement(28);
            bar.setUI(new BasicScrollBarUI(){
                @Override protected JButton createDecreaseButton(int orientation){return empty();}
                @Override protected JButton createIncreaseButton(int orientation){return empty();}
                private JButton empty(){JButton b=new JButton();b.setPreferredSize(new Dimension(0,0));return b;}
                @Override protected void paintTrack(Graphics g,JComponent c,Rectangle r){g.setColor(pane.getViewport().getBackground());g.fillRect(r.x,r.y,r.width,r.height);}
                @Override protected void paintThumb(Graphics graphics,JComponent c,Rectangle r){if(r.isEmpty()||!scrollbar.isEnabled())return;Graphics2D g=quality(graphics);g.setColor(isDragging?accent():isThumbRollover()?mix(new Color(98,109,124),accent(),.7f):new Color(83,93,108));g.fillRoundRect(r.x+3,r.y+3,Math.max(5,r.width-6),Math.max(5,r.height-6),7,7);g.dispose();}
            });
        }
        new WheelMotion(pane);
    }
    private static final class WheelMotion implements MouseWheelListener,HierarchyListener {
        final JScrollPane pane;private Timer timer;private double target;private JScrollBar activeBar;
        WheelMotion(JScrollPane pane){this.pane=pane;pane.addMouseWheelListener(this);pane.addHierarchyListener(this);}
        @Override public void mouseWheelMoved(MouseWheelEvent event){
            JScrollBar bar=event.isShiftDown()?pane.getHorizontalScrollBar():pane.getVerticalScrollBar();
            if(event.getPreciseWheelRotation()==0)return;
            int max=Math.max(bar.getMinimum(),bar.getMaximum()-bar.getVisibleAmount());
            double start=timer!=null&&timer.isRunning()&&activeBar==bar?target:bar.getValue();
            double next=Math.max(bar.getMinimum(),Math.min(max,start+event.getPreciseWheelRotation()*bar.getUnitIncrement()*2.4));
            if(next==start){
                Container parent=pane.getParent();while(parent!=null&&!(parent instanceof JScrollPane))parent=parent.getParent();
                if(parent instanceof JScrollPane outer){Point point=SwingUtilities.convertPoint(pane,event.getPoint(),outer);outer.dispatchEvent(new MouseWheelEvent(outer,event.getID(),event.getWhen(),event.getModifiersEx(),point.x,point.y,event.getXOnScreen(),event.getYOnScreen(),event.getClickCount(),event.isPopupTrigger(),event.getScrollType(),event.getScrollAmount(),event.getWheelRotation(),event.getPreciseWheelRotation()));event.consume();}
                return;
            }
            event.consume();target=next;activeBar=bar;if(!MOTION){bar.setValue((int)Math.round(target));return;}
            if(timer!=null)timer.stop();long began=System.nanoTime();double from=bar.getValue();
            timer=new Timer(16,e->{double t=Math.min(1,(System.nanoTime()-began)/190_000_000.0),ease=1-Math.pow(1-t,3);int limit=Math.max(bar.getMinimum(),bar.getMaximum()-bar.getVisibleAmount());bar.setValue((int)Math.round(Math.max(bar.getMinimum(),Math.min(limit,from+(target-from)*ease))));if(t>=1){timer.stop();target=bar.getValue();}});timer.setCoalesce(true);timer.start();
        }
        @Override public void hierarchyChanged(HierarchyEvent event){if((event.getChangeFlags()&HierarchyEvent.DISPLAYABILITY_CHANGED)!=0&&!pane.isDisplayable()&&timer!=null)timer.stop();}
    }
    static class RoundPanel extends JPanel {
        final int radius;final boolean outlined;
        RoundPanel(LayoutManager layout,Color color,int radius,boolean outlined){super(layout);this.radius=radius;this.outlined=outlined;setBackground(color);setOpaque(false);}
        @Override protected void paintComponent(Graphics graphics){Graphics2D g=quality(graphics);g.setPaint(new GradientPaint(0,0,mix(getBackground(),INK,.017f),0,getHeight(),getBackground()));g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius);if(outlined){g.setColor(LINE);g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius);}g.dispose();super.paintComponent(graphics);}
    }
    static final class Stage extends JPanel {
        private BufferedImage old;private float progress=1;private Timer timer;
        Stage(){super(new BorderLayout(0,22));setBackground(BG);}
        void remember(){stop();old=null;progress=1;if(MOTION&&isShowing()&&getWidth()>0){BufferedImage snapshot=new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=snapshot.createGraphics();printAll(g);g.dispose();old=snapshot;}}
        void reveal(){if(!MOTION)return;progress=0;long start=System.nanoTime();timer=new Timer(16,e->{progress=Math.min(1,(System.nanoTime()-start)/340_000_000f);if(progress>=1){stop();old=null;}repaint();});timer.setCoalesce(true);timer.start();}
        void stop(){if(timer!=null){timer.stop();timer=null;}}
        @Override protected void paintChildren(Graphics graphics){float ease=1-(float)Math.pow(1-progress,3);Graphics2D incoming=quality(graphics);incoming.translate(0,(1-ease)*14);incoming.setComposite(AlphaComposite.SrcOver.derive(ease));super.paintChildren(incoming);incoming.dispose();if(old!=null&&progress<1){Graphics2D g=quality(graphics);g.setComposite(AlphaComposite.SrcOver.derive(1-ease));g.drawImage(old,0,(int)(-ease*8),null);g.dispose();}}
        @Override public void removeNotify(){stop();old=null;super.removeNotify();}
    }
    static final class PageBody extends JPanel implements Scrollable {
        PageBody(LayoutManager layout){super(layout);setBackground(BG);}
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 28;}
        public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return Math.max(28,r.height-28);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return getParent()!=null&&getParent().getHeight()>=getPreferredSize().height;}
    }
    /** Exibe toda a fotografia com proporção preservada. A imagem nunca é ampliada para cortar o sujeito. */
    static final class PhotoPanel extends JPanel {
        private final BufferedImage photo;private final int radius;private final float shade;
        PhotoPanel(String asset,int radius,float shade,double focalX,double focalY){photo=image(asset);this.radius=radius;this.shade=shade;setOpaque(false);}
        @Override protected void paintComponent(Graphics graphics){
            Graphics2D g=quality(graphics);g.clip(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),radius,radius));g.setColor(getParent()==null?DARK:getParent().getBackground());g.fillRect(0,0,getWidth(),getHeight());
            if(photo!=null){double scale=Math.min((double)getWidth()/photo.getWidth(),(double)getHeight()/photo.getHeight());int w=(int)Math.round(photo.getWidth()*scale),h=(int)Math.round(photo.getHeight()*scale);g.drawImage(photo,(getWidth()-w)/2,(getHeight()-h)/2,w,h,null);}
            if(shade>0){g.setPaint(new GradientPaint(0,0,new Color(15,17,20,(int)(255*shade)),getWidth(),0,new Color(15,17,20,0)));g.fillRect(0,0,getWidth(),getHeight());}
            g.dispose();super.paintComponent(graphics);
        }
    }
    static final class SoftButton extends JButton {
        private final boolean primary,nav;private float hover;private Timer timer;
        SoftButton(String title,boolean primary,boolean nav){super(title);this.primary=primary;this.nav=nav;setOpaque(false);setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);setFont(font(13,true));setForeground(primary?DARK:INK);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));setRolloverEnabled(true);addMouseListener(new MouseAdapter(){@Override public void mouseEntered(MouseEvent e){animate(1);}@Override public void mouseExited(MouseEvent e){animate(0);}});}
        private void animate(float target){if(timer!=null)timer.stop();if(!MOTION){hover=target;repaint();return;}float from=hover;long start=System.nanoTime();timer=new Timer(16,e->{float t=Math.min(1,(System.nanoTime()-start)/240_000_000f),ease=target>from?1+2.4f*(float)Math.pow(t-1,3)+1.4f*(float)Math.pow(t-1,2):1-(1-t)*(1-t);hover=from+(target-from)*ease;repaint();if(t>=1){hover=target;timer.stop();}});timer.setCoalesce(true);timer.start();}
        @Override public void removeNotify(){if(timer!=null)timer.stop();super.removeNotify();}
        @Override public void paint(Graphics graphics){Graphics2D g=quality(graphics);float lift=MOTION?(nav?1:3.5f)*hover:0;if(getModel().isPressed())lift=0;g.translate(0,-lift);if(!isEnabled())g.setComposite(AlphaComposite.SrcOver.derive(.45f));super.paint(g);g.dispose();}
        @Override protected void paintComponent(Graphics graphics){
            Graphics2D g=quality(graphics);int w=getWidth(),h=getHeight(),y=4,body=h-9;float strength=Math.max(0,Math.min(1,hover));
            Color base=nav?getBackground():primary?accent():SURFACE,bg=mix(base,primary?INK:new Color(60,70,80),strength*(primary?.22f:.65f));
            if(getModel().isPressed())bg=mix(bg,DARK,.16f);
            if(!nav){for(int i=3;i>=1;i--){g.setColor(new Color(accent().getRed(),accent().getGreen(),accent().getBlue(),(int)(strength*(primary?10:7))));g.fillRoundRect(3-i,y+3-i,w-6+i*2,body+i*2,14,14);}}
            g.setPaint(new GradientPaint(0,y,mix(bg,INK,primary?.08f:.025f),0,y+body,bg));g.fillRoundRect(2,y,w-4,body,12,12);
            g.setColor(mix(nav?bg:LINE,accent(),strength*(primary?.75f:.55f)));g.drawRoundRect(2,y,w-5,body-1,12,12);
            if(primary){g.setColor(new Color(255,255,255,(int)(25+strength*45)));g.drawLine(14,y+1,w-14,y+1);}
            if(hasFocus()){g.setColor(accent());g.drawRoundRect(0,1,w-1,h-3,14,14);}g.dispose();super.paintComponent(graphics);
        }
    }
    static final class InputBorder extends AbstractBorder {
        @Override public Insets getBorderInsets(Component c){return new Insets(11,13,11,13);}
        @Override public Insets getBorderInsets(Component c,Insets i){i.top=11;i.left=13;i.bottom=11;i.right=13;return i;}
        @Override public void paintBorder(Component c,Graphics graphics,int x,int y,int w,int h){Graphics2D g=quality(graphics);if(c.hasFocus()){g.setColor(new Color(accent().getRed(),accent().getGreen(),accent().getBlue(),24));g.setStroke(new BasicStroke(4));g.drawRoundRect(x+2,y+2,w-5,h-5,10,10);g.setStroke(new BasicStroke(1));}g.setColor(c.hasFocus()?accent():LINE);g.drawRoundRect(x,y,w-1,h-1,10,10);g.dispose();}
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
                case "atualizar" -> {g.draw(new Arc2D.Double(4,4,16,16,45,285,Arc2D.OPEN));g.drawLine(19,3,20,9);g.drawLine(14,8,20,9);}
                case "recepcao" -> {g.drawRoundRect(3,12,18,9,2,2);g.drawOval(9,3,6,6);g.drawLine(2,12,22,12);g.drawLine(7,16,12,16);}
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
    /** Miniaturas derivadas em memória, na resolução física do monitor, sem arquivos duplicados. */
    private static final Map<String,BufferedImage> PORTRAITS=new java.util.LinkedHashMap<>(32,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,BufferedImage> entry){return size()>64;}
    };
    private static BufferedImage portrait(String asset,int pixels){
        BufferedImage source=image(asset);if(source==null)return null;
        String key=asset+":"+pixels;BufferedImage cached=PORTRAITS.get(key);if(cached!=null)return cached;
        // Preserva toda a fotografia, incluindo cabelo, ombros e margens originais.
        int side=Math.max(source.getWidth(),source.getHeight());
        BufferedImage scaled;
        if(source.getWidth()==side&&source.getHeight()==side)scaled=source;
        else{scaled=new BufferedImage(side,side,BufferedImage.TYPE_INT_RGB);Graphics2D canvas=scaled.createGraphics();canvas.setColor(INPUT);canvas.fillRect(0,0,side,side);canvas.drawImage(source,(side-source.getWidth())/2,(side-source.getHeight())/2,null);canvas.dispose();}
        int target=Math.min(pixels,side);
        // Reduções sucessivas conservam detalhes do rosto que se perdem numa redução única.
        while(scaled.getWidth()>target){
            int next=Math.max(target,scaled.getWidth()/2);
            BufferedImage step=new BufferedImage(next,next,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=step.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(scaled,0,0,next,next,null);g.dispose();
            scaled=step;
        }
        PORTRAITS.put(key,scaled);return scaled;
    }
    static final class Portrait extends JComponent {
        final String asset;final int size;
        Portrait(String asset,int size){this.asset=asset;this.size=size;setPreferredSize(new Dimension(size,size));setMinimumSize(new Dimension(size,size));setMaximumSize(new Dimension(size,size));getAccessibleContext();}
        @Override protected void paintComponent(Graphics graphics){
            Graphics2D g=quality(graphics);int d=Math.min(getWidth(),getHeight()),x=(getWidth()-d)/2,y=(getHeight()-d)/2;
            if(d<=0){g.dispose();return;}
            g.setColor(INPUT);g.fillRoundRect(x,y,d,d,16,16);
            double deviceScale=Math.max(Math.abs(g.getTransform().getScaleX()),Math.abs(g.getTransform().getScaleY()));
            BufferedImage photo=portrait(asset,(int)Math.ceil(d*deviceScale));
            if(photo!=null){Shape clip=g.getClip();g.clip(new RoundRectangle2D.Double(x,y,d,d,16,16));g.drawImage(photo,x,y,d,d,null);g.setClip(clip);}
            else new Glyph("Alunos",MUTED,d/2).paintIcon(this,g,x+d/4,y+d/4);
            g.setColor(new Color(90,96,107));g.drawRoundRect(x,y,d-1,d-1,16,16);g.dispose();
        }
    }
    static final class Search extends JPanel {
        private String hint="Buscar nos registros...";
        final JTextField field=new JTextField(){
            @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);if(getText().isEmpty()&&!hasFocus()){Graphics2D g=quality(graphics);g.setColor(MUTED);g.setFont(getFont());FontMetrics fm=g.getFontMetrics();g.drawString(hint,0,(getHeight()-fm.getHeight())/2+fm.getAscent());g.dispose();}}
        };
        Search(){super(new BorderLayout(10,0));setBackground(INPUT);setBorder(new InputBorder());add(new JLabel(new Glyph("buscar",MUTED,18)),BorderLayout.WEST);field.setBorder(BorderFactory.createEmptyBorder());field.setFont(font(13,false));field.setBackground(INPUT);field.setForeground(INK);field.setCaretColor(ORANGE);add(field,BorderLayout.CENTER);setPreferredSize(new Dimension(260,46));}
        Search(String hint){this();this.hint=hint;field.getAccessibleContext().setAccessibleName(hint);field.setCaretColor(accent());}
    }
    static final class CheckIcon implements Icon {
        public int getIconWidth(){return 21;}public int getIconHeight(){return 21;}
        public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=quality(graphics);boolean selected=c instanceof AbstractButton b&&b.isSelected();g.setColor(selected?accent():INPUT);g.fillRoundRect(x+1,y+1,18,18,5,5);g.setColor(selected?accent():LINE);g.drawRoundRect(x+1,y+1,18,18,5,5);if(selected)new Glyph("check",DARK,16).paintIcon(c,g,x+2,y+2);g.dispose();}
    }
    /** Passagem da abertura para o login; não bloqueia o teclado ou os cliques. */
    static final class ScreenFade extends JComponent {
        private BufferedImage previous;private Timer timer;private float opacity=1;
        ScreenFade(BufferedImage previous){this.previous=previous;setOpaque(false);setEnabled(false);}
        @Override public boolean contains(int x,int y){return false;}
        @Override public void addNotify(){super.addNotify();long start=System.nanoTime();timer=new Timer(16,e->{float t=Math.min(1,(System.nanoTime()-start)/200_000_000f);opacity=1-t*t*(3-2*t);repaint();if(t>=1){timer.stop();previous=null;setVisible(false);}});timer.setCoalesce(true);timer.start();}
        @Override public void removeNotify(){if(timer!=null)timer.stop();previous=null;super.removeNotify();}
        @Override protected void paintComponent(Graphics graphics){if(previous==null)return;Graphics2D g=quality(graphics);g.setComposite(AlphaComposite.SrcOver.derive(opacity));g.drawImage(previous,0,0,getWidth(),getHeight(),null);g.dispose();}
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
