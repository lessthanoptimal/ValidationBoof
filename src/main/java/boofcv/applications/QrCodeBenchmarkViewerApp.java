package boofcv.applications;

import boofcv.gui.BoofSwingUtil;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import georegression.struct.shapes.Polygon2D_F64;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class QrCodeBenchmarkViewerApp extends JPanel{
    ImageZoomPanel imagePanel = new ImageZoomPanel();

    File resultsDirectory = new File("/home/pja/projects/ValidationBoof/thirdparty/qrcode/results/zxing");

    JFrame window;

    File currentFile;

    public QrCodeBenchmarkViewerApp( File file ) {
        super(new BorderLayout());

        openFile(file);

        add(BorderLayout.CENTER,imagePanel);
        imagePanel.getImagePanel().setPreferredSize(new Dimension(800,800));

        window = ShowImages.showWindow(this,"QR Code Benchmark");
        window.setPreferredSize(new Dimension(800,800));
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        createMenuBar();
        window.setVisible(true);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menuFile = new JMenu("File");
        menuFile.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menuFile);

        JMenuItem menuItemFile = new JMenuItem("Open File");
        BoofSwingUtil.setMenuItemKeys(menuItemFile,KeyEvent.VK_O,KeyEvent.VK_O);
        menuItemFile.addActionListener(e->openFile(null));
        menuFile.add(menuItemFile);

        JMenuItem menuItemNext = new JMenuItem("Open Next File");
        BoofSwingUtil.setMenuItemKeys(menuItemNext,KeyEvent.VK_N,KeyEvent.VK_I);
        menuItemNext.addActionListener(e -> openNextFile());
        menuFile.add(menuItemNext);


        menuFile.addSeparator();
//        menuFile.add(menuItenQuit);

        window.setJMenuBar(menuBar);
    }

    public void openNextFile() {
        File parent = currentFile.getParentFile();
        if( parent == null )
            return;

        File[] files = parent.listFiles();
        if( files == null || files.length <= 1 )
            return;
        File closest = null;

        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            String name = f.getName().toLowerCase();
            // filter out common non image/video files
            if( name.endsWith(".txt") || name.endsWith(".yaml") || name.endsWith(".xml"))
                continue;

            if( currentFile.compareTo(f) < 0 ) {
                if( closest == null || closest.compareTo(f) > 0 ) {
                    closest = f;
                }
            }
        }

        if( closest != null ) {
            openFile(closest);
        }
    }

    public void openFile( File file ) {
        if( file == null ) {
            file = BoofSwingUtil.openFileChooser(null, BoofSwingUtil.FileTypes.IMAGES);
            if( file == null )
                return;
        }

        BufferedImage buffered = UtilImageIO.loadImage(file.getAbsolutePath());

        if( buffered == null )
            return;

        currentFile = file;
        System.out.println(file);

        try {
            String n = FilenameUtils.getBaseName(file.getName());
            File localPath = new File(file.getParentFile().getName(),n+".txt");
            File f = new File(resultsDirectory,localPath.getPath());
            if( f.exists() ) {
                List<Polygon2D_F64> locations = detections(f);

                System.out.println("Detected QR: "+locations.size());
                Graphics2D g2 = buffered.createGraphics();
                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke(10));
                for (Polygon2D_F64 p : locations) {
                    VisualizeShapes.drawPolygon(p, true, g2, true);
                }
            } else {
                System.out.println("Does not exist: "+f.getPath());
            }
        } catch( IOException e ){
            System.err.println(e.getMessage());
        }

        imagePanel.setBufferedImageNoChange(buffered);
        if( imagePanel.getImagePanel().getPreferredSize().width < 100 ) {
            System.out.println("setting preferred size");
            imagePanel.setPreferredSize(new Dimension(800,800));
        }

        double scale = BoofSwingUtil.selectZoomToShowAll(imagePanel,buffered.getWidth(),buffered.getHeight());
        imagePanel.setScale(scale);
        imagePanel.repaint();

    }

    private static List<Polygon2D_F64> detections(File file ) throws IOException {
        List<Polygon2D_F64> found = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new FileReader(file));

        while( true ) {
            String line = reader.readLine();
            if( line == null )
                break;
            if( line.length() == 0)
                continue;

            if( line.charAt(0) == '#')
                continue;
            if( line.startsWith("message"))
                continue;
            if( line.startsWith("milliseconds"))
                continue;

            String words[] = line.split(" ");
            if( words.length != 8 )
                throw new IOException("Unexpected number of words. "+words.length);

            Polygon2D_F64 poly = new Polygon2D_F64(4);
            for (int i = 0; i < 4; i++) {
                poly.get(i).set(Double.parseDouble(words[i*2]),Double.parseDouble(words[i*2+1]));
            }
            found.add(poly);
        }
        return found;
    }

    public static void main(String[] args) {
        File f = BoofSwingUtil.openFileChooser(null, BoofSwingUtil.FileTypes.IMAGES);
        if( f == null )
            return;

        SwingUtilities.invokeLater(()->new QrCodeBenchmarkViewerApp(f));
    }
}
