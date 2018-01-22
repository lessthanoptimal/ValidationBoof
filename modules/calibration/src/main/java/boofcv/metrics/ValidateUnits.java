package boofcv.metrics;

/**
 * @author Peter Abeles
 */
public enum ValidateUnits {
	MM(1.0/1000.0,"MM"),
	CM(1.0/100.0,"CM"),
	M(1.0,"M"),
	KM(1000.0,"KM"),
	INCHES(0.0254,"IN"),
	FEET(0.3048,"FT"),
	YARDS(0.9144,"YDS"),
	MILES(1609.34,"MILES");

	String shortName;
	double toMeters;

	ValidateUnits( double toMeters , String shortName) {
		this.shortName = shortName;
		this.toMeters = toMeters;
	}

	public static ValidateUnits lookup( String name ) {
		for( ValidateUnits u : values() ) {
			if( name.compareToIgnoreCase(u.getShortName()) == 0) {
				return u;
			}
		}
		return null;
	}

	public String getShortName() {
		return shortName;
	}

	public double getToMeters() {
		return toMeters;
	}
}
