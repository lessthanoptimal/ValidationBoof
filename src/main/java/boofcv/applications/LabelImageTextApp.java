package boofcv.applications;

import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.DogArray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Application for labeling text inside of images using various types of bounding boxes.
 *
 * @author Peter Abeles
 */
public class LabelImageTextApp extends JPanel {
    // TODO assign text to a region
    // TODO delete a point
    // TODO delete a region
    // TODO keep on moving a corner while mouse is down
    // TODO add ability to select AABB
    // TODO rotated BB

    ImageDisplay display = new ImageDisplay();
    Controls controls = new Controls();

    DogArray<LabeledText> labeled = new DogArray<>(LabeledText::new, LabeledText::reset);

    // Which labeled object is being manipulated
    int activeIdx = -1;
    // Which point on the bounding polygon is being manipulated
    int selectedPoint = -1;

    // Currently open file
    File fileImage = new File("");
    File fileLabel = new File("");

    static {
        BoofSwingUtil.initializeSwing();
    }

    public LabelImageTextApp() {
        super(new BorderLayout());

        display.setListener(scale -> controls.setZoom(scale));
        display.getImagePanel().requestFocus();
        display.getImagePanel().addMouseListener(new HandleMouse());

        add(controls, BorderLayout.WEST);
        add(display, BorderLayout.CENTER);
    }

    public JMenuBar createMenuBar() {
        var menuBar = new JMenuBar();

        JMenu menuFile = new JMenu("File");
        menuFile.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menuFile);

        var itemOpen = new JMenuItem("Open Image");
        itemOpen.addActionListener(e -> openImage());
        BoofSwingUtil.setMenuItemKeys(itemOpen, KeyEvent.VK_O, KeyEvent.VK_O);
        menuFile.add(itemOpen);

        // TODO open labels
        // TODO save labels
        // TODO saveAs labels

