package validation;

import boofcv.BoofVersion;
import boofcv.abst.scene.ImageRecognition;
import boofcv.abst.scene.nister2006.ConfigImageRecognitionNister2006;
import boofcv.abst.scene.nister2006.ImageRecognitionNister2006;
import boofcv.io.UtilIO;
import boofcv.io.image.ImageFileListIterator;
import boofcv.io.recognition.RecognitionIO;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.FactoryFilterLambdas;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 *
 *
 * @author Peter Abeles
 */
public class ImageRecognitionUtils<T extends ImageBase<T>> {
    public static final File DATA_DIRECTORY = new File("thirdparty/cbir/data");
    public static final String MODEL_PATH = "cbir_models";

    public static final String CONFIG_FILE_NAME = "config.yaml";
    public static final String MODEL_FILE_NAME = "model";

    public boolean force = false;

    // rescale images so that they have this pixel count approximately
    public int targetPixelCount = 800*600;

    public int querySize = 1000;

    public ImageType<T> imageType;

    public PrintStream err = System.err;
    public PrintStream out = System.out;

    // number of times an exception happened while reading images
    public long imageReadFaults;

    public ImageRecognitionUtils(ImageType<T> imageType) {
        this.imageType = imageType;
    }

    /**
     * Creates a new model and saves it to disk
     */
    public void createAndSave(ModelInfo model, List<String> imagePaths) {
        File directory = new File(MODEL_PATH,model.name);

        // If it already exists, don't regenerate it
        if (!force && new File(directory,MODEL_FILE_NAME).exists()) {
            out.println(MODEL_FILE_NAME+" exists already.");
            return;
        }

        if (!directory.exists())
            BoofMiscOps.checkTrue(directory.mkdirs());

        UtilIO.saveConfig(model.config,new File(directory,CONFIG_FILE_NAME));

        ImageRecognitionNister2006<T,?> target = new ImageRecognitionNister2006<>(model.config, imageType);

        // TODO create a config for image preprocessing and save that to the directory too

        out.println("Learning "+model.name+" images.size="+imagePaths.size());
        ImageFileListIterator<T> iterator = new ImageFileListIterator<>(imagePaths, imageType);
        iterator.setFilter(FactoryFilterLambdas.createDownSampleFilter(targetPixelCount,imageType));
        long time0 = System.currentTimeMillis();
        try {
            target.learnModel(iterator);
        } catch (RuntimeException e) {
            e.printStackTrace(err);
            err.println("Failed. Iterator was at "+iterator.getIndex());
            return;
        }
        long time1 = System.currentTimeMillis();
        out.println("Elapsed time: "+(time1-time0)*1e-3+" (s)");

        RecognitionIO.saveNister2006(target,new File(directory,MODEL_FILE_NAME));
        out.println("done");
    }

