package com.cliptransfer.sender;

import com.cliptransfer.common.TransferTask;

/**
 * 发送端事件监听器接口
 * 定义发送过程中的各种事件回调
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public interface SenderEventListener {
    
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
     */
    void onTaskCompleted(TransferTask task);
    
    /**
     * 任务失败事件
     * @param task 传输任务
     * @param error 错误信息
     */
    void onTaskFailed(TransferTask task, String error);
    
    /**
     * 任务暂停事件
     * @param task 传输任务
     */
    void onTaskPaused(TransferTask task);
    
    /**
     * 任务恢复事件
     * @param task 传输任务
     */
    void onTaskResumed(TransferTask task);
    
    /**
     * 任务取消事件
     * @param task 传输任务
     */
    void onTaskCancelled(TransferTask task);
    
    /**
     * 错误事件
     * @param error 错误信息
     */
    void onError(String error);
}