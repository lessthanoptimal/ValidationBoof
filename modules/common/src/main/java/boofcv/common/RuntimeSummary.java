package boofcv.common;

import org.ddogleg.stats.StatisticsDogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ejml.FancyPrint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class RuntimeSummary {
    public int digits = 7;
    public PrintStream out = System.out;

    public List<SummaryInfo> summary = new ArrayList<>();

    public void reset() {
        summary.clear();
    }


    /**
     * Standard initialization. Creates the log. Prints the default header.
     */
    public void initializeLog(String directory, Class which, String fileName ) {
        try {
            out = new PrintStream(new File(directory,fileName));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        BoofRegressionConstants.printGenerator(out, which);
        out.println("# Elapsed time in milliseconds");
        out.println();
    }

    public void printUnitsRow(boolean summary) {
        String format = "%"+(1+digits)+"s";
        if( summary )
            out.println("Summary:");
        out.printf("  %24s    N   "+format+" "+format+" "+format+" "+format+" "+format+"\n","","Mean","P05","P50","P95","MAX");
    }

    public void printStatsRow(String name , DogArray_F64 measurements)
    {
        FancyPrint f = new FancyPrint(new DecimalFormat("#"),digits+1,4);

        measurements.sort();
        int N = measurements.size;
        String mean = f.p(StatisticsDogArray.mean(measurements));
        String P05 = f.p(measurements.getFraction(0.05));
        String P50 = f.p(measurements.getFraction(0.50));
        String P95 = f.p(measurements.getFraction(0.95));
        String MAX = f.p(measurements.getFraction(1.00));

        // The two spaces before the string is used to make it visually easier to see that this is a seperate block of
        // results in documents with multiple blocks
        String format = "%"+digits+"s";
        out.printf("  %-24s %6d  "+format+"  "+format+"  "+format+"  "+format+"  "+format+"\n",name,N,mean,P05,P50,P95,MAX);
        out.flush();
    }

    public void printSummaryResults() {
        printUnitsRow(true);
        for( SummaryInfo info : summary ) {
            printStatsRow(info.name,info.measurements);
        }
    }

    public void saveSummary( String name , DogArray_F64 measurements ) {
        SummaryInfo info = new SummaryInfo();
        info.name = name;
        info.measurements.addAll(measurements);
        this.summary.add(info);
    }

    private static class SummaryInfo
    {
        DogArray_F64 measurements = new DogArray_F64();
        String name;
    }
}
