package boofcv.metrics;

import java.util.List;

/**
 * Returns a list of
 *
 * @author Peter Abeles
 **/
public interface ImageRetrievalEvaluationData {

    /**
     * List of images to train the classifier on
     */
    List<String> getTraining();

    /**
     * List of images it should add to the dataset for later retrieval
     */
    List<String> getDataBase();

    /**
     * List of images it will query to find the best matches in the dataset
     */
    List<String> getQuery();

    /**
     * Returns true if the query image is in the same set as the dataset image.
     *
     * @param query index of image in query set
     * @param dataset index of image in dataset set
     * @return true if they are in the of the same scene
     */
    boolean isMatch(int query, int dataset );

    /**
     * Returns the number of matches to the query in the dataset
     */
    int getTotalMatches(int query);
}
