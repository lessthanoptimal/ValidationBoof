package validate.shape;

import georegression.struct.point.Point2D_F64;
import validate.misc.PointFileCodec;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts from a list of points with shape indexes into a list of point sets.  Changed file format
 * at one point
 *
 * @author Peter Abeles
 */
public class ConvertPointListToSets {
	public static void convert( String nameList , String nameDesc , String nameOutput )
	{
		List<Point2D_F64> all = PointFileCodec.load(nameList);

		List<PolygonTruthIndexes> indexes = UtilShapeDetector.loadDescription(new File(nameDesc));

		List<List<Point2D_F64>> sets = new ArrayList<List<Point2D_F64>>();

		for( PolygonTruthIndexes a : indexes ) {
			List<Point2D_F64> set = new ArrayList<Point2D_F64>();

			for ( int index : a.indexes ) {
				set.add( all.get(index));
			}

			sets.add(set);
		}

		PointFileCodec.saveSets(nameOutput,"Hand selected points",sets);
	}

	public static void main(String[] args) {
		String directory = "data/shape/set04";

		File[] files = new File(directory).listFiles();

		String descriptionFile = new File(directory,"description.txt").getPath();

		for( File f : files ) {
			String name = f.getName();
			if( name.contains("image") && name.contains("txt")) {
				System.out.println("processing "+name);
				ConvertPointListToSets.convert(f.getAbsolutePath(),descriptionFile,f.getAbsolutePath());
			}
		}
	}
}
