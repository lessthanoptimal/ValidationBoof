package boofcv.metrics.point;

/**
 * @author Peter Abeles
 */
public enum EvaluatedAlgorithm {
	KLT("KLT"),
	FAST_BRIEF("FAST-BRIEF"),
	FH_BRIEF("FH-BRIEF"),
	FH_SURF("FH-SURF"),
	FH_BRIEF_KLT("FH-BRIEF-KLT"),
	FH_SURF_KLT("FH-SURF-KLT");
//	BRIEF_KLT("BRIEF-KLT")

	String name;

	EvaluatedAlgorithm(String name ) {
		this.name = name;
	}

	public String toString() {
		return name;
	}
}
