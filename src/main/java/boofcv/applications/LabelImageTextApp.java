package boofcv.applications;

import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Application for labeling text inside of images using various types of bounding boxes.
 *
 * @author Peter Abeles
 */
public class LabelImageTextApp extends JPanel {
    // TODO keep on moving a corner while mouse is down
    // TODO add ability to select AABB
    // TODO rotated BB

    JFrame window;
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

    String fileChooserPrefName = getClass().getSimpleName();

    static {
        BoofSwingUtil.initializeSwing();
    }

    public LabelImageTextApp() {
        super(new BorderLayout());

        display.setListener(scale -> controls.setZoom(scale));
        display.getImagePanel().requestFocus();
        display.getImagePanel().addMouseListener(new HandleMouse());

        display.addKeyListener(new KeyControls());

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

        var itemNext = new JMenuItem("Open Next");
        itemNext.addActionListener(e -> openNext());
        BoofSwingUtil.setMenuItemKeys(itemNext, KeyEvent.VK_I, KeyEvent.VK_I);
        menuFile.add(itemNext);

        var itemSave = new JMenuItem("Save Labels");
        itemSave.addActionListener(e -> saveLabels(fileLabel));
        BoofSwingUtil.setMenuItemKeys(itemSave, KeyEvent.VK_S, KeyEvent.VK_S);
        menuFile.add(itemSave);

        // Let the user select the file to save to
        var itemSaveAs = new JMenuItem("Save Labels As");
        itemSaveAs.addActionListener(e -> {
            File f = BoofSwingUtil.fileChooser(fileChooserPrefName, display, false, ".", null);
            if (f == null || f.isDirectory())
                return;
            saveLabels(f);
        });
        menuFile.add(itemSaveAs);

        // TODO open labels

        return menuBar;
    }

    public void openImage() {
        File file = BoofSwingUtil.fileChooser(fileChooserPrefName, display, true, ".", null,
                BoofSwingUtil.FileTypes.IMAGES);
        if (file == null)
            return;

        if (!openImageFile(file)) {
            JOptionPane.showMessageDialog(display, "Could not load image");
        }
    }

    private boolean openImageFile(File file) {
        BoofSwingUtil.checkGuiThread();
        BufferedImage image = UtilImageIO.loadImage(file.getPath());
        if (image == null) {
            return false;
        }

        // clear selection
        labeled.reset();
        controls.regionLabel.setText("");
        activeIdx = -1;
        selectedPoint = -1;

        fileImage = file;
        // By default, it stores labeled images right next to the input image
        fileLabel = new File(file.getParentFile(), FilenameUtils.getBaseName(file.getName()) + ".txt");
        if (fileLabel.exists()) {
            parseLabeled(fileLabel);
        }
        // Update the display image
        display.setImage(image);
        display.repaint();

        if (window != null)
            window.setTitle("Text Labeler: " + file.getName());
        return true;
    }

    public void openSelectedFiles() {
        BufferedImage image = UtilImageIO.loadImageNotNull(fileImage.getPath());
        display.setImage(image);
        parseLabeled(fileLabel);
        System.out.println("text.size=" + labeled.size);

        SwingUtilities.invokeLater(() -> controls.setImageSize(image.getWidth(), image.getHeight()));
    }

    /**
     * Opens the next image in the parent direction based on alphabetical order
     */
    public void openNext() {
        System.out.println("openNext");
        // Save current work in progress
        saveLabels(fileLabel);

        // Find the next image
        File parent = fileImage.getParentFile();
        List<String> images = UtilIO.listImages(parent.getPath(), true);

        int currentIdx = images.indexOf(fileImage.getAbsolutePath());

        // Return if it's at the last image or it couldn't find the image
        if (currentIdx < 0 || currentIdx + 1 >= images.size())
            return;

        // Attempt to open the next image
        File f = new File(images.get(currentIdx + 1));

        System.out.println("   opening " + f.getPath());

        if (!openImageFile(f)) {
            JOptionPane.showMessageDialog(display, "Failed to open " + f.getName());
        } else {
            System.out.println(f.getParent());
        }
    }

