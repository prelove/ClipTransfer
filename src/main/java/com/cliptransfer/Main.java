package com.cliptransfer;

import com.cliptransfer.common.Config;
import com.cliptransfer.common.LoggerUtil;
import com.cliptransfer.common.TaskManager;
import com.cliptransfer.ui.MainTrayIcon;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * ClipTransfer main application entry point
 * Responsible for initializing system components and starting the tray application
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    
    public static void main(String[] args) {
        try {
            // Check system tray support
            if (!SystemTray.isSupported()) {
                showErrorDialog("System tray is not supported, the application cannot run!");
                System.exit(1);
            }
            
            // Set system look and feel
            setupLookAndFeel();
            
            // Initialize configuration
            Config config = new Config();
            
            // Initialize logging system
            LoggerUtil.initLogger(config.getLogLevel());
            logger.info("ClipTransfer is starting...");
            
            // Clean up old logs (keep 30 days)
            LoggerUtil.cleanupOldLogs(30);
            
            // Initialize task manager
            TaskManager taskManager = new TaskManager();
            
            // Create and show tray icon
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        MainTrayIcon trayIcon = new MainTrayIcon(config, taskManager);
                        trayIcon.show();
                        logger.info("ClipTransfer startup completed");
                    } catch (Exception e) {
                        logger.severe("Failed to start tray application: " + e.getMessage());
                        showErrorDialog("Failed to start application: " + e.getMessage());
                        System.exit(1);
                    }
                }
            });
            
        } catch (Exception e) {
            System.err.println("Failed to start application: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Failed to start application: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Setup system look and feel
     */
    private static void setupLookAndFeel() {
        try {
            // Use system look and feel class name
            String systemLookAndFeelClassName = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(systemLookAndFeelClassName);
            
            // Set UI font (support for international characters)
            setUIFont();
            
            System.out.println("Successfully set system look and feel: " + systemLookAndFeelClassName);
            
        } catch (ClassNotFoundException e) {
            System.err.println("System look and feel class not found: " + e.getMessage());
            fallbackToDefaultLookAndFeel();
        } catch (InstantiationException e) {
            System.err.println("Cannot instantiate system look and feel: " + e.getMessage());
            fallbackToDefaultLookAndFeel();
        } catch (IllegalAccessException e) {
            System.err.println("Cannot access system look and feel: " + e.getMessage());
            fallbackToDefaultLookAndFeel();
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Unsupported system look and feel: " + e.getMessage());
            fallbackToDefaultLookAndFeel();
        } catch (Exception e) {
            System.err.println("Failed to set look and feel: " + e.getMessage());
            fallbackToDefaultLookAndFeel();
        }
    }
    
    /**
     * Fallback to default look and feel
     */
    private static void fallbackToDefaultLookAndFeel() {
        try {
            // Use cross-platform look and feel as fallback
            String crossPlatformLookAndFeelClassName = UIManager.getCrossPlatformLookAndFeelClassName();
            UIManager.setLookAndFeel(crossPlatformLookAndFeelClassName);
            System.out.println("Fallback to default look and feel: " + crossPlatformLookAndFeelClassName);
        } catch (Exception e) {
            System.err.println("Failed to set default look and feel: " + e.getMessage());
            // If even the default look and feel fails, keep as is
        }
    }
    
    /**
     * Set UI font
     */
    private static void setUIFont() {
        try {
            // Detect system and set appropriate font
            String osName = System.getProperty("os.name").toLowerCase();
            Font defaultFont;
            
            if (osName.contains("windows")) {
                // Windows system - try Microsoft YaHei
                if (isFontAvailable("Microsoft YaHei")) {
                    defaultFont = new Font("Microsoft YaHei", Font.PLAIN, 12);
                } else if (isFontAvailable("SimHei")) {
                    defaultFont = new Font("SimHei", Font.PLAIN, 12);
                } else {
                    defaultFont = new Font("Dialog", Font.PLAIN, 12);
                }
            } else if (osName.contains("mac")) {
                // macOS system - try PingFang SC
                if (isFontAvailable("PingFang SC")) {
                    defaultFont = new Font("PingFang SC", Font.PLAIN, 12);
                } else if (isFontAvailable("Hiragino Sans GB")) {
                    defaultFont = new Font("Hiragino Sans GB", Font.PLAIN, 12);
                } else {
                    defaultFont = new Font("Dialog", Font.PLAIN, 12);
                }
            } else {
                // Linux and other systems - try Noto Sans CJK SC
                if (isFontAvailable("Noto Sans CJK SC")) {
                    defaultFont = new Font("Noto Sans CJK SC", Font.PLAIN, 12);
                } else if (isFontAvailable("WenQuanYi Micro Hei")) {
                    defaultFont = new Font("WenQuanYi Micro Hei", Font.PLAIN, 12);
                } else {
                    defaultFont = new Font("Dialog", Font.PLAIN, 12);
                }
            }
            
            // Set default font for all UI components
            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof javax.swing.plaf.FontUIResource) {
                    UIManager.put(key, new javax.swing.plaf.FontUIResource(defaultFont));
                }
            }
            
            System.out.println("Successfully set UI font: " + defaultFont.getName());
            
        } catch (Exception e) {
            System.err.println("Failed to set font: " + e.getMessage());
        }
    }
    
    /**
     * Check if font is available
     */
    private static boolean isFontAvailable(String fontName) {
        try {
            String[] availableFontNames = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
            for (String availableFontName : availableFontNames) {
                if (availableFontName.equalsIgnoreCase(fontName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Show error dialog
     */
    private static void showErrorDialog(String message) {
        try {
            JOptionPane.showMessageDialog(
                null,
                message,
                "ClipTransfer - Error",
                JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception e) {
            System.err.println("Failed to show error dialog: " + e.getMessage());
            System.err.println("Original error: " + message);
        }
    }
}