package boofcv.metrics;

import boofcv.io.UtilIO;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray_I32;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Images are organized into blocks. Each block contains images which are related. One image from the block
 * is used to train and query. All the others go into the databse. Images in blocks are consecutive in their
 * file names.
 *
 * @author Peter Abeles
 **/
public class InriaSets implements ImageRetrievalEvaluationData {
    int divisor;
    List<String> training = new ArrayList<>();
    List<String> database = new ArrayList<>();

    // Number of matches each query has in the database
    DogArray_I32 matches = new DogArray_I32();

    public InriaSets(int divisor, File directory) {
        this.divisor = divisor;
        List<String> all = UtilIO.listImages(directory.getPath(), true);

        for (int i = 0; i < all.size(); i++) {
            int fileNumber = Integer.parseInt(FilenameUtils.getBaseName(new File(all.get(i)).getName()));

            if (fileNumber%divisor==0) {
                training.add(all.get(i));
            } else {
                database.add(all.get(i));
            }
        }

        // Count number of matches each query has.
        matches.resize(training.size(), 0);
        int countQuery = -1;
        for (int i = 0; i < all.size(); i++) {
            int fileNumber = Integer.parseInt(FilenameUtils.getBaseName(new File(all.get(i)).getName()));
            if (fileNumber%divisor!=0) {
                matches.data[countQuery]++;
            } else {
                countQuery++;
            }
        }
    }

    @Override
    public List<String> getTraining() {
        return training;
    }

    @Override
    public List<String> getDataBase() {
        return database;
    }

    @Override
    public List<String> getQuery() {
        return training;
    }

    @Override
    public boolean isMatch(int query, int dataset) {
        int numberQuery = Integer.parseInt(FilenameUtils.getBaseName(new File(training.get(query)).getName()));
        int numberDataset = Integer.parseInt(FilenameUtils.getBaseName(new File(database.get(dataset)).getName()));
        return numberQuery/divisor == numberDataset/divisor;
    }

    @Override
    public int getTotalMatches(int query) {
        return matches.get(query);
    }
}
