package boofcv.common;

import org.ddogleg.stats.UtilStatisticsQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.ejml.FancyPrint;

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

    public void standardInitialize( Class owner , String path ) {

    }

    public void printHeader(boolean summary) {
        String format = "%"+(1+digits)+"s";
        if( summary )
            out.println("Summary:");
        out.printf("%24s    N   "+format+" "+format+" "+format+" "+format+" "+format+"\n","","Mean","P05","P50","P95","MAX");
    }

    public void printStats(String name , GrowQueue_F64 measurements)
    {
        FancyPrint f = new FancyPrint(new DecimalFormat("#"),digits+1,4);

        measurements.sort();
        int N = measurements.size;
        String mean = f.p(UtilStatisticsQueue.mean(measurements));
        String P05 = f.p(measurements.getFraction(0.05));
        String P50 = f.p(measurements.getFraction(0.50));
        String P95 = f.p(measurements.getFraction(0.95));
        String MAX = f.p(measurements.getFraction(1.00));

        String format = "%"+digits+"s";
        out.printf("%24s %6d  "+format+"  "+format+"  "+format+"  "+format+"  "+format+"\n",name,N,mean,P05,P50,P95,MAX);
        out.flush();
    }

    public void printSummary() {
        printHeader(true);
        for( SummaryInfo info : summary ) {
            printStats(info.name,info.measurements);
        }
    }

    public void saveSummary( String name , GrowQueue_F64 measurements ) {
        SummaryInfo info = new SummaryInfo();
        info.name = name;
        info.measurements.addAll(measurements);
        this.summary.add(info);
    }

    private static class SummaryInfo
    {
        GrowQueue_F64 measurements = new GrowQueue_F64();
        String name;
    }
}
