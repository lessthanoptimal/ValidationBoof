package boofcv.metrics.ecocheck;

import boofcv.BoofVersion;
import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckDetector;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckFound;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Detects
 *
 * @author Peter Abeles
 */
public class DetectEcoCheckImages<T extends ImageGray<T>> {
    /**
     * Where results are written to
     */
    public File outputPath = new File(".");

    public ProcessTime processTime = (a, b) -> {
    };

    public ECoCheckDetector<T> detector;

    private File root;

    T gray;

    public DetectEcoCheckImages(ConfigECoCheckMarkers configMarkers, Class<T> imageType) {
        detector = FactoryFiducial.ecocheck(null, configMarkers, imageType).getDetector();
        gray = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
    }

    public void detect(File directory) {
        this.root = directory;
        detectRecursive(directory);
    }

    public void detectRecursive(File directory) {
        File[] children = directory.listFiles();
        if (children == null)
            return;

        // Ensure the order is consistent
        List<File> listChildren = Arrays.asList(children);
        Collections.sort(listChildren);

        boolean foundImage = false;

        File relativeToRoot = root.toPath().relativize(directory.toPath()).toFile();
        File base = new File(outputPath, relativeToRoot.getPath());

        DogArray_F64 processingTime = new DogArray_F64();

        for (File c : listChildren) {
            if (c.isDirectory()) {
                detectRecursive(c);
                continue;
            }

            if (!UtilImageIO.isImage(c))
                continue;

            if (!foundImage) {
                System.out.println("Processing: "+directory.getPath());
                if (!base.exists())
                    BoofMiscOps.checkTrue(base.mkdirs());
                foundImage = true;
            }

            BufferedImage buffered = UtilImageIO.loadImage(c.getPath());
            ConvertBufferedImage.convertFrom(buffered, true, gray);

            long time0 = System.nanoTime();
            detector.process(gray);
            long time1 = System.nanoTime();
            processingTime.add((time1 - time0) * 1e-6);

            saveResults(new File(base, FilenameUtils.getBaseName(c.getName()) + ".txt"));
        }

        if (processingTime.size > 0) {
            processTime.detectionTimes(base.getPath(), processingTime);
        }
    }

    private void saveResults(File outputPath) {

        // Merge split marker and remove unknown markers
        List<ECoCheckFound> filtered = ECoCheckUtils.mergeAndRemoveUnknown(detector.found.toList());

        try (PrintStream out = new PrintStream(outputPath)) {
            out.println("# ECoCheck Detections. BoofCV " + BoofVersion.VERSION);
            out.println("image.shape=" + gray.width + "," + gray.height);
            out.println("markers=" + filtered.size());

            for (int i = 0; i < filtered.size(); i++) {
                ECoCheckFound found = filtered.get(i);
                out.println("marker=" + found.markerID);
                out.println("decoded_cells.size=" + found.decodedCells.size);
                out.println("shape=" + found.squareRows + "," + found.squareCols);
                out.println("corners.size=" + found.corners.size);
                for (int cornerIdx = 0; cornerIdx < found.corners.size; cornerIdx++) {
                    PointIndex2D_F64 c = found.corners.get(cornerIdx);
                    out.printf("%d %.5f %.5f\n", c.index, c.p.x, c.p.y);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface ProcessTime {
        void detectionTimes(String name, DogArray_F64 times);
    }

    public static void main(String[] args) {
        ConfigECoCheckMarkers config = ConfigECoCheckMarkers.singleShape(9, 7, 1.0, 1);

        var app = new DetectEcoCheckImages<>(config, GrayF32.class);
        app.outputPath = new File("ecocheck_found");
        app.detect(new File("ecocheck"));
        System.out.println("Done!");
    }
}
