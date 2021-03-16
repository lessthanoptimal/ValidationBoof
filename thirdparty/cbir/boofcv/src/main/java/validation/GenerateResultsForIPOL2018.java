package validation;

import boofcv.io.MirrorStream;
import boofcv.io.UtilIO;
import boofcv.metrics.ImageRetrievalEvaluateResults;
import boofcv.metrics.ImageRetrievalEvaluationData;
import boofcv.misc.BoofMiscOps;
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
    public static ImageRetrievalEvaluationData foo(List<String> all) {
        return new ImageRetrievalEvaluationData() {
            @Override public List<String> getTraining() {throw new RuntimeException("Bad");}
            @Override public List<String> getDataBase() {throw new RuntimeException("Bad");}
            @Override public List<String> getQuery() {return all;}
            @Override public boolean isMatch(int query, int dataset) {
                return (query/4)==(dataset/4);
            }
            @Override public int getTotalMatches(int query) {
                return 4;
            }
        };
    }

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("args.length="+args.length);
        String directoryData = args.length > 0 ? args[0] : "data";
        System.out.println("input data path: "+directoryData);

        // Sanity check to make sure the JPEG issue is being handled
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("JPEG");
        while (readers.hasNext()) {
            System.out.println("JPEG formats: " + readers.next());
        }
        var defaultModel = new ImageRecognitionUtils.ModelInfo("default");
        File resultsDir = new File("cbir_models/"+defaultModel.name);
        if (resultsDir.exists()) {
            System.out.println("Directory already exists. Will attempt to resume. "+resultsDir.getAbsolutePath());
        } else {
            BoofMiscOps.checkTrue(resultsDir.mkdirs());
        }

        // Create the logs
        try (PrintStream outStream = new PrintStream(new File(resultsDir,"stdout_log.txt"));
             PrintStream errorStream = new PrintStream(new File(resultsDir,"stderr_log.txt"));
             PrintStream resultsSummary = new PrintStream(new File(resultsDir,"results_summary.txt"));
             PrintStream resultsDetailed = new PrintStream(new File(resultsDir,"results_detailed.txt"))) {
            try {
                // Load file paths to all the images
                List<String> ukbenchImages = UtilIO.listImages(new File(directoryData, "full").getPath(), true);
                System.out.println("ukbench.size=" + ukbenchImages.size());

                List<String> flickrImages = new ArrayList<>();
                File flickerDir = new File(new File(directoryData, "images").getPath());
                File[] children = flickerDir.listFiles();
                if (children == null)
                    throw new RuntimeException("No flicker directories");
                List<File> sorted = Arrays.asList(children);
                Collections.sort(sorted);
                for (File d : sorted) {
                    if (!d.isDirectory() || d.isHidden())
                        continue;
                    // All valid directories are numbers
                    try {
                        Integer.parseInt(d.getName());
                    } catch (Exception e) {
                        continue;
                    }
                    flickrImages.addAll(UtilIO.listImages(d.getAbsolutePath(), true));
                }

                System.out.println("flicker.size=" + flickrImages.size());

                ImageRecognitionUtils<GrayU8> utils = new ImageRecognitionUtils<>(ImageType.SB_U8);
                utils.pathHome = new File("cbir_models");

                // Save to the logs and print to standard outputs
                utils.err = new PrintStream(new MirrorStream(System.err, errorStream));
                utils.out = new PrintStream(new MirrorStream(System.out, outStream));

                // Generate the model, add images to data base, generate classification results
                System.out.println("Creating model...");
                utils.createAndSave(defaultModel, ukbenchImages);
                System.out.println("Adding images...");
                List<String> everything = new ArrayList<>(ukbenchImages.size() + flickrImages.size());
                everything.addAll(ukbenchImages);
                everything.addAll(flickrImages);
                utils.addImagesToDataBase(defaultModel.name, everything);
                System.out.println("Classifying images...");
                utils.classify(defaultModel.name, ukbenchImages);
                System.out.println("Performance Metrics...");
                ImageRetrievalEvaluateResults evaluate = new ImageRetrievalEvaluateResults();
                evaluate.err = utils.err;
                evaluate.outSummary = new PrintStream(new MirrorStream(System.out, resultsSummary));
                evaluate.outDetailed = resultsDetailed;
                evaluate.evaluate(defaultModel.name,
                        new File("cbir_models/" + defaultModel.name + "/results.csv"), foo(ukbenchImages));
            } catch (RuntimeException e) {
                e.printStackTrace();
                e.printStackTrace(errorStream);
            }
        }
    }
}
