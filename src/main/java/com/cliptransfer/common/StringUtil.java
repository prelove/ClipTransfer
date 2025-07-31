package com.cliptransfer.common;

/**
 * String utility class
 * Provides Java 8 compatible string operation methods
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class StringUtil {
    
    /**
     * Repeat string specified number of times (Java 8 compatible String.repeat implementation)
     * @param str string to repeat
     * @param count repeat count
     * @return repeated string
     */
    public static String repeat(String str, int count) {
        if (str == null) {
            return null;
        }
        if (count <= 0) {
            return "";
        }
        if (count == 1) {
            return str;
        }
        
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    /**
     * Check if string is empty or null
     * @param str string to check
     * @return whether it's empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
    
    /**
     * Check if string is blank or null
     * @param str string to check
     * @return whether it's blank
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().length() == 0;
    }
}