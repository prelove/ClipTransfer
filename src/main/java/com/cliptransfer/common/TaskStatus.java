package com.cliptransfer.common;

/**
 * Transfer task status enumeration
 * Defines various states of tasks during the transfer process
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public enum TaskStatus {
    /**
     * Pending - task created but not started
     */
    PENDING("PENDING", "Pending"),
    
    /**
     * Running - task is transferring
     */
    RUNNING("RUNNING", "Running"),
    
    /**
     * Paused - task was paused by user
     */
    PAUSED("PAUSED", "Paused"),
    
    /**
     * Completed - task completed successfully
     */
    COMPLETED("COMPLETED", "Completed"),
    
    /**
     * Failed - task failed
     */
    FAILED("FAILED", "Failed"),
    
    /**
     * Cancelled - task was cancelled by user
     */
    CANCELLED("CANCELLED", "Cancelled");
    
    private final String value;
    private final String description;
    
    TaskStatus(String value, String description) {
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
     * Get task status from string value
     */
    public static TaskStatus fromValue(String value) {
        for (TaskStatus status : TaskStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown task status: " + value);
    }
    
    /**
     * Check if this is a terminal status
     */
    public boolean isTerminalStatus() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
    
    /**
     * Check if task can be restarted
     */
    public boolean canRestart() {
        return this == FAILED || this == CANCELLED || this == PAUSED;
    }
}