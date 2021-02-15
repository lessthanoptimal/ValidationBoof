package boofcv.metrics;

import boofcv.BoofVersion;
import boofcv.abst.scene.ImageRecognition;
import boofcv.io.image.ImageFileListIterator;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Saves the first N results from image retrieval. First it trains the classifier and adds images. Procesisng
 * time for each step is saved.
 *
 * @author Peter Abeles
 **/
public class ImageRetrievalGenerateResults<T extends ImageBase<T>> {
    public ImageType<T> imageType;

    public PrintStream err = System.err;

    // Number of matches it will request
    public int querySize = 1000;

    // Number of faults/exception
    int totalExceptions;
    // number of times an image retrieval failed
    int totalFailed;

    // How long each operation took in milliseconds. If possible IO time is removed
    public double timeTrainingMS;
    public double timeAddingMS;
    public double timeLookUpMS;

    public ImageRetrievalGenerateResults(ImageType<T> imageType) {
        this.imageType = imageType;
    }

    public void process(ImageRecognition<T> target, ImageRetrievalEvaluationData sets, File outputFile) {
        // reset metrics
        totalExceptions = 0;
        timeTrainingMS = 0;

        // learn how to classify this dataset
        timeTrainingMS = BoofMiscOps.timeNano(() ->
                target.learnModel(new ImageFileListIterator<>(sets.getTraining(), imageType))) * 1e-6;

        // Add images
        timeAddingMS = 0;
        {
            List<String> list = sets.getDataBase();
            ImageFileListIterator<T> iterator = new ImageFileListIterator<>(list, imageType);
            int index=0;
            try {
                while (iterator.hasNext()) {
                    T image = iterator.next();
                    index = iterator.getIndex();
                    if (index%100==0)
                        System.out.println("adding "+index+"/"+list.size());

                    long time0 = System.nanoTime();
                    target.addImage(""+index, image);
                    long time1 = System.nanoTime();
                    timeAddingMS += (time1-time0)*1e-6;
                }
            } catch (RuntimeException e) {
                totalExceptions++;
                e.printStackTrace(err);
                err.println("Exception adding: " + list.get(index));
            }
        }

        // Look up images
        timeLookUpMS = 0;
        DogArray<ImageRecognition.Match> matches = new DogArray<>(ImageRecognition.Match::new);
        List<String> query = sets.getQuery();
        ImageFileListIterator<T> iterator = new ImageFileListIterator<>(query, imageType);

        try {
            PrintStream out = new PrintStream(new FileOutputStream(outputFile));
            out.println("# BoofCV Image Retrieval Results");
            out.println("# BoofCV Version "+ BoofVersion.VERSION+" GIT_SHA "+BoofVersion.GIT_SHA);

            while (iterator.hasNext()) {
                // Look up the best matches
                try {
                    T image = iterator.next();
                    long time0 = System.nanoTime();
                    if (!target.findBestMatch(image, matches)) {
                        totalFailed++;
                        continue;
                    }
                    long time1 = System.nanoTime();
                    timeLookUpMS += (time1 - time0) * 1e-6;

                    // Save the file name and index. This information is redundant but can act as a sanity check
                    out.print(iterator.getIndex()+","+query.get(iterator.getIndex()));

                    // The number of results
                    int N = Math.min(querySize, matches.size);
                    out.print(","+N);

                    // Save the results
                    for (int i = 0; i < N; i++) {
                        out.print(","+matches.get(i).id);
                    }
                    out.println();
                } catch (RuntimeException e) {
                    totalExceptions++;
                    e.printStackTrace(err);
                    err.println("Exception looking up: " + query.get(iterator.getIndex()));
                }
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace(err);
            e.printStackTrace();
        }
    }
}
