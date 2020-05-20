package boofcv.metrics.qrcode;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.common.DataSetDoesNotExist;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.GrowQueue_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import static boofcv.metrics.BaseEstimateSquareFiducialToCamera.loadImageFilesByPrefix;

/**
 * Detects qr codes in an image and saves the results to a file
 *
 * @author Peter Abeles
 */
public class DetectQrCodesInImages<T extends ImageGray<T>> {
    File outputDirectory = new File(".");

    public GrowQueue_F64 periodMS = new GrowQueue_F64();

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void process( QrCodeDetector<T> detector,  File dataSetDir ) throws IOException {

        if (!dataSetDir.exists()) {
            throw new DataSetDoesNotExist("The data set directory doesn't exist. " + dataSetDir.getPath());
        }

        List<String> files = loadImageFilesByPrefix(dataSetDir);

        T image = GeneralizedImageOps.createSingleBand(detector.getImageType(),1,1);

        periodMS.reset();
        for( String path : files ) {
            BufferedImage orig = UtilImageIO.loadImage(path);
            ConvertBufferedImage.convertFrom(orig,image,true);

            String imageFileName = new File(path).getName();

            File outFile = new File(outputDirectory, FilenameUtils.getBaseName(imageFileName)+".txt");
            PrintStream out = new PrintStream(outFile);
            out.println("# Detected qrcodes inside of "+path+" using "+detector.getClass().getSimpleName());
            out.println("# 2 lines for each detection.");
            out.println("# Line 1: 4 corners representing bounding box in pixels");
            out.println("# Line 2: Encoded message");

            long before = System.nanoTime();
            detector.process(image);
            long after= System.nanoTime();

            periodMS.add((after-before)*1e-6);

            List<QrCode> found = detector.getDetections();
            for (int i = 0; i < found.size(); i++) {
                QrCode qr = found.get(i);
                for (int j = 0; j < qr.bounds.size(); j++) {
                    if( j >= 1 )
                        out.print(" ");
                    Point2D_F64 p = qr.bounds.get(j);
                    out.printf("%.20f %.20f",p.x,p.y);
                }
                out.println();
                out.println(qr.message.replaceAll("\\r\\n|\\r|\\n", ""));
            }

            out.close();
        }
    }
}