    public void addImagesToDataBase( String modelName, String benchmarkName, List<String> paths ) {
        File directory = new File(MODEL_PATH,modelName);

        // If it already exists, don't regenerate it
        if (!force && new File(directory,benchmarkName).exists()) {
            out.println(benchmarkName+" exists already.");
            return;
        }

        System.out.println("Loading DB");
        // Load the model without images
        ImageRecognitionNister2006<T,?> database =
                RecognitionIO.loadNister2006(new File(directory,MODEL_FILE_NAME), imageType);
        System.out.println("Clearing DB");

        // Make sure there really are no images in it
        database.clearDatabase();

        ImageFileListIterator<T> iterator = new ImageFileListIterator<>(paths, imageType);
        iterator.setFilter(FactoryFilterLambdas.createDownSampleFilter(targetPixelCount,imageType));
        imageReadFaults = 0;
        iterator.setException((index,path,e)->{imageReadFaults++;e.printStackTrace(err);});

        int index=0;
        long time0 = System.currentTimeMillis();
        try {
            long timePrev = System.currentTimeMillis();
            while (iterator.hasNext()) {
                T image = iterator.next();
                index = iterator.getIndex();
                database.addImage(""+index, image);

                if (index%100==0) {
                    // Print out how long it took to process these images so that performance issues are
                    // easier to diagnosis
                    long timeCurrent = System.currentTimeMillis();
                    double elapsed = (timeCurrent-timePrev)*1e-3;
                    timePrev = timeCurrent;
                    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    String memory = String.format("%.1f",Runtime.getRuntime().freeMemory()/(1024.0*1024.0));
                    out.printf("Added %d / %d elapsed %.1f (s) %s %s (mb)\n",
                            index,paths.size(),elapsed, time, memory);
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace(err);
            err.println("Failed on index="+index+" path="+paths.get(index));
            return;
        }
        long time1 = System.currentTimeMillis();
        out.println("Elapsed time: "+(time1-time0)*1e-3+" (s)");

        // Save the model with images
        RecognitionIO.saveNister2006(database,new File(directory,benchmarkName));
        out.println("Done! read_faults="+imageReadFaults);
    }

    public void classify( String modelName, String benchmarkName, List<String> paths ) {
        File directory = new File(MODEL_PATH,modelName);

        // If it already exists, don't regenerate it
        if (!force && new File(directory,benchmarkName+"_results.csv").exists()) {
            out.println(benchmarkName+"_results.csv"+" exists already.");
            return;
        }

        // Load the model without images
        ImageRecognition<T> database = RecognitionIO.loadNister2006(new File(directory,benchmarkName), imageType);

        ImageFileListIterator<T> iterator = new ImageFileListIterator<>(paths, imageType);
        iterator.setFilter(FactoryFilterLambdas.createDownSampleFilter(targetPixelCount,imageType));
        imageReadFaults = 0;
        iterator.setException((index,path,e)->{imageReadFaults++;e.printStackTrace(err);});

        int index=0;
        try {
            PrintStream resultsOut = new PrintStream(new FileOutputStream(new File(directory,benchmarkName+"_results.csv")));
            resultsOut.println("# BoofCV Image Retrieval Results");
            resultsOut.println("# BoofCV Version "+ BoofVersion.VERSION+" GIT_SHA "+BoofVersion.GIT_SHA);

            long time0 = System.currentTimeMillis();
            long timePrev = System.currentTimeMillis();
            DogArray<ImageRecognition.Match> matches = new DogArray<>(ImageRecognition.Match::new);
            while (iterator.hasNext()) {
                T image = iterator.next();
                index = iterator.getIndex();

                if (index%100==0) {
                    long timeCurrent = System.currentTimeMillis();
                    double elapsed = (timeCurrent-timePrev)*1e-3;
                    timePrev = timeCurrent;
                    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    out.printf("Classifying %d / %d elapsed %.1f (s) %s\n",index,paths.size(),elapsed, time);
                }

                if (!database.query(image, querySize, matches)) {
                    err.println("Failed to retrieve. "+index);
                    continue;
                }

                // Save the file name and index. This information is redundant but can act as a sanity check
                resultsOut.print(iterator.getIndex()+","+paths.get(iterator.getIndex()));

                // The number of results
                resultsOut.print(","+matches.size);

                // Save the results
                for (int i = 0; i < matches.size; i++) {
                    resultsOut.print(","+matches.get(i).id);
                }
                resultsOut.println();
            }
            long time1 = System.currentTimeMillis();
            resultsOut.close();
            out.println("Elapsed time: "+(time1-time0)*1e-3+" (s)");
        } catch (Exception e) {
            e.printStackTrace(err);
            err.println("Failed on index="+index+" path="+paths.get(index));
        }
        out.println("Done! read_faults="+imageReadFaults);
    }

    public static class ModelInfo {
        public final String name;
        public final ConfigImageRecognitionNister2006 config = new ConfigImageRecognitionNister2006();

        public ModelInfo(String name) {
            this.name = name;
        }
    }
}
