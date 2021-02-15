package boofcv.metrics;

import boofcv.io.UtilIO;

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
public class ImageFilesInFixedSets implements ImageRetrievalEvaluationData {
    int block;
    List<String> training = new ArrayList<>();
    List<String> database = new ArrayList<>();

    public ImageFilesInFixedSets(int train, int block, File directory) {
        this.block = block;
        List<String> all = UtilIO.listImages(directory.getPath(), true);

        for (int i = 0; i < all.size(); i++) {
            if (i%block==train) {
                training.add(all.get(i));
            } else {
                database.add(all.get(i));
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
        return query == dataset/(block-1);
    }

    @Override
    public int getTotalMatches(int query) {
        return block-1;
    }
}
