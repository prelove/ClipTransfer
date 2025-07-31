package com.cliptransfer.common;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configuration management class
 * Handles application settings loading, saving, and management
 * Supports default values and user customization
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class Config {
    private static final Logger logger = Logger.getLogger(Config.class.getName());
    
    // Configuration file paths
    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".cliptransfer";
    private static final String USER_CONFIG_FILE = CONFIG_DIR + File.separator + "config.properties";
    private static final String DEFAULT_CONFIG_FILE = "config.properties";
    
    // Default configuration values
    public static final int DEFAULT_CHUNK_SIZE = 512 * 1024; // 512KB
    public static final int DEFAULT_SEND_INTERVAL = 2000; // 2 seconds
    public static final int DEFAULT_RECEIVE_INTERVAL = 1000; // 1 second
    public static final String DEFAULT_LOG_LEVEL = "INFO";
    public static final String DEFAULT_DOWNLOAD_PATH = System.getProperty("user.home") + File.separator + "Downloads";
    
    // Configuration properties
    private final Properties properties;
    
    /**
     * Constructor
     */
    public Config() {
        this.properties = new Properties();
        loadConfig();
    }
    
    /**
     * Load configuration
     */
    public void loadConfig() {
        // First load default configuration from resources
        loadDefaultConfigFromResources();
        
        // Then load user configuration (overrides defaults)
        loadUserConfig();
        
        logger.info("Configuration loading completed");
    }
    
    /**
     * Load default configuration from resources
     */
    private void loadDefaultConfigFromResources() {
        InputStream is = null;
        try {
            // Try multiple ways to load resource file
            is = Config.class.getResourceAsStream("/" + DEFAULT_CONFIG_FILE);
            if (is == null) {
                // If first method fails, try loading from classpath root
                is = Config.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE);
            }
            
            if (is != null) {
                properties.load(new InputStreamReader(is, "UTF-8"));
                
                // Handle special path variables
                String downloadPath = properties.getProperty("download.path");
                if (downloadPath != null && !downloadPath.startsWith(File.separator) && 
                    !downloadPath.contains(":")) {
                    // If it's a relative path, make it relative to user directory
                    properties.setProperty("download.path", 
                        System.getProperty("user.home") + File.separator + downloadPath);
                }
                
                logger.info("Default configuration file loaded successfully: " + DEFAULT_CONFIG_FILE);
            } else {
                logger.info("Default configuration file not found, using hardcoded defaults");
                setHardcodedDefaults();
            }
        } catch (IOException e) {
            logger.warning("Failed to read default configuration file: " + e.getMessage());
            setHardcodedDefaults();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore close exception
                }
            }
        }
    }
    
    /**
     * Load user configuration
     */
    private void loadUserConfig() {
        File userConfigFile = new File(USER_CONFIG_FILE);
        
        if (userConfigFile.exists()) {
            try (FileInputStream fis = new FileInputStream(userConfigFile);
                 InputStreamReader isr = new InputStreamReader(fis, "UTF-8")) {
                
                properties.load(isr);
                logger.info("User configuration file loaded: " + USER_CONFIG_FILE);
                
            } catch (IOException e) {
                logger.warning("Failed to load user configuration file: " + e.getMessage());
            }
        } else {
            logger.info("User configuration file does not exist, will be created on first save");
        }
    }
    
    /**
     * Set hardcoded default values
     */
    private void setHardcodedDefaults() {
        properties.setProperty("chunk.size", String.valueOf(DEFAULT_CHUNK_SIZE));
        properties.setProperty("send.interval", String.valueOf(DEFAULT_SEND_INTERVAL));
        properties.setProperty("receive.interval", String.valueOf(DEFAULT_RECEIVE_INTERVAL));
        properties.setProperty("log.level", DEFAULT_LOG_LEVEL);
        properties.setProperty("download.path", DEFAULT_DOWNLOAD_PATH);
        
        logger.info("Using hardcoded default configuration values");
    }
    
    /**
     * Save configuration
     */
    public void saveConfig() {
        try {
            // Ensure configuration directory exists
            File configDir = new File(CONFIG_DIR);
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            
            // Save user configuration
            try (FileOutputStream fos = new FileOutputStream(USER_CONFIG_FILE);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8")) {
                
                properties.store(osw, "ClipTransfer User Configuration - Generated on " + 
                    new java.util.Date());
                
                logger.info("Configuration saved successfully: " + USER_CONFIG_FILE);
                
            }
            
        } catch (IOException e) {
            logger.severe("Failed to save configuration: " + e.getMessage());
            throw new RuntimeException("Configuration save failed", e);
        }
    }
    
    // Getter methods
    
    public int getChunkSize() {
        String value = properties.getProperty("chunk.size", String.valueOf(DEFAULT_CHUNK_SIZE));
        try {
            int chunkSize = Integer.parseInt(value);
            // Validate range: 1KB to 10MB
            if (chunkSize < 1024 || chunkSize > 10 * 1024 * 1024) {
                logger.warning("Invalid chunk size: " + chunkSize + ", using default: " + DEFAULT_CHUNK_SIZE);
                return DEFAULT_CHUNK_SIZE;
            }
            return chunkSize;
        } catch (NumberFormatException e) {
            logger.warning("Invalid chunk size format: " + value + ", using default: " + DEFAULT_CHUNK_SIZE);
            return DEFAULT_CHUNK_SIZE;
        }
    }
    
    public int getSendInterval() {
        String value = properties.getProperty("send.interval", String.valueOf(DEFAULT_SEND_INTERVAL));
        try {
            int interval = Integer.parseInt(value);
            // Validate range: 100ms to 60s
            if (interval < 100 || interval > 60000) {
                logger.warning("Invalid send interval: " + interval + ", using default: " + DEFAULT_SEND_INTERVAL);
                return DEFAULT_SEND_INTERVAL;
            }
            return interval;
        } catch (NumberFormatException e) {
            logger.warning("Invalid send interval format: " + value + ", using default: " + DEFAULT_SEND_INTERVAL);
            return DEFAULT_SEND_INTERVAL;
        }
    }
    
    public int getReceiveInterval() {
        String value = properties.getProperty("receive.interval", String.valueOf(DEFAULT_RECEIVE_INTERVAL));
        try {
            int interval = Integer.parseInt(value);
            // Validate range: 100ms to 10s
            if (interval < 100 || interval > 10000) {
                logger.warning("Invalid receive interval: " + interval + ", using default: " + DEFAULT_RECEIVE_INTERVAL);
                return DEFAULT_RECEIVE_INTERVAL;
            }
            return interval;
        } catch (NumberFormatException e) {
            logger.warning("Invalid receive interval format: " + value + ", using default: " + DEFAULT_RECEIVE_INTERVAL);
            return DEFAULT_RECEIVE_INTERVAL;
        }
    }
    
    public String getLogLevel() {
        return properties.getProperty("log.level", DEFAULT_LOG_LEVEL);
    }
    
    public String getDownloadPath() {
        String path = properties.getProperty("download.path", DEFAULT_DOWNLOAD_PATH);
        
        // Ensure directory exists
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
            logger.info("Created download directory: " + path);
        }
        
        return path;
    }
    
    // Setter methods
    
    public void setChunkSize(int chunkSize) {
        if (chunkSize < 1024 || chunkSize > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Chunk size must be between 1KB and 10MB");
        }
        properties.setProperty("chunk.size", String.valueOf(chunkSize));
    }
    
    public void setSendInterval(int interval) {
        if (interval < 100 || interval > 60000) {
            throw new IllegalArgumentException("Send interval must be between 100ms and 60s");
        }
        properties.setProperty("send.interval", String.valueOf(interval));
    }
    
    public void setReceiveInterval(int interval) {
        if (interval < 100 || interval > 10000) {
            throw new IllegalArgumentException("Receive interval must be between 100ms and 10s");
        }
        properties.setProperty("receive.interval", String.valueOf(interval));
    }
    
    public void setLogLevel(String logLevel) {
        if (logLevel == null || logLevel.trim().isEmpty()) {
            throw new IllegalArgumentException("Log level cannot be null or empty");
        }
        properties.setProperty("log.level", logLevel.trim());
    }
    
    public void setDownloadPath(String downloadPath) {
        if (downloadPath == null || downloadPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Download path cannot be null or empty");
        }
        properties.setProperty("download.path", downloadPath.trim());
    }
    
    /**
     * Get configuration directory
     */
    public static String getConfigDirectory() {
        return CONFIG_DIR;
    }
    
    /**
     * Reset to default values
     */
    public void resetToDefaults() {
        properties.clear();
        setHardcodedDefaults();
        logger.info("Configuration reset to default values");
    }
    
    @Override
    public String toString() {
        return String.format("Config{chunkSize=%d, sendInterval=%d, receiveInterval=%d, logLevel='%s', downloadPath='%s'}", 
                getChunkSize(), getSendInterval(), getReceiveInterval(), getLogLevel(), getDownloadPath());
    }
}