package validate.applications;

import boofcv.io.image.UtilImageIO;
import georegression.struct.point.Point2D_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

// TODO a way to delete one entire qr code and not just a point at a time
//      button ?
// TODO Instant zoom in keyboard
// TODO Select a file using GUI
// TODO Move to next file using GUI
// TODO Drag a point to fix it
public class HandSelectQRCodeApp extends HandSelectBase {
    SelectQrCodeCornerPanel imagePanel = new SelectQrCodeCornerPanel();

    public HandSelectQRCodeApp(BufferedImage image , String outputName ) {
        super(outputName);

//        if( new File(outputName).exists() ) {
//            List<List<Point2D_F64>> sets = PointFileCodec.loadSets(outputName);
//            if( sets == null ) {
//                imagePanel.addPointSet(PointFileCodec.load(outputName));
//            } else {
//                imagePanel.setSets(sets);
//            }
//        }
        infoPanel.setImageShape(image.getWidth(),image.getHeight());
        imagePanel.setBufferedImage(image);
        initGui(image.getWidth(),image.getHeight(),imagePanel);
    }

    @Override
    public String getApplicationName() {
        return "Select QR Code Corners";
    }

    @Override
    public void setScale( double scale ) {
        imagePanel.setScale(scale);
    }

    @Override
    public void clearPoints() {
        imagePanel.clearPoints();
        imagePanel.repaint();
    }

    @Override
    public void save() {
        System.out.println("I refuse to save your results");

    }

    static class QRCorners
    {
        public List<Point2D_F64> corners = new ArrayList<>();
        public String message = "UNKNOWN";
    }

    public static void main(String[] args) {
        String imagePath = "1487126823099.jpg";

        String outputName = new File(imagePath).getAbsolutePath();
        outputName = outputName.substring(0,outputName.length()-4)+".txt";

        BufferedImage image = UtilImageIO.loadImage(imagePath);

        new HandSelectQRCodeApp(image,outputName);
    }
}