        return menuBar;
    }

    public void openImage() {
        File file = BoofSwingUtil.fileChooser(getClass().getSimpleName(), display, true, ".", null,
                BoofSwingUtil.FileTypes.IMAGES);
        if (file == null)
            return;

        BufferedImage image = UtilImageIO.loadImage(file.getPath());
        if (image == null) {
            JOptionPane.showMessageDialog(display, "Could not load image");
            return;
        }

        // clear selection
        activeIdx = -1;
        selectedPoint = -1;

        fileImage = file;
        // By default, it stores labeled images right next to the input image
        fileLabel = new File(file.getParentFile(), file.getName() + ".txt");
        if (fileLabel.exists()) {
            parseLabeled(fileLabel);
        }
        // Update the display image
        display.setImage(image);
    }

    public void openSelectedFiles() {
        BufferedImage image = UtilImageIO.loadImageNotNull(fileImage.getPath());
        display.setImage(image);
        parseLabeled(fileLabel);
        System.out.println("text.size=" + labeled.size);

        SwingUtilities.invokeLater(() -> controls.setImageSize(image.getWidth(), image.getHeight()));
    }

    public void openNext() {

    }

    private void parseLabeled(File f) {
        labeled.reset();

        try (var input = new BufferedReader(new FileReader(f))) {
            boolean foundTiming = false;
            boolean readPolygon = true;
            String line = input.readLine();
            while (line != null) {
                try {
                    if (line.isEmpty())
                        continue;

                    // skip comments
                    if (line.charAt(0) == '#')
                        continue;

                    if (foundTiming) {
                        if (readPolygon) {
                            LabeledText label = labeled.grow();
                            String[] words = line.split(" ");
                            int count = Integer.parseInt(words[0]);
                            for (int i = 0; i < count; i++) {
                                Point2D_F64 v = label.region.vertexes.grow();
                                v.x = Double.parseDouble(words[i * 2 + 1]);
                                v.y = Double.parseDouble(words[i * 2 + 2]);
                            }
                        } else {
                            byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                            byte[] decoded = Base64.getDecoder().decode(bytes);
                            labeled.getTail().text = new String(decoded, StandardCharsets.UTF_8);
                        }
                        readPolygon = !readPolygon;
                    } else if (line.startsWith("milliseconds")) {
                        foundTiming = true;
                    }
                } finally {
                    line = input.readLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public class ImageDisplay extends ImageZoomPanel {
        Line2D.Double line = new Line2D.Double();
        Ellipse2D.Double ellipse = new Ellipse2D.Double();
        Path2D path = new java.awt.geom.Path2D.Double();
        BasicStroke stroke = new BasicStroke(5.0f);

        @Override
        protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
            BoofSwingUtil.antialiasing(g2);

            g2.setStroke(new BasicStroke(5));

            for (int i = 0; i < labeled.size; i++) {
                LabeledText label = labeled.get(i);

                // Highlight the selected region
                if (i == activeIdx) {
                    g2.setColor(Color.RED);
                } else {
                    g2.setColor(new Color(125, 255, 255));
                }
                renderPolygon(g2, label.region, false);

                // TODO highlight selected corner

                // Make sure it has text assigned to it
                if (label.text.isEmpty())
                    continue;

                double size = label.region.getSideLength(1);

                g2.setFont(new Font("Serif", Font.BOLD, (int) (size * 0.4)));
                g2.setColor(Color.GREEN);
                Point2D_F64 p = label.region.get(0);
                g2.drawString(label.text, (float) (scale * p.x), (float) (scale * p.y));
            }
        }

        void renderPolygon(Graphics2D g2, Polygon2D_F64 polygon, boolean filled) {
            Point2D_F64 p = polygon.get(0);
            path.reset();
            path.moveTo(p.x * scale, p.y * scale);
            for (int i = 1; i <= polygon.size(); ++i) {
                p = polygon.get(i % polygon.size());
                path.lineTo(p.x * scale, p.y * scale);
            }
            if (filled)
                g2.fill(path);
            else
                g2.draw(path);
        }
    }

    public class Controls extends DetectBlackShapePanel {
        public Controls() {
            selectZoom = spinner(1, MIN_ZOOM, MAX_ZOOM, 1.0);
            display.addMouseWheelListener((e) -> {
                setZoom(BoofSwingUtil.mouseWheelImageZoom(zoom, e));
            });

            addLabeled(imageSizeLabel, "Size");
            addLabeled(selectZoom, "Zoom");

            setPreferredSize(new Dimension(250, 200));
        }

        @Override
        public void controlChanged(Object source) {
            if (source == selectZoom) {
                zoom = ((Number) selectZoom.getValue()).doubleValue();
                display.setScale(zoom);
            }
        }
    }

    /**
     * Let's the user select and adjust text regions
     */
    public class HandleMouse extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            display.requestFocus();
            // Original image coordinates, accounting for scale
            Point2D_F64 p = display.pixelToPoint(e.getX(), e.getY());

            // Center view if right mouse button
            if (SwingUtilities.isRightMouseButton(e)) {
                display.centerView(p.x, p.y);
                return;
            }

            // Did the user click an existing corner?
            if (checkAndHandleClickedCorner(p.x ,p.y)) {
                display.repaint();
                return;
            }

            // nothing is selected, start a new polygon
            if (activeIdx == -1) {
                activeIdx = labeled.size();
                labeled.grow();
            }
            LabeledText active = labeled.get(activeIdx);

            // current polygon is at max sides. start a new one
            if (active.region.size() == 4) {
                activeIdx = labeled.size();
                labeled.grow();
                active = labeled.getTail();
            }

            selectedPoint = active.region.size();
            active.region.vertexes.grow().setTo(p.x, p.y);
            display.repaint();
        }
    }

    private boolean checkAndHandleClickedCorner( double cx , double cy ) {
        double tol = 10.0/display.getScale();
        for( int i = 0; i < labeled.size(); i++ ) {
            Polygon2D_F64 polygon = labeled.get(i).region;
            int matched = -1;
            for (int j = 0; j < polygon.size(); j++) {
                if( polygon.get(j).distance(cx,cy) <= tol ) {
                    matched = j;
                    break;
                }
            }

            if( matched >= 0 ) {
                activeIdx = i;
                selectedPoint = matched;
                return true;
            }
        }
        return false;
    }

    private static class LabeledText {
        public String text = "";
        public Polygon2D_F64 region = new Polygon2D_F64();

        public void reset() {
            text = "";
            region.vertexes.reset();
        }
    }

    public static void main(String[] args) {
        var app = new LabelImageTextApp();
//        app.fileImage = new File("/home/pja/projects/ninox360/DanfossOCR/dataset/11_13_2022/IMG_0012.JPG");
//        app.fileLabel = new File("/home/pja/projects/ninox360/DanfossOCR/libs/paddleocr/results/11_13_2022/IMG_0012.txt");

        SwingUtilities.invokeLater(() -> {
            app.openImage();
            JFrame window = ShowImages.showWindow(app, "Text Labeler");
            window.setJMenuBar(app.createMenuBar());
        });
    }
}
