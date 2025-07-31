package com.cliptransfer.ui;

import com.cliptransfer.common.FileUtil;
import com.cliptransfer.common.TaskManager;
import com.cliptransfer.common.TaskStatus;
import com.cliptransfer.common.TransferTask;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Task list window class
 * Provides viewing and management functionality for all transfer tasks
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class TaskListWindow extends JFrame {
    private static final Logger logger = Logger.getLogger(TaskListWindow.class.getName());
    
    private final TaskManager taskManager;
    
    // UI Components
    private JTable taskTable;
    private DefaultTableModel taskTableModel;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private JScrollPane taskScrollPane;
    
    private JComboBox<String> statusFilterComboBox;
    private JButton refreshButton;
    private JButton deleteButton;
    private JButton cleanupButton;
    private JButton exportButton;
    
    private JLabel totalTasksLabel;
    private JLabel completedTasksLabel;
    private JLabel failedTasksLabel;
    private JLabel totalSizeLabel;
    
    // Table column definitions
    private static final String[] TASK_COLUMNS = {
        "File Name", "Type", "Size", "Status", "Progress", "Created", "Completed", "Task ID"
    };
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Constructor
     */
    public TaskListWindow(TaskManager taskManager) {
        this.taskManager = taskManager;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        refreshTasks();
        
        logger.info("Task list window initialization completed");
    }
    
    /**
     * Initialize components
     */
    private void initializeComponents() {
        setTitle("ClipTransfer - Task List");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        // Task table
        taskTableModel = new DefaultTableModel(TASK_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        taskTable = new JTable(taskTableModel);
        taskTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        taskTable.setRowHeight(25);
        
        // Set table sorting
        tableSorter = new TableRowSorter<>(taskTableModel);
        taskTable.setRowSorter(tableSorter);
        
        // Set column widths
        TableColumnModel columnModel = taskTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(200); // File Name
        columnModel.getColumn(1).setPreferredWidth(60);  // Type
        columnModel.getColumn(2).setPreferredWidth(100); // Size
        columnModel.getColumn(3).setPreferredWidth(80);  // Status
        columnModel.getColumn(4).setPreferredWidth(80);  // Progress
        columnModel.getColumn(5).setPreferredWidth(130); // Created
        columnModel.getColumn(6).setPreferredWidth(130); // Completed
        columnModel.getColumn(7).setPreferredWidth(100); // Task ID
        
        taskScrollPane = new JScrollPane(taskTable);
        taskScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // Filter and control components
        statusFilterComboBox = new JComboBox<>(new String[] {
            "All", "Pending", "Running", "Paused", "Completed", "Failed", "Cancelled"
        });
        
        refreshButton = new JButton("Refresh");
        deleteButton = new JButton("Delete Selected");
        cleanupButton = new JButton("Cleanup Completed");
        exportButton = new JButton("Export List");
        
        // Statistics labels
        totalTasksLabel = new JLabel("Total Tasks: 0");
        completedTasksLabel = new JLabel("Completed: 0");
        failedTasksLabel = new JLabel("Failed: 0");
        totalSizeLabel = new JLabel("Total Size: 0 B");
    }
    
    /**
     * Setup layout
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Top toolbar
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBorder(new TitledBorder("Task Management"));
        
        // Left filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Status Filter:"));
        filterPanel.add(statusFilterComboBox);
        
        // Right button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(refreshButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(cleanupButton);
        buttonPanel.add(exportButton);
        
        toolbarPanel.add(filterPanel, BorderLayout.WEST);
        toolbarPanel.add(buttonPanel, BorderLayout.EAST);
        
        // Bottom statistics panel
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 10, 5));
        statsPanel.setBorder(new TitledBorder("Statistics"));
        statsPanel.add(totalTasksLabel);
        statsPanel.add(completedTasksLabel);
        statsPanel.add(failedTasksLabel);
        statsPanel.add(totalSizeLabel);
        
        // Task table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(new TitledBorder("Task List"));
        tablePanel.add(taskScrollPane, BorderLayout.CENTER);
        
        add(toolbarPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
        add(statsPanel, BorderLayout.SOUTH);
        
        // Set margins
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        // Status filter
        statusFilterComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyStatusFilter();
            }
        });
        
        // Refresh button
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshTasks();
            }
        });
        
        // Delete button
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedTasks();
            }
        });
        
        // Cleanup button
        cleanupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cleanupCompletedTasks();
            }
        });
        
        // Export button
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportTaskList();
            }
        });
        
        // Table double-click event
        taskTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showTaskDetails();
                }
            }
        });
        
        // Table selection event
        taskTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
    }
    
    /**
     * Refresh task list
     */
    public void refreshTasks() {
        try {
            // Clear table
            taskTableModel.setRowCount(0);
            
            // Load all tasks
            List<TransferTask> allTasks = taskManager.getAllTasks();
            
            for (TransferTask task : allTasks) {
                addTaskToTable(task);
            }
            
            // Update statistics
            updateStatistics();
            
            // Apply filter
            applyStatusFilter();
            
            logger.info("Task list refresh completed, total tasks: " + allTasks.size());
            
        } catch (Exception e) {
            logger.severe("Failed to refresh task list: " + e.getMessage());
            JOptionPane.showMessageDialog(
                this,
                "Failed to refresh task list: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    /**
     * Add task to table
     */
    private void addTaskToTable(TransferTask task) {
        String createTime = task.getCreateTime() > 0 ? 
            DATE_FORMAT.format(new Date(task.getCreateTime())) : "--";
        String endTime = task.getEndTime() > 0 ? 
            DATE_FORMAT.format(new Date(task.getEndTime())) : "--";
        String progress = String.format("%.1f%%", task.getProgressPercentage());
        
        taskTableModel.addRow(new Object[] {
            task.getFileName(),
            task.getTransferType().getValue(),
            FileUtil.formatFileSize(task.getTotalSize()),
            task.getStatus().getDescription(),
            progress,
            createTime,
            endTime,
            task.getTaskId().substring(0, 8) + "..." // Display short ID
        });
    }
    
    /**
     * Apply status filter
     */
    private void applyStatusFilter() {
        String selectedStatus = (String) statusFilterComboBox.getSelectedItem();
        
        if ("All".equals(selectedStatus)) {
            tableSorter.setRowFilter(null);
        } else {
            // Filter by status
            RowFilter<DefaultTableModel, Object> filter = RowFilter.regexFilter(getStatusFilterPattern(selectedStatus), 3);
            tableSorter.setRowFilter(filter);
        }
    }
    
    /**
     * Get status filter pattern
     */
    private String getStatusFilterPattern(String statusName) {
        switch (statusName) {
            case "Pending": return "Pending";
            case "Running": return "Running";
            case "Paused": return "Paused";
            case "Completed": return "Completed";
            case "Failed": return "Failed";
            case "Cancelled": return "Cancelled";
            default: return ".*";
        }
    }
    
    /**
     * Delete selected tasks
     */
    private void deleteSelectedTasks() {
        int[] selectedRows = taskTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select tasks to delete first", "Tip", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete the selected " + selectedRows.length + " tasks?\nThis operation cannot be undone!",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            try {
                int deletedCount = 0;
                
                // Delete from back to front to avoid index changes
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    int modelRow = taskTable.convertRowIndexToModel(selectedRows[i]);
                    String displayId = (String) taskTableModel.getValueAt(modelRow, 7);
                    
                    // Find complete task ID based on display ID
                    List<TransferTask> allTasks = taskManager.getAllTasks();
                    for (TransferTask task : allTasks) {
                        if (task.getTaskId().startsWith(displayId.substring(0, 8))) {
                            if (taskManager.removeTask(task.getTaskId())) {
                                deletedCount++;
                            }
                            break;
                        }
                    }
                }
                
                // Refresh list
                refreshTasks();
                
                JOptionPane.showMessageDialog(
                    this,
                    "Successfully deleted " + deletedCount + " tasks",
                    "Delete Completed",
                    JOptionPane.INFORMATION_MESSAGE
                );
                
                logger.info("Deleted tasks: " + deletedCount);
                
            } catch (Exception e) {
                logger.severe("Failed to delete tasks: " + e.getMessage());
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to delete tasks: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
    
    /**
     * Cleanup completed tasks
     */
    private void cleanupCompletedTasks() {
        String[] options = {"Keep 7 days", "Keep 30 days", "Clean all", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "Select cleanup strategy:",
            "Cleanup Completed Tasks",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        if (choice >= 0 && choice < 3) {
            try {
                int keepDays;
                switch (choice) {
                    case 0: keepDays = 7; break;
                    case 1: keepDays = 30; break;
                    case 2: keepDays = 0; break;
                    default: return;
                }
                
                int cleanedCount = taskManager.cleanupCompletedTasks(keepDays);
                
                refreshTasks();
                
                JOptionPane.showMessageDialog(
                    this,
                    "Cleanup completed, deleted " + cleanedCount + " completed tasks",
                    "Cleanup Completed",
                    JOptionPane.INFORMATION_MESSAGE
                );
                
                logger.info("Cleaned up completed tasks: " + cleanedCount + ", keep days: " + keepDays);
                
            } catch (Exception e) {
                logger.severe("Failed to cleanup tasks: " + e.getMessage());
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to cleanup tasks: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
    
    /**
     * Export task list
     */
    private void exportTaskList() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Task List");
        fileChooser.setSelectedFile(new java.io.File("ClipTransfer_Tasks_" + 
            new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = fileChooser.getSelectedFile();
                exportTasksToFile(file);
                
                JOptionPane.showMessageDialog(
                    this,
                    "Task list exported successfully:\n" + file.getAbsolutePath(),
                    "Export Completed",
                    JOptionPane.INFORMATION_MESSAGE
                );
                
                logger.info("Task list exported successfully: " + file.getAbsolutePath());
                
            } catch (Exception e) {
                logger.severe("Failed to export task list: " + e.getMessage());
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to export task list: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
    
    /**
     * Export tasks to file
     */
    private void exportTasksToFile(java.io.File file) throws Exception {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(
            new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(file), "UTF-8"))) {
            
            writer.println("ClipTransfer Task List Export");
            writer.println("Export Time: " + DATE_FORMAT.format(new Date()));
            writer.println(repeatString("=", 80));
            writer.println();
            
            List<TransferTask> allTasks = taskManager.getAllTasks();
            for (TransferTask task : allTasks) {
                writer.println("Task ID: " + task.getTaskId());
                writer.println("File Name: " + task.getFileName());
                writer.println("Type: " + task.getTransferType().getValue());
                writer.println("Size: " + FileUtil.formatFileSize(task.getTotalSize()));
                writer.println("Status: " + task.getStatus().getDescription());
                writer.println("Progress: " + String.format("%.1f%%", task.getProgressPercentage()));
                writer.println("Created Time: " + (task.getCreateTime() > 0 ? 
                    DATE_FORMAT.format(new Date(task.getCreateTime())) : "--"));
                writer.println("Completed Time: " + (task.getEndTime() > 0 ? 
                    DATE_FORMAT.format(new Date(task.getEndTime())) : "--"));
                if (task.getErrorMessage() != null) {
                    writer.println("Error Message: " + task.getErrorMessage());
                }
                writer.println(repeatString("-", 40));
            }
            
            // Statistics
            Map<String, Object> stats = taskManager.getTaskStatistics();
            writer.println();
            writer.println("Statistics:");
            writer.println("Total Tasks: " + stats.get("total_tasks"));
            writer.println("Total Size: " + FileUtil.formatFileSize((Long) stats.get("total_size")));
            writer.println("Completed Size: " + FileUtil.formatFileSize((Long) stats.get("completed_size")));
        }
    }
    
    /**
     * Repeat string (Java 8 compatible)
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    /**
     * Show task details
     */
    private void showTaskDetails() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = taskTable.convertRowIndexToModel(selectedRow);
            String displayId = (String) taskTableModel.getValueAt(modelRow, 7);
            
            // Find complete task based on display ID
            List<TransferTask> allTasks = taskManager.getAllTasks();
            for (TransferTask task : allTasks) {
                if (task.getTaskId().startsWith(displayId.substring(0, 8))) {
                    showTaskDetailsDialog(task);
                    break;
                }
            }
        }
    }
    
    /**
     * Show task details dialog
     */
    private void showTaskDetailsDialog(TransferTask task) {
        StringBuilder details = new StringBuilder();
        details.append("Task Detailed Information:\n\n");
        details.append("Task ID: ").append(task.getTaskId()).append("\n");
        details.append("File Name: ").append(task.getFileName()).append("\n");
        details.append("File Path: ").append(task.getFilePath() != null ? task.getFilePath() : "--").append("\n");
        details.append("Transfer Type: ").append(task.getTransferType().getValue()).append("\n");
        details.append("File Size: ").append(FileUtil.formatFileSize(task.getTotalSize())).append("\n");
        details.append("Chunk Size: ").append(FileUtil.formatFileSize(task.getChunkSize())).append("\n");
        details.append("Total Chunks: ").append(task.getChunkTotal()).append("\n");
        details.append("Completed Chunks: ").append(task.getCompletedChunks().size()).append("\n");
        details.append("Failed Chunks: ").append(task.getFailedChunks().size()).append("\n");
        details.append("Progress: ").append(String.format("%.1f%%", task.getProgressPercentage())).append("\n");
        details.append("Status: ").append(task.getStatus().getDescription()).append("\n");
        details.append("Created Time: ").append(task.getCreateTime() > 0 ? 
            DATE_FORMAT.format(new Date(task.getCreateTime())) : "--").append("\n");
        details.append("Start Time: ").append(task.getStartTime() > 0 ? 
            DATE_FORMAT.format(new Date(task.getStartTime())) : "--").append("\n");
        details.append("End Time: ").append(task.getEndTime() > 0 ? 
            DATE_FORMAT.format(new Date(task.getEndTime())) : "--").append("\n");
        details.append("File MD5: ").append(task.getFileMd5() != null ? task.getFileMd5() : "--").append("\n");
        
        if (task.getErrorMessage() != null) {
            details.append("Error Message: ").append(task.getErrorMessage()).append("\n");
        }
        
        if (!task.getFolderManifest().isEmpty()) {
            details.append("Folder Manifest: ").append(task.getFolderManifest().size()).append(" files\n");
        }
        
        JTextArea textArea = new JTextArea(details.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        
        JOptionPane.showMessageDialog(
            this,
            scrollPane,
            "Task Details - " + task.getFileName(),
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    /**
     * Update statistics
     */
    private void updateStatistics() {
        Map<String, Object> stats = taskManager.getTaskStatistics();
        
        totalTasksLabel.setText("Total Tasks: " + stats.get("total_tasks"));
        
        @SuppressWarnings("unchecked")
        Map<TaskStatus, Integer> statusCounts = (Map<TaskStatus, Integer>) stats.get("status_counts");
        completedTasksLabel.setText("Completed: " + statusCounts.get(TaskStatus.COMPLETED));
        failedTasksLabel.setText("Failed: " + statusCounts.get(TaskStatus.FAILED));
        
        totalSizeLabel.setText("Total Size: " + FileUtil.formatFileSize((Long) stats.get("total_size")));
    }
    
    /**
     * Update button states
     */
    private void updateButtonStates() {
        boolean hasSelection = taskTable.getSelectedRowCount() > 0;
        deleteButton.setEnabled(hasSelection);
    }
}