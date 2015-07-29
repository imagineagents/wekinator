/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wekimini;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.JTextArea;
import wekimini.gui.Console;
import wekimini.util.TeeStream;
import wekimini.util.Util;

/**
 * TODO: Make sure no problems when running mutliple wekinators at once
 * TODO: Test on Windows and linux
 *  
 * @author rebecca
 */
public class LoggingManager {
    private final Wekinator w;
    //private File loggingFileLocation;
    private final String logFileLocation;
    private final String backupLogLocation;
    private static final Logger logger = Logger.getLogger(LoggingManager.class.getName());
    private FileHandler fileHandler = null;
    private WekinatorConsoleHandler consoleHandler = null;
    private final LinkedList<Handler> handlers;
    private static PrintStream teeStdOut;
    private static PrintStream teeStdErr;

    
    public static String getUniversalLoggingLocation() {
        if (Util.isMac()) {
            String logParent = System.getProperty("user.home") + "/Library/Application Support/Wekinator";
            File f = new File(logParent);
            f.mkdirs();
            return logParent + "/wekinator_main.log";
        } else if (Util.isWindows()) {
            String try1 = System.getenv("APPDATA");
            if (try1 != null) {
                return System.getenv("APPDATA") + File.separator + "wekinator_main.log";
            } else {
                return System.getProperty("java.io.tmpdir") + File.separator + "wekinator_main.log";
            }
        } else {
            //TODO: test on linux
            return "%t" + File.separator + "wekinator_main.log";
        }
    }
    
    public static String getPrimaryLoggingFilename() {
        if (Util.isMac()) {
            String logParent = System.getProperty("user.home") + "/Library/Application Support/Wekinator";
            File f = new File(logParent);
            f.mkdirs();
            return logParent + "/wekinator%g.log";
        } else if (Util.isWindows()) {
            String try1 = System.getenv("APPDATA"); //Most likely user directory\AppData\Roaming\ on win7
            if (try1 != null) {
                return System.getenv("APPDATA") + File.separator + "wekinator%g.log";
            } else {
                return System.getProperty("java.io.tmpdir") + File.separator + "wekinator%g.log";
            }
        } else {
            //TODO: test on linux
            return "%t" + File.separator + "wekinator%g.log"; //temp dir / wekinatorN.log
        }
    }
    
    public LoggingManager(Wekinator w) {
        this.w = w;
        consoleHandler = new WekinatorConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        handlers = new LinkedList<>();
        logFileLocation = getPrimaryLoggingFilename();
        backupLogLocation = "%h" + File.separator + "wekinator" + "%g.log"; //home dir / WekinatorN.log
    }
    
    public void startLoggingToFile() throws IOException {
        String location;
        if (fileHandler != null) {
            logger.log(Level.WARNING, "Already logging to file; exiting without creating new log");
            return;
        }
        try {
            fileHandler = new FileHandler(logFileLocation, Integer.MAX_VALUE, 5);
            location = logFileLocation;
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not write to log at {0}", logFileLocation);
            logger.log(Level.WARNING, null, ex);
            try {
                fileHandler = new FileHandler(backupLogLocation);
                location = backupLogLocation;
                logger.log(Level.INFO, "Logging instead to {0}", backupLogLocation);
            } catch (IOException ex1) {
                logger.log(Level.SEVERE, "Could not write to log at {0}", backupLogLocation);
                logger.log(Level.WARNING, null, ex1);
                throw ex1;
            }
        }
        fileHandler.setFormatter(new SimpleFormatter());
        fileHandler.setLevel(Level.ALL);
        handlers.add(fileHandler);
        Logger.getLogger(Wekinator.class.getPackage().getName()).addHandler(fileHandler);
        logger.log(Level.INFO, "Set up file logging; logging location {0}", location);
    }
    
    public void stopLoggingToFile() {
        if (fileHandler != null) {
            Logger.getLogger(Wekinator.class.getPackage().getName()).removeHandler(fileHandler);
            handlers.remove(fileHandler);
        }
    }
    
    public void setFileLoggingLevel(Level newLevel) {
        fileHandler.setLevel(newLevel);
    }
    
    public void dumpLogToConsoleGUI() {
        System.out.println("NOT YET IMPLEMENTED TODO");
    }
    
    public void startLoggingToConsoleGUI(JTextArea a) {
        consoleHandler.setTextArea(a);
        dumpLogToConsoleGUI();
        handlers.add(consoleHandler);
        Logger.getLogger(Wekinator.class.getPackage().getName()).addHandler(consoleHandler);
    }
    
    public void stopLoggingToConsoleGUI() {
        if (consoleHandler != null) {
            Logger.getLogger(Wekinator.class.getPackage().getName()).removeHandler(consoleHandler);
            handlers.remove(consoleHandler);
        }
    }
    
    public void setConsoleGUILogLevel(Level newLevel) {
        
    }
    
    public void flushLogFile() {
        if (fileHandler != null) {
            fileHandler.flush();
        }
    }
    
    //TODO: call before wekinator quits
    public void prepareToDie() {
        flushLogFile();
    }
    
    public static void setupUniversalLog(String versionString) {
        PrintStream logOut = null;
        try {
            String s = getUniversalLoggingLocation();
            File logFile = new File(s);
            logOut = new PrintStream(new FileOutputStream(logFile, false));
            
            teeStdOut = new TeeStream(System.out, logOut);
            teeStdErr = new TeeStream(System.err, logOut);
            
            System.setOut(teeStdOut);
            System.setErr(teeStdErr);
            
            System.out.println("RUNNING WEKINATOR VERSION " + versionString);
            logger.log(Level.INFO, "Universal log file at {0}", s);
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, "Could not set up universal logs");
            logger.log(Level.SEVERE, null, ex);
        } 
    } 
    
    public static void flushUniversalLogs() {
        if (teeStdErr != null) { //TODO DO WE GET HERE?
            teeStdErr.flush();  
        }
        if (teeStdOut != null) {
            teeStdOut.flush();
        }
    }
    
    public static void closeUniversalLogs() {
        flushUniversalLogs();
        if (teeStdErr != null) {
            teeStdErr.close();
        }
        if (teeStdOut != null) {
            teeStdOut.close();
        }
    }
}
