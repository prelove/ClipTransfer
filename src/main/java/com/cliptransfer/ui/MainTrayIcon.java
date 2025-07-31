package com.cliptransfer.ui;

import com.cliptransfer.common.Config;
import com.cliptransfer.common.TaskManager;
import com.cliptransfer.receiver.ReceiverService;
import com.cliptransfer.sender.SenderService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * Main tray icon class
 * Provides system tray interface and menu functionality
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class MainTrayIcon {
    private static final Logger logger = Logger.getLogger(MainTrayIcon.class.getName());
    
    private final Config config;
    private final TaskManager taskManager;
    private final SenderService senderService;
    private final ReceiverService receiverService;
    
    private TrayIcon trayIcon;
    private SystemTray systemTray;
    
    // UI Windows
    private SenderWindow senderWindow;
    private ReceiverWindow receiverWindow;
    private ConfigWindow configWindow;
    private TaskListWindow taskListWindow;
    
    /**
     * Constructor
     */
    public MainTrayIcon(Config config, TaskManager taskManager) {
        this.config = config;
        this.taskManager = taskManager;
        this.senderService = new SenderService(config, taskManager);
        this.receiverService = new ReceiverService(config, taskManager);
        
        initializeServices();
        createTrayIcon();
        createWindows();
        
        logger.info("Main tray icon initialization completed");
    }
    
    /**
     * Initialize services
     */
    private void initializeServices() {
        // Set sender event listener
        senderService.setEventListener(new SenderEventListenerImpl());
        
        // Set receiver event listener
        receiverService.setEventListener(new ReceiverEventListenerImpl());
        
        // Start receiver service by default
        receiverService.startListening();
        
        logger.info("Core services initialization completed");
    }
    
    /**
     * Create tray icon
     */
    private void createTrayIcon() {
        systemTray = SystemTray.getSystemTray();
        
        // Create tray icon image
        Image trayImage = createTrayImage();
        
        // Create popup menu
        PopupMenu popupMenu = createPopupMenu();
        
        // Create tray icon
        trayIcon = new TrayIcon(trayImage, "ClipTransfer - Clipboard File Transfer Tool", popupMenu);
        trayIcon.setImageAutoSize(true);
        
        // Set double-click event
        trayIcon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showMainMenu();
            }
        });
        
        logger.info("Tray icon creation completed");
    }
    
    /**
     * Create tray icon image
     */
    private Image createTrayImage() {
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Set antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw background circle
        g2d.setColor(new Color(0, 123, 255));
        g2d.fillOval(0, 0, size, size);
        
        // Draw letter "C"
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "C";
        int x = (size - fm.stringWidth(text)) / 2;
        int y = (size + fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(text, x, y);
        
        g2d.dispose();
        return image;
    }
    
    /**
     * Create popup menu
     */
    private PopupMenu createPopupMenu() {
        PopupMenu menu = new PopupMenu();
        
        // Send Files
        MenuItem sendFileItem = new MenuItem("Send Files/Folders");
        sendFileItem.addActionListener(e -> showSenderWindow());
        menu.add(sendFileItem);
        
        // Receive Management
        MenuItem receiveItem = new MenuItem("Receive Management");
        receiveItem.addActionListener(e -> showReceiverWindow());
        menu.add(receiveItem);
        
        menu.addSeparator();
        
        // Task List
        MenuItem taskListItem = new MenuItem("Task List");
        taskListItem.addActionListener(e -> showTaskListWindow());
        menu.add(taskListItem);
        
        // Configuration Settings
        MenuItem configItem = new MenuItem("Configuration Settings");
        configItem.addActionListener(e -> showConfigWindow());
        menu.add(configItem);
        
        menu.addSeparator();
        
        // About
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        menu.add(aboutItem);
        
        // Exit
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> exitApplication());
        menu.add(exitItem);
        
        return menu;
    }
    
    /**
     * Create UI windows
     */
    private void createWindows() {
        // Create sender window
        senderWindow = new SenderWindow(config, senderService);
        
        // Create receiver window
        receiverWindow = new ReceiverWindow(config, receiverService);
        
        // Create configuration window
        configWindow = new ConfigWindow(config);
        
        // Create task list window
        taskListWindow = new TaskListWindow(taskManager);
        
        logger.info("UI windows creation completed");
    }
    
    /**
     * Show tray icon
     */
    public void show() {
        try {
            systemTray.add(trayIcon);
            showTrayMessage("ClipTransfer Started", "Double-click tray icon to open main menu", TrayIcon.MessageType.INFO);
            logger.info("Tray icon displayed successfully");
        } catch (AWTException e) {
            logger.severe("Failed to display tray icon: " + e.getMessage());
            throw new RuntimeException("Tray icon display failed", e);
        }
    }
    
    /**
     * Hide tray icon
     */
    public void hide() {
        if (trayIcon != null) {
            systemTray.remove(trayIcon);
            logger.info("Tray icon hidden");
        }
    }
    
    /**
     * Show tray message
     */
    public void showTrayMessage(String title, String message, TrayIcon.MessageType messageType) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, messageType);
        }
    }
    
    /**
     * Show main menu
     */
    private void showMainMenu() {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem sendItem = new JMenuItem("Send Files/Folders");
        sendItem.addActionListener(e -> showSenderWindow());
        menu.add(sendItem);
        
        JMenuItem receiveItem = new JMenuItem("Receive Management");
        receiveItem.addActionListener(e -> showReceiverWindow());
        menu.add(receiveItem);
        
        menu.addSeparator();
        
        JMenuItem taskItem = new JMenuItem("Task List");
        taskItem.addActionListener(e -> showTaskListWindow());
        menu.add(taskItem);
        
        JMenuItem configItem = new JMenuItem("Configuration Settings");
        configItem.addActionListener(e -> showConfigWindow());
        menu.add(configItem);
        
        // Get mouse position and show menu
        Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
        
        // Create a hidden frame to display popup menu
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setSize(1, 1);
        frame.setLocation(mouseLocation.x, mouseLocation.y);
        frame.setVisible(true);
        
        menu.show(frame, 0, 0);
        
        // Hide frame after menu closes
        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
            
            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                frame.dispose();
            }
            
            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                frame.dispose();
            }
        });
    }
    
    /**
     * Show sender window
     */
    private void showSenderWindow() {
        if (senderWindow != null) {
            senderWindow.setVisible(true);
            senderWindow.toFront();
        }
    }
    
    /**
     * Show receiver window
     */
    private void showReceiverWindow() {
        if (receiverWindow != null) {
            receiverWindow.setVisible(true);
            receiverWindow.toFront();
        }
    }
    
    /**
     * Show configuration window
     */
    private void showConfigWindow() {
        if (configWindow != null) {
            configWindow.setVisible(true);
            configWindow.toFront();
        }
    }
    
    /**
     * Show task list window
     */
    private void showTaskListWindow() {
        if (taskListWindow != null) {
            taskListWindow.refreshTasks();
            taskListWindow.setVisible(true);
            taskListWindow.toFront();
        }
    }
    
    /**
     * Show about dialog
     */
    private void showAboutDialog() {
        String message = "ClipTransfer v1.0.0\n\n" +
                        "Clipboard File Transfer Tool\n" +
                        "Suitable for remote desktop environments with only one-way clipboard sync\n\n" +
                        "Author: prelove\n" +
                        "Build Date: 2025-07-18\n\n" +
                        "Supported Features:\n" +
                        "• File and folder transfer\n" +
                        "• Resume transfer\n" +
                        "• MD5 verification\n" +
                        "• Progress display\n" +
                        "• Task management";
        
        JOptionPane.showMessageDialog(
            null,
            message,
            "About ClipTransfer",
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    /**
     * Exit application
     */
    private void exitApplication() {
        int result = JOptionPane.showConfirmDialog(
            null,
            "Are you sure you want to exit ClipTransfer?",
            "Confirm Exit",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            logger.info("User confirmed application exit");
            
            // Shutdown services
            senderService.shutdown();
            receiverService.shutdown();
            
            // Hide tray icon
            hide();
            
            // Exit program
            System.exit(0);
        }
    }
    
    /**
     * Sender event listener implementation
     */
    private class SenderEventListenerImpl implements com.cliptransfer.sender.SenderEventListener {
        @Override
        public void onTaskStarted(com.cliptransfer.common.TransferTask task) {
            SwingUtilities.invokeLater(() -> {
                showTrayMessage("Start Sending", "Sending: " + task.getFileName(), TrayIcon.MessageType.INFO);
                if (senderWindow != null && senderWindow.isVisible()) {
                    senderWindow.updateTaskStatus(task);
                }
            });
        }
        
        @Override
        public void onProgress(com.cliptransfer.common.TransferTask task, int completedChunks, int totalChunks) {
            SwingUtilities.invokeLater(() -> {
                if (senderWindow != null && senderWindow.isVisible()) {
                    senderWindow.updateProgress(task, completedChunks, totalChunks);
                }
            });
        }
        
        @Override
        public void onTaskCompleted(com.cliptransfer.common.TransferTask task) {
            SwingUtilities.invokeLater(() -> {
                showTrayMessage("Send Completed", task.getFileName() + " sent successfully", TrayIcon.MessageType.INFO);
                if (senderWindow != null && senderWindow.isVisible()) {
                    senderWindow.updateTaskStatus(task);
                }
            });
        }
        
        @Override
        public void onTaskFailed(com.cliptransfer.common.TransferTask task, String error) {
            SwingUtilities.invokeLater(() -> {
                showTrayMessage("Send Failed", task.getFileName() + " send failed: " + error, TrayIcon.MessageType.ERROR);
                if (senderWindow != null && senderWindow.isVisible()) {
                    senderWindow.updateTaskStatus(task);
                }
            });
        }
        
        @Override
        public void onTaskPaused(com.cliptransfer.common.TransferTask task) {
            SwingUtilities.invokeLater(() -> {
                if (senderWindow != null && senderWindow.isVisible()) {
                    senderWindow.updateTaskStatus(task);
                }
            });
        }
        
        @Override
        public void onTaskResumed(com.cliptransfer.common.TransferTask task) {
            SwingUtilities.invokeLater(() -> {
                if (senderWindow != null && senderWindow.isVisible()) {
                    senderWindow.updateTaskStatus(task);
                }
            });
        }
        
        @Override
        public void onTaskCancelled(com.cliptransfer.common.TransferTask task) {
            SwingUtilities.invokeLater(() -> {
                showTrayMessage("Task Cancelled", task.getFileName() + " cancelled", TrayIcon.MessageType.WARNING);
                if (senderWindow != null && senderWindow.isVisible()) {
                    senderWindow.updateTaskStatus(task);
                }
            });
        }
        
        @Override
        public void onError(String error) {
            SwingUtilities.invokeLater(() -> {
                showTrayMessage("Send Error", error, TrayIcon.MessageType.ERROR);
                logger.warning("Sender error: " + error);
            });
        }
    }
    
    /**
     * Receiver event listener implementation
     */
    private class ReceiverEventListenerImpl implements com.cliptransfer.receiver.ReceiverEventListener {
        @Override
        public void onListeningStarted() {
            SwingUtilities.invokeLater(() -> {
                if (receiverWindow != null && receiverWindow.isVisible()) {
                    receiverWindow.updateListeningStatus(true);
                }
            });
        }
        
        @Override
        public void onListeningStopped() {
            SwingUtilities.invokeLater(() -> {
                if (receiverWindow != null && receiverWindow.isVisible()) {
                    receiverWindow.updateListeningStatus(false);
                }
            });
        }
        
        @Override
        public void onTaskStarted(com.cliptransfer.common.TransferTask task) {
            SwingUtilities.invokeLater(() -> {
                showTrayMessage("Start Receiving", "Receiving: " + task.getFileName(), TrayIcon.MessageType.INFO);
                if (receiverWindow != null && receiverWindow.isVisible()) {
                    receiverWindow.addReceivingTask(task);
                }
            });
        }
        
        @Override
        public void onProgress(com.cliptransfer.common.TransferTask task, int completedChunks, int totalChunks) {
            SwingUtilities.invokeLater(() -> {
                if (receiverWindow != null && receiverWindow.isVisible()) {
                    receiverWindow.updateProgress(task, completedChunks, totalChunks);
                }
            });
        }
        
        @Override
        public void onTaskCompleted(com.cliptransfer.common.TransferTask task, java.io.File outputFile) {
            SwingUtilities.invokeLater(() -> {
                showTrayMessage("Receive Completed", task.getFileName() + " received successfully\nSaved to: " + outputFile.getParent(), TrayIcon.MessageType.INFO);
                if (receiverWindow != null && receiverWindow.isVisible()) {
                    receiverWindow.updateTaskStatus(task);
                }
            });
        }
        
        @Override
        public void onTaskFailed(com.cliptransfer.common.TransferTask task, String error) {
            SwingUtilities.invokeLater(() -> {
                showTrayMessage("Receive Failed", task.getFileName() + " receive failed: " + error, TrayIcon.MessageType.ERROR);
                if (receiverWindow != null && receiverWindow.isVisible()) {
                    receiverWindow.updateTaskStatus(task);
                }
            });
        }
        
        @Override
        public void onTaskIncomplete(com.cliptransfer.common.TransferTask task, java.util.List<Integer> missingChunks) {
            SwingUtilities.invokeLater(() -> {
                showTrayMessage("Receive Incomplete", task.getFileName() + " missing " + missingChunks.size() + " chunks", TrayIcon.MessageType.WARNING);
                if (receiverWindow != null && receiverWindow.isVisible()) {
                    receiverWindow.showMissingChunks(task, missingChunks);
                }
            });
        }
        
        @Override
        public void onError(String error) {
            SwingUtilities.invokeLater(() -> {
                showTrayMessage("Receive Error", error, TrayIcon.MessageType.ERROR);
                logger.warning("Receiver error: " + error);
            });
        }
    }
}