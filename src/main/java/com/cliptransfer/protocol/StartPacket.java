package com.cliptransfer.protocol;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cliptransfer.common.TransferType;

/**
 * 文件传输开始包
 * 包含文件基本信息、分块信息和文件夹清单（如果是文件夹传输）
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class StartPacket extends Packet {
    private static final Logger logger = Logger.getLogger(StartPacket.class.getName());
    
    private String fileName;              // 文件名
    private TransferType transferType;    // 传输类型
    private long totalSize;               // 文件总大小
    private int chunkSize;                // 块大小
    private int chunkTotal;               // 总块数
    private String fileMd5;               // 文件MD5
    private List<FileManifestEntry> folderManifest; // 文件夹清单（可选）
    
    /**
     * 构造函数
     */
    public StartPacket(String fileId, String fileName, TransferType transferType, 
                      long totalSize, int chunkSize, int chunkTotal, String fileMd5) {
        super(PacketType.START, fileId);
        this.fileName = fileName;
        this.transferType = transferType;
        this.totalSize = totalSize;
        this.chunkSize = chunkSize;
        this.chunkTotal = chunkTotal;
        this.fileMd5 = fileMd5;
        this.folderManifest = new ArrayList<>();
        
        logger.info("创建START包: " + fileName + ", 大小: " + totalSize + " bytes, 块数: " + chunkTotal);
    }
    
    /**
     * 默认构造函数（用于反序列化）
     */
    public StartPacket() {
        super();
        this.folderManifest = new ArrayList<>();
    }
    
    /**
     * 从JSON对象创建START包
     */
    public static StartPacket fromJson(JSONObject json) {
        try {
            StartPacket packet = new StartPacket();
            
            // 基础字段
            packet.type = PacketType.START;
            packet.fileId = json.getString("file_id");
            packet.fileName = json.getString("file_name");
            packet.transferType = TransferType.fromValue(json.getString("transfer_type"));
            packet.totalSize = json.getLong("total_size");
            packet.chunkSize = json.getInt("chunk_size");
            packet.chunkTotal = json.getInt("chunk_total");
            packet.fileMd5 = json.getString("file_md5");
            
            // 时间戳
            if (json.has("start_time")) {
                try {
                    packet.timestamp = DATE_FORMAT.parse(json.getString("start_time")).getTime();
                } catch (ParseException e) {
                    logger.warning("解析开始时间失败，使用当前时间: " + e.getMessage());
                    packet.timestamp = System.currentTimeMillis();
                }
            } else {
                packet.timestamp = System.currentTimeMillis();
            }
            
            // 文件夹清单（可选）
            if (json.has("folder_manifest")) {
                JSONArray manifestArray = json.getJSONArray("folder_manifest");
                for (int i = 0; i < manifestArray.length(); i++) {
                    JSONObject entryJson = manifestArray.getJSONObject(i);
                    FileManifestEntry entry = FileManifestEntry.fromJson(entryJson);
                    packet.folderManifest.add(entry);
                }
                logger.info("加载文件夹清单，文件数: " + packet.folderManifest.size());
            }
            
            logger.info("从JSON创建START包成功: " + packet.fileName);
            return packet;
            
        } catch (Exception e) {
            logger.severe("从JSON创建START包失败: " + e.getMessage());
            throw new RuntimeException("START包反序列化失败", e);
        }
    }
    
    @Override
    protected JSONObject toJson() {
        JSONObject json = new JSONObject();
        
        // 基础字段
        json.put("type", type.getValue());
        json.put("file_id", fileId);
        json.put("file_name", fileName);
        json.put("transfer_type", transferType.getValue());
        json.put("total_size", totalSize);
        json.put("chunk_size", chunkSize);
        json.put("chunk_total", chunkTotal);
        json.put("file_md5", fileMd5);
        json.put("start_time", getFormattedTimestamp());
        
        // 文件夹清单（如果有）
        if (folderManifest != null && !folderManifest.isEmpty()) {
            JSONArray manifestArray = new JSONArray();
            for (FileManifestEntry entry : folderManifest) {
                manifestArray.put(entry.toJson());
            }
            json.put("folder_manifest", manifestArray);
        }
        
        return json;
    }
    
    @Override
    public boolean isValid() {
        return super.isValid() && 
               fileName != null && !fileName.trim().isEmpty() &&
               transferType != null &&
               totalSize > 0 &&
               chunkSize > 0 &&
               chunkTotal > 0 &&
               fileMd5 != null && !fileMd5.trim().isEmpty();
    }
    
    /**
     * 添加文件清单条目（用于文件夹传输）
     */
    public void addManifestEntry(String path, long modificationTime) {
        folderManifest.add(new FileManifestEntry(path, modificationTime));
        logger.fine("添加文件清单条目: " + path);
    }
    
    /**
     * 添加文件清单条目
     */
    public void addManifestEntry(FileManifestEntry entry) {
        folderManifest.add(entry);
        logger.fine("添加文件清单条目: " + entry.getPath());
    }
    
    // Getter和Setter方法
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public TransferType getTransferType() {
        return transferType;
    }
    
    public void setTransferType(TransferType transferType) {
        this.transferType = transferType;
    }
    
    public long getTotalSize() {
        return totalSize;
    }
    
    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }
    
    public int getChunkSize() {
        return chunkSize;
    }
    
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
    
    public int getChunkTotal() {
        return chunkTotal;
    }
    
    public void setChunkTotal(int chunkTotal) {
        this.chunkTotal = chunkTotal;
    }
    
    public String getFileMd5() {
        return fileMd5;
    }
    
    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }
    
    public List<FileManifestEntry> getFolderManifest() {
        return folderManifest;
    }
    
    public void setFolderManifest(List<FileManifestEntry> folderManifest) {
        this.folderManifest = folderManifest != null ? folderManifest : new ArrayList<>();
    }
    
    @Override
    public String toString() {
        return String.format("StartPacket{fileId='%s', fileName='%s', type=%s, size=%d, chunks=%d, md5='%s'}", 
                fileId, fileName, transferType, totalSize, chunkTotal, fileMd5);
    }
}