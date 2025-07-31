package com.cliptransfer.protocol;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * 协议包基类
 * 所有协议包的基础类，提供公共功能
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public abstract class Packet {
    private static final Logger logger = Logger.getLogger(Packet.class.getName());
    protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    
    protected PacketType type;    // 包类型
    protected String fileId;      // 文件唯一标识
    protected long timestamp;     // 时间戳
    
    /**
     * 构造函数
     */
    public Packet(PacketType type, String fileId) {
        this.type = type;
        this.fileId = fileId;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 默认构造函数（用于反序列化）
     */
    protected Packet() {
    }
    
    /**
     * 序列化为JSON字符串
     */
    public String toJsonString() {
        try {
            JSONObject json = toJson();
            String jsonString = json.toString();
            logger.fine("序列化包: " + type + ", fileId: " + fileId + ", 大小: " + jsonString.length());
            return jsonString;
        } catch (Exception e) {
            logger.severe("序列化包失败: " + e.getMessage());
            throw new RuntimeException("包序列化失败", e);
        }
    }
    
    /**
     * 从JSON字符串反序列化
     */
    public static Packet fromJsonString(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            String typeStr = json.getString("type");
            PacketType packetType = PacketType.fromValue(typeStr);
            
            Packet packet;
            switch (packetType) {
                case START:
                    packet = StartPacket.fromJson(json);
                    break;
                case CHUNK:
                    packet = ChunkPacket.fromJson(json);
                    break;
                case END:
                    packet = EndPacket.fromJson(json);
                    break;
                default:
                    throw new IllegalArgumentException("未支持的包类型: " + typeStr);
            }
            
            logger.fine("反序列化包: " + packetType + ", fileId: " + packet.getFileId());
            return packet;
        } catch (Exception e) {
            logger.severe("反序列化包失败: " + e.getMessage());
            throw new RuntimeException("包反序列化失败", e);
        }
    }
    
    /**
     * 转换为JSON对象（子类实现）
     */
    protected abstract JSONObject toJson();
    
    /**
     * 验证包的有效性
     */
    public boolean isValid() {
        return type != null && fileId != null && !fileId.trim().isEmpty();
    }
    
    // Getter和Setter方法
    
    public PacketType getType() {
        return type;
    }
    
    public void setType(PacketType type) {
        this.type = type;
    }
    
    public String getFileId() {
        return fileId;
    }
    
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * 获取格式化的时间戳字符串
     */
    public String getFormattedTimestamp() {
        return DATE_FORMAT.format(new Date(timestamp));
    }
    
    @Override
    public String toString() {
        return String.format("%s{type=%s, fileId='%s', timestamp='%s'}", 
                getClass().getSimpleName(), type, fileId, getFormattedTimestamp());
    }
}