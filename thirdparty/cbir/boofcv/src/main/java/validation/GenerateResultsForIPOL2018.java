package validation;

import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006;
import boofcv.factory.feature.describe.ConfigConvertTupleDesc;
import boofcv.io.MirrorStream;
import boofcv.io.UtilIO;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

/**
 * @author Peter Abeles
 */
public class GenerateResultsForIPOL2018 {
    public static final String NAME = "IPOL2018";

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("args.length="+args.length);
        String directoryData = args.length > 0 ? args[0] : "data";
        System.out.println("input data path: "+directoryData);

        // Sanity check to make sure the JPEG issue is being handled
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("JPEG");
        while (readers.hasNext()) {
            System.out.println("JPEG formats: " + readers.next());
        }

        // Create the logs
        PrintStream errorStream = new PrintStream("stderr_log.txt");
        try (PrintStream outStream = new PrintStream("stdout_log.txt")) {

            // Load file paths to all the images
            List<String> ukbenchImages = UtilIO.listImages(new File(directoryData,"full").getPath(), true);
            System.out.println("ukbench.size=" + ukbenchImages.size());

            List<String> flickrImages = new ArrayList<>();
//            File flickerDir = new File(new File(directoryData,"images").getPath());
//            File[] children = flickerDir.listFiles();
//            if (children == null)
//                throw new RuntimeException("No flicker directories");
//            List<File> sorted = Arrays.asList(children);
//            Collections.sort(sorted);
//            for (File d : sorted) {
//                if (!d.isDirectory() || d.isHidden())
//                    continue;
//                // All valid directories are numbers
//                try {
//                    Integer.parseInt(d.getName());
//                } catch (Exception e) {
//                    continue;
//                }
//                flickrImages.addAll(UtilIO.listImages(d.getAbsolutePath(), true));
//            }
//
//            System.out.println("flicker.size=" + flickrImages.size());

            ImageRecognitionUtils<GrayU8> utils = new ImageRecognitionUtils<>(ImageType.SB_U8);
            utils.pathHome = new File("cbir_models");

            // Save to the logs and print to standard outputs
            utils.err = new PrintStream(new MirrorStream(System.err, errorStream));
            utils.out = new PrintStream(new MirrorStream(System.out, outStream));

            var defaultModel = new ImageRecognitionUtils.ModelInfo("default");
            defaultModel.config.features.detectFastHessian.extract.threshold = 0;
            defaultModel.config.features.detectFastHessian.extract.radius = 2;
            // You can get better retrieval with more features, but you start running into hard limits.
            // With this value you can train on about 20,000 images with SURF before you blow past the limits
            // of an integer length array. This can be fixed in code without much difficulty, but you are
            // already using 8G of memory.
            defaultModel.config.features.detectFastHessian.maxFeaturesAll = 1500;
//            defaultModel.config.features.detectFastHessian.maxFeaturesPerScale = 600;
            defaultModel.config.minimumDepthFromRoot = 3;
            defaultModel.config.distanceNorm = RecognitionVocabularyTreeNister2006.DistanceTypes.L1;
            // 0.8747
            defaultModel.config.minimumDepthFromRoot = 2;
            defaultModel.config.tree.branchFactor = 27;
            defaultModel.config.tree.maximumLevel = 4;
            // 0.8813
            defaultModel.config.minimumDepthFromRoot = 0;
            // 0.8818
            defaultModel.config.features.detectFastHessian.extract.radius = 6;
            defaultModel.config.features.detectFastHessian.maxFeaturesAll = 500;
            // 0.8439 and 30min to run. 2x faster!
            defaultModel.config.features.detectFastHessian.extract.radius = 2;
            // 0.8601 and 30min still


            // Generate the model, add images to data base, generate classification results
            System.out.println("Creating model...");
            utils.createAndSave(defaultModel, ukbenchImages);
            System.out.println("Adding images...");
            List<String> everything = new ArrayList<>(ukbenchImages.size() + flickrImages.size());
            everything.addAll(ukbenchImages);
            everything.addAll(flickrImages);
            utils.addImagesToDataBase(defaultModel.name, NAME, everything);
            System.out.println("Classifying images...");
            utils.classify(defaultModel.name, NAME, ukbenchImages);
        } catch (RuntimeException e) {
            e.printStackTrace();
            e.printStackTrace(errorStream);
        } finally {
            errorStream.close();
        }

        // TODO compute results
    }
}
