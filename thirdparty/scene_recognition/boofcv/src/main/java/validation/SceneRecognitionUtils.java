package validation;

import boofcv.BoofVersion;
import boofcv.abst.scene.SceneRecognition;
import boofcv.abst.scene.nister2006.ConfigSceneRecognitionNister2006;
import boofcv.abst.scene.nister2006.SceneRecognitionNister2006;
import boofcv.io.UtilIO;
import boofcv.io.image.ImageFileListIterator;
import boofcv.io.recognition.RecognitionIO;
import boofcv.metrics.ImageRetrievalEvaluationData;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class SceneRecognitionUtils<T extends ImageBase<T>> {

    public static final String CONFIG_FILE_NAME = "config.yaml";
    //  where the trained model without any images is saved
    public static final String MODEL_NAME = "model";
    // Where the trained model with images in the database is saved
    public static final String DB_NAME = "model_images";

    public File pathHome = new File(".");

    public boolean force = false;

    public int querySize = 1000;

    public ImageType<T> imageType;

    public PrintStream err = System.err;
    public PrintStream out = System.out;

    // number of times an exception happened while reading images
    public long imageReadFaults;


    public SceneRecognitionUtils(ImageType<T> imageType) {
        this.imageType = imageType;
    }

    /**
     * Creates a new model and saves it to disk
     */
    public boolean createAndSave(ModelInfo model, List<String> imagePaths) {
        File directory = new File(pathHome, model.name);

        // If it already exists, don't regenerate it
        if (!force && new File(directory, MODEL_NAME).exists()) {
            out.println(MODEL_NAME + " exists already.");
            return true;
        }

        if (!directory.exists())
            BoofMiscOps.checkTrue(directory.mkdirs());

        // Save the config, but only the differences so that it's easy to see the changes
        UtilIO.saveConfig(model.config, new ConfigSceneRecognitionNister2006(), new File(directory, CONFIG_FILE_NAME));

        SceneRecognitionNister2006<T, ?> target = new SceneRecognitionNister2006<>(model.config, imageType);
        target.setVerbose(System.out, null);

        out.println("Learning " + model.name + " images.size=" + imagePaths.size());
        ImageFileListIterator<T> iterator = new ImageFileListIterator<>(imagePaths, imageType);
        long time0 = System.currentTimeMillis();
        try {
            target.learnModel(iterator);
        } catch (RuntimeException e) {
            e.printStackTrace(err);
            err.println("Failed. Iterator was at " + iterator.getIndex());
            return false;
        }
        long time1 = System.currentTimeMillis();
        out.println("Elapsed time: " + (time1 - time0) * 1e-3 + " (s)");

        RecognitionIO.saveNister2006(target, new File(directory, MODEL_NAME));
        out.println("done");
        return true;
    }

    public boolean addImagesToDataBase(String modelName, List<String> paths) {
        File directory = new File(pathHome, modelName);

        // If it already exists, don't regenerate it
        if (!force && new File(directory, DB_NAME).exists()) {
            out.println(DB_NAME + " exists already.");
            return true;
        }

        System.out.println("Loading DB");
        // Load the model without images
        SceneRecognitionNister2006<T, ?> database =
                RecognitionIO.loadNister2006(new File(directory, MODEL_NAME), imageType);
        System.out.println("Clearing DB");

        // Make sure there really are no images in it
        database.clearDatabase();

        ImageFileListIterator<T> iterator = new ImageFileListIterator<>(paths, imageType);
        imageReadFaults = 0;
        iterator.setException((index, path, e) -> {
            imageReadFaults++;
            e.printStackTrace(err);
        });

        int index = 0;
        long time0 = System.currentTimeMillis();
        try {
            long timePrev = System.currentTimeMillis();
            while (iterator.hasNext()) {
                T image = iterator.next();
                index = iterator.getIndex();
                database.addImage("" + index, image);

                if (index % 100 == 0) {
                    // Print out how long it took to process these images so that performance issues are
                    // easier to diagnosis
                    long timeCurrent = System.currentTimeMillis();
                    double elapsed = (timeCurrent - timePrev) * 1e-3;
                    timePrev = timeCurrent;
                    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    String memory = String.format("%.1f", Runtime.getRuntime().freeMemory() / (1024.0 * 1024.0));
                    out.printf("Added %d / %d elapsed %.1f (s) %s %s (mb)\n",
                            index, paths.size(), elapsed, time, memory);
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace(err);
            err.println("Failed on index=" + index + " path=" + paths.get(index));
            return false;
        }
        long time1 = System.currentTimeMillis();
        out.println("Elapsed time: " + (time1 - time0) * 1e-3 + " (s)");

        // Save the model with images
        RecognitionIO.saveNister2006(database, new File(directory, DB_NAME));
        out.println("Done! read_faults=" + imageReadFaults);
        return true;
    }

    public boolean classify(String modelName, List<String> paths) {
        File directory = new File(pathHome, modelName);

        // If it already exists, don't regenerate it
        if (!force && new File(directory, "results.csv").exists()) {
            out.println(modelName + "_results.csv" + " exists already.");
            return true;
        }

        // Load the model without images
        System.out.print("  loading model ");
        long timeLoad0 = System.currentTimeMillis();
        SceneRecognition<T> database = RecognitionIO.loadNister2006(new File(directory, DB_NAME), imageType);
        System.out.println((System.currentTimeMillis() - timeLoad0) / 1000.0 + " (s)");
//        ((SceneRecognitionNister2006<T,?>)database).getDatabaseN().setVerbose(System.out,null);

        ImageFileListIterator<T> iterator = new ImageFileListIterator<>(paths, imageType);
        imageReadFaults = 0;
        iterator.setException((index, path, e) -> {
            imageReadFaults++;
            e.printStackTrace(err);
        });

        int index = 0;
        try {
            PrintStream resultsOut = new PrintStream(new FileOutputStream(new File(directory, "results.csv")));
            resultsOut.println("# BoofCV Image Retrieval Results");
            resultsOut.println("# BoofCV Version " + BoofVersion.VERSION + " GIT_SHA " + BoofVersion.GIT_SHA);

            long time0 = System.currentTimeMillis();
            long timePrev = System.currentTimeMillis();
            DogArray<SceneRecognition.Match> matches = new DogArray<>(SceneRecognition.Match::new);
            while (iterator.hasNext()) {
                T image = iterator.next();
                index = iterator.getIndex();

                if (index % 100 == 0) {
                    long timeCurrent = System.currentTimeMillis();
                    double elapsed = (timeCurrent - timePrev) * 1e-3;
                    timePrev = timeCurrent;
                    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    out.printf("Classifying %d / %d elapsed %.1f (s) %s\n", index, paths.size(), elapsed, time);
                }

                if (!database.query(image, querySize, matches)) {
                    err.println("Failed to retrieve. index=" + index + " path=" + iterator.getPaths().get(index));
                    continue;
                }

                // Save the file name and index. This information is redundant but can act as a sanity check
                resultsOut.print(iterator.getIndex() + "," + paths.get(iterator.getIndex()));

                // The number of results
                resultsOut.print("," + matches.size);

                // Save the results
                for (int i = 0; i < matches.size; i++) {
                    resultsOut.print("," + matches.get(i).id);
                }
                resultsOut.println();
            }
            long time1 = System.currentTimeMillis();
            resultsOut.close();
            out.println("Elapsed time: " + (time1 - time0) * 1e-3 + " (s)");
        } catch (Exception e) {
            e.printStackTrace(err);
            err.println("Failed on index=" + index + " path=" + paths.get(index));
            return false;
        }
        out.println("Done! read_faults=" + imageReadFaults);
        return true;
    }

    public static ImageRetrievalEvaluationData evaluateByFormat(String queryFormat,
                                                                List<String> training,
                                                                List<String> database,
                                                                List<String> query) {
        if (queryFormat.equalsIgnoreCase("ukbench")) {
            return SceneRecognitionUtils.evaluateUkbench(training, database, query);
        } else if (queryFormat.equalsIgnoreCase("holidays")) {
            return SceneRecognitionUtils.evaluateHolidays(training, database, query);
        } else {
            throw new IllegalArgumentException("Unknown query format: " + queryFormat);
        }
    }

    public static ImageRetrievalEvaluationData evaluateUkbench(List<String> training,
                                                               List<String> database,
                                                               List<String> query) {
        // @formatter:off
        return new ImageRetrievalEvaluationData() {
            @Override public List<String> getTraining() {return training;}
            @Override public List<String> getDataBase() {return database;}
            @Override public List<String> getQuery() {return query;}
            @Override public boolean isMatch(int query, int dataset) {
                return (query/4)==(dataset/4);
            }
            @Override public int getTotalMatches(int query) {
                return 4;
            }
        };
        // @formatter:on
    }

    public static ImageRetrievalEvaluationData evaluateHolidays(List<String> training,
                                                                List<String> database,
                                                                List<String> query) {
        // Inria files indicate which images are related based on the file name, which is a number.
        final int divisor = 100;
        int lastNumber = Integer.parseInt(FilenameUtils.getBaseName(new File(query.get(query.size() - 1)).getName()));
        int totalSets = lastNumber / divisor + 1;

        // Number of matches each query has in the database
        DogArray_I32 setCounts = new DogArray_I32();
        setCounts.resize(totalSets);

        // precompute number of membership in each set
        for (int i = 0; i < query.size(); i++) {
            int number = Integer.parseInt(FilenameUtils.getBaseName(new File(query.get(i)).getName()));
            setCounts.data[number / divisor]++;
        }

        // @formatter:off
        return new ImageRetrievalEvaluationData() {
            @Override public List<String> getTraining() {return training;}
            @Override public List<String> getDataBase() {return database;}
            @Override public List<String> getQuery() {return query;}
            @Override public boolean isMatch(int queryID, int datasetID) {
                // query images are first in the list
                if (datasetID>=query.size())
                    return false;
                int numberQuery = Integer.parseInt(FilenameUtils.getBaseName(new File(query.get(queryID)).getName()));
                int numberDataset = Integer.parseInt(FilenameUtils.getBaseName(new File(query.get(datasetID)).getName()));
                return numberQuery/divisor == numberDataset/divisor;
            }
            @Override public int getTotalMatches(int queryID) {
                if (queryID>=query.size())
                    return 0;
                int number = Integer.parseInt(FilenameUtils.getBaseName(new File(query.get(queryID)).getName()));
                return setCounts.get(number/divisor);
            }
        };
        // @formatter:on
    }

    /**
     * Free up some space and delete all the generated models but leave results behind
     */
    public void deleteModels(String modelName) {
        File directory = new File(pathHome, modelName);

        UtilIO.deleteRecursive(new File(directory, MODEL_NAME));
        UtilIO.deleteRecursive(new File(directory, DB_NAME));
    }

    public static class ModelInfo {
        public final String name;
        public final ConfigSceneRecognitionNister2006 config = new ConfigSceneRecognitionNister2006();

        public ModelInfo(String name) {
            this.name = name;
        }

        public ModelInfo(String name, ConfigSceneRecognitionNister2006 config) {
            this.name = name;
            this.config.setTo(config);
        }
    }
}
