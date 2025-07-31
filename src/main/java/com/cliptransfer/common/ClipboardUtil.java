package com.cliptransfer.common;

import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Clipboard utility class
 * Provides clipboard read/write functionality, supports text content setting and getting
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class ClipboardUtil {
    private static final Logger logger = Logger.getLogger(ClipboardUtil.class.getName());
    private static final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    
    /**
     * Set clipboard text content
     * @param text text to set
     * @return whether setting was successful
     */
    public static boolean setClipboardText(String text) {
        if (text == null) {
            logger.warning("Attempting to set null text to clipboard");
            return false;
        }
        
        try {
            StringSelection selection = new StringSelection(text);
            clipboard.setContents(selection, null);
            
            logger.fine("Successfully set clipboard content, length: " + text.length());
            return true;
            
        } catch (IllegalStateException e) {
            logger.warning("Clipboard is being used by another application: " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.severe("Failed to set clipboard content: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get clipboard text content
     * @return text in clipboard, returns null if getting fails
     */
    public static String getClipboardText() {
        try {
            Transferable transferable = clipboard.getContents(null);
            
            if (transferable == null) {
                logger.fine("Clipboard content is empty");
                return null;
            }
            
            if (!transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                logger.fine("Clipboard does not contain text content");
                return null;
            }
            
            String text = (String) transferable.getTransferData(DataFlavor.stringFlavor);
            
            if (text != null) {
                logger.fine("Successfully got clipboard content, length: " + text.length());
            }
            
            return text;
            
        } catch (UnsupportedFlavorException e) {
            logger.fine("Clipboard data format not supported: " + e.getMessage());
            return null;
        } catch (IOException e) {
            logger.warning("Failed to read clipboard content: " + e.getMessage());
            return null;
        } catch (IllegalStateException e) {
            logger.warning("Clipboard is being used by another application: " + e.getMessage());
            return null;
        } catch (Exception e) {
            logger.severe("Unknown error occurred while getting clipboard content: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Clear clipboard content
     * @return whether clearing was successful
     */
    public static boolean clearClipboard() {
        return setClipboardText("");
    }
    
    /**
     * Check if clipboard contains text
     * @return whether it contains text
     */
    public static boolean hasText() {
        try {
            Transferable transferable = clipboard.getContents(null);
            return transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor);
        } catch (Exception e) {
            logger.fine("Error occurred while checking clipboard text: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get clipboard content size (character count)
     * @return content size, returns -1 if getting fails
     */
    public static int getClipboardTextSize() {
        String text = getClipboardText();
        return text != null ? text.length() : -1;
    }
    
    /**
     * Safely set clipboard content (with retry mechanism)
     * @param text text to set
     * @param maxRetries maximum retry count
     * @param retryDelayMs retry interval (milliseconds)
     * @return whether setting was successful
     */
    public static boolean setClipboardTextSafely(String text, int maxRetries, int retryDelayMs) {
        for (int i = 0; i <= maxRetries; i++) {
            if (setClipboardText(text)) {
                return true;
            }
            
            if (i < maxRetries) {
                logger.info("Clipboard setting failed, retrying after " + retryDelayMs + "ms (" + (i + 1) + "/" + maxRetries + ")");
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warning("Retry wait was interrupted: " + e.getMessage());
                    return false;
                }
            }
        }
        
        logger.severe("Clipboard setting failed after " + maxRetries + " retries");
        return false;
    }
    
    /**
     * Safely get clipboard content (with retry mechanism)
     * @param maxRetries maximum retry count
     * @param retryDelayMs retry interval (milliseconds)
     * @return clipboard content, returns null if getting fails
     */
    public static String getClipboardTextSafely(int maxRetries, int retryDelayMs) {
        for (int i = 0; i <= maxRetries; i++) {
            String text = getClipboardText();
            if (text != null) {
                return text;
            }
            
            if (i < maxRetries) {
                logger.fine("Clipboard getting failed, retrying after " + retryDelayMs + "ms (" + (i + 1) + "/" + maxRetries + ")");
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warning("Retry wait was interrupted: " + e.getMessage());
                    return null;
                }
            }
        }
        
        logger.warning("Clipboard getting failed after " + maxRetries + " retries");
        return null;
    }
}