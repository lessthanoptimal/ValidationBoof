package boofcv.applications;

import boofcv.common.misc.EllipseFileCodec;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author Peter Abeles
 */
public class HandSelectEllipsesApp extends HandSelectBase {


    public HandSelectEllipsesApp(@Nullable File file) {
        super(new SelectEllipsePanel(false), file);
    }

    @Override
    public void process(File file, BufferedImage image) {
        this.infoPanel.setImageShape(image.getWidth(), image.getHeight());
        SelectEllipsePanel gui = (SelectEllipsePanel) this.imagePanel;

        File outputFile = selectOutputFile(file);
        if (outputFile.exists()) {
            gui.list.addAll(EllipseFileCodec.load(outputFile.getPath()));
        }

        gui.setImage(image);
    }

    @Override
    public void save() {
        SelectEllipsePanel gui = (SelectEllipsePanel) this.imagePanel;
        File outputFile = selectOutputFile(inputFile);
        EllipseFileCodec.save(outputFile.getPath(), " rotated ellipses. x y a b phi", gui.list);
    }

    @Override
    public String getApplicationName() {
        return "Select Ellipses";
    }

    @Override
    public void setScale(double scale) {
        SelectEllipsePanel gui = (SelectEllipsePanel) this.imagePanel;
        gui.setScale(scale);
    }

    @Override
    public void clearPoints() {
        SelectEllipsePanel gui = (SelectEllipsePanel) this.imagePanel;
        gui.clearPoints();
        gui.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HandSelectEllipsesApp(null));
    }
}
