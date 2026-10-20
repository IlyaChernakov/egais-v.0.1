package app;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import act.GetActsWriteOffEgais;
import ui.MainFrame;

/**
 * Точка входа программы.
 * <p>
 * Без аргументов открывается графический интерфейс. Если переданы аргументы,
 * программа работает как консольная утилита (см. {@link act.GetActsWriteOffEgais}).
 */
public final class EgaisWriteOffApp {

    private EgaisWriteOffApp() {
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            System.exit(GetActsWriteOffEgais.run(args));
        }
        launchGui();
    }

    private static void launchGui() {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Графический интерфейс недоступен в этой среде.");
            GetActsWriteOffEgais.printUsage(System.err);
            System.exit(2);
        }
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Оставляем стандартный внешний вид — программа остаётся работоспособной.
            }
            new MainFrame().setVisible(true);
        });
    }
}
