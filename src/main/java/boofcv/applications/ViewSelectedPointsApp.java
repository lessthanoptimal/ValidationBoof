package boofcv.applications;

import boofcv.common.parsing.ObservedLandmarkMarkers;
import boofcv.common.parsing.ParseCalibrationConfigFiles;
import boofcv.common.parsing.UniqueMarkerObserved;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.controls.JSpinnerNumber;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Used to open and display previously selected points inside an image
 *
 * @author Peter Abeles
 */
public class ViewSelectedPointsApp extends JPanel {

    DisplayPanel display = new DisplayPanel();
    ControlPanel controls = new ControlPanel();

    protected JMenuBar menuBar;

    // Window the application is shown in
    protected JFrame window;

    // name of the application
    String appName;

    ObservedLandmarkMarkers landmarks = new ObservedLandmarkMarkers();

    public ViewSelectedPointsApp() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1000, 1000));
        setupMenuBar();
        add(BorderLayout.WEST, controls);
        add(BorderLayout.CENTER, display);
    }

    protected void setupMenuBar() {
        menuBar = new JMenuBar();

        JMenu menuFile = new JMenu("File");
        menuFile.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menuFile);

        var itemOpenImage = new JMenuItem("Open Image");
        BoofSwingUtil.setMenuItemKeys(itemOpenImage, KeyEvent.VK_I, KeyEvent.VK_I);
        itemOpenImage.addActionListener((e) -> openImage());
        menuFile.add(itemOpenImage);

        var itemOpenFile = new JMenuItem("Open Points File");
        BoofSwingUtil.setMenuItemKeys(itemOpenFile, KeyEvent.VK_O, KeyEvent.VK_O);
        itemOpenFile.addActionListener((e) -> openPoints());
        menuFile.add(itemOpenFile);
    }

    private void openImage() {
        File file = BoofSwingUtil.openFileChooser(ViewSelectedPointsApp.this, BoofSwingUtil.FileTypes.IMAGES);
        if (file == null)
            return;

        BufferedImage image = UtilImageIO.loadImage(file.getPath());
        if (image == null) {
            JOptionPane.showMessageDialog(this, "Failed to open image");
            return;
        }

        display.setImage(image);
        display.repaint();
    }

    private void openPoints() {
        File file = BoofSwingUtil.openFileChooser(ViewSelectedPointsApp.this, BoofSwingUtil.FileTypes.FILES);
        if (file == null)
            return;

        try {
            landmarks = ParseCalibrationConfigFiles.parseObservedLandmarkMarker(file);
            display.repaint();
            return;
        } catch (RuntimeException ignore){}
        try {
            List<UniqueMarkerObserved> doc = ParseCalibrationConfigFiles.parseUniqueMarkerTruth(file);
            ObservedLandmarkMarkers obs = new ObservedLandmarkMarkers();
            for (UniqueMarkerObserved uniqueMarkerObserved : doc) {
                obs.markers.grow().setTo(uniqueMarkerObserved);
            }
            this.landmarks = obs;
            display.repaint();
        } catch (RuntimeException ignore){}
    }

    class DisplayPanel extends ShapeVisualizePanel {
        @Override
        protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
            super.paintInPanel(tran, g2);
            BoofSwingUtil.antialiasing(g2);


            ObservedLandmarkMarkers landmarks = ViewSelectedPointsApp.this.landmarks;
            landmarks.markers.forEach(marker->{
                marker.landmarks.forEach(p->{
                    VisualizeFeatures.drawPoint(g2, scale*p.p.x, scale*p.p.y, 5, Color.RED, true);
                });
            });
        }

        @Override
        public void setScale( double scale ) {
            controls.setZoom(scale);
            super.setScale(controls.selectZoom.value.doubleValue());
        }
    }

    class ControlPanel extends StandardAlgConfigPanel {
        protected JSpinnerNumber selectZoom = spinnerWrap(1.0, MIN_ZOOM, MAX_ZOOM, 1.0);
        protected JLabel imageSizeLabel = new JLabel();

        public ControlPanel() {
            addLabeled(imageSizeLabel, "Image Size");
            addLabeled(selectZoom.spinner, "Zoom");
        }

        @Override
        public void controlChanged(Object source) {
            if (source == selectZoom.spinner) {
                selectZoom.value = (Number) selectZoom.spinner.getValue();
                display.setScale(selectZoom.value.doubleValue());
            }
        }

        public void setZoom(double _zoom) {
            _zoom = Math.max(MIN_ZOOM, _zoom);
            _zoom = Math.min(MAX_ZOOM, _zoom);
            if (_zoom == selectZoom.value.doubleValue())
                return;
            selectZoom.value = _zoom;

            BoofSwingUtil.invokeNowOrLater(() -> selectZoom.spinner.setValue(selectZoom.value));
        }

        public void setImageSize(final int width, final int height) {
            BoofSwingUtil.invokeNowOrLater(() -> imageSizeLabel.setText(width + " x " + height));
        }
    }

    public void displayImmediate( String appName ) {
        this.appName = appName;
        window = ShowImages.showWindow(this, appName, true);
        window.setJMenuBar(menuBar);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            var app = new ViewSelectedPointsApp();
            app.displayImmediate("Selected Point Viewer");
        });
    }
}
