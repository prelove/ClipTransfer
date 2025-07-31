package com.cliptransfer.protocol;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * 文件清单条目
 * 用于记录文件夹中每个文件的路径和时间戳信息
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class FileManifestEntry {
    private static final Logger logger = Logger.getLogger(FileManifestEntry.class.getName());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    
    private String path;           // 文件相对路径
    private long modificationTime; // 修改时间（毫秒时间戳）
    
    /**
     * 构造函数
     */
    public FileManifestEntry() {
    }
    
    /**
     * 构造函数
     * @param path 文件相对路径
     * @param modificationTime 修改时间（毫秒时间戳）
     */
    public FileManifestEntry(String path, long modificationTime) {
        this.path = path;
        this.modificationTime = modificationTime;
    }
    
    /**
     * 从JSON对象创建条目
     */
    public static FileManifestEntry fromJson(JSONObject json) {
        try {
            FileManifestEntry entry = new FileManifestEntry();
            entry.path = json.getString("path");
            
            // 尝试解析时间戳（支持字符串和长整型）
            if (json.has("mod_time")) {
                Object modTime = json.get("mod_time");
                if (modTime instanceof String) {
                    // ISO格式字符串
                    entry.modificationTime = DATE_FORMAT.parse((String) modTime).getTime();
                } else if (modTime instanceof Number) {
                    // 时间戳
                    entry.modificationTime = ((Number) modTime).longValue();
                } else {
                    logger.warning("无法解析修改时间，使用当前时间: " + modTime);
                    entry.modificationTime = System.currentTimeMillis();
                }
            } else {
                entry.modificationTime = System.currentTimeMillis();
            }
            
            return entry;
        } catch (Exception e) {
            logger.severe("从JSON创建文件清单条目失败: " + e.getMessage());
            throw new RuntimeException("解析文件清单条目失败", e);
        }
    }
    
    /**
     * 转换为JSON对象
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("path", path);
        json.put("mod_time", DATE_FORMAT.format(new Date(modificationTime)));
        return json;
    }
    
    // Getter和Setter方法
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public long getModificationTime() {
        return modificationTime;
    }
    
    public void setModificationTime(long modificationTime) {
        this.modificationTime = modificationTime;
    }
    
    /**
     * 获取格式化的修改时间字符串
     */
    public String getFormattedModificationTime() {
        return DATE_FORMAT.format(new Date(modificationTime));
    }
    
    @Override
    public String toString() {
        return String.format("FileManifestEntry{path='%s', modTime='%s'}", 
                path, getFormattedModificationTime());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        FileManifestEntry that = (FileManifestEntry) obj;
        return modificationTime == that.modificationTime && 
               (path != null ? path.equals(that.path) : that.path == null);
    }
    
    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (int) (modificationTime ^ (modificationTime >>> 32));
        return result;
    }
}