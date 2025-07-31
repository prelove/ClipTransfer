package com.cliptransfer.common;

/**
 * Transfer type enumeration
 * Defines types of content being transferred
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public enum TransferType {
    /**
     * Single file transfer
     */
    FILE("FILE", "File"),
    
    /**
     * Folder transfer (compressed as ZIP)
     */
    FOLDER("FOLDER", "Folder");
    
    private final String value;
    private final String description;
    
    TransferType(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get transfer type from string value
     */
    public static TransferType fromValue(String value) {
        for (TransferType type : TransferType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transfer type: " + value);
    }
}