package com.cliptransfer.protocol;

/**
 * 协议包类型枚举
 * 定义传输过程中的各种包类型
 * 
 * @author dts
 * @version 1.0.0
 * @since 2025-07-18
 */
public enum PacketType {
    /**
     * 文件传输开始包
     */
    START("START"),
    
    /**
     * 数据块包
     */
    CHUNK("CHUNK"),
    
    /**
     * 文件传输结束包
     */
    END("END");
    
    private final String value;
    
    PacketType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * 根据字符串值获取包类型
     */
    public static PacketType fromValue(String value) {
        for (PacketType type : PacketType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的包类型: " + value);
    }
}