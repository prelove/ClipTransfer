package com.cliptransfer.common;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * Logger utility class
 * Provides unified logging configuration and management functionality
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class LoggerUtil {
    private static final String LOG_DIR = System.getProperty("user.home") + File.separator + 
                                          ".cliptransfer" + File.separator + "logs";
    private static final String LOG_FILE_PREFIX = "cliptransfer";
    private static boolean isInitialized = false;
    
    /**
     * Initialize logging system
     * @param logLevel log level
     */
    public static void initLogger(String logLevel) {
        if (isInitialized) {
            return;
        }
        
        try {
            // Ensure log directory exists
            File logDir = new File(LOG_DIR);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            // Get root logger
            Logger rootLogger = Logger.getLogger("");
            
            // Remove default console handlers
            Handler[] handlers = rootLogger.getHandlers();
            for (Handler handler : handlers) {
                rootLogger.removeHandler(handler);
            }
            
            // Set log level
            Level level = parseLogLevel(logLevel);
            rootLogger.setLevel(level);
            
            // Add console handler
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(level);
            consoleHandler.setFormatter(new SimpleLogFormatter());
            rootLogger.addHandler(consoleHandler);
            
            // Add file handler
            String logFileName = LOG_DIR + File.separator + LOG_FILE_PREFIX + "_%g.log";
            FileHandler fileHandler = new FileHandler(
                logFileName,
                1024 * 1024, // 1MB
                5,           // Keep 5 files
                true         // Append mode
            );
            fileHandler.setLevel(level);
            fileHandler.setFormatter(new DetailedLogFormatter());
            rootLogger.addHandler(fileHandler);
            
            isInitialized = true;
            
            Logger logger = Logger.getLogger(LoggerUtil.class.getName());
            logger.info("Logging system initialized successfully, level: " + logLevel + ", log directory: " + LOG_DIR);
            
        } catch (IOException e) {
            System.err.println("Failed to initialize logging system: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Parse log level
     */
    private static Level parseLogLevel(String logLevel) {
        if (logLevel == null || logLevel.trim().isEmpty()) {
            return Level.INFO;
        }
        
        try {
            return Level.parse(logLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid log level: " + logLevel + ", using default level INFO");
            return Level.INFO;
        }
    }
    
    /**
     * Simple log formatter (for console)
     */
    private static class SimpleLogFormatter extends Formatter {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        
        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            
            // Time
            sb.append(dateFormat.format(new Date(record.getMillis())));
            sb.append(" ");
            
            // Level
            sb.append(String.format("%-7s", record.getLevel().getName()));
            sb.append(" ");
            
            // Class name (short)
            String className = record.getSourceClassName();
            if (className != null) {
                int lastDot = className.lastIndexOf('.');
                if (lastDot >= 0) {
                    className = className.substring(lastDot + 1);
                }
                sb.append(String.format("%-15s", className));
                sb.append(" - ");
            }
            
            // Message
            sb.append(record.getMessage());
            sb.append("\n");
            
            // Exception information
            if (record.getThrown() != null) {
                sb.append(getStackTrace(record.getThrown()));
            }
            
            return sb.toString();
        }
    }
    
    /**
     * Detailed log formatter (for file)
     */
    private static class DetailedLogFormatter extends Formatter {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        
        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            
            // Time
            sb.append(dateFormat.format(new Date(record.getMillis())));
            sb.append(" ");
            
            // Thread
            sb.append(String.format("[%-10s]", Thread.currentThread().getName()));
            sb.append(" ");
            
            // Level
            sb.append(String.format("%-7s", record.getLevel().getName()));
            sb.append(" ");
            
            // Full class name and method name
            if (record.getSourceClassName() != null) {
                sb.append(record.getSourceClassName());
                if (record.getSourceMethodName() != null) {
                    sb.append(".");
                    sb.append(record.getSourceMethodName());
                    sb.append("()");
                }
                sb.append(" - ");
            }
            
            // Message
            sb.append(record.getMessage());
            sb.append("\n");
            
            // Exception information
            if (record.getThrown() != null) {
                sb.append(getStackTrace(record.getThrown()));
            }
            
            return sb.toString();
        }
    }
    
    /**
     * Get exception stack trace
     */
    private static String getStackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Get log directory
     */
    public static String getLogDirectory() {
        return LOG_DIR;
    }
    
    /**
     * Clean up old log files
     * @param keepDays days to keep
     */
    public static void cleanupOldLogs(int keepDays) {
        try {
            File logDir = new File(LOG_DIR);
            if (!logDir.exists()) {
                return;
            }
            
            long cutoffTime = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L);
            File[] logFiles = logDir.listFiles((dir, name) -> 
                name.startsWith(LOG_FILE_PREFIX) && name.endsWith(".log"));
            
            if (logFiles != null) {
                int deletedCount = 0;
                for (File logFile : logFiles) {
                    if (logFile.lastModified() < cutoffTime) {
                        if (logFile.delete()) {
                            deletedCount++;
                        }
                    }
                }
                
                if (deletedCount > 0) {
                    Logger logger = Logger.getLogger(LoggerUtil.class.getName());
                    logger.info("Cleaned up old log files: " + deletedCount + " files");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to clean up log files: " + e.getMessage());
        }
    }
}