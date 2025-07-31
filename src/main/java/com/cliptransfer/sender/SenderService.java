package com.cliptransfer.sender;

import java.io.File;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.cliptransfer.common.ClipboardUtil;
import com.cliptransfer.common.Config;
import com.cliptransfer.common.FileUtil;
import com.cliptransfer.common.MD5Util;
import com.cliptransfer.common.TaskManager;
import com.cliptransfer.common.TransferTask;
import com.cliptransfer.common.TransferType;
import com.cliptransfer.protocol.ChunkPacket;
import com.cliptransfer.protocol.EndPacket;
import com.cliptransfer.protocol.FileManifestEntry;
import com.cliptransfer.protocol.StartPacket;

/**
 * Sender core service class
 * Responsible for file/folder encoding, chunking, sending and progress management
 * Supports pause, resume and cancel functionality
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class SenderService {
    private static final Logger logger = Logger.getLogger(SenderService.class.getName());
    
    private final Config config;
    private final TaskManager taskManager;
    private final ExecutorService executorService;
    private final AtomicBoolean isRunning;
    
    // Current transfer task
    private TransferTask currentTask;
    private final AtomicBoolean isPaused;
    private final AtomicBoolean isStopped;
    
    // Event listener
    private SenderEventListener eventListener;
    
    /**
     * Constructor
     */
    public SenderService(Config config, TaskManager taskManager) {
        this.config = config;
        this.taskManager = taskManager;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "SenderService-Thread");
            thread.setDaemon(true);
            return thread;
        });
        this.isRunning = new AtomicBoolean(false);
        this.isPaused = new AtomicBoolean(false);
        this.isStopped = new AtomicBoolean(false);
        
        logger.info("Sender service initialization completed");
    }
    
    /**
     * Set event listener
     */
    public void setEventListener(SenderEventListener listener) {
        this.eventListener = listener;
    }
    
    /**
     * Send file
     * @param filePath file path
     * @return task ID
     */
    public String sendFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }
        
        if (file.isDirectory()) {
            return sendFolder(filePath);
        } else {
            return sendSingleFile(file);
        }
    }
    
    /**
     * Send single file
     */
    private String sendSingleFile(File file) {
        logger.info("Preparing to send file: " + file.getName() + ", size: " + FileUtil.formatFileSize(file.length()));
        
        try {
            // Generate task ID
            String taskId = UUID.randomUUID().toString();
            
            // Calculate file MD5
            String fileMd5 = MD5Util.calculateFileMD5(file);
            
            // Create task
            TransferTask task = new TransferTask(
                taskId, 
                file.getName(), 
                file.getAbsolutePath(),
                TransferType.FILE, 
                file.length(), 
                config.getChunkSize()
            );
            task.setFileMd5(fileMd5);
            
            // Add to task manager
            taskManager.addTask(task);
            
            // Send asynchronously
            CompletableFuture.runAsync(() -> performSendTask(task, file), executorService);
            
            return taskId;
            
        } catch (Exception e) {
            logger.severe("Failed to send file: " + e.getMessage());
            fireOnError("Failed to send file: " + e.getMessage());
            throw new RuntimeException("File sending failed", e);
        }
    }
    
    /**
     * Send folder
     */
    private String sendFolder(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("Folder does not exist: " + folderPath);
        }
        
        logger.info("Preparing to send folder: " + folder.getName());
        
        try {
            // Generate task ID
            String taskId = UUID.randomUUID().toString();
            
            // Create temporary ZIP file
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "cliptransfer");
            tempDir.mkdirs();
            File zipFile = new File(tempDir, folder.getName() + "_" + System.currentTimeMillis() + ".zip");
            
            // Compress folder
            List<FileManifestEntry> manifest = FileUtil.compressFolderToZip(folder, zipFile);
            
            // Calculate ZIP file MD5
            String zipMd5 = MD5Util.calculateFileMD5(zipFile);
            
            // Create task
            TransferTask task = new TransferTask(
                taskId,
                folder.getName() + ".zip",
                folder.getAbsolutePath(),
                TransferType.FOLDER,
                zipFile.length(),
                config.getChunkSize()
            );
            task.setFileMd5(zipMd5);
            task.setFolderManifest(manifest);
            
            // Add to task manager
            taskManager.addTask(task);
            
            // Send asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    performSendTask(task, zipFile);
                } finally {
                    // Clean up temporary file
                    if (zipFile.exists()) {
                        zipFile.delete();
                        logger.info("Cleaned up temporary ZIP file: " + zipFile.getName());
                    }
                }
            }, executorService);
            
            return taskId;
            
        } catch (Exception e) {
            logger.severe("Failed to send folder: " + e.getMessage());
            fireOnError("Failed to send folder: " + e.getMessage());
            throw new RuntimeException("Folder sending failed", e);
        }
    }
    
    /**
     * Perform send task
     */
    private void performSendTask(TransferTask task, File file) {
        currentTask = task;
        isStopped.set(false);
        isPaused.set(false);
        
        try {
            logger.info("Start sending task: " + task.getFileName() + " (ID: " + task.getTaskId() + ")");
            
            // Mark task as started
            task.start();
            taskManager.updateTask(task);
            isRunning.set(true);
            fireOnTaskStarted(task);
            
            // Send START packet
            sendStartPacket(task, file);
            
            // Send data chunks
            sendDataChunks(task, file);
            
            // Check if stopped or cancelled
            if (isStopped.get()) {
                task.cancel();
                taskManager.updateTask(task);
                fireOnTaskCancelled(task);
                return;
            }
            
            // Send END packet
            sendEndPacket(task);
            
            // Mark task as completed
            task.complete();
            taskManager.updateTask(task);
            fireOnTaskCompleted(task);
            
            logger.info("Task sending completed: " + task.getFileName());
            
        } catch (Exception e) {
            logger.severe("Task sending failed: " + task.getFileName() + ", " + e.getMessage());
            task.fail("Sending failed: " + e.getMessage());
            taskManager.updateTask(task);
            fireOnTaskFailed(task, e.getMessage());
        } finally {
            isRunning.set(false);
            currentTask = null;
        }
    }
    
    /**
     * Send START packet
     */
    private void sendStartPacket(TransferTask task, File file) throws InterruptedException {
        logger.info("Sending START packet: " + task.getFileName());
        
        StartPacket startPacket = new StartPacket(
            task.getTaskId(),
            task.getFileName(),
            task.getTransferType(),
            task.getTotalSize(),
            task.getChunkSize(),
            task.getChunkTotal(),
            task.getFileMd5()
        );
        
        // Add folder manifest (if folder transfer)
        if (task.getTransferType() == TransferType.FOLDER) {
            startPacket.setFolderManifest(task.getFolderManifest());
        }
        
        // Send to clipboard
        String packetJson = startPacket.toJsonString();
        boolean success = ClipboardUtil.setClipboardTextSafely(packetJson, 3, 500);
        
        if (!success) {
            throw new RuntimeException("Failed to send START packet");
        }
        
        logger.info("START packet sent successfully, waiting for receiver confirmation...");
        
        // Wait for send interval
        Thread.sleep(config.getSendInterval());
    }
    
    /**
     * Send data chunks
     */
    private void sendDataChunks(TransferTask task, File file) throws InterruptedException {
        logger.info("Start sending data chunks, total chunks: " + task.getChunkTotal());
        
        for (int chunkIndex = 0; chunkIndex < task.getChunkTotal(); chunkIndex++) {
            // Check if paused
            while (isPaused.get() && !isStopped.get()) {
                Thread.sleep(100);
            }
            
            // Check if stopped
            if (isStopped.get()) {
                logger.info("Task was stopped, interrupting data chunk sending");
                return;
            }
            
            try {
                // Read file chunk
                long offset = (long) chunkIndex * task.getChunkSize();
                byte[] chunkData = FileUtil.readFileChunk(file, offset, task.getChunkSize());
                
                // Calculate chunk MD5
                String chunkMd5 = MD5Util.calculateMD5(chunkData);
                
                // Base64 encode
                String encodedData = Base64.getEncoder().encodeToString(chunkData);
                
                // Create CHUNK packet
                ChunkPacket chunkPacket = new ChunkPacket(
                    task.getTaskId(),
                    chunkIndex,
                    task.getChunkTotal(),
                    chunkMd5,
                    encodedData
                );
                
                // Send to clipboard
                String packetJson = chunkPacket.toJsonString();
                boolean success = ClipboardUtil.setClipboardTextSafely(packetJson, 3, 500);
                
                if (success) {
                    // Mark chunk as completed
                    task.markChunkCompleted(chunkIndex, chunkData.length);
                    taskManager.updateTask(task);
                    fireOnProgress(task, chunkIndex + 1, task.getChunkTotal());
                    
                    logger.fine("Sent data chunk: " + (chunkIndex + 1) + "/" + task.getChunkTotal() + 
                               ", size: " + chunkData.length + " bytes");
                } else {
                    logger.warning("Failed to send data chunk: " + chunkIndex);
                    task.markChunkFailed(chunkIndex, "Clipboard sending failed");
                    taskManager.updateTask(task);
                }
                
                // Wait for send interval
                Thread.sleep(config.getSendInterval());
                
            } catch (Exception e) {
                logger.warning("Failed to process data chunk: " + chunkIndex + ", " + e.getMessage());
                task.markChunkFailed(chunkIndex, e.getMessage());
                taskManager.updateTask(task);
            }
        }
        
        logger.info("Data chunk sending completed");
    }
    
    /**
     * Send END packet
     */
    private void sendEndPacket(TransferTask task) throws InterruptedException {
        logger.info("Sending END packet: " + task.getFileName());
        
        EndPacket endPacket = new EndPacket(
            task.getTaskId(),
            task.getFileName(),
            task.getChunkTotal()
        );
        
        // Send to clipboard
        String packetJson = endPacket.toJsonString();
        boolean success = ClipboardUtil.setClipboardTextSafely(packetJson, 3, 500);
        
        if (!success) {
            throw new RuntimeException("Failed to send END packet");
        }
        
        logger.info("END packet sent successfully");
        
        // Wait for send interval
        Thread.sleep(config.getSendInterval());
    }
    
    /**
     * Pause current task
     */
    public void pauseCurrentTask() {
        if (currentTask != null && isRunning.get()) {
            isPaused.set(true);
            currentTask.pause();
            taskManager.updateTask(currentTask);
            fireOnTaskPaused(currentTask);
            logger.info("Paused task: " + currentTask.getFileName());
        }
    }
    
    /**
     * Resume current task
     */
    public void resumeCurrentTask() {
        if (currentTask != null && isPaused.get()) {
            isPaused.set(false);
            currentTask.start();
            taskManager.updateTask(currentTask);
            fireOnTaskResumed(currentTask);
            logger.info("Resumed task: " + currentTask.getFileName());
        }
    }
    
    /**
     * Stop current task
     */
    public void stopCurrentTask() {
        if (currentTask != null && isRunning.get()) {
            isStopped.set(true);
            logger.info("Stopped task: " + currentTask.getFileName());
        }
    }
    
    /**
     * Get current task
     */
    public TransferTask getCurrentTask() {
        return currentTask;
    }
    
    /**
     * Check if running
     */
    public boolean isRunning() {
        return isRunning.get();
    }
    
    /**
     * Check if paused
     */
    public boolean isPaused() {
        return isPaused.get();
    }
    
    /**
     * Shutdown service
     */
    public void shutdown() {
        logger.info("Shutdown sender service");
        isStopped.set(true);
        executorService.shutdown();
    }
    
    // Event firing methods
    
    private void fireOnTaskStarted(TransferTask task) {
        if (eventListener != null) {
            try {
                eventListener.onTaskStarted(task);
            } catch (Exception e) {
                logger.warning("Event handling exception: onTaskStarted, " + e.getMessage());
            }
        }
    }
    
    private void fireOnProgress(TransferTask task, int completedChunks, int totalChunks) {
        if (eventListener != null) {
            try {
                eventListener.onProgress(task, completedChunks, totalChunks);
            } catch (Exception e) {
                logger.warning("Event handling exception: onProgress, " + e.getMessage());
            }
        }
    }
    
    private void fireOnTaskCompleted(TransferTask task) {
        if (eventListener != null) {
            try {
                eventListener.onTaskCompleted(task);
            } catch (Exception e) {
                logger.warning("Event handling exception: onTaskCompleted, " + e.getMessage());
            }
        }
    }
    
    private void fireOnTaskFailed(TransferTask task, String error) {
        if (eventListener != null) {
            try {
                eventListener.onTaskFailed(task, error);
            } catch (Exception e) {
                logger.warning("Event handling exception: onTaskFailed, " + e.getMessage());
            }
        }
    }
    
    private void fireOnTaskPaused(TransferTask task) {
        if (eventListener != null) {
            try {
                eventListener.onTaskPaused(task);
            } catch (Exception e) {
                logger.warning("Event handling exception: onTaskPaused, " + e.getMessage());
            }
        }
    }
    
    private void fireOnTaskResumed(TransferTask task) {
        if (eventListener != null) {
            try {
                eventListener.onTaskResumed(task);
            } catch (Exception e) {
                logger.warning("Event handling exception: onTaskResumed, " + e.getMessage());
            }
        }
    }
    
    private void fireOnTaskCancelled(TransferTask task) {
        if (eventListener != null) {
            try {
                eventListener.onTaskCancelled(task);
            } catch (Exception e) {
                logger.warning("Event handling exception: onTaskCancelled, " + e.getMessage());
            }
        }
    }
    
    private void fireOnError(String error) {
        if (eventListener != null) {
            try {
                eventListener.onError(error);
            } catch (Exception e) {
                logger.warning("Event handling exception: onError, " + e.getMessage());
            }
        }
    }
}