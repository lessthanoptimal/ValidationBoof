package boofcv.regression;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;

/**
 * Launches a new java process with the specified task. Monitors its progress and kills the process if frozen.
 * Can specify memory, arguments, and when a process is considered frozen. The user can terminate the process
 * gracefully by pressing certain keys. By default the standard error and out from the process are redirected
 * to the master application's std error and std out.
 *
 * @author Peter Abeles
 */
public class JavaRuntimeLauncher {

    // Where to send output and error streams from the process
    PrintStream out = System.out;
    PrintStream err = System.err;

    private String classPath;
    // amount of memory allocated to the JVM
    private long memoryInMB = 200;
    // if the process doesn't finish in this number of milliseconds it's considered frozen and killed
    private long frozenTime = 60*1000;

    // amount of time it actually took to execute in milliseconds
    private long durationMilli;

    // save for future debugging
    private String[] jvmArgs;

    // can the user kill the process by pressing Q?
    boolean userKillWithQ = false;

    // periodically print messages indicating that the process is still alive
    boolean printAlive = true;

    /**
     * Constructor.  Configures which library it is to be launching a class from/related to
     * @param pathJars List of paths to all the jars
     */
    public JavaRuntimeLauncher( List<String> pathJars ) {

        String sep = System.getProperty("path.separator");

        if( pathJars != null ) {
            classPath = "";

            for( String s : pathJars ) {
                classPath = classPath + sep + s;
            }
        }
    }

    /**
     * Specifies the amount of time the process has to complete.  After which it is considered frozen and
     * will be killed
     * @param frozenTime time in milliseconds
     */
    public void setFrozenTime(long frozenTime) {
        this.frozenTime = frozenTime;
    }

    /**
     * Specifies the amount of memory the process will be allocated in megabytes
     * @param memoryInMB megabytes
     */
    public void setMemoryInMB(long memoryInMB) {
        this.memoryInMB = memoryInMB;
    }

    /**
     * Returns how long the operation took to complete. In milliseconds
     */
    public long getDurationMilli() {
        return durationMilli;
    }

    /**
     * Launches the class with the provided arguments.
     * @param mainClass Class
     * @param args it's arguments
     * @return true if successful or false if it ended on error
     */
    public Exit launch( Class mainClass , String ...args ) {

        jvmArgs = configureArguments(mainClass,args);

        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(jvmArgs);

            // If it exits too quickly it might not get any error messages if it crashes right away
            // so the work around is to sleep
            Thread.sleep(500);

            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            BufferedReader error = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

            // print the output from the slave
            if( !monitorSlave(pr, input, error) )
                return Exit.FROZEN;

            if( pr.exitValue() != 0 ) {
                return Exit.RETURN_NOT_ZERO;
            } else {
                return Exit.NORMAL;
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Prints out the standard out and error from the slave and checks its health.  Exits if
     * the slave has finished or is declared frozen.
     *
     * @return true if successful or false if it was forced to kill the slave because it was frozen
     */
    private boolean monitorSlave(Process pr,
                                 BufferedReader input, BufferedReader error)
            throws IOException, InterruptedException {

        // flush the input buffer
        if( userKillWithQ )
            System.in.skip(System.in.available());

        // If the total amount of time allocated to the slave exceeds the maximum number of trials multiplied
        // by the maximum runtime plus some fudge factor the slave is declared as frozen

        boolean frozen = false;

        long startTime = System.currentTimeMillis();
        long lastAliveMessage = startTime;
        for(;;) {
            while( userKillWithQ && System.in.available() > 0 ) {
                if( System.in.read() == 'q' ) {
                    System.out.println("User requested for the application to quit by pressing 'q'");
                    System.exit(0);
                }
            }

            printError(error);

            if( input.ready() ) {
                printInputBuffer(input);
            } else {
                Thread.sleep(500);
            }

            try {
                // exit value throws an exception is the process has yet to stop
                pr.exitValue();
                break;
            } catch( IllegalThreadStateException e) {
                long ellapsedTime = System.currentTimeMillis() - startTime;

                // check to see if the process is frozen
                if(ellapsedTime > frozenTime ) {
                    pr.destroy(); // kill the process
                    frozen = true;
                    break;
                }

                // let everyone know its still alive
                if( printAlive && System.currentTimeMillis() - lastAliveMessage > 300000 ) {
                    int percent = (int)(100*(ellapsedTime/(double)frozenTime));
                    if( userKillWithQ )
                        System.out.println("\nMaster is still alive: "+new Date()+"  Press 'q' and enter to quit. "+percent+"%");
                    else
                        System.out.println("\nMaster is still alive: "+new Date()+"  "+percent+"%");
                    lastAliveMessage = System.currentTimeMillis();
                }
            }
        }
        durationMilli = System.currentTimeMillis()-startTime;
        return !frozen;
    }

    protected void printError(BufferedReader error) throws IOException {
        while( error.ready() ) {
            int val = error.read();
            if( val < 0 ) break;

            err.print(Character.toChars(val));
        }
    }

    protected void printInputBuffer(BufferedReader input) throws IOException {

        while( input.ready() ) {
            int val = input.read();
            if( val < 0 ) break;

            out.print(Character.toChars(val));
        }
    }

    private String[] configureArguments( Class mainClass , String ...args ) {
        String[] out = new String[8+args.length];

        String app = System.getProperty("java.home")+"/bin/java";

        // run headless to avoid this issue:
        // Exception in thread "main" java.awt.AWTError: Can't connect to X11 window server using
        // 'localhost:10.0' as the value of the DISPLAY variable.

        out[0] = app;
        out[1] = "-server";
        out[2] = "-Djava.awt.headless=true";
        out[3] = "-Xms"+memoryInMB+"M";
        out[4] = "-Xmx"+memoryInMB+"M";
        out[5] = "-classpath";
        out[6] = classPath;
        out[7] = mainClass.getName();
        for (int i = 0; i < args.length; i++) {
            out[8+i] = args[i];
        }
        return out;
    }

    public boolean isUserKillWithQ() {
        return userKillWithQ;
    }

    public void setUserKillWithQ(boolean userKillWithQ) {
        this.userKillWithQ = userKillWithQ;
    }

    public boolean isPrintAlive() {
        return printAlive;
    }

    public void setPrintAlive(boolean printAlive) {
        this.printAlive = printAlive;
    }

    public String getClassPath() {
        return classPath;
    }

    public long getAllocatedMemoryInMB() {
        return memoryInMB;
    }

    public long getFrozenTime() {
        return frozenTime;
    }

    public String[] getArguments() {
        return jvmArgs;
    }

    public PrintStream getOut() {
        return out;
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public PrintStream getErr() {
        return err;
    }

    public void setErr(PrintStream err) {
        this.err = err;
    }

    public enum Exit
    {
        /**
         * Exited normally.
         */
        NORMAL,
        /**
         * Did not finish in the required amount of time
         */
        FROZEN,
        /**
         * exited with a non zero return value
         */
        RETURN_NOT_ZERO,
    }
}
