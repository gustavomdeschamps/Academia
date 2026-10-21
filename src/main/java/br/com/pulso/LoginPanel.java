package br.com.pulso;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/** Entrada centralizada; autenticação e erros fazem parte do mesmo formulário. */
final class LoginPanel extends JPanel {
    @FunctionalInterface interface Access {void open()throws Exception;}
    private final AcademiaService service;
    private final Access access;
    private final JTextField user=new JTextField();
    private final JPasswordField password=new JPasswordField();
    private final JLabel error=new JLabel(" "),caps=new JLabel(" ");
    private final JButton enter=new Visual.SoftButton("Entrar",true,false),show=new Visual.SoftButton("Mostrar",false,false),accounts=new Visual.SoftButton("Contas da apresentação",false,false);
    private final JPanel card;
    private boolean busy,revealed;
    private BufferedImage background;
    private int backgroundWidth,backgroundHeight;
    private double backgroundScale;

    LoginPanel(AcademiaService service,Access access){
        this.service=service;this.access=access;setLayout(null);setBackground(Visual.DARK);setName("loginCentral");
        card=new JPanel(new GridBagLayout()){
            @Override protected void paintComponent(Graphics graphics){Graphics2D g=Visual.quality(graphics);int w=getWidth(),h=getHeight();for(int i=12;i>0;i--){g.setColor(new Color(0,0,0,4));g.fillRoundRect(12-i,12-i,w-24+i*2,h-24+i*2,28,28);}g.setPaint(new GradientPaint(0,0,new Color(28,32,39,248),w,h,new Color(15,18,23,248)));g.fillRoundRect(0,0,w-1,h-1,26,26);g.setPaint(new GradientPaint(0,0,new Color(255,176,136,95),w,h,new Color(82,91,105,40)));g.drawRoundRect(0,0,w-1,h-1,26,26);g.dispose();}
        };card.setOpaque(false);card.setBorder(new EmptyBorder(22,30,22,30));add(card);
        GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=0;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(0,0,8,0);
        JComponent brand=new JComponent(){@Override protected void paintComponent(Graphics graphics){Graphics2D g=Visual.quality(graphics);Visual.brand(g,getWidth()/2,49,43);g.dispose();}};brand.setName("loginMarca");brand.setPreferredSize(new Dimension(360,72));card.add(brand,c);
        c.gridy++;card.add(text("Entre na sua academia",25,true,Visual.INK),c);
        c.gridy++;c.insets=new Insets(0,0,19,0);card.add(text("Academia Movimento",13,false,Visual.MUTED),c);
        user.setName("loginUsuario");password.setName("loginSenha");input(user);input(password);user.getAccessibleContext().setAccessibleName("Usuário");password.getAccessibleContext().setAccessibleName("Senha");
        c.gridy++;c.insets=new Insets(0,0,13,0);card.add(field("Usuário",user),c);
        JPanel secret=new JPanel(new BorderLayout(8,0));secret.setOpaque(false);secret.add(password,BorderLayout.CENTER);show.setName("loginMostrarSenha");show.setPreferredSize(new Dimension(82,46));show.setFont(Visual.font(11,true));show.setToolTipText("Mostrar ou ocultar a senha digitada");show.addActionListener(event->{revealed=!revealed;password.setEchoChar(revealed?0:'\u2022');show.setText(revealed?"Ocultar":"Mostrar");show.getAccessibleContext().setAccessibleName(revealed?"Ocultar senha":"Mostrar senha");password.requestFocusInWindow();});secret.add(show,BorderLayout.EAST);
        c.gridy++;c.insets=new Insets(0,0,3,0);card.add(field("Senha",secret),c);
        caps.setFont(Visual.font(11,false));caps.setForeground(Visual.ORANGE);caps.setPreferredSize(new Dimension(320,20));caps.setName("loginCapsLock");c.gridy++;c.insets=new Insets(0,0,10,0);card.add(caps,c);
        KeyAdapter keys=new KeyAdapter(){@Override public void keyReleased(KeyEvent event){caps();}};password.addKeyListener(keys);password.addFocusListener(new FocusAdapter(){@Override public void focusGained(FocusEvent event){caps();}});
        enter.setName("loginEntrar");enter.setIcon(new Visual.Glyph("seta",Visual.DARK,18));enter.setIconTextGap(12);enter.setPreferredSize(new Dimension(340,48));enter.addActionListener(event->authenticate());c.gridy++;c.insets=new Insets(0,0,7,0);card.add(enter,c);
        error.setName("loginErro");error.setFont(Visual.font(12,false));error.setForeground(new Color(255,158,157));error.setPreferredSize(new Dimension(340,39));error.getAccessibleContext().setAccessibleName("Resultado do acesso");c.gridy++;c.insets=new Insets(0,0,7,0);card.add(error,c);
        accounts.setName("loginContas");accounts.setFont(Visual.font(12,false));accounts.setPreferredSize(new Dimension(340,35));accounts.addActionListener(event->accounts());c.gridy++;c.insets=new Insets(0,0,12,0);card.add(accounts,c);
        c.gridy++;c.insets=new Insets(0,0,0,0);JLabel foot=text("Acesso da administração e da recepção",11,false,Visual.MUTED);foot.setHorizontalAlignment(SwingConstants.CENTER);card.add(foot,c);
    }
    JButton defaultButton(){return enter;}
    @Override public void addNotify(){super.addNotify();SwingUtilities.invokeLater(()->{if(isDisplayable())user.requestFocusInWindow();});}
    @Override public void removeNotify(){background=null;super.removeNotify();}
    @Override public void doLayout(){int width=Math.min(438,Math.max(300,getWidth()-48)),height=card.getPreferredSize().height;card.setBounds((getWidth()-width)/2,Math.max(14,(getHeight()-height)/2),width,height);}
    private static JLabel text(String value,int size,boolean bold,Color color){JLabel label=new JLabel(value);label.setFont(Visual.font(size,bold));label.setForeground(color);return label;}
    private static void input(JTextField field){field.setFont(Visual.font(15,false));field.setForeground(Visual.INK);field.setCaretColor(Visual.ORANGE);field.setBackground(Visual.INPUT);field.setBorder(new Visual.InputBorder());field.setPreferredSize(new Dimension(270,46));}
    private static JPanel field(String title,JComponent field){JPanel group=new JPanel(new BorderLayout(0,7));group.setOpaque(false);JLabel label=text(title,12,true,Visual.MUTED);label.setLabelFor(field);group.add(label,BorderLayout.NORTH);group.add(field,BorderLayout.CENTER);return group;}
    private void caps(){try{caps.setText(Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK)?"Caps Lock está ativado":" ");}catch(UnsupportedOperationException ignored){caps.setText(" ");}}
    private void message(String value){error.setText("<html>"+value+"</html>");error.getAccessibleContext().setAccessibleDescription(value);}
    private void loading(boolean value){busy=value;putClientProperty("loginBusy",value);user.setEnabled(!value);password.setEnabled(!value);show.setEnabled(!value);accounts.setEnabled(!value);enter.setEnabled(!value);enter.setText(value?"Entrando…":"Entrar");}
    private void authenticate(){
        if(busy)return;String login=user.getText().strip();char[] secret=password.getPassword();
        if(login.isEmpty()||secret.length==0){Arrays.fill(secret,'\0');message("Informe o usuário e a senha para entrar.");if(login.isEmpty())user.requestFocusInWindow();else password.requestFocusInWindow();return;}
        error.setText(" ");loading(true);
        new SwingWorker<AcademiaService.User,Void>(){
            @Override protected AcademiaService.User doInBackground()throws Exception{try{return service.login(login,new String(secret));}finally{Arrays.fill(secret,'\0');}}
            @Override protected void done(){
                try{get();password.setText("");access.open();}
                catch(InterruptedException interrupted){service.logout();message("O acesso foi interrompido. Tente novamente.");}
                catch(ExecutionException failed){service.logout();if(failed.getCause() instanceof IllegalArgumentException invalid)message(AcademiaService.INVALIDO.equals(invalid.getMessage())?"Usuário ou senha inválidos. Confira a senha ou consulte as contas da apresentação.":invalid.getMessage());else message("Não foi possível consultar o acesso. Feche o sistema e tente novamente.");password.requestFocusInWindow();password.selectAll();}
                catch(Exception failed){service.logout();message("Não foi possível abrir a área de trabalho. Tente novamente.");}
                finally{loading(false);}
            }
        }.execute();
    }
    private void accounts(){
        JPanel options=new JPanel(new GridLayout(0,1,0,14));options.setBackground(Visual.SURFACE);
        options.add(text("Contas iniciais da demonstração",17,true,Visual.INK));
        String[][] data={{"Administrador","admin","Admin@123"},{"Recepção","recepcao","Recepcao@123"}};
        JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(this),"Contas da apresentação",Dialog.ModalityType.APPLICATION_MODAL);
        for(String[] account:data){JPanel row=new JPanel(new BorderLayout(18,0));row.setBackground(Visual.SURFACE);JLabel values=new JLabel("<html><b>"+account[0]+"</b><br>Usuário: "+account[1]+"<br>Senha: "+account[2]+"</html>");values.setFont(Visual.font(13,false));values.setForeground(Visual.INK);row.add(values,BorderLayout.CENTER);JButton fill=new Visual.SoftButton("Preencher",true,false);fill.setName("conta-"+account[1]);fill.addActionListener(event->{user.setText(account[1]);password.setText(account[2]);error.setText(" ");dialog.dispose();enter.requestFocusInWindow();});row.add(fill,BorderLayout.EAST);options.add(row);}
        options.add(text("O preenchimento mantém a conferência de login e senha.",11,false,Visual.MUTED));options.setBorder(new EmptyBorder(26,26,26,26));dialog.setContentPane(options);dialog.pack();dialog.setLocationRelativeTo(this);dialog.setResizable(false);dialog.setVisible(true);
    }
    @Override protected void paintComponent(Graphics graphics){
        super.paintComponent(graphics);int w=getWidth(),h=getHeight();if(w<1||h<1)return;
        double scale=Math.max(Math.abs(((Graphics2D)graphics).getTransform().getScaleX()),Math.abs(((Graphics2D)graphics).getTransform().getScaleY()));
        if(background==null||backgroundWidth!=w||backgroundHeight!=h||backgroundScale!=scale){
            backgroundWidth=w;backgroundHeight=h;backgroundScale=scale;background=new BufferedImage((int)Math.ceil(w*scale),(int)Math.ceil(h*scale),BufferedImage.TYPE_INT_RGB);Graphics2D original=background.createGraphics(),g=Visual.quality(original);original.dispose();g.scale(scale,scale);g.setColor(Visual.DARK);g.fillRect(0,0,w,h);BufferedImage photo=Visual.image("login");
            if(photo!=null){double ratio=Math.min((w-48.0)/photo.getWidth(),(h-36.0)/photo.getHeight());int pw=(int)(photo.getWidth()*ratio),ph=(int)(photo.getHeight()*ratio);g.setComposite(AlphaComposite.SrcOver.derive(.70f));g.drawImage(photo,(w-pw)/2,(h-ph)/2,pw,ph,null);g.setComposite(AlphaComposite.SrcOver);}
            g.setPaint(new RadialGradientPaint(w*.5f,h*.35f,Math.max(w,h)*.8f,new float[]{0,.5f,1},new Color[]{new Color(15,17,20,35),new Color(15,17,20,130),new Color(9,11,15,240)}));g.fillRect(0,0,w,h);g.setColor(new Color(153,161,175,35));g.drawLine(28,25,w-28,25);g.drawLine(28,h-26,w-28,h-26);g.dispose();
        }
        graphics.drawImage(background,0,0,w,h,null);
    }
}
