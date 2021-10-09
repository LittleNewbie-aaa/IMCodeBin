package com.liheit.im.core;

import com.liheit.im.core.protocol.Pkg;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

class ProtocolDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
//        System.out.println("rec pkg:" + Arrays.toString(msg.copy().array()));
        Pkg p = new Pkg();

        short begin = msg.readUnsignedByte();
//        assert (Pkg.BEGIN_TAG == begin);
        p.setType(msg.readUnsignedByte());

        p.setSn(msg.readShort());
        p.setCmd(msg.readShort());
        int size = msg.readUnsignedShort();
        p.setSize(size);
        p.setBody(msg.readBytes(size).array());
        p.setCrc(msg.readUnsignedByte());

//        assert Pkg.END_TAG == msg.readUnsignedByte();
        msg.readUnsignedByte();
        out.add(p);
    }
}
