package com.liheit.im.core;

import com.liheit.im.core.protocol.Pkg;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

class ProtocolEncode extends MessageToMessageEncoder<Pkg> {
    /*@Override
    protected void encode(ChannelHandlerContext ctx, Pkg msg, ByteBuf out) throws Exception {
        out.writeByte(msg.getBeginTag());
        out.writeByte(msg.getType());
        out.writeShort(msg.getSn());
        out.writeShort(msg.getCmd());
        out.writeShort(msg.getSize());
        out.writeBytes(msg.getBody());
        out.writeByte(msg.getCrc());
        out.writeByte(msg.getEntTag());
    }*/
    @Override
    protected void encode(ChannelHandlerContext ctx, Pkg msg, List<Object> out) throws Exception {

        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(msg.getPackageSize());
        buf.writeByte(((byte) msg.getBeginTag()));
        buf.writeByte(((byte) msg.getType()));
        buf.writeShort(((short) msg.getSn()));
        buf.writeShort(((short) msg.getCmd()));
        buf.writeShort(((short) msg.getSize()));
        buf.writeBytes(msg.getBody());
        buf.writeByte(((byte) msg.getCrc()));
        buf.writeByte(((byte) msg.getEntTag()));
//        System.out.println("send pkg:"+Arrays.toString(buf.copy().array()));
        out.add(buf);
    }
    /*@Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {

        Pkg p = new Pkg();
        assert Pkg.BEGIN_TAG == msg.readUnsignedByte();

        p.setType(msg.readUnsignedByte());
        p.setSn(msg.readUnsignedShort());
        p.setCmd(msg.readUnsignedShort());
        p.setSize(msg.readUnsignedShort());
        p.setBody(msg.readBytes(p.getSize()).array());
        p.setCrc(msg.readUnsignedByte());

        assert Pkg.END_TAG == msg.readUnsignedByte();
        out.add(p);
    }*/
}
