package boofcv.regression;

import boofcv.common.BoofRegressionConstants;
import boofcv.common.RegressionRunner;
import boofcv.struct.image.ImageDataType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Peter Abeles
 */
public class MasterRegressionApplication {

	public static List<String> lookupListOfRegressions() {
		List<String> out = new ArrayList<>();

		File[] dirModules = new File("modules").listFiles();
		if( dirModules == null )
			return out;

		for( File m : dirModules ) {
			if( m.isDirectory() && new File(m,"module_regression.jar").exists() ) {
				Collection<File> found = FileUtils.listFiles(new File(m,"src/main/java/boofcv/regression"),null,false);

				for( File f : found ) {
					if( f.getName().endsWith("Regression.java")) {
						out.add(FilenameUtils.removeExtension(f.getName()));
					}
				}
			}
		}
		Collections.sort(out);
		return out;
	}

	public static boolean runRegression( String name , ImageDataType imageType)
	{
		File[] dirModules = new File("modules").listFiles();
		if( dirModules == null )
			return false;

		for( File m : dirModules ) {
			if( m.isDirectory() && new File(m,"module_regression.jar").exists() ) {
				Collection<File> found = FileUtils.listFiles(new File(m,"src/main/java/boofcv/regression"),null,false);

				for( File f : found ) {
					if( FilenameUtils.removeExtension(f.getName()).matches(name)) {
						return runRegression(new File(m,"module_regression.jar"),name,imageType);
					}
				}
			}
		}
		return false;
	}

	public static boolean runRegression( File pathJar , String className , ImageDataType imageType ) {
		List<String> path = new ArrayList<>();
		path.add( pathJar.getAbsolutePath() );

		JavaRuntimeLauncher launcher = new JavaRuntimeLauncher(path);

		launcher.setFrozenTime(5*60*60*1000);
		launcher.setMemoryInMB(5000);

		JavaRuntimeLauncher.Exit exit;
		if( imageType != null ) {
			exit = launcher.launch(RegressionRunner.class,"boofcv.regression."+className,imageType.name());
		} else {
			exit = launcher.launch(RegressionRunner.class,"boofcv.regression."+className);
		}

		switch( exit ) {
			case FROZEN:
				System.out.println("Froze!");
				return false;

			case RETURN_NOT_ZERO:
				System.out.println("failed!");
				return false;
		}
		return true;
	}

	public static void saveMachineInfo() {
		try {
			PrintStream out = new PrintStream(new File(BoofRegressionConstants.CURRENT_DIRECTORY,"MachineInfo.txt"));

			out.println("Runtime.getRuntime().availableProcessors(): " +Runtime.getRuntime().availableProcessors());
			out.println("Runtime.getRuntime().freeMemory(): " +Runtime.getRuntime().freeMemory());
			out.println("Runtime.getRuntime().totalMemory(): " + Runtime.getRuntime().totalMemory());

			Properties properties = System.getProperties();
			Set<Object> keys = properties.keySet();
			for( Object key : keys ) {
				out.println("=========== "+key.toString());
				out.println(properties.getProperty(key.toString()));
			}

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();

		BoofRegressionConstants.clearCurrentResults();

		PrintStream errorStream = new PrintStream(
				new File(BoofRegressionConstants.CURRENT_DIRECTORY,
						"ERRORLOG_"+MasterRegressionApplication.class.getSimpleName()+".txt"));

		ImageDataType[] dataTypes = new ImageDataType[]{ImageDataType.U8,ImageDataType.F32};

		saveMachineInfo();

		List<String> listOfRegressions = lookupListOfRegressions();

		for( String s : listOfRegressions ) {
			System.out.println(s);
		}

		// Run regressions which take take an image as input first
		for( String s : listOfRegressions ) {
			if( s.endsWith("FRegression")) {
				if( !runRegression(s,null) ) {
					errorStream.println("FAILED "+s);
					System.out.println("FAILED "+s);
				} else {
					System.out.println("SUCCESS "+s);
				}
			}
		}

		// Run regressions which take an image of a specific type first
		for( ImageDataType imageType : dataTypes ) {
			for( String s : listOfRegressions ) {
				if( s.endsWith("FRegression"))
					continue;
				if( !runRegression(s,imageType) ) {
					errorStream.println("FAILED "+imageType+"  "+s);
					System.out.println(imageType+" FAILED "+s);
				} else {
					System.out.println(imageType+" SUCCESS "+s);

				}
			}
		}

		// print how long the test took
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime-startTime;

		double elapsedSeconds = elapsedTime/1000.0;

		double secondsPerDay = 24*60*60;
		double secondsPerHour = 60*60;
		double secondsPerMinute = 60;

		int days = (int)(elapsedSeconds/secondsPerDay);
		int hours = (int)((elapsedSeconds-days*secondsPerDay)/secondsPerHour);
		int minute = (int)((elapsedSeconds-days*secondsPerDay-hours*secondsPerHour)/secondsPerMinute);
		double seconds = elapsedSeconds-days*secondsPerDay-hours*secondsPerHour-minute*secondsPerMinute;

		System.out.printf("%d Days %02d Hours %02d Minutes %6.2f Seconds\n",days,hours,minute,seconds);
		System.out.println();
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		System.out.println(dateFormat.format(new Date()));

		errorStream.close();
	}
}