    /**
     * Reads a labeled file
     */
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
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Saves the labels to disk
     */
    private void saveLabels(File f) {
        Base64.Encoder encoder = Base64.getEncoder();
        try (var output = new BufferedWriter((new FileWriter(f)))) {
            output.write("# Handle labeled image " + f.getName() + "\n");
            output.write("milliseconds NaN\n");
            for (int i = 0; i < labeled.size; i++) {
                LabeledText l = labeled.get(i);
                output.write("" + l.region.size());
                for (int pointIdx = 0; pointIdx < l.region.size(); pointIdx++) {
                    Point2D_F64 p = l.region.get(pointIdx);
                    output.write(" " + p.x + " " + p.y);
                }
                output.write('\n');
                byte[] bytes = encoder.encode(l.text.getBytes(StandardCharsets.UTF_8));
                output.write(new String(bytes, StandardCharsets.UTF_8) + "\n");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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

                double size = Math.max(8, label.smallestSide());

                g2.setFont(new Font("Serif", Font.BOLD, (int) (scale * size * 0.7)));
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
        JTextField regionLabel = textfield(100, 100);

        public Controls() {
            selectZoom = spinner(1, MIN_ZOOM, MAX_ZOOM, 1.0);
            display.addMouseWheelListener((e) -> {
                setZoom(BoofSwingUtil.mouseWheelImageZoom(zoom, e));
            });

            addLabeled(imageSizeLabel, "Size");
            addLabeled(selectZoom, "Zoom");
            addAlignCenter(new JLabel("Region Text"));
            addAlignCenter(regionLabel, "Text that's assigned to a region");

            setPreferredSize(new Dimension(250, 200));
        }

        public JTextField textfield(int panelWidth, int panelHeight) {
            JTextField field = new JTextField();
            field.addActionListener(this);
            field.setPreferredSize(new Dimension(panelWidth, panelHeight));
            return field;
        }

        @Override
        public void controlChanged(final Object source) {
            if (source == selectZoom) {
                zoom = ((Number) selectZoom.getValue()).doubleValue();
                display.setScale(zoom);
            } else if (source == regionLabel) {
                if (activeIdx == -1 && activeIdx < labeled.size)
                    return;
                LabeledText region = labeled.get(activeIdx);
                region.text = regionLabel.getText();
                display.repaint();
            }
        }
    }

    /**
     * Lets the user select and adjust text regions
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
            if (checkAndHandleClickedCorner(p.x, p.y)) {
                display.repaint();
                return;
            }

            // nothing is selected, start a new polygon
            if (activeIdx == -1) {
                controls.regionLabel.setText("");
                activeIdx = labeled.size();
                labeled.grow();
            }
            LabeledText active = labeled.get(activeIdx);

            // current polygon is at max sides. start a new one
            if (active.region.size() == 4) {
                controls.regionLabel.setText("");
                activeIdx = labeled.size();
                labeled.grow();
                active = labeled.getTail();
            }

            selectedPoint = active.region.size();
            active.region.vertexes.grow().setTo(p.x, p.y);
            display.repaint();
        }
    }

    /**
     * Use the keyboard to manipulate labels
     */
    class KeyControls extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (activeIdx < 0 || selectedPoint < 0 || activeIdx >= labeled.size)
                return;
            switch (e.getKeyCode()) {
                // Delete selected region
                case KeyEvent.VK_DELETE, KeyEvent.VK_BACK_SPACE -> {
                    controls.regionLabel.setText("");
                    labeled.remove(activeIdx);
                    activeIdx = -1;
                }
            }
            display.repaint();
        }
    }

    private boolean checkAndHandleClickedCorner(double cx, double cy) {
        double tol = 10.0 / display.getScale();
        for (int i = 0; i < labeled.size(); i++) {
            Polygon2D_F64 polygon = labeled.get(i).region;
            int matched = -1;
            for (int j = 0; j < polygon.size(); j++) {
                if (polygon.get(j).distance(cx, cy) <= tol) {
                    matched = j;
                    break;
                }
            }

            if (matched >= 0) {
                activeIdx = i;
                selectedPoint = matched;
                controls.regionLabel.setText(labeled.get(i).text);
                return true;
            }
        }
        return false;
    }

    /**
     * A labeled region in the image. Specifies a polygon and text associated with it.
     */
    private static class LabeledText {
        public String text = "";
        public Polygon2D_F64 region = new Polygon2D_F64();

        public double smallestSide() {
            double size = Double.MAX_VALUE;
            for (int i = 0; i < region.size(); i++) {
                size = Math.min(size, region.getSideLength(i));
            }
            return size;
        }

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
            JFrame window = ShowImages.showWindow(app, "Text Labeler", true);
            app.window = window;
            window.setJMenuBar(app.createMenuBar());
        });
    }
}
