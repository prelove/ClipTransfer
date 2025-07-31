package com.cliptransfer.common;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Task manager class
 * Responsible for task storage, persistence, and management
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class TaskManager {
    private static final Logger logger = Logger.getLogger(TaskManager.class.getName());
    
    private static final String TASKS_DIR = System.getProperty("user.home") + File.separator + 
                                           ".cliptransfer" + File.separator + "tasks";
    private static final String TASKS_FILE = TASKS_DIR + File.separator + "tasks.json";
    
    // Task storage
    private final Map<String, TransferTask> tasks;
    
    /**
     * Constructor
     */
    public TaskManager() {
        this.tasks = new ConcurrentHashMap<>();
        
        // Ensure tasks directory exists
        File tasksDir = new File(TASKS_DIR);
        if (!tasksDir.exists()) {
            tasksDir.mkdirs();
        }
        
        // Load existing tasks
        loadTasksFromDisk();
        
        logger.info("Task manager initialized");
    }
    
    /**
     * Add task
     */
    public void addTask(TransferTask task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        
        tasks.put(task.getTaskId(), task);
        saveTasksToDisk();
        
        logger.info("Added task: " + task.getFileName() + " (ID: " + task.getTaskId() + ")");
    }
    
    /**
     * Update task
     */
    public void updateTask(TransferTask task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        
        if (tasks.containsKey(task.getTaskId())) {
            tasks.put(task.getTaskId(), task);
            saveTasksToDisk();
            
            logger.fine("Updated task: " + task.getFileName() + " (Status: " + task.getStatus() + ")");
        } else {
            logger.warning("Attempted to update non-existent task: " + task.getTaskId());
        }
    }
    
    /**
     * Remove task
     */
    public boolean removeTask(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return false;
        }
        
        TransferTask removedTask = tasks.remove(taskId);
        if (removedTask != null) {
            saveTasksToDisk();
            logger.info("Removed task: " + removedTask.getFileName() + " (ID: " + taskId + ")");
            return true;
        } else {
            logger.warning("Attempted to remove non-existent task: " + taskId);
            return false;
        }
    }
    
    /**
     * Get task
     */
    public TransferTask getTask(String taskId) {
        return tasks.get(taskId);
    }
    
    /**
     * Get all tasks
     */
    public List<TransferTask> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }
    
    /**
     * Get tasks by status
     */
    public List<TransferTask> getTasksByStatus(TaskStatus status) {
        List<TransferTask> result = new ArrayList<>();
        for (TransferTask task : tasks.values()) {
            if (task.getStatus() == status) {
                result.add(task);
            }
        }
        return result;
    }
    
    /**
     * Get task count
     */
    public int getTaskCount() {
        return tasks.size();
    }
    
    /**
     * Clear all tasks
     */
    public void clearAllTasks() {
        tasks.clear();
        saveTasksToDisk();
        logger.info("Cleared all tasks");
    }
    
    /**
     * Clean up completed tasks
     */
    public int cleanupCompletedTasks(int keepDays) {
        long cutoffTime = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L);
        int cleanedCount = 0;
        
        Iterator<Map.Entry<String, TransferTask>> iterator = tasks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, TransferTask> entry = iterator.next();
            TransferTask task = entry.getValue();
            
            // Clean up completed tasks older than cutoff time
            if (task.getStatus() == TaskStatus.COMPLETED && 
                task.getEndTime() > 0 && task.getEndTime() < cutoffTime) {
                iterator.remove();
                cleanedCount++;
            }
        }
        
        if (cleanedCount > 0) {
            saveTasksToDisk();
            logger.info("Cleaned up " + cleanedCount + " completed tasks (keeping " + keepDays + " days)");
        }
        
        return cleanedCount;
    }
    
    /**
     * Get task statistics
     */
    public Map<String, Object> getTaskStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Count by status
        Map<TaskStatus, Integer> statusCounts = new HashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            statusCounts.put(status, 0);
        }
        
        long totalSize = 0;
        long completedSize = 0;
        
        for (TransferTask task : tasks.values()) {
            // Count by status
            TaskStatus status = task.getStatus();
            statusCounts.put(status, statusCounts.get(status) + 1);
            
            // Calculate size
            totalSize += task.getTotalSize();
            if (status == TaskStatus.COMPLETED) {
                completedSize += task.getTotalSize();
            }
        }
        
        stats.put("total_tasks", tasks.size());
        stats.put("status_counts", statusCounts);
        stats.put("total_size", totalSize);
        stats.put("completed_size", completedSize);
        
        return stats;
    }
    
    /**
     * Save tasks to disk (Java 8 compatible)
     */
    private void saveTasksToDisk() {
        try {
            JSONArray tasksArray = new JSONArray();
            
            for (TransferTask task : tasks.values()) {
                JSONObject taskJson = taskToJson(task);
                tasksArray.put(taskJson);
            }
            
            // Write to file using Java 8 compatible method
            try (FileOutputStream fos = new FileOutputStream(TASKS_FILE);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                 BufferedWriter writer = new BufferedWriter(osw)) {
                
                writer.write(tasksArray.toString(2)); // Pretty print with 2-space indent
            }
            
            logger.fine("Saved " + tasks.size() + " tasks to disk");
            
        } catch (Exception e) {
            logger.severe("Failed to save tasks to disk: " + e.getMessage());
        }
    }
    
    /**
     * Load tasks from disk (Java 8 compatible)
     */
    private void loadTasksFromDisk() {
        File tasksFile = new File(TASKS_FILE);
        if (!tasksFile.exists()) {
            logger.info("Tasks file does not exist, starting with empty task list");
            return;
        }
        
        try {
            // Read file content using Java 8 compatible method
            StringBuilder content = new StringBuilder();
            try (FileInputStream fis = new FileInputStream(tasksFile);
                 InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                 BufferedReader reader = new BufferedReader(isr)) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            
            if (content.length() == 0) {
                logger.info("Tasks file is empty");
                return;
            }
            
            // Parse JSON
            JSONArray tasksArray = new JSONArray(content.toString());
            
            for (int i = 0; i < tasksArray.length(); i++) {
                JSONObject taskJson = tasksArray.getJSONObject(i);
                try {
                    TransferTask task = taskFromJson(taskJson);
                    tasks.put(task.getTaskId(), task);
                } catch (Exception e) {
                    logger.warning("Failed to parse task " + i + ": " + e.getMessage());
                }
            }
            
            logger.info("Loaded " + tasks.size() + " tasks from disk");
            
        } catch (Exception e) {
            logger.severe("Failed to load tasks from disk: " + e.getMessage());
        }
    }
    
    /**
     * Convert task to JSON
     */
    private JSONObject taskToJson(TransferTask task) {
        JSONObject json = new JSONObject();
        
        json.put("taskId", task.getTaskId());
        json.put("fileName", task.getFileName());
        json.put("filePath", task.getFilePath());
        json.put("transferType", task.getTransferType().getValue());
        json.put("totalSize", task.getTotalSize());
        json.put("chunkSize", task.getChunkSize());
        json.put("chunkTotal", task.getChunkTotal());
        json.put("status", task.getStatus().getValue());
        json.put("errorMessage", task.getErrorMessage());
        json.put("createTime", task.getCreateTime());
        json.put("startTime", task.getStartTime());
        json.put("endTime", task.getEndTime());
        json.put("transferredBytes", task.getTransferredBytes());
        json.put("fileMd5", task.getFileMd5());
        
        // Convert completed chunks to array
        JSONArray completedChunksArray = new JSONArray();
        for (Integer chunkIndex : task.getCompletedChunks()) {
            completedChunksArray.put(chunkIndex);
        }
        json.put("completedChunks", completedChunksArray);
        
        // Convert failed chunks to object
        JSONObject failedChunksObject = new JSONObject();
        for (Map.Entry<Integer, String> entry : task.getFailedChunks().entrySet()) {
            failedChunksObject.put(entry.getKey().toString(), entry.getValue());
        }
        json.put("failedChunks", failedChunksObject);
        
        return json;
    }
    
    /**
     * Convert JSON to task
     */
    private TransferTask taskFromJson(JSONObject json) {
        String taskId = json.getString("taskId");
        String fileName = json.getString("fileName");
        String filePath = json.optString("filePath", null);
        TransferType transferType = TransferType.fromValue(json.getString("transferType"));
        long totalSize = json.getLong("totalSize");
        int chunkSize = json.getInt("chunkSize");
        
        TransferTask task = new TransferTask(taskId, fileName, filePath, transferType, totalSize, chunkSize);
        
        // Set additional properties
        task.setChunkTotal(json.getInt("chunkTotal"));
        task.setFileMd5(json.optString("fileMd5", null));
        
        // Restore status (but don't restore RUNNING status to avoid confusion)
        TaskStatus status = TaskStatus.fromValue(json.getString("status"));
        if (status == TaskStatus.RUNNING || status == TaskStatus.PAUSED) {
            // Reset running/paused tasks to pending
            status = TaskStatus.PENDING;
        }
        
        // Set status through reflection or recreate task state
        if (status == TaskStatus.COMPLETED) {
            task.complete();
        } else if (status == TaskStatus.FAILED) {
            task.fail(json.optString("errorMessage", "Unknown error"));
        } else if (status == TaskStatus.CANCELLED) {
            task.cancel();
        }
        
        // Restore timestamps
        try {
            java.lang.reflect.Field createTimeField = task.getClass().getDeclaredField("createTime");
            createTimeField.setAccessible(true);
            createTimeField.setLong(task, json.getLong("createTime"));
            
            java.lang.reflect.Field startTimeField = task.getClass().getDeclaredField("startTime");
            startTimeField.setAccessible(true);
            startTimeField.setLong(task, json.getLong("startTime"));
            
            java.lang.reflect.Field endTimeField = task.getClass().getDeclaredField("endTime");
            endTimeField.setAccessible(true);
            endTimeField.setLong(task, json.getLong("endTime"));
        } catch (Exception e) {
            logger.warning("Failed to restore task timestamps: " + e.getMessage());
        }
        
        return task;
    }
    
    /**
     * Get tasks directory
     */
    public static String getTasksDirectory() {
        return TASKS_DIR;
    }
}