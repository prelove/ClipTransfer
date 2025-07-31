package com.cliptransfer.receiver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import com.cliptransfer.protocol.Packet;
import com.cliptransfer.protocol.StartPacket;

/**
 * Receiver core service class
 * Responsible for clipboard polling, data receiving, merging and file reconstruction
 * Supports resume transfer and deduplication processing
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class ReceiverService {
    private static final Logger logger = Logger.getLogger(ReceiverService.class.getName());
    
    private final Config config;
    private final TaskManager taskManager;
    private final ScheduledExecutorService scheduledExecutor;
    private final AtomicBoolean isRunning;
    
    // Receiving state management
    private final Map<String, TransferTask> receivingTasks; // Currently receiving tasks
    private final Map<String, Map<Integer, byte[]>> chunkBuffers; // Chunk buffers
    private String lastClipboardContent; // Last clipboard content (for deduplication)
    
    // Event listener
    private ReceiverEventListener eventListener;
    
    /**
     * Constructor
     */
    public ReceiverService(Config config, TaskManager taskManager) {
        this.config = config;
        this.taskManager = taskManager;
        this.scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "ReceiverService-Thread");
            thread.setDaemon(true);
            return thread;
        });
        this.isRunning = new AtomicBoolean(false);
        this.receivingTasks = new ConcurrentHashMap<>();
        this.chunkBuffers = new ConcurrentHashMap<>();
        this.lastClipboardContent = "";
        
        logger.info("Receiver service initialization completed");
    }
    
    /**
     * Set event listener
     */
    public void setEventListener(ReceiverEventListener listener) {
        this.eventListener = listener;
    }
    
    /**
     * Start listening to clipboard
     */
    public void startListening() {
        if (isRunning.get()) {
            logger.warning("Receiver service is already running");
            return;
        }
        
        isRunning.set(true);
        logger.info("Start listening to clipboard, interval: " + config.getReceiveInterval() + "ms");
        
        // Start clipboard polling task
        scheduledExecutor.scheduleWithFixedDelay(
            this::pollClipboard,
            0,
            config.getReceiveInterval(),
            TimeUnit.MILLISECONDS
        );
        
        fireOnListeningStarted();
    }
    
    /**
     * Stop listening to clipboard
     */
    public void stopListening() {
        if (!isRunning.get()) {
            logger.warning("Receiver service is not running");
            return;
        }
        
        isRunning.set(false);
        logger.info("Stop listening to clipboard");
        
        fireOnListeningStopped();
    }
    
    /**
     * Poll clipboard
     */
    private void pollClipboard() {
        if (!isRunning.get()) {
            return;
        }
        
        try {
            String clipboardContent = ClipboardUtil.getClipboardText();
            
            // Check if content has changed (deduplication)
            if (clipboardContent == null || clipboardContent.equals(lastClipboardContent)) {
                return;
            }
            
            lastClipboardContent = clipboardContent;
            
            // Try to parse as protocol packet
            Packet packet = parsePacket(clipboardContent);
            if (packet != null) {
                handlePacket(packet);
            }
            
        } catch (Exception e) {
            logger.fine("Error occurred while polling clipboard: " + e.getMessage());
        }
    }
    
    /**
     * Parse clipboard content as protocol packet
     */
    private Packet parsePacket(String content) {
        try {
            // Check if it's JSON format
            if (!content.trim().startsWith("{") || !content.trim().endsWith("}")) {
                return null;
            }
            
            Packet packet = Packet.fromJsonString(content);
            
            // Validate packet
            if (!packet.isValid()) {
                logger.fine("Invalid protocol packet");
                return null;
            }
            
            logger.fine("Parsed protocol packet: " + packet.getType() + ", fileId: " + packet.getFileId());
            return packet;
            
        } catch (Exception e) {
            logger.fine("Failed to parse protocol packet: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Handle protocol packet
     */
    private void handlePacket(Packet packet) {
        try {
            switch (packet.getType()) {
                case START:
                    handleStartPacket((StartPacket) packet);
                    break;
                case CHUNK:
                    handleChunkPacket((ChunkPacket) packet);
                    break;
                case END:
                    handleEndPacket((EndPacket) packet);
                    break;
                default:
                    logger.warning("Unknown packet type: " + packet.getType());
            }
        } catch (Exception e) {
            logger.severe("Failed to handle protocol packet: " + e.getMessage());
            fireOnError("Failed to handle protocol packet: " + e.getMessage());
        }
    }
    
    /**
     * Handle START packet
     */
    private void handleStartPacket(StartPacket startPacket) {
        String fileId = startPacket.getFileId();
        logger.info("Received START packet: " + startPacket.getFileName() + " (ID: " + fileId + ")");
        
        // Check if task with same ID already exists
        if (receivingTasks.containsKey(fileId)) {
            logger.warning("Task already exists, ignoring duplicate START packet: " + fileId);
            return;
        }
        
        // Create receiving task
        TransferTask task = new TransferTask(
            fileId,
            startPacket.getFileName(),
            null, // Receiver has no source path
            startPacket.getTransferType(),
            startPacket.getTotalSize(),
            startPacket.getChunkSize()
        );
        task.setChunkTotal(startPacket.getChunkTotal());
        task.setFileMd5(startPacket.getFileMd5());
        task.setFolderManifest(startPacket.getFolderManifest());
        task.start();
        
        // Add to manager
        receivingTasks.put(fileId, task);
        chunkBuffers.put(fileId, new ConcurrentHashMap<>());
        taskManager.addTask(task);
        
        fireOnTaskStarted(task);
        logger.info("Start receiving task: " + task.getFileName() + ", total chunks: " + task.getChunkTotal());
    }
    
    /**
     * Handle CHUNK packet
     */
    private void handleChunkPacket(ChunkPacket chunkPacket) {
        String fileId = chunkPacket.getFileId();
        int chunkIndex = chunkPacket.getChunkIndex();
        
        // Check if task exists
        TransferTask task = receivingTasks.get(fileId);
        if (task == null) {
            logger.warning("Received CHUNK packet for unknown task: " + fileId);
            return;
        }
        
        logger.fine("Received CHUNK packet: " + chunkIndex + "/" + chunkPacket.getChunkTotal() + 
                   " (fileId: " + fileId + ")");
        
        try {
            // Check if this chunk was already received (deduplication)
            Map<Integer, byte[]> taskChunks = chunkBuffers.get(fileId);
            if (taskChunks.containsKey(chunkIndex)) {
                logger.fine("Duplicate chunk, ignoring: " + chunkIndex);
                return;
            }
            
            // Base64 decode
            byte[] chunkData = Base64.getDecoder().decode(chunkPacket.getData());
            
            // Verify chunk MD5
            String actualMd5 = MD5Util.calculateMD5(chunkData);
            if (!actualMd5.equals(chunkPacket.getChunkMd5())) {
                logger.warning("Chunk MD5 verification failed: " + chunkIndex + 
                              ", expected: " + chunkPacket.getChunkMd5() + 
                              ", actual: " + actualMd5);
                task.markChunkFailed(chunkIndex, "MD5 verification failed");
                taskManager.updateTask(task);
                return;
            }
            
            // Save chunk data
            taskChunks.put(chunkIndex, chunkData);
            task.markChunkCompleted(chunkIndex, chunkData.length);
            taskManager.updateTask(task);
            
            fireOnProgress(task, task.getCompletedChunks().size(), task.getChunkTotal());
            
            logger.fine("Chunk received successfully: " + chunkIndex + "/" + task.getChunkTotal() + 
                       ", progress: " + String.format("%.2f%%", task.getProgressPercentage()));
            
        } catch (Exception e) {
            logger.warning("Failed to process chunk: " + chunkIndex + ", " + e.getMessage());
            task.markChunkFailed(chunkIndex, e.getMessage());
            taskManager.updateTask(task);
        }
    }
    
    /**
     * Handle END packet
     */
    private void handleEndPacket(EndPacket endPacket) {
        String fileId = endPacket.getFileId();
        logger.info("Received END packet: " + endPacket.getFileName() + " (ID: " + fileId + ")");
        
        // Check if task exists
        TransferTask task = receivingTasks.get(fileId);
        if (task == null) {
            logger.warning("Received END packet for unknown task: " + fileId);
            return;
        }
        
        // Check if all chunks have been received
        if (!task.isCompleted()) {
            logger.warning("Received END packet before completion, missing chunks: " + task.getMissingChunks().size());
            fireOnTaskIncomplete(task, task.getMissingChunks());
            return;
        }
        
        // Asynchronously reconstruct file
        scheduledExecutor.execute(() -> reconstructFile(task));
    }
    
    /**
     * Reconstruct file
     */
    private void reconstructFile(TransferTask task) {
        String fileId = task.getTaskId();
        logger.info("Start reconstructing file: " + task.getFileName());
        
        try {
            // Ensure download directory exists
            String downloadPath = config.getDownloadPath();
            FileUtil.ensureDirectory(downloadPath);
            
            // Create target file
            File targetFile = new File(downloadPath, task.getFileName());
            
            // Generate new name if file already exists
            if (targetFile.exists()) {
                targetFile = generateUniqueFileName(targetFile);
            }
            
            // Merge chunks
            Map<Integer, byte[]> chunks = chunkBuffers.get(fileId);
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                for (int i = 0; i < task.getChunkTotal(); i++) {
                    byte[] chunkData = chunks.get(i);
                    if (chunkData == null) {
                        throw new IOException("Missing chunk: " + i);
                    }
                    fos.write(chunkData);
                }
            }
            
            logger.info("File reconstruction completed: " + targetFile.getName() + 
                       ", size: " + FileUtil.formatFileSize(targetFile.length()));
            
            // Verify file MD5
            if (task.getFileMd5() != null) {
                boolean isValid = MD5Util.verifyFileMD5(targetFile, task.getFileMd5());
                if (!isValid) {
                    task.fail("File MD5 verification failed");
                    taskManager.updateTask(task);
                    fireOnTaskFailed(task, "File MD5 verification failed");
                    return;
                }
            }
            
            // Extract folder if it's a folder transfer
            if (task.getTransferType() == TransferType.FOLDER) {
                extractFolder(task, targetFile);
            }
            
            // Mark task as completed
            task.complete();
            taskManager.updateTask(task);
            
            // Clean up cache
            receivingTasks.remove(fileId);
            chunkBuffers.remove(fileId);
            
            fireOnTaskCompleted(task, targetFile);
            logger.info("Task receiving completed: " + task.getFileName());
            
        } catch (Exception e) {
            logger.severe("File reconstruction failed: " + task.getFileName() + ", " + e.getMessage());
            task.fail("File reconstruction failed: " + e.getMessage());
            taskManager.updateTask(task);
            fireOnTaskFailed(task, e.getMessage());
        }
    }
    
    /**
     * Extract folder
     */
    private void extractFolder(TransferTask task, File zipFile) {
        try {
            // Create extraction directory
            String folderName = task.getFileName();
            if (folderName.endsWith(".zip")) {
                folderName = folderName.substring(0, folderName.length() - 4);
            }
            
            File extractDir = new File(zipFile.getParent(), folderName);
            if (extractDir.exists()) {
                extractDir = generateUniqueDirectory(extractDir);
            }
            
            // Extract ZIP file
            FileUtil.extractZipToDirectory(zipFile, extractDir, task.getFolderManifest());
            
            // Delete temporary ZIP file
            zipFile.delete();
            
            logger.info("Folder extraction completed: " + extractDir.getName());
            
        } catch (Exception e) {
            logger.severe("Folder extraction failed: " + e.getMessage());
            throw new RuntimeException("Folder extraction failed", e);
        }
    }
    
    /**
     * Generate unique file name
     */
    private File generateUniqueFileName(File originalFile) {
        String name = originalFile.getName();
        String baseName;
        String extension = "";
        
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = name.substring(0, dotIndex);
            extension = name.substring(dotIndex);
        } else {
            baseName = name;
        }
        
        int counter = 1;
        File newFile;
        do {
            String newName = baseName + "_" + counter + extension;
            newFile = new File(originalFile.getParent(), newName);
            counter++;
        } while (newFile.exists());
        
        return newFile;
    }
    
    /**
     * Generate unique directory name
     */
    private File generateUniqueDirectory(File originalDir) {
        String baseName = originalDir.getName();
        int counter = 1;
        File newDir;
        do {
            String newName = baseName + "_" + counter;
            newDir = new File(originalDir.getParent(), newName);
            counter++;
        } while (newDir.exists());
        
        return newDir;
    }
    
    /**
     * Get currently receiving tasks list
     */
    public java.util.Collection<TransferTask> getReceivingTasks() {
        return receivingTasks.values();
    }
    
    /**
     * Check if listening
     */
    public boolean isListening() {
        return isRunning.get();
    }
    
    /**
     * Shutdown service
     */
    public void shutdown() {
        logger.info("Shutdown receiver service");
        stopListening();
        scheduledExecutor.shutdown();
    }
    
    // Event firing methods
    
    private void fireOnListeningStarted() {
        if (eventListener != null) {
            try {
                eventListener.onListeningStarted();
            } catch (Exception e) {
                logger.warning("Event handling exception: onListeningStarted, " + e.getMessage());
            }
        }
    }
    
    private void fireOnListeningStopped() {
        if (eventListener != null) {
            try {
                eventListener.onListeningStopped();
            } catch (Exception e) {
                logger.warning("Event handling exception: onListeningStopped, " + e.getMessage());
            }
        }
    }
    
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
    
    private void fireOnTaskCompleted(TransferTask task, File outputFile) {
        if (eventListener != null) {
            try {
                eventListener.onTaskCompleted(task, outputFile);
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
    
    private void fireOnTaskIncomplete(TransferTask task, java.util.List<Integer> missingChunks) {
        if (eventListener != null) {
            try {
                eventListener.onTaskIncomplete(task, missingChunks);
            } catch (Exception e) {
                logger.warning("Event handling exception: onTaskIncomplete, " + e.getMessage());
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