package validate.fiducial.qrcode;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageGray;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import validate.DataSetDoesNotExist;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.List;

import static validate.fiducial.BaseEstimateSquareFiducialToCamera.loadImageFilesByPrefix;

/**
 * @author Peter Abeles
 */
public class EvaluateQrCodeDecoding<T extends ImageGray<T>> {

    public PrintStream out = System.out;
    public PrintStream err = System.err;

    public void process(QrCodeDetector<T> detector, File dataSetDir ) throws IOException {

        out.println("# QR Code Decoding results");

        if (!dataSetDir.exists()) {
            throw new DataSetDoesNotExist("The data set directory doesn't exist. " + dataSetDir.getPath());
        }

        List<String> files = loadImageFilesByPrefix(dataSetDir);

        T image = GeneralizedImageOps.createSingleBand(detector.getImageType(),1,1);

        int total = 0;
        int correct = 0;
        int missed = 0;

        for( String path : files ) {
            BufferedImage orig = UtilImageIO.loadImage(path);
            ConvertBufferedImage.convertFrom(orig,image,true);

            total++;
            String imageFileName = new File(path).getName();

            detector.process(image);

            List<QrCode> found = detector.getDetections();

            if( found.size() == 1 ) {
                File parent = new File(path).getParentFile();
                String name = FilenameUtils.getBaseName(imageFileName)+".txt";
                File fileMessage = new File(parent,name);
                if( !fileMessage.exists() ) {
                    err.println("Can't find truth file "+fileMessage);
                    missed++;
                } else {

                    String expected = FileUtils.readFileToString(fileMessage, Charset.defaultCharset());

                    QrCode qr = found.get(0);

                    qr.message = qr.message.replaceAll("\\r","");
                    if( qr.message.endsWith("\n")) {
                        qr.message = qr.message.substring(0,qr.message.length()-1);
                    }

                    if (qr.message.equals(expected)) {
                        correct++;
                    } else {
                        System.out.println("failed "+name);
                    }
                }
            } else {
                missed++;
            }

        }
        out.println("Total   "+total);
        out.println("Correct "+correct);
        out.println("Missed  "+missed);
    }
}
