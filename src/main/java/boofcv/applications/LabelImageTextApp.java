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
    // TODO let user select and open a file
    // TODO create left control panel with zoom and stuff
    // TODO add ability to select AABB
    // TODO rotated BB

    ImageDisplay display = new ImageDisplay();
    Controls controls = new Controls();

    DogArray<LabeledText> labeled = new DogArray<>(LabeledText::new, LabeledText::reset);

    // Currently open file
    File fileImage = new File("");
    File fileLabel = new File("");

    static {
        BoofSwingUtil.initializeSwing();
    }

    public LabelImageTextApp() {
        super(new BorderLayout());

        display.setListener(scale -> controls.setZoom(scale));

        add(controls, BorderLayout.WEST);
        add(display, BorderLayout.CENTER);
    }

    public JMenuBar createMenuBar() {
        var menuBar = new JMenuBar();

        return menuBar;
    }

    public void openFile() {
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

                g2.setColor(Color.RED);
                renderPolygon(g2, label.region, false);

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
        app.fileImage = new File("/home/pja/projects/ninox360/DanfossOCR/dataset/11_13_2022/IMG_0012.JPG");
        app.fileLabel = new File("/home/pja/projects/ninox360/DanfossOCR/libs/paddleocr/results/11_13_2022/IMG_0012.txt");

        SwingUtilities.invokeLater(() -> {
            app.openFile();
            ShowImages.showWindow(app, "Text Labeler");
        });
    }
}
