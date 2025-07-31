package com.cliptransfer.receiver;

import com.cliptransfer.common.TransferTask;

import java.io.File;
import java.util.List;

/**
 * 接收端事件监听器接口
 * 定义接收过程中的各种事件回调
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public interface ReceiverEventListener {
    
    /**
     * 开始监听事件
     */
    void onListeningStarted();
    
    /**
     * 停止监听事件
     */
    void onListeningStopped();
    
    /**
     * 任务开始事件
     * @param task 传输任务
     */
    void onTaskStarted(TransferTask task);
    
    /**
     * 进度更新事件
     * @param task 传输任务
     * @param completedChunks 已完成块数
     * @param totalChunks 总块数
     */
    void onProgress(TransferTask task, int completedChunks, int totalChunks);
    
    /**
     * 任务完成事件
     * @param task 传输任务
     * @param outputFile 输出文件
     */
    void onTaskCompleted(TransferTask task, File outputFile);
    
    /**
     * 任务失败事件
     * @param task 传输任务
     * @param error 错误信息
     */
    void onTaskFailed(TransferTask task, String error);
    
    /**
     * 任务不完整事件（收到END包但仍有缺失块）
     * @param task 传输任务
     * @param missingChunks 缺失的块索引列表
     */
    void onTaskIncomplete(TransferTask task, List<Integer> missingChunks);
    
    /**
     * 错误事件
     * @param error 错误信息
     */
    void onError(String error);
}