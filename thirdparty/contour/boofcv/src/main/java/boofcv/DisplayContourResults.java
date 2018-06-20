package boofcv;

import boofcv.alg.color.ColorRgb;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.applications.CommandLineAppBase;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point2D_I16;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DisplayContourResults extends CommandLineAppBase {
    @Option(name="-i",aliases = {"--Input"}, usage="Input directory.")
    String inputDir;
    @Option(name="-o",aliases = {"--Output"}, usage="Output directory.")
    String outputDir=".";
    @Option(name="-t",aliases = {"--Threshold"}, usage="Binarization threshold.")
    int threshold=0;


    Planar<GrayU8> rgb = new Planar(GrayU8.class,1,1,3);
    GrayU8 gray = new GrayU8(1,1);
    GrayU8 binary = new GrayU8(1,1);

    List<File> inputFiles = new ArrayList<>();
    List<File> contourFiles = new ArrayList<>();

    DisplayPanel gui = new DisplayPanel();

    int displayImage = 0;

    public DisplayContourResults() {
        readImage = false;

        gui.setScaling(ScaleOptions.MANUAL);
        gui.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                try {
                    showWindow(displayImage++);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        gui.addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                System.out.println("wheel "+gui.scale);
                double curr = gui.scale;

                if( e.getWheelRotation() > 0 )
                    curr *= 1.1;
                else
                    curr /= 1.1;

                gui.setScale(curr);
                gui.repaint();
            }
        });
        gui.requestFocus();

        gui.setPreferredSize(new Dimension(800,800));
        ShowImages.showWindow(gui,"Visualized",true);
    }

    @Override
    protected void processFile(BufferedImage image, File inputFile, File outputDirectory) {
        inputFiles.add(inputFile);

        String contourName = FilenameUtils.removeExtension(inputFile.getName())+".txt";
        contourFiles.add( new File(outputDirectory,contourName));

        System.out.print("@");
        if( inputFile.length()%50==0)
            System.out.println();
    }

    public void showWindow( int which ) throws IOException {
        BufferedImage image = UtilImageIO.loadImage(inputFiles.get(which).getAbsolutePath());

        ConvertBufferedImage.convertFrom(image,rgb,true);
        ColorRgb.rgbToGray_Weighted(rgb,gray);
        binary.reshape(gray.width,gray.height);
        ThresholdImageOps.threshold(gray,binary,threshold,true);
        VisualizeBinaryData.renderBinary(binary,false,image);

        List<List<Point2D_I16>> contours = loadContours(contourFiles.get(which));

        gui.setImage(image);
        gui.setContours(contours);
        gui.autoSetPreferredSize();
        gui.repaint();

        System.out.println("Displayed results. size="+contours.size());
    }

    private static List<List<Point2D_I16>> loadContours( File file ) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));

        List<List<Point2D_I16>> contours = new ArrayList<>();

        try {
            while (true) {
                String line = reader.readLine();

                if (line == null)
                    break;

                if (line.startsWith("#"))
                    continue;

                String words[] = line.split(" ");
                List<Point2D_I16> contour = new ArrayList<>();
                for (int i = 0; i < words.length / 2; i++) {
                    int x = Integer.parseInt(words[i * 2]);
                    int y = Integer.parseInt(words[i * 2 + 1]);
                    contour.add(new Point2D_I16((short) x, (short) y));
                }
                contours.add(contour);
            }
            return contours;
        } catch( RuntimeException e ) {
            System.err.println("Failed to read contour file "+file.getPath());
        }
        return contours;
    }

    private class DisplayPanel extends ImagePanel {
        public List<List<Point2D_I16>> contours;

        public void setContours( List<List<Point2D_I16>> contours ) {
            synchronized (this) {
                this.contours = contours;
            }
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D)g;

            synchronized (this) {
                if( contours == null)
                    return;

                Polygon poly = new Polygon();
                g2.setStroke(new BasicStroke(1));
                g2.setColor(Color.RED);
                for( List<Point2D_I16> contour : contours ) {
                    poly.reset();
                    for (int i = 0; i < contour.size(); i++) {
                        Point2D_I16 p = contour.get(i);
                        poly.addPoint((int)(scale*p.x),(int)(scale*p.y));
                    }
                    g2.draw(poly);
                }
            }
        }
    }

    public static void main(String[] args) {
        DisplayContourResults generator = new DisplayContourResults();
        CmdLineParser parser = new CmdLineParser(generator);
        try {
            parser.parseArgument(args);
            if( generator.inputDir == null )
                printHelpExit(parser);
            if( generator.outputDir == null )
                printHelpExit(parser);
            if( generator.threshold == 0 )
                printHelpExit(parser);

            generator.processRootDirectory(generator.inputDir,generator.outputDir);
            generator.showWindow(generator.displayImage++);
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            printHelpExit(parser);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
