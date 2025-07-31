package com.cliptransfer.common;

import com.cliptransfer.protocol.FileManifestEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Transfer task class
 * Represents a file or folder transfer task, including progress tracking and status management
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class TransferTask {
    private static final Logger logger = Logger.getLogger(TransferTask.class.getName());
    
    // Basic task information
    private final String taskId;
    private final String fileName;
    private final String filePath;
    private final TransferType transferType;
    private final long totalSize;
    private final int chunkSize;
    
    // Calculated values
    private int chunkTotal;
    
    // Task status
    private TaskStatus status;
    private String errorMessage;
    
    // Time tracking
    private long createTime;
    private long startTime;
    private long endTime;
    
    // Progress tracking
    private final Set<Integer> completedChunks;
    private final Map<Integer, String> failedChunks;
    private long transferredBytes;
    
    // File verification
    private String fileMd5;
    
    // Folder information
    private List<FileManifestEntry> folderManifest;
    
    // Transfer speed calculation
    private long lastProgressUpdateTime;
    private long lastTransferredBytes;
    
    /**
     * Constructor
     */
    public TransferTask(String taskId, String fileName, String filePath, 
                       TransferType transferType, long totalSize, int chunkSize) {
        this.taskId = taskId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.transferType = transferType;
        this.totalSize = totalSize;
        this.chunkSize = chunkSize;
        
        // Calculate chunk count
        this.chunkTotal = (int) Math.ceil((double) totalSize / chunkSize);
        
        // Initialize status
        this.status = TaskStatus.PENDING;
        this.createTime = System.currentTimeMillis();
        
        // Initialize progress tracking
        this.completedChunks = ConcurrentHashMap.newKeySet();
        this.failedChunks = new ConcurrentHashMap<>();
        this.transferredBytes = 0;
        
        // Initialize folder manifest
        this.folderManifest = new ArrayList<>();
        
        logger.info("Created transfer task: " + fileName + " (ID: " + taskId + 
                   ", Size: " + FileUtil.formatFileSize(totalSize) + 
                   ", Chunks: " + chunkTotal + ")");
    }
    
    /**
     * Start task
     */
    public void start() {
        this.status = TaskStatus.RUNNING;
        this.startTime = System.currentTimeMillis();
        this.lastProgressUpdateTime = this.startTime;
        this.lastTransferredBytes = 0;
        
        logger.info("Task started: " + fileName);
    }
    
    /**
     * Pause task
     */
    public void pause() {
        this.status = TaskStatus.PAUSED;
        logger.info("Task paused: " + fileName);
    }
    
    /**
     * Complete task
     */
    public void complete() {
        this.status = TaskStatus.COMPLETED;
        this.endTime = System.currentTimeMillis();
        
        long duration = endTime - startTime;
        double avgSpeed = duration > 0 ? (totalSize * 1000.0 / duration) : 0;
        
        logger.info("Task completed: " + fileName + 
                   " (Duration: " + (duration / 1000.0) + "s" +
                   ", Avg Speed: " + FileUtil.formatFileSize((long) avgSpeed) + "/s)");
    }
    
    /**
     * Fail task
     */
    public void fail(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.endTime = System.currentTimeMillis();
        
        logger.warning("Task failed: " + fileName + " - " + errorMessage);
    }
    
    /**
     * Cancel task
     */
    public void cancel() {
        this.status = TaskStatus.CANCELLED;
        this.endTime = System.currentTimeMillis();
        
        logger.info("Task cancelled: " + fileName);
    }
    
    /**
     * Mark chunk as completed
     */
    public void markChunkCompleted(int chunkIndex, int chunkSize) {
        if (chunkIndex < 0 || chunkIndex >= chunkTotal) {
            logger.warning("Invalid chunk index: " + chunkIndex);
            return;
        }
        
        completedChunks.add(chunkIndex);
        failedChunks.remove(chunkIndex);
        transferredBytes += chunkSize;
        
        // Update progress time
        lastProgressUpdateTime = System.currentTimeMillis();
        
        logger.fine("Chunk completed: " + chunkIndex + "/" + chunkTotal + 
                   " (Size: " + chunkSize + ", Progress: " + 
                   String.format("%.1f%%", getProgressPercentage()) + ")");
    }
    
    /**
     * Mark chunk as failed
     */
    public void markChunkFailed(int chunkIndex, String reason) {
        if (chunkIndex < 0 || chunkIndex >= chunkTotal) {
            logger.warning("Invalid chunk index: " + chunkIndex);
            return;
        }
        
        completedChunks.remove(chunkIndex);
        failedChunks.put(chunkIndex, reason);
        
        logger.warning("Chunk failed: " + chunkIndex + "/" + chunkTotal + " - " + reason);
    }
    
    /**
     * Get progress percentage
     */
    public double getProgressPercentage() {
        if (chunkTotal == 0) {
            return 0.0;
        }
        return (completedChunks.size() * 100.0) / chunkTotal;
    }
    
    /**
     * Check if task is completed
     */
    public boolean isCompleted() {
        return completedChunks.size() == chunkTotal && failedChunks.isEmpty();
    }
    
    /**
     * Get missing chunks
     */
    public List<Integer> getMissingChunks() {
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < chunkTotal; i++) {
            if (!completedChunks.contains(i)) {
                missing.add(i);
            }
        }
        return missing;
    }
    
    /**
     * Get transfer speed (bytes per second)
     */
    public double getTransferSpeed() {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastProgressUpdateTime;
        
        if (timeDiff <= 0) {
            return 0.0;
        }
        
        long bytesDiff = transferredBytes - lastTransferredBytes;
        lastTransferredBytes = transferredBytes;
        
        return (bytesDiff * 1000.0) / timeDiff;
    }
    
    /**
     * Get estimated remaining time (milliseconds)
     */
    public long getEstimatedRemainingTime() {
        double speed = getTransferSpeed();
        if (speed <= 0) {
            return -1;
        }
        
        long remainingBytes = totalSize - transferredBytes;
        return (long) (remainingBytes * 1000.0 / speed);
    }
    
    /**
     * Get task duration (milliseconds)
     */
    public long getDuration() {
        if (startTime == 0) {
            return 0;
        }
        
        long endTimeToUse = endTime > 0 ? endTime : System.currentTimeMillis();
        return endTimeToUse - startTime;
    }
    
    // Getter methods
    
    public String getTaskId() {
        return taskId;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public TransferType getTransferType() {
        return transferType;
    }
    
    public long getTotalSize() {
        return totalSize;
    }
    
    public int getChunkSize() {
        return chunkSize;
    }
    
    public int getChunkTotal() {
        return chunkTotal;
    }
    
    public void setChunkTotal(int chunkTotal) {
        this.chunkTotal = chunkTotal;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public Set<Integer> getCompletedChunks() {
        return new HashSet<>(completedChunks);
    }
    
    public Map<Integer, String> getFailedChunks() {
        return new HashMap<>(failedChunks);
    }
    
    public long getTransferredBytes() {
        return transferredBytes;
    }
    
    public String getFileMd5() {
        return fileMd5;
    }
    
    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }
    
    public List<FileManifestEntry> getFolderManifest() {
        return new ArrayList<>(folderManifest);
    }
    
    public void setFolderManifest(List<FileManifestEntry> folderManifest) {
        this.folderManifest = folderManifest != null ? 
            new ArrayList<>(folderManifest) : new ArrayList<>();
    }
    
    @Override
    public String toString() {
        return String.format("TransferTask{id='%s', fileName='%s', type=%s, size=%s, status=%s, progress=%.1f%%}",
                taskId, fileName, transferType, FileUtil.formatFileSize(totalSize), 
                status, getProgressPercentage());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TransferTask that = (TransferTask) obj;
        return Objects.equals(taskId, that.taskId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }
}