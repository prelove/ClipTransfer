package com.cliptransfer.ui;

import com.cliptransfer.common.Config;
import com.cliptransfer.common.FileUtil;
import com.cliptransfer.common.TransferTask;
import com.cliptransfer.sender.SenderService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Logger;

/**
 * Sender window class
 * Provides file/folder selection and sending functionality interface
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class SenderWindow extends JFrame {
    private static final Logger logger = Logger.getLogger(SenderWindow.class.getName());
    
    private final Config config;
    private final SenderService senderService;
    
    // UI Components
    private JTextField filePathField;
    private JButton browseButton;
    private JButton sendButton;
    private JButton pauseButton;
    private JButton stopButton;
    
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JLabel speedLabel;
    private JLabel timeLabel;
    
    private JTextArea logArea;
    private JScrollPane logScrollPane;
    
    // Current task
    private TransferTask currentTask;
    
    /**
     * Constructor
     */
    public SenderWindow(Config config, SenderService senderService) {
        this.config = config;
        this.senderService = senderService;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
        logger.info("Sender window initialization completed");
    }
    
    /**
     * Initialize components
     */
    private void initializeComponents() {
        setTitle("ClipTransfer - Send Files/Folders");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        
        // File selection components
        filePathField = new JTextField();
        filePathField.setEditable(false);
        browseButton = new JButton("Browse...");
        
        // Control buttons
        sendButton = new JButton("Start Sending");
        pauseButton = new JButton("Pause");
        stopButton = new JButton("Stop");
        
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);
        
        // Progress components
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        
        statusLabel = new JLabel("Status: Waiting for file selection");
        speedLabel = new JLabel("Transfer Speed: --");
        timeLabel = new JLabel("Remaining Time: --");
        
        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logScrollPane = new JScrollPane(logArea);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        addLog("Sender ready, please select files or folders to transfer");
    }
    
    /**
     * Setup layout
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Top file selection panel
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        filePanel.setBorder(new TitledBorder("Select File/Folder"));
        filePanel.add(new JLabel("Path: "), BorderLayout.WEST);
        filePanel.add(filePathField, BorderLayout.CENTER);
        filePanel.add(browseButton, BorderLayout.EAST);
        
        // Control button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(sendButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(stopButton);
        
        // Progress panel
        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        progressPanel.setBorder(new TitledBorder("Transfer Progress"));
        progressPanel.add(progressBar, BorderLayout.NORTH);
        
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 2));
        infoPanel.add(statusLabel);
        infoPanel.add(speedLabel);
        infoPanel.add(timeLabel);
        progressPanel.add(infoPanel, BorderLayout.CENTER);
        
        // Log panel
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new TitledBorder("Log Information"));
        logPanel.add(logScrollPane, BorderLayout.CENTER);
        
        // Assemble main panel
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(filePanel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.CENTER);
        topPanel.add(progressPanel, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);
        add(logPanel, BorderLayout.CENTER);
        
        // Set margins
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        // Browse button
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseFile();
            }
        });
        
        // Send button
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startSending();
            }
        });
        
        // Pause button
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pauseSending();
            }
        });
        
        // Stop button
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopSending();
            }
        });
    }
    
    /**
     * Browse file/folder
     */
    private void browseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setDialogTitle("Select File or Folder to Send");
        
        // Set current directory
        String lastPath = filePathField.getText();
        if (!lastPath.isEmpty()) {
            File lastFile = new File(lastPath);
            if (lastFile.getParent() != null) {
                fileChooser.setCurrentDirectory(new File(lastFile.getParent()));
            }
        }
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
            
            // Show file information
            if (selectedFile.isFile()) {
                addLog("Selected file: " + selectedFile.getName() + 
                      " (size: " + FileUtil.formatFileSize(selectedFile.length()) + ")");
            } else if (selectedFile.isDirectory()) {
                addLog("Selected folder: " + selectedFile.getName());
            }
            
            sendButton.setEnabled(true);
        }
    }
    
    /**
     * Start sending
     */
    private void startSending() {
        String filePath = filePathField.getText().trim();
        if (filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a file or folder to send first", "Tip", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "File or folder does not exist", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // Check if service is running
            if (senderService.isRunning()) {
                JOptionPane.showMessageDialog(this, "A task is already being sent", "Tip", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            addLog("Start sending: " + file.getName());
            
            // Start sending
            String taskId = senderService.sendFile(filePath);
            addLog("Task ID: " + taskId);
            
            // Update interface state
            sendButton.setEnabled(false);
            pauseButton.setEnabled(true);
            stopButton.setEnabled(true);
            browseButton.setEnabled(false);
            
            statusLabel.setText("Status: Sending...");
            progressBar.setString("Preparing...");
            
        } catch (Exception e) {
            logger.severe("Send failed: " + e.getMessage());
            addLog("Send failed: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Send failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Pause sending
     */
    private void pauseSending() {
        if (senderService.isRunning()) {
            if (senderService.isPaused()) {
                // Resume
                senderService.resumeCurrentTask();
                pauseButton.setText("Pause");
                addLog("Resume sending");
            } else {
                // Pause
                senderService.pauseCurrentTask();
                pauseButton.setText("Resume");
                addLog("Pause sending");
            }
        }
    }
    
    /**
     * Stop sending
     */
    private void stopSending() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to stop the current sending task?",
            "Confirm Stop",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            senderService.stopCurrentTask();
            addLog("User stopped sending task");
            resetInterface();
        }
    }
    
    /**
     * Reset interface
     */
    private void resetInterface() {
        sendButton.setEnabled(true);
        pauseButton.setEnabled(false);
        pauseButton.setText("Pause");
        stopButton.setEnabled(false);
        browseButton.setEnabled(true);
        
        progressBar.setValue(0);
        progressBar.setString("Ready");
        statusLabel.setText("Status: Waiting for file selection");
        speedLabel.setText("Transfer Speed: --");
        timeLabel.setText("Remaining Time: --");
        
        currentTask = null;
    }
    
    /**
     * Update task status
     */
    public void updateTaskStatus(TransferTask task) {
        this.currentTask = task;
        
        switch (task.getStatus()) {
            case RUNNING:
                statusLabel.setText("Status: Sending - " + task.getFileName());
                break;
            case PAUSED:
                statusLabel.setText("Status: Paused - " + task.getFileName());
                break;
            case COMPLETED:
                statusLabel.setText("Status: Send completed - " + task.getFileName());
                addLog("Send completed: " + task.getFileName());
                resetInterface();
                break;
            case FAILED:
                statusLabel.setText("Status: Send failed - " + task.getFileName());
                addLog("Send failed: " + task.getFileName() + " - " + task.getErrorMessage());
                resetInterface();
                break;
            case CANCELLED:
                statusLabel.setText("Status: Cancelled - " + task.getFileName());
                addLog("Task cancelled: " + task.getFileName());
                resetInterface();
                break;
        }
    }
    
    /**
     * Update progress
     */
    public void updateProgress(TransferTask task, int completedChunks, int totalChunks) {
        if (task != currentTask) {
            return;
        }
        
        double progress = task.getProgressPercentage();
        progressBar.setValue((int) progress);
        progressBar.setString(String.format("%.1f%% (%d/%d)", progress, completedChunks, totalChunks));
        
        // Update speed and time
        double speed = task.getTransferSpeed();
        if (speed > 0) {
            speedLabel.setText("Transfer Speed: " + FileUtil.formatFileSize((long) speed) + "/s");
            
            long remainingTime = task.getEstimatedRemainingTime();
            if (remainingTime > 0) {
                timeLabel.setText("Remaining Time: " + formatTime(remainingTime));
            }
        }
    }
    
    /**
     * Format time
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
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