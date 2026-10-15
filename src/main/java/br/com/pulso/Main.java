package br.com.pulso;

import javax.swing.*;
import java.nio.file.*;

public final class Main {
    public static void main(String[] args) {
        try {
            boolean screenshots=args.length>0 && args[0].equals("--screenshots");
            if(screenshots){System.setProperty("pulso.reduceMotion","true");System.setProperty("pulso.skipIntro","true");}
            Files.createDirectories(Path.of("data"));
            String url=screenshots?"jdbc:h2:mem:prints;DB_CLOSE_DELAY=-1":"jdbc:h2:file:./data/pulso";
            Database db=new Database(url); db.seed();
            if(args.length>0 && args[0].equals("--prepare-db")) return;
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            Visual.install();
            SwingUtilities.invokeLater(()-> {
                try {
                    AcademiaFrame frame=new AcademiaFrame(new AcademiaService(db));
                    if(screenshots) { frame.captureScreens(); frame.dispose(); System.exit(0); }
                    else frame.setVisible(true);
                } catch(Exception e) { e.printStackTrace(); JOptionPane.showMessageDialog(null,e.getMessage(),"Não foi possível abrir",JOptionPane.ERROR_MESSAGE); }
            });
        } catch(Exception e) { e.printStackTrace(); JOptionPane.showMessageDialog(null,e.getMessage(),"Não foi possível iniciar",JOptionPane.ERROR_MESSAGE); System.exit(1); }
    }
}
