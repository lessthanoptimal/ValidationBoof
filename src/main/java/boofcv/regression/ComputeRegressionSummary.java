package boofcv.regression;

import boofcv.BoofVersion;
import boofcv.ValidationBoofVersion;
import boofcv.common.BoofRegressionConstants;
import georegression.GeoRegressionVersion;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.ddogleg.DDoglegVersion;
import org.ejml.EjmlVersion;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Examines the regression results an computes some metrics, saves summary to disk and optionally e-mails out the
 * results.
 *
 * @author Peter Abeles
 */
public class ComputeRegressionSummary {
    Map<String, byte[]> baselineMD5 = new HashMap<>();
    Map<String, byte[]> currentMD5 = new HashMap<>();

    List<String> currentUnique = new ArrayList<>();
    List<String> baselineUnique = new ArrayList<>();
    List<String> missmatched = new ArrayList<>();
    int totalMatched;

    String summary;
    boolean hasMissMatch;

    String emailUsername;
    String emailPassword;
    String emailDestination;
    private long elapsedTime;

    /**
     * E-mail out a summary of the results
     *
     * @param file File with e-mail information
     */
    public void emailSummary( File file ) throws IOException {
        if( !file.exists() ) {
            System.err.println("E-Mail login file doesn't exist");
            return;
        }
        if( summary == null )
            throw new RuntimeException("You must compute the summary before calling this function");

        BufferedReader reader = new BufferedReader(new FileReader(file));

        emailUsername = reader.readLine();
        emailPassword = reader.readLine();
        emailDestination = reader.readLine();

        sendEmailSSL(summary);
    }

    public void generateSummary() throws IOException {
        computeMD5(new File(BoofRegressionConstants.BASELINE_DIRECTORY),baselineMD5);
        computeMD5(new File(BoofRegressionConstants.CURRENT_DIRECTORY),currentMD5);

        for( String key : currentMD5.keySet() ) {
            byte[] c = currentMD5.get(key);
            byte[] b = baselineMD5.get(key);

            if( b == null ) {
                currentUnique.add(key);
            } else {
                if( !Arrays.equals(c,b) ) {
                    missmatched.add(key);
                } else {
                    totalMatched++;
                }
            }
        }

        for( String key : baselineMD5.keySet() ) {
            if( !currentMD5.containsKey(key)) {
                baselineUnique.add(key);
            }
        }

        hasMissMatch = missmatched.size() > 0;

        // make it easier to read later on
        Collections.sort(missmatched);
        Collections.sort(currentUnique);
        Collections.sort(baselineUnique);

        summary = createSummary();

        FileUtils.write(new File(BoofRegressionConstants.CURRENT_DIRECTORY,"summary.txt"),summary,"UTF-8");
    }

    private void sendEmailSSL(String summary ) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        Session session = Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(emailUsername,emailPassword);
                    }
                });

        try {
            String noticed = "  "+(hasMissMatch ? "Miss Match" : "");

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailUsername+"@gmail.com"));
            message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(emailDestination));
            message.setSubject("ValidationBoof Summary"+noticed);
            message.setText(summary);

            Transport.send(message);

            System.out.println("Sent summary to "+emailDestination);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private String createSummary() {
        String summary;

        summary =  "Finished Time:         "+MasterRegressionApplication.formatDate(new Date())+"\n";
        summary += "Elapsed Time:          "+MasterRegressionApplication.formatElapsed(elapsedTime)+"\n";
        summary += "\n";

        summary += String.format("%18s %15s %20.20s %5s\n","Name","Version","SHA","Dirty");
        summary += String.format("%18s %15s %20.20s %5s\n","BoofCV",
                BoofVersion.VERSION, BoofVersion.GIT_SHA,(0!=BoofVersion.DIRTY));
        summary += String.format("%18s %15s %20.20s %5s\n","ValidationBoof",
                ValidationBoofVersion.VERSION, ValidationBoofVersion.GIT_SHA,(0!=ValidationBoofVersion.DIRTY));
        summary += String.format("%18s %15s %20.20s %5s\n","GeoRegression",
                GeoRegressionVersion.VERSION, GeoRegressionVersion.GIT_SHA,(0!=GeoRegressionVersion.DIRTY));
        summary += String.format("%18s %15s %20.20s %5s\n","DDogleg",
                DDoglegVersion.VERSION, DDoglegVersion.GIT_SHA,(0!=DDoglegVersion.DIRTY));
        summary += String.format("%18s %15s %20.20s %5s\n","EJML",
                EjmlVersion.VERSION, EjmlVersion.GIT_SHA,(0!=EjmlVersion.DIRTY));
        summary += "\n";
        summary += "java.runtime.version:  "+System.getProperty("java.runtime.version")+"\n";
        summary += "java.vendor:           "+System.getProperty("java.vendor")+"\n";
        summary += "os.name+arch:          "+System.getProperty("os.name")+" "+System.getProperty("os.arch")+"\n";
        summary += "os.version:            "+System.getProperty("os.version")+"\n";
        summary += "\n";
        summary += "Total Matched:         "+totalMatched+"\n\n";

        if( missmatched.isEmpty() && baselineUnique.isEmpty() && currentUnique.isEmpty() ) {
            summary += "NO EXCEPTIONS!\n";
        } else {
            summary += "Total Miss Matched:    " + missmatched.size() + "\n";
            summary += "Total Baseline Unique: " + baselineUnique.size() + "\n";
            summary += "Total Current Unique:  " + currentUnique.size() + "\n";
            summary += "\n";

            if (missmatched.size()>0) {
                summary += "Miss Matched\n";
                for (String s : missmatched) {
                    summary += "  " + s + "\n";
                }
                summary += "\n";
            }

            if (baselineUnique.size()>0) {
                summary += "Baseline Unique\n";
                for (String s : baselineUnique) {
                    summary += "  " + s + "\n";
                }
                summary += "\n";
            }
            if (currentUnique.size()>0) {
                summary += "Current Unique\n";
                for (String s : currentUnique) {
                    summary += "  " + s + "\n";
                }
                summary += "\n";
            }
        }

        var runtimeSummary = new ComputeRuntimeRegressionSummary();
        summary += "\n";
        summary += "------------------------------------------------------------------------\n";
        summary += runtimeSummary.computeSummary();

        return summary;
    }

    private void computeMD5(File directory, Map<String, byte[]> output) {
        Collection<File> files = FileUtils.listFiles(directory, FileFilterUtils.prefixFileFilter("ACC_"),FileFilterUtils.directoryFileFilter());

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            for (File f : files) {
                String txt = FileUtils.readFileToString(f, "UTF-8");
                // filter out new lines because of the differences are not important and it varies between windows and linux
                txt = txt.replaceAll("\\r?\\n", "");

                byte[] bytesOfMessage = txt.getBytes("UTF-8");
                byte[] thedigest = md.digest(bytesOfMessage);

                // Should be something like U8/calibration/ACC_Foo.txt
                String parent0 = f.getParentFile().getParentFile().getName();
                String parent1 = f.getParentFile().getName();

                File path;
                if( parent1.toLowerCase().equals("other")) {
                    path = new File(parent1);
                } else {
                    path = new File(parent0,parent1);
                }

                String key = new File(path,f.getName()).toString();

                output.put(key,thedigest);
            }
        } catch( IOException | NoSuchAlgorithmException e ) {
            throw new RuntimeException(e);
        }
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public static void main(String[] args) throws IOException {
        ComputeRegressionSummary summary = new ComputeRegressionSummary();
        summary.generateSummary();
        summary.emailSummary(new File("email_login.txt"));
    }

}
