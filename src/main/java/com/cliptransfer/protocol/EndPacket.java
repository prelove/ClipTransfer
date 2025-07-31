package com.cliptransfer.protocol;

import org.json.JSONObject;

import java.text.ParseException;
import java.util.logging.Logger;

/**
 * 文件传输结束包
 * 标识文件传输完成
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public class EndPacket extends Packet {
    private static final Logger logger = Logger.getLogger(EndPacket.class.getName());
    
    private String fileName;    // 文件名
    private int chunkTotal;     // 总块数
    
    /**
     * 构造函数
     */
    public EndPacket(String fileId, String fileName, int chunkTotal) {
        super(PacketType.END, fileId);
        this.fileName = fileName;
        this.chunkTotal = chunkTotal;
        
        logger.info("创建END包: " + fileName + ", 总块数: " + chunkTotal);
    }
    
    /**
     * 默认构造函数（用于反序列化）
     */
    public EndPacket() {
        super();
    }
    
    /**
     * 从JSON对象创建END包
     */
    public static EndPacket fromJson(JSONObject json) {
        try {
            EndPacket packet = new EndPacket();
            
            // 基础字段
            packet.type = PacketType.END;
            packet.fileId = json.getString("file_id");
            packet.fileName = json.getString("file_name");
            packet.chunkTotal = json.getInt("chunk_total");
            
            // 时间戳
            if (json.has("end_time")) {
                try {
                    packet.timestamp = DATE_FORMAT.parse(json.getString("end_time")).getTime();
                } catch (ParseException e) {
                    logger.warning("解析结束时间失败，使用当前时间: " + e.getMessage());
                    packet.timestamp = System.currentTimeMillis();
                }
            } else {
                packet.timestamp = System.currentTimeMillis();
            }
            
            logger.info("从JSON创建END包成功: " + packet.fileName);
            return packet;
            
        } catch (Exception e) {
            logger.severe("从JSON创建END包失败: " + e.getMessage());
            throw new RuntimeException("END包反序列化失败", e);
        }
    }
    
    @Override
    protected JSONObject toJson() {
        JSONObject json = new JSONObject();
        
        json.put("type", type.getValue());
        json.put("file_id", fileId);
        json.put("file_name", fileName);
        json.put("chunk_total", chunkTotal);
        json.put("end_time", getFormattedTimestamp());
        
        return json;
    }
    
    @Override
    public boolean isValid() {
        return super.isValid() && 
               fileName != null && !fileName.trim().isEmpty() &&
               chunkTotal > 0;
    }
    
    // Getter和Setter方法
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public int getChunkTotal() {
        return chunkTotal;
    }
    
    public void setChunkTotal(int chunkTotal) {
        this.chunkTotal = chunkTotal;
    }
    
    @Override
    public String toString() {
        return String.format("EndPacket{fileId='%s', fileName='%s', chunkTotal=%d}", 
                fileId, fileName, chunkTotal);
    }
}