package com.cliptransfer.ui;

import com.cliptransfer.common.Config;
import com.cliptransfer.common.FileUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Logger;

/**
 * Configuration window class
 * Provides settings interface for application configuration parameters
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class ConfigWindow extends JFrame {
    private static final Logger logger = Logger.getLogger(ConfigWindow.class.getName());
    
    private final Config config;
    
    // UI components
    private JSpinner chunkSizeSpinner;
    private JSpinner sendIntervalSpinner;
    private JSpinner receiveIntervalSpinner;
    private JComboBox<String> logLevelComboBox;
    private JTextField downloadPathField;
    private JButton browsePathButton;
    
    private JButton saveButton;
    private JButton cancelButton;
    private JButton resetButton;
    
    // Original configuration values (for restoration on cancel)
    private int originalChunkSize;
    private int originalSendInterval;
    private int originalReceiveInterval;
    private String originalLogLevel;
    private String originalDownloadPath;
    
    /**
     * Constructor
     */
    public ConfigWindow(Config config) {
        this.config = config;
        
        saveOriginalValues();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        loadCurrentValues();
        
        logger.info("Configuration window initialization completed");
    }
    
    /**
     * Save original configuration values
     */
    private void saveOriginalValues() {
        originalChunkSize = config.getChunkSize();
        originalSendInterval = config.getSendInterval();
        originalReceiveInterval = config.getReceiveInterval();
        originalLogLevel = config.getLogLevel();
        originalDownloadPath = config.getDownloadPath();
    }
    
    /**
     * Initialize components
     */
    private void initializeComponents() {
        setTitle("ClipTransfer - Configuration Settings");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);
        setResizable(false);
        
        // Chunk size setting
        chunkSizeSpinner = new JSpinner(new SpinnerNumberModel(
            Config.DEFAULT_CHUNK_SIZE / 1024, // Default value (KB)
            1,     // Minimum value (1KB)
            10240, // Maximum value (10MB)
            128    // Step size (128KB)
        ));
        
        // Send interval setting
        sendIntervalSpinner = new JSpinner(new SpinnerNumberModel(
            Config.DEFAULT_SEND_INTERVAL, // Default value (milliseconds)
            100,   // Minimum value
            60000, // Maximum value (60 seconds)
            100    // Step size
        ));
        
        // Receive interval setting
        receiveIntervalSpinner = new JSpinner(new SpinnerNumberModel(
            Config.DEFAULT_RECEIVE_INTERVAL, // Default value (milliseconds)
            100,   // Minimum value
            10000, // Maximum value (10 seconds)
            100    // Step size
        ));
        
        // Log level setting
        String[] logLevels = {"SEVERE", "WARNING", "INFO", "FINE", "FINER", "FINEST"};
        logLevelComboBox = new JComboBox<>(logLevels);
        
        // Download path setting
        downloadPathField = new JTextField();
        browsePathButton = new JButton("Browse...");
        
        // Control buttons
        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
        resetButton = new JButton("Reset to Default");
    }
    
    /**
     * Setup layout
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Main configuration panel
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(new TitledBorder("Configuration Parameters"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Chunk size
        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(new JLabel("Chunk Size (KB):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        configPanel.add(chunkSizeSpinner, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        configPanel.add(new JLabel("Range: 1-10240 KB"), gbc);
        
        // Send interval
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        configPanel.add(new JLabel("Send Interval (ms):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        configPanel.add(sendIntervalSpinner, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        configPanel.add(new JLabel("Range: 100-60000 ms"), gbc);
        
        // Receive interval
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        configPanel.add(new JLabel("Receive Interval (ms):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        configPanel.add(receiveIntervalSpinner, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        configPanel.add(new JLabel("Range: 100-10000 ms"), gbc);
        
        // Log level
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        configPanel.add(new JLabel("Log Level:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        configPanel.add(logLevelComboBox, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        configPanel.add(new JLabel("INFO recommended for daily use"), gbc);
        
        // Download path
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        configPanel.add(new JLabel("Download Path:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        configPanel.add(downloadPathField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        configPanel.add(browsePathButton, gbc);
        
        // Description panel
        JPanel descPanel = new JPanel(new BorderLayout());
        descPanel.setBorder(new TitledBorder("Configuration Description"));
        
        JTextArea descArea = new JTextArea();
        descArea.setEditable(false);
        descArea.setBackground(getBackground());
        descArea.setText(
            "• Chunk Size: Size of individual data chunks, affects transfer efficiency and clipboard usage\n" +
            "• Send Interval: Interval time for sender to send data chunks\n" +
            "• Receive Interval: Interval time for receiver to poll clipboard\n" +
            "• Log Level: Controls the verbosity of log output\n" +
            "• Download Path: Save location for received files"
        );
        descPanel.add(descArea, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(resetButton);
        
        // Assemble main panel
        add(configPanel, BorderLayout.NORTH);
        add(descPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Set margins
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        // Browse path button
        browsePathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browsePath();
            }
        });
        
        // Save button
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveConfiguration();
            }
        });
        
        // Cancel button
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelConfiguration();
            }
        });
        
        // Reset button
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetToDefaults();
            }
        });
    }
    
    /**
     * Load current configuration values
     */
    private void loadCurrentValues() {
        chunkSizeSpinner.setValue(config.getChunkSize() / 1024); // Convert to KB
        sendIntervalSpinner.setValue(config.getSendInterval());
        receiveIntervalSpinner.setValue(config.getReceiveInterval());
        logLevelComboBox.setSelectedItem(config.getLogLevel());
        downloadPathField.setText(config.getDownloadPath());
    }
    
    /**
     * Browse download path
     */
    private void browsePath() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Download Directory");
        
        // Set current directory
        String currentPath = downloadPathField.getText();
        if (!currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                fileChooser.setCurrentDirectory(currentDir);
            }
        }
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            downloadPathField.setText(selectedDir.getAbsolutePath());
        }
    }
    
    /**
     * Save configuration
     */
    private void saveConfiguration() {
        try {
            // Validate input values
            int chunkSize = (Integer) chunkSizeSpinner.getValue() * 1024; // Convert to bytes
            int sendInterval = (Integer) sendIntervalSpinner.getValue();
            int receiveInterval = (Integer) receiveIntervalSpinner.getValue();
            String logLevel = (String) logLevelComboBox.getSelectedItem();
            String downloadPath = downloadPathField.getText().trim();
            
            // Validate download path
            if (downloadPath.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Download path cannot be empty", "Configuration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            File downloadDir = new File(downloadPath);
            if (!downloadDir.exists()) {
                int result = JOptionPane.showConfirmDialog(
                    this,
                    "Download directory does not exist, create it?\n" + downloadPath,
                    "Create Directory",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (result == JOptionPane.YES_OPTION) {
                    if (!FileUtil.ensureDirectory(downloadPath)) {
                        JOptionPane.showMessageDialog(this, "Failed to create download directory", "Configuration Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else {
                    return;
                }
            }
            
            // Apply configuration
            config.setChunkSize(chunkSize);
            config.setSendInterval(sendInterval);
            config.setReceiveInterval(receiveInterval);
            config.setLogLevel(logLevel);
            config.setDownloadPath(downloadPath);
            
            // Save configuration
            config.saveConfig();
            
            JOptionPane.showMessageDialog(
                this,
                "Configuration saved successfully!\nSome changes may require application restart to take effect.",
                "Save Successful",
                JOptionPane.INFORMATION_MESSAGE
            );
            
            setVisible(false);
            logger.info("Configuration saved successfully");
            
        } catch (Exception e) {
            logger.severe("Failed to save configuration: " + e.getMessage());
            JOptionPane.showMessageDialog(
                this,
                "Failed to save configuration: " + e.getMessage(),
                "Save Failed",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    /**
     * Cancel configuration
     */
    private void cancelConfiguration() {
        // Restore original values
        chunkSizeSpinner.setValue(originalChunkSize / 1024);
        sendIntervalSpinner.setValue(originalSendInterval);
        receiveIntervalSpinner.setValue(originalReceiveInterval);
        logLevelComboBox.setSelectedItem(originalLogLevel);
        downloadPathField.setText(originalDownloadPath);
        
        setVisible(false);
        logger.info("Configuration changes cancelled");
    }
    
    /**
     * Reset to default values
     */
    private void resetToDefaults() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to reset all configurations to default values?",
            "Confirm Reset",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            chunkSizeSpinner.setValue(Config.DEFAULT_CHUNK_SIZE / 1024);
            sendIntervalSpinner.setValue(Config.DEFAULT_SEND_INTERVAL);
            receiveIntervalSpinner.setValue(Config.DEFAULT_RECEIVE_INTERVAL);
            logLevelComboBox.setSelectedItem(Config.DEFAULT_LOG_LEVEL);
            downloadPathField.setText(Config.DEFAULT_DOWNLOAD_PATH);
            
            logger.info("Configuration reset to default values");
        }
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            // Reload current configuration when showing
            saveOriginalValues();
            loadCurrentValues();
        }
        super.setVisible(visible);
    }
}
