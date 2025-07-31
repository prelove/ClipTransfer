package com.cliptransfer.ui;

import com.cliptransfer.common.Config;
import com.cliptransfer.common.FileUtil;
import com.cliptransfer.common.TransferTask;
import com.cliptransfer.receiver.ReceiverService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * Receiver window class
 * Provides receiving management and monitoring functionality interface
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class ReceiverWindow extends JFrame {
    private static final Logger logger = Logger.getLogger(ReceiverWindow.class.getName());
    
    private final Config config;
    private final ReceiverService receiverService;
    
    // UI Components
    private JButton startButton;
    private JButton stopButton;
    private JButton openFolderButton;
    private JLabel statusLabel;
    private JLabel downloadPathLabel;
    
    // Receiving task table
    private JTable receivingTable;
    private DefaultTableModel receivingTableModel;
    private JScrollPane receivingScrollPane;
    
    // Log area
    private JTextArea logArea;
    private JScrollPane logScrollPane;
    
    // Table column definitions
    private static final String[] RECEIVING_COLUMNS = {
        "File Name", "Type", "Size", "Progress", "Status", "Speed"
    };
    
    /**
     * Constructor
     */
    public ReceiverWindow(Config config, ReceiverService receiverService) {
        this.config = config;
        this.receiverService = receiverService;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        updateListeningStatus(receiverService.isListening());
        
        logger.info("Receiver window initialization completed");
    }
    
    /**
     * Initialize components
     */
    private void initializeComponents() {
        setTitle("ClipTransfer - Receive Management");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // Control buttons
        startButton = new JButton("Start Listening");
        stopButton = new JButton("Stop Listening");
        openFolderButton = new JButton("Open Download Folder");
        
        // Status labels
        statusLabel = new JLabel("Status: Not Listening");
        downloadPathLabel = new JLabel("Download Path: " + config.getDownloadPath());
        
        // Receiving task table
        receivingTableModel = new DefaultTableModel(RECEIVING_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        receivingTable = new JTable(receivingTableModel);
        receivingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        receivingTable.setRowHeight(25);
        
        // Set column widths
        TableColumnModel columnModel = receivingTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(200); // File Name
        columnModel.getColumn(1).setPreferredWidth(60);  // Type
        columnModel.getColumn(2).setPreferredWidth(100); // Size
        columnModel.getColumn(3).setPreferredWidth(120); // Progress
        columnModel.getColumn(4).setPreferredWidth(80);  // Status
        columnModel.getColumn(5).setPreferredWidth(100); // Speed
        
        receivingScrollPane = new JScrollPane(receivingTable);
        receivingScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logScrollPane = new JScrollPane(logArea);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        addLog("Receiver ready, can start listening to clipboard");
    }
    
    /**
     * Setup layout
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Top control panel
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        controlPanel.setBorder(new TitledBorder("Receive Control"));
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(openFolderButton);
        
        // Status panel
        JPanel statusPanel = new JPanel(new GridLayout(2, 1, 5, 2));
        statusPanel.add(statusLabel);
        statusPanel.add(downloadPathLabel);
        
        controlPanel.add(buttonPanel, BorderLayout.WEST);
        controlPanel.add(statusPanel, BorderLayout.CENTER);
        
        // Receiving task panel
        JPanel receivingPanel = new JPanel(new BorderLayout());
        receivingPanel.setBorder(new TitledBorder("Receiving Tasks"));
        receivingPanel.add(receivingScrollPane, BorderLayout.CENTER);
        
        // Log panel
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new TitledBorder("Log Information"));
        logPanel.add(logScrollPane, BorderLayout.CENTER);
        
        // Central split pane
        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, receivingPanel, logPanel);
        centerSplitPane.setDividerLocation(300);
        centerSplitPane.setResizeWeight(0.6);
        
        add(controlPanel, BorderLayout.NORTH);
        add(centerSplitPane, BorderLayout.CENTER);
        
        // Set margins
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        // Start listening button
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startListening();
            }
        });
        
        // Stop listening button
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopListening();
            }
        });
        
        // Open download folder button
        openFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openDownloadFolder();
            }
        });
        
        // Table double-click event
        receivingTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showTaskDetails();
                }
            }
        });
    }
    
    /**
     * Start listening
     */
    private void startListening() {
        try {
            receiverService.startListening();
            addLog("Started listening to clipboard, interval: " + config.getReceiveInterval() + "ms");
        } catch (Exception e) {
            logger.severe("Failed to start listening: " + e.getMessage());
            addLog("Failed to start listening: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to start listening: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Stop listening
     */
    private void stopListening() {
        try {
            receiverService.stopListening();
            addLog("Stopped listening to clipboard");
        } catch (Exception e) {
            logger.severe("Failed to stop listening: " + e.getMessage());
            addLog("Failed to stop listening: " + e.getMessage());
        }
    }
    
    /**
     * Open download folder
     */
    private void openDownloadFolder() {
        try {
            String downloadPath = config.getDownloadPath();
            File downloadDir = new File(downloadPath);
            
            // Ensure directory exists
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            
            // Open directory using system default method
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(downloadDir);
            } else {
                // Fallback: display path
                JOptionPane.showMessageDialog(
                    this,
                    "Download Directory: " + downloadPath,
                    "Download Directory",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
            
        } catch (Exception e) {
            logger.warning("Failed to open download directory: " + e.getMessage());
            JOptionPane.showMessageDialog(
                this,
                "Cannot open download directory: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    /**
     * Show task details
     */
    private void showTaskDetails() {
        int selectedRow = receivingTable.getSelectedRow();
        if (selectedRow >= 0) {
            String fileName = (String) receivingTableModel.getValueAt(selectedRow, 0);
            String details = "Task Details:\n" +
                           "File Name: " + fileName + "\n" +
                           "Type: " + receivingTableModel.getValueAt(selectedRow, 1) + "\n" +
                           "Size: " + receivingTableModel.getValueAt(selectedRow, 2) + "\n" +
                           "Progress: " + receivingTableModel.getValueAt(selectedRow, 3) + "\n" +
                           "Status: " + receivingTableModel.getValueAt(selectedRow, 4) + "\n" +
                           "Speed: " + receivingTableModel.getValueAt(selectedRow, 5);
            
            JOptionPane.showMessageDialog(
                this,
                details,
                "Task Details",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }
    
    /**
     * Update listening status
     */
    public void updateListeningStatus(boolean isListening) {
        if (isListening) {
            statusLabel.setText("Status: Listening to clipboard");
            statusLabel.setForeground(new Color(0, 128, 0));
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        } else {
            statusLabel.setText("Status: Not listening");
            statusLabel.setForeground(Color.RED);
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }
    
    /**
     * Add receiving task
     */
    public void addReceivingTask(TransferTask task) {
        String type = task.getTransferType().getValue();
        String size = FileUtil.formatFileSize(task.getTotalSize());
        String progress = "0.0%";
        String status = task.getStatus().getDescription();
        String speed = "--";
        
        receivingTableModel.addRow(new Object[] {
            task.getFileName(), type, size, progress, status, speed
        });
        
        addLog("Started receiving: " + task.getFileName() + " (" + size + ")");
    }
    
    /**
     * Update progress
     */
    public void updateProgress(TransferTask task, int completedChunks, int totalChunks) {
        // Find corresponding row
        int row = findTaskRow(task.getFileName());
        if (row >= 0) {
            String progress = String.format("%.1f%% (%d/%d)", 
                task.getProgressPercentage(), completedChunks, totalChunks);
            receivingTableModel.setValueAt(progress, row, 3);
            
            // Update speed
            double speed = task.getTransferSpeed();
            if (speed > 0) {
                String speedText = FileUtil.formatFileSize((long) speed) + "/s";
                receivingTableModel.setValueAt(speedText, row, 5);
            }
        }
    }
    
    /**
     * Update task status
     */
    public void updateTaskStatus(TransferTask task) {
        int row = findTaskRow(task.getFileName());
        if (row >= 0) {
            receivingTableModel.setValueAt(task.getStatus().getDescription(), row, 4);
            
            // Remove from table if task is completed or failed
            if (task.getStatus().isTerminalStatus()) {
                SwingUtilities.invokeLater(() -> {
                    receivingTableModel.removeRow(row);
                });
                
                if (task.getStatus() == com.cliptransfer.common.TaskStatus.COMPLETED) {
                    addLog("Receive completed: " + task.getFileName());
                } else if (task.getStatus() == com.cliptransfer.common.TaskStatus.FAILED) {
                    addLog("Receive failed: " + task.getFileName() + " - " + task.getErrorMessage());
                }
            }
        }
    }
    
    /**
     * Show missing chunks information
     */
    public void showMissingChunks(TransferTask task, List<Integer> missingChunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Task: ").append(task.getFileName()).append("\n");
        sb.append("Missing chunks count: ").append(missingChunks.size()).append("\n");
        sb.append("Total chunks: ").append(task.getChunkTotal()).append("\n\n");
        
        if (missingChunks.size() <= 20) {
            sb.append("Missing chunk indices: ");
            for (int i = 0; i < missingChunks.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(missingChunks.get(i));
            }
        } else {
            sb.append("Too many missing chunks, please check network connection or resend");
        }
        
        addLog("Receive incomplete: " + task.getFileName() + ", missing " + missingChunks.size() + " chunks");
        
        JOptionPane.showMessageDialog(
            this,
            sb.toString(),
            "Receive Incomplete",
            JOptionPane.WARNING_MESSAGE
        );
    }
    
    /**
     * Find task row
     */
    private int findTaskRow(String fileName) {
        for (int i = 0; i < receivingTableModel.getRowCount(); i++) {
            if (fileName.equals(receivingTableModel.getValueAt(i, 0))) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Add log
     */
    private void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}