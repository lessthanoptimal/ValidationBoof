package boofcv.common;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.factory.feature.associate.ConfigAssociateGreedy;
import boofcv.factory.feature.associate.FactoryAssociation;

/**
 * @author Peter Abeles
 */
public class DefaultConfigs {
    public static ConfigAssociateGreedy configGreedy() {
        ConfigAssociateGreedy configGreedy = new ConfigAssociateGreedy();
        configGreedy.forwardsBackwards = true;
        configGreedy.maxErrorThreshold = -1;
        configGreedy.scoreRatioThreshold = 1.0;
        return configGreedy;
    }

    public static<T> AssociateDescription<T> associateGreedy(ScoreAssociation<T> score ) {
        return FactoryAssociation.greedy(configGreedy(),score);
    }
}
