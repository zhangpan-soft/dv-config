package com.dv.config.api.message;

import com.dv.config.api.json.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 消息解码器:先读长度，再读完整消息
 * 解决TCP粘包/拆包问题，正确移动readerIndex
 */
public class MessageDecoder extends ByteToMessageDecoder {

    // 最大消息长度（防止恶意攻击，根据业务调整）
    private static final int MAX_MESSAGE_LENGTH = 1024 * 1024; // 1MB

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 1. 判断是否有至少4个字节（长度字段）
        if (in.readableBytes() < 4) {
            return; // 字节不足，等待后续数据
        }

        // 2. 标记当前读索引，用于回滚
        in.markReaderIndex();

        // 3. 读取4字节的消息长度
        int msgLength = in.readInt();

        // 4. 校验消息长度
        if (msgLength <= 0 || msgLength > MAX_MESSAGE_LENGTH) {
            ctx.close(); // 非法长度，关闭连接
            return;
        }

        // 5. 判断是否有足够的消息字节
        if (in.readableBytes() < msgLength) {
            in.resetReaderIndex(); // 回滚读索引，等待后续数据
            return;
        }

        // 6. 读取完整的消息字节
        byte[] msgBytes = new byte[msgLength];
        in.readBytes(msgBytes);

        // 7. 反序列化为对象
        try {
            Message message = JsonUtil.fromJson(msgBytes, new TypeReference<>(){});
            out.add(message);
        } catch (Exception e) {
            ctx.fireExceptionCaught(e); // 抛给异常处理器
        }
    }
}