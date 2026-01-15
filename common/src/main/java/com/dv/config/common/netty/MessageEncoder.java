package com.dv.config.common.netty;

import com.dv.config.common.JsonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 消息编码器:长度前缀法（4字节长度 + 消息字节数组）
 */
public class MessageEncoder extends MessageToByteEncoder<Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        // 1. 将对象序列化为字节数组 (使用统一的 JsonUtil)
        byte[] msgBytes = JsonUtil.toBytes(msg);
        // 2. 写入4字节的消息长度（大端序，Netty默认）
        out.writeInt(msgBytes.length);
        // 3. 写入消息字节数组
        out.writeBytes(msgBytes);
    }
}