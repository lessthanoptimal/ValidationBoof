package validate.fiducial;

import boofcv.misc.BoofMiscOps;

import java.io.File;
import java.io.PrintStream;
import java.util.*;

import static validate.fiducial.FiducialCommon.parseDetections;

/**
 * Evaluates sequences where the same target(s) are always visible and are expected to be detectable
 *
 * @author Peter Abeles
 */
public class EvaluateAlwaysVisibleSequence implements FiducialEvaluateInterface {

	PrintStream outputResults = System.out;
	PrintStream err = System.err;

	int totalExpected;
	int totalCorrect;

	@Override
	public void evaluate(File resultsDirectory, File dataset) {

		outputResults.println("# "+dataset.getName()+"   sequence with known always visible targets");

		FiducialCommon.Library library = FiducialCommon.parseScenario(new File(dataset, "library.txt"));
		List<String> visible = FiducialCommon.parseVisibleFile(new File(dataset, "visible.txt"));

		List<String> results = BoofMiscOps.directoryList(resultsDirectory.getAbsolutePath(), "csv");
		Collections.sort(results);

		Map<Long,Tally> map = new HashMap<Long,Tally>();

		for( String name : visible ) {
			long id = library.nameToID(name);
			map.put( id , new Tally(id));
		}

		int numFalsePositive = 0;

		for (int i = 0; i < results.size(); i++) {

			List<FiducialCommon.Detected> detected = parseDetections(new File(results.get(i)));

			for( FiducialCommon.Detected d : detected ) {
				Tally t = map.get((long)d.id);
				if( t == null ) {
					numFalsePositive++;
					continue;
				}

				t.currentFrame++;
			}

//			if(detected.size() == 0 ) {
//				System.out.println(results.get(i));
//			}

			for( Tally t : map.values() ) {
				if( t.currentFrame >= 1 ) {
					t.numVisible++;
				}
				if( t.currentFrame > 1 ) {
					t.numMultiple++;
				}
				t.currentFrame = 0;
			}
		}

		outputResults.println("total frames "+results.size());
		outputResults.println("false positives "+numFalsePositive);

		// make sure its in the same order always
		List<Tally> list = new ArrayList<Tally>(map.values());
		Collections.sort(list, new Comparator<Tally>() {
			@Override
			public int compare(Tally o1, Tally o2) {
				if( o1.id < o2.id )
					return -1;
				else if( o1.id > o2.id )
					return 1;
				else
					return 0;
			}
		});

		totalCorrect = 0;
		outputResults.println(" fiducial id | visible | multiple ");
		for( Tally t : list ) {
			outputResults.printf("%05d          %4d         %4d\n", t.id, t.numVisible, t.numMultiple);
			totalCorrect += t.numVisible;
		}
		outputResults.flush();

		totalExpected = visible.size()*results.size();
	}

	@Override
	public void setErrorStream(PrintStream errorLog) {
		this.err = errorLog;
	}

	@Override
	public int getTotalExpected() {
		return totalExpected;
	}

	@Override
	public int getTotalCorrect() {
		return totalCorrect;
	}

	@Override
	public void setOutputResults(PrintStream outputResults) {
		this.outputResults = outputResults;
	}

	protected static class Tally {
		public long id;
		// number of frames it was visible at least once
		public int numVisible;
		// number of frames it was seem multiple times
		public int numMultiple;

		// number of times it was visible this frame
		public int currentFrame;

		public Tally(long id) {
			this.id = id;
		}
	}
}
