package com.cliptransfer.protocol;

import org.json.JSONObject;

import java.text.ParseException;
import java.util.logging.Logger;

/**
 * 数据块包
 * 包含文件的一个分块数据
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class ChunkPacket extends Packet {
    private static final Logger logger = Logger.getLogger(ChunkPacket.class.getName());
    
    private int chunkIndex;     // 块索引（从0开始）
    private int chunkTotal;     // 总块数
    private String chunkMd5;    // 块MD5校验值
    private String data;        // Base64编码的数据
    
    /**
     * 构造函数
     */
    public ChunkPacket(String fileId, int chunkIndex, int chunkTotal, String chunkMd5, String data) {
        super(PacketType.CHUNK, fileId);
        this.chunkIndex = chunkIndex;
        this.chunkTotal = chunkTotal;
        this.chunkMd5 = chunkMd5;
        this.data = data;
        
        logger.fine("创建CHUNK包: fileId=" + fileId + ", index=" + chunkIndex + "/" + chunkTotal + 
                   ", 数据大小=" + (data != null ? data.length() : 0));
    }
    
    /**
     * 默认构造函数（用于反序列化）
     */
    public ChunkPacket() {
        super();
    }
    
    /**
     * 从JSON对象创建CHUNK包
     */
    public static ChunkPacket fromJson(JSONObject json) {
        try {
            ChunkPacket packet = new ChunkPacket();
            
            // 基础字段
            packet.type = PacketType.CHUNK;
            packet.fileId = json.getString("file_id");
            packet.chunkIndex = json.getInt("chunk_index");
            packet.chunkTotal = json.getInt("chunk_total");
            packet.chunkMd5 = json.getString("chunk_md5");
            packet.data = json.getString("data");
            
            // 时间戳
            if (json.has("send_time")) {
                try {
                    packet.timestamp = DATE_FORMAT.parse(json.getString("send_time")).getTime();
                } catch (ParseException e) {
                    logger.warning("解析发送时间失败，使用当前时间: " + e.getMessage());
                    packet.timestamp = System.currentTimeMillis();
                }
            } else {
                packet.timestamp = System.currentTimeMillis();
            }
            
            logger.fine("从JSON创建CHUNK包成功: index=" + packet.chunkIndex + "/" + packet.chunkTotal);
            return packet;
            
        } catch (Exception e) {
            logger.severe("从JSON创建CHUNK包失败: " + e.getMessage());
            throw new RuntimeException("CHUNK包反序列化失败", e);
        }
    }
    
    @Override
    protected JSONObject toJson() {
        JSONObject json = new JSONObject();
        
        json.put("type", type.getValue());
        json.put("file_id", fileId);
        json.put("chunk_index", chunkIndex);
        json.put("chunk_total", chunkTotal);
        json.put("chunk_md5", chunkMd5);
        json.put("data", data);
        json.put("send_time", getFormattedTimestamp());
        
        return json;
    }
    
    @Override
    public boolean isValid() {
        return super.isValid() && 
               chunkIndex >= 0 &&
               chunkTotal > 0 &&
               chunkIndex < chunkTotal &&
               chunkMd5 != null && !chunkMd5.trim().isEmpty() &&
               data != null && !data.trim().isEmpty();
    }
    
    /**
     * 获取数据大小（Base64编码后的大小）
     */
    public int getDataSize() {
        return data != null ? data.length() : 0;
    }
    
    /**
     * 获取原始数据大小（Base64解码后的大小，估算值）
     */
    public int getOriginalDataSize() {
        if (data == null) return 0;
        // Base64编码后大小约为原始大小的4/3
        return (int) (data.length() * 0.75);
    }
    
    // Getter和Setter方法
    
    public int getChunkIndex() {
        return chunkIndex;
    }
    
    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }
    
    public int getChunkTotal() {
        return chunkTotal;
    }
    
    public void setChunkTotal(int chunkTotal) {
        this.chunkTotal = chunkTotal;
    }
    
    public String getChunkMd5() {
        return chunkMd5;
    }
    
    public void setChunkMd5(String chunkMd5) {
        this.chunkMd5 = chunkMd5;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        return String.format("ChunkPacket{fileId='%s', index=%d/%d, md5='%s', dataSize=%d}", 
                fileId, chunkIndex, chunkTotal, chunkMd5, getDataSize());
    }
}