package com.cliptransfer.common;

import com.cliptransfer.protocol.FileManifestEntry;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 文件工具类
 * 提供文件/文件夹操作功能，包括压缩、解压、分块读取等
 * 采用流式处理，避免大文件内存溢出
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class FileUtil {
    private static final Logger logger = Logger.getLogger(FileUtil.class.getName());
    private static final int BUFFER_SIZE = 8192; // 8KB缓冲区
    
    /**
     * 格式化文件大小
     * @param sizeInBytes 字节大小
     * @return 格式化的大小字符串
     */
    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < 1024 * 1024) {
            return String.format("%.2f KB", sizeInBytes / 1024.0);
        } else if (sizeInBytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", sizeInBytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", sizeInBytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 检查文件或目录是否存在且可读
     * @param path 文件路径
     * @return 是否有效
     */
    public static boolean isValidPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        
        File file = new File(path);
        return file.exists() && file.canRead();
    }
    
    /**
     * 创建目录（如果不存在）
     * @param dirPath 目录路径
     * @return 是否成功创建或已存在
     */
    public static boolean ensureDirectory(String dirPath) {
        if (dirPath == null || dirPath.trim().isEmpty()) {
            return false;
        }
        
        File dir = new File(dirPath);
        if (dir.exists()) {
            return dir.isDirectory();
        }
        
        boolean created = dir.mkdirs();
        if (created) {
            logger.info("创建目录成功: " + dirPath);
        } else {
            logger.warning("创建目录失败: " + dirPath);
        }
        
        return created;
    }
    
    /**
     * 将文件夹压缩为ZIP文件
     * @param sourceDir 源文件夹路径
     * @param zipFile 目标ZIP文件
     * @return 文件清单列表
     */
    public static List<FileManifestEntry> compressFolderToZip(File sourceDir, File zipFile) {
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new IllegalArgumentException("源目录不存在或不是目录: " + sourceDir);
        }
        
        logger.info("开始压缩文件夹: " + sourceDir.getName() + " -> " + zipFile.getName());
        List<FileManifestEntry> manifest = new ArrayList<>();
        
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            compressDirectoryRecursive(sourceDir, sourceDir, zos, manifest);
            
            logger.info("文件夹压缩完成: " + zipFile.getName() + 
                       ", 大小: " + formatFileSize(zipFile.length()) + 
                       ", 文件数: " + manifest.size());
            
            return manifest;
            
        } catch (IOException e) {
            logger.severe("压缩文件夹失败: " + e.getMessage());
            throw new RuntimeException("文件夹压缩失败", e);
        }
    }
    
    /**
     * 递归压缩目录
     */
    private static void compressDirectoryRecursive(File sourceDir, File baseDir, 
                                                  ZipOutputStream zos, List<FileManifestEntry> manifest) throws IOException {
        File[] files = sourceDir.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                // 递归处理子目录
                compressDirectoryRecursive(file, baseDir, zos, manifest);
            } else {
                // 压缩文件
                String relativePath = getRelativePath(baseDir, file);
                
                ZipEntry zipEntry = new ZipEntry(relativePath);
                zipEntry.setTime(file.lastModified());
                zos.putNextEntry(zipEntry);
                
                // 流式复制文件内容
                try (FileInputStream fis = new FileInputStream(file);
                     BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE)) {
                    
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                }
                
                zos.closeEntry();
                
                // 添加到清单
                manifest.add(new FileManifestEntry(relativePath, file.lastModified()));
                logger.fine("压缩文件: " + relativePath);
            }
        }
    }
    
    /**
     * 解压ZIP文件到指定目录
     * @param zipFile ZIP文件
     * @param destDir 目标目录
     * @param manifest 文件清单（用于恢复时间戳）
     */
    public static void extractZipToDirectory(File zipFile, File destDir, List<FileManifestEntry> manifest) {
        if (!zipFile.exists() || !zipFile.isFile()) {
            throw new IllegalArgumentException("ZIP文件不存在: " + zipFile);
        }
        
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        
        logger.info("开始解压ZIP文件: " + zipFile.getName() + " -> " + destDir.getName());
        
        try (FileInputStream fis = new FileInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis)) {
            
            ZipEntry entry;
            int fileCount = 0;
            
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    // 创建目录
                    File dir = new File(destDir, entry.getName());
                    dir.mkdirs();
                } else {
                    // 解压文件
                    File file = new File(destDir, entry.getName());
                    
                    // 确保父目录存在
                    file.getParentFile().mkdirs();
                    
                    // 流式复制文件内容
                    try (FileOutputStream fos = new FileOutputStream(file);
                         BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE)) {
                        
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            bos.write(buffer, 0, bytesRead);
                        }
                    }
                    
                    // 恢复文件时间戳
                    restoreFileTimestamp(file, entry, manifest);
                    
                    fileCount++;
                    logger.fine("解压文件: " + entry.getName());
                }
                zis.closeEntry();
            }
            
            logger.info("ZIP文件解压完成: " + fileCount + " 个文件");
            
        } catch (IOException e) {
            logger.severe("解压ZIP文件失败: " + e.getMessage());
            throw new RuntimeException("ZIP解压失败", e);
        }
    }
    
    /**
     * 恢复文件时间戳
     */
    private static void restoreFileTimestamp(File file, ZipEntry entry, List<FileManifestEntry> manifest) {
        try {
            // 优先使用清单中的时间戳
            if (manifest != null) {
                for (FileManifestEntry manifestEntry : manifest) {
                    if (manifestEntry.getPath().equals(entry.getName())) {
                        file.setLastModified(manifestEntry.getModificationTime());
                        return;
                    }
                }
            }
            
            // 使用ZIP条目中的时间戳
            if (entry.getTime() != -1) {
                file.setLastModified(entry.getTime());
            }
        } catch (Exception e) {
            logger.warning("恢复文件时间戳失败: " + file.getName() + ", " + e.getMessage());
        }
    }
    
    /**
     * 获取相对路径
     */
    private static String getRelativePath(File baseDir, File file) {
        String basePath = baseDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        
        if (filePath.startsWith(basePath)) {
            String relativePath = filePath.substring(basePath.length());
            // 去掉开头的路径分隔符
            if (relativePath.startsWith(File.separator)) {
                relativePath = relativePath.substring(1);
            }
            // 统一使用正斜杠作为路径分隔符（ZIP标准）
            return relativePath.replace(File.separator, "/");
        }
        
        return file.getName();
    }
    
    /**
     * 读取文件指定范围的字节
     * @param file 文件
     * @param offset 偏移量
     * @param length 读取长度
     * @return 字节数组
     */
    public static byte[] readFileChunk(File file, long offset, int length) {
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在: " + file);
        }
        
        if (offset < 0 || offset >= file.length()) {
            throw new IllegalArgumentException("偏移量超出文件范围: " + offset);
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(offset);
            
            // 计算实际需要读取的长度
            long remainingBytes = file.length() - offset;
            int actualLength = (int) Math.min(length, remainingBytes);
            
            byte[] buffer = new byte[actualLength];
            int bytesRead = raf.read(buffer);
            
            if (bytesRead != actualLength) {
                throw new IOException("读取字节数不匹配: 期望=" + actualLength + ", 实际=" + bytesRead);
            }
            
            return buffer;
            
        } catch (IOException e) {
            logger.severe("读取文件块失败: " + e.getMessage());
            throw new RuntimeException("文件块读取失败", e);
        }
    }
    
    /**
     * 生成唯一的临时文件名
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀
     * @param directory 目标目录
     * @return 唯一的文件对象
     */
    public static File createTempFile(String prefix, String suffix, File directory) {
        try {
            return File.createTempFile(prefix, suffix, directory);
        } catch (IOException e) {
            logger.severe("创建临时文件失败: " + e.getMessage());
            throw new RuntimeException("临时文件创建失败", e);
        }
    }
    
    /**
     * 删除文件或目录（递归删除）
     * @param file 文件或目录
     * @return 是否删除成功
     */
    public static boolean deleteRecursively(File file) {
        if (!file.exists()) {
            return true;
        }
        
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        
        boolean deleted = file.delete();
        if (deleted) {
            logger.fine("删除文件/目录: " + file.getName());
        } else {
            logger.warning("删除文件/目录失败: " + file.getName());
        }
        
        return deleted;
    }
}