package br.com.pulso;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/** Entrada curta com fotografia em cache, luz, partículas e abertura lateral. */
final class OpeningPanel extends JPanel {
    static final int DURATION_MS=2600;
    private final Runnable finish;
    private Timer timer;
    private long began;
    private float progress;
    private boolean completed;
    private BufferedImage scene,aura;
    private int sceneWidth,sceneHeight;
    private double sceneScale;

    OpeningPanel(Runnable finish){this.finish=finish;setBackground(Visual.DARK);setName("aberturaPulso");getAccessibleContext().setAccessibleName("Abertura Pulso");}
    @Override public void addNotify(){
        super.addNotify();
        if(!Visual.MOTION){SwingUtilities.invokeLater(()->{if(isDisplayable())complete();});return;}
        began=System.nanoTime();timer=new Timer(16,event->{progress=Math.min(1,(System.nanoTime()-began)/(DURATION_MS*1_000_000f));repaint();if(progress>=1)complete();});timer.setCoalesce(true);timer.start();
    }
    private void complete(){if(completed)return;completed=true;if(timer!=null)timer.stop();finish.run();}
    @Override public void removeNotify(){if(timer!=null)timer.stop();scene=null;aura=null;super.removeNotify();}
    private static float phase(float p,float from,float to){float t=Math.max(0,Math.min(1,(p-from)/(to-from)));return t*t*(3-2*t);}
    private void prepare(int w,int h,double scale){
        if(scene!=null&&sceneWidth==w&&sceneHeight==h&&sceneScale==scale)return;
        sceneWidth=w;sceneHeight=h;sceneScale=scale;scene=new BufferedImage((int)Math.ceil(w*scale),(int)Math.ceil(h*scale),BufferedImage.TYPE_INT_RGB);Graphics2D original=scene.createGraphics(),g=Visual.quality(original);original.dispose();g.scale(scale,scale);
        g.setColor(Visual.DARK);g.fillRect(0,0,w,h);BufferedImage photo=Visual.image("abertura");
        if(photo!=null){
            double ratio=Math.min((w-64.0)/photo.getWidth(),(h-78.0)/photo.getHeight());int pw=(int)(photo.getWidth()*ratio),ph=(int)(photo.getHeight()*ratio),x=(w-pw)/2,y=(h-ph)/2;
            Graphics2D picture=(Graphics2D)g.create();picture.clip(new RoundRectangle2D.Double(x,y,pw,ph,16,16));picture.drawImage(photo,x,y,pw,ph,null);picture.dispose();
            g.setPaint(new GradientPaint(0,h*.28f,new Color(15,17,20,0),0,h*.93f,new Color(15,17,20,240)));g.fillRect(0,0,w,h);
            g.setColor(new Color(255,193,154,48));g.draw(new RoundRectangle2D.Double(x,y,pw,ph,16,16));
        }
        g.dispose();
        // A luz difusa também é preparada uma vez, evitando recalcular o degradê a cada quadro.
        aura=new BufferedImage(scene.getWidth(),scene.getHeight(),BufferedImage.TYPE_INT_ARGB_PRE);Graphics2D light=aura.createGraphics();light.scale(scale,scale);light.setPaint(new RadialGradientPaint(w/2f,h*.67f,Math.min(w,h)/2f,new float[]{0,.5f,1},new Color[]{new Color(255,139,99,26),new Color(255,139,99,9),new Color(255,139,99,0)}));light.fillRect(0,0,w,h);light.dispose();
    }
    @Override protected void paintComponent(Graphics graphics){
        super.paintComponent(graphics);int w=getWidth(),h=getHeight();if(w<1||h<1)return;Graphics2D g=Visual.quality(graphics);double scale=Math.max(Math.abs(g.getTransform().getScaleX()),Math.abs(g.getTransform().getScaleY()));prepare(w,h,scale);
        float reveal=phase(progress,0,.19f),title=phase(progress,.12f,.44f),open=phase(progress,0,.48f);g.setComposite(AlphaComposite.SrcOver.derive(reveal));g.drawImage(scene,0,0,w,h,null);
        // Os dois painéis se abrem pelas bordas, sem indicador de carregamento.
        g.setComposite(AlphaComposite.SrcOver);g.setColor(Visual.DARK);int shutter=(int)(w*.5*(1-open));g.fillRect(0,0,shutter,h);g.fillRect(w-shutter,0,shutter,h);
        float light=(float)Math.sin(Math.PI*phase(progress,.04f,.82f));int cy=(int)(h*.67);
        g.setComposite(AlphaComposite.SrcOver.derive(light));g.drawImage(aura,0,0,w,h,null);g.setComposite(AlphaComposite.SrcOver);
        Graphics2D beam=(Graphics2D)g.create();beam.rotate(-.32,w/2.0,h/2.0);int bx=(int)(-w*.7+progress*w*2.5);
        beam.setPaint(new GradientPaint(bx,0,new Color(255,210,170,0),bx+110,0,new Color(255,210,170,(int)(24*light)),true));beam.fillRect(bx,-h,110,h*3);beam.dispose();
        for(int i=0;i<16;i++){double px=(i*.61803398875%1)*w,py=h*(.13+(i*.381966%1)*.7)-progress*(18+i*2);int alpha=(int)((20+(i%4)*13)*light);g.setColor(new Color(255,190,140,alpha));double d=i%3==0?3:1.6;g.fill(new Ellipse2D.Double(px,py,d,d));}
        g.setComposite(AlphaComposite.SrcOver.derive(title));int size=Math.min(102,Math.max(58,w/14)),baseline=cy+(int)((1-title)*22);Visual.brand(g,w/2,baseline,size);
        g.setFont(Visual.font(12,true));g.setColor(new Color(218,207,199));String name="ACADEMIA MOVIMENTO";g.drawString(name,(w-g.getFontMetrics().stringWidth(name))/2,baseline+34);
        float subtitle=phase(progress,.34f,.64f);g.setComposite(AlphaComposite.SrcOver.derive(subtitle));g.setFont(Visual.font(16,false));g.setColor(new Color(193,194,199));String text="Cada movimento conta.";g.drawString(text,(w-g.getFontMetrics().stringWidth(text))/2,baseline+69+(int)((1-subtitle)*9));g.dispose();
    }
}
