package com.cliptransfer.common;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * MD5工具类
 * 提供文件和字节数组的MD5计算功能
 * 支持大文件流式计算，避免内存溢出
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class MD5Util {
    private static final Logger logger = Logger.getLogger(MD5Util.class.getName());
    private static final int BUFFER_SIZE = 8192; // 8KB缓冲区
    
    /**
     * 计算字节数组的MD5值
     * @param data 字节数组
     * @return MD5字符串（32位小写）
     */
    public static String calculateMD5(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("数据不能为null");
        }
        
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] hash = md5.digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.severe("MD5算法不可用: " + e.getMessage());
            throw new RuntimeException("MD5计算失败", e);
        }
    }
    
    /**
     * 计算字符串的MD5值
     * @param text 字符串
     * @return MD5字符串（32位小写）
     */
    public static String calculateMD5(String text) {
        if (text == null) {
            throw new IllegalArgumentException("文本不能为null");
        }
        
        try {
            return calculateMD5(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            logger.severe("UTF-8编码不支持: " + e.getMessage());
            throw new RuntimeException("MD5计算失败", e);
        }
    }
    
    /**
     * 计算文件的MD5值（流式处理，适合大文件）
     * @param file 文件对象
     * @return MD5字符串（32位小写）
     */
    public static String calculateFileMD5(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在或不是有效文件: " + file);
        }
        
        logger.info("开始计算文件MD5: " + file.getName() + ", 大小: " + file.length() + " bytes");
        long startTime = System.currentTimeMillis();
        
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE)) {
            
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytes = 0;
            
            while ((bytesRead = bis.read(buffer)) != -1) {
                md5.update(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                
                // 每处理100MB输出一次进度日志
                if (totalBytes % (100 * 1024 * 1024) == 0) {
                    logger.info("MD5计算进度: " + (totalBytes / 1024 / 1024) + "MB");
                }
            }
            
            byte[] hash = md5.digest();
            String md5String = bytesToHex(hash);
            
            long endTime = System.currentTimeMillis();
            logger.info("文件MD5计算完成: " + file.getName() + ", MD5: " + md5String + 
                       ", 耗时: " + (endTime - startTime) + "ms");
            
            return md5String;
            
        } catch (IOException e) {
            logger.severe("读取文件失败: " + e.getMessage());
            throw new RuntimeException("文件MD5计算失败", e);
        } catch (NoSuchAlgorithmException e) {
            logger.severe("MD5算法不可用: " + e.getMessage());
            throw new RuntimeException("文件MD5计算失败", e);
        }
    }
    
    /**
     * 计算输入流的MD5值（流式处理）
     * @param inputStream 输入流
     * @return MD5字符串（32位小写）
     */
    public static String calculateStreamMD5(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("输入流不能为null");
        }
        
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, bytesRead);
            }
            
            byte[] hash = md5.digest();
            return bytesToHex(hash);
            
        } catch (IOException e) {
            logger.severe("读取输入流失败: " + e.getMessage());
            throw new RuntimeException("输入流MD5计算失败", e);
        } catch (NoSuchAlgorithmException e) {
            logger.severe("MD5算法不可用: " + e.getMessage());
            throw new RuntimeException("输入流MD5计算失败", e);
        }
    }
    
    /**
     * 验证文件MD5值
     * @param file 文件对象
     * @param expectedMD5 期望的MD5值
     * @return 是否匹配
     */
    public static boolean verifyFileMD5(File file, String expectedMD5) {
        if (expectedMD5 == null || expectedMD5.trim().isEmpty()) {
            throw new IllegalArgumentException("期望的MD5值不能为空");
        }
        
        String actualMD5 = calculateFileMD5(file);
        boolean isMatch = actualMD5.equalsIgnoreCase(expectedMD5.trim());
        
        logger.info("MD5验证 - 文件: " + file.getName() + 
                   ", 期望: " + expectedMD5 + 
                   ", 实际: " + actualMD5 + 
                   ", 结果: " + (isMatch ? "匹配" : "不匹配"));
        
        return isMatch;
    }
    
    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串（小写）
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}