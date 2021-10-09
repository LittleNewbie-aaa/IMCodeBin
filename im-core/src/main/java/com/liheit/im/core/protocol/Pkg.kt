package com.liheit.im.core.protocol

import com.liheit.im.utils.toUnsignedByte
import com.liheit.im.utils.toUnsignedShort
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled


data class Pkg(
        val beginTag: Short = BEGIN_TAG,
        var type: Short = 0,
        var sn: Int = 0,
        var cmd: Int = 0,
        var size: Int = 0,
        var body: ByteArray? = null,
        var crc: Short = 0,
        val entTag: Short = END_TAG
) {
    companion object {
        @JvmField
        val BEGIN_TAG: Short = 0xFB
        @JvmField
        val END_TAG: Short = 0xFE
        @JvmField
        val MAX_PACKAGE_LENGHT = 0xFFFF


        @JvmField
        val PACKETE_TYPE_GSON: Short = 0 //  0 －表示JSON字符串，以’\0’结尾；
        @JvmField
        val PACKETE_TYPE_URL: Short = 1 //  1 －表示url方式下载的JSON字符串，以’\0’结尾；
        @JvmField
        val PACKETE_TYPE_ZIP: Short = 2//


        @JvmStatic
        fun createPkg(sn: Int, cmd: Int, type: Short, byteArray: ByteArray): Pkg {
            var p = Pkg(cmd = cmd, type = type, sn = sn, body = byteArray, size = byteArray.size)
            p.calculateCrc()
            return p
        }
    }

    fun getPackageSize(): Int {
        return 10 + size
    }


    fun calculateCrc() {
        var nRet = 0;

        /*nRet *= 2
        nRet = nRet.toUnsignedByte()
        nRet += beginTag
        nRet = nRet.toUnsignedByte()

        nRet *= 2
        nRet = nRet.toUnsignedByte()
        nRet += type
        nRet = nRet.toUnsignedByte()

        nRet *= 2
        nRet = nRet.toUnsignedByte()
        nRet += sn
        nRet = nRet.toUnsignedByte()

        nRet *= 2
        nRet = nRet.toUnsignedByte()
        nRet += cmd
        nRet = nRet.toUnsignedByte()

        nRet *= 2
        nRet = nRet.toUnsignedByte()
        nRet += size
        nRet = nRet.toUnsignedByte()

        for (x in body!!) {
            nRet *= 2
            nRet = nRet.toUnsignedByte()
            nRet += x
            nRet = nRet.toUnsignedByte()
        }

        crc = nRet.toUnsignedByte().toShort()*/


        val buf =  Unpooled.buffer(getPackageSize() - 2)
        buf.writeByte(beginTag.toByte().toInt())
        buf.writeByte(type.toInt().toUnsignedByte())
        buf.writeShort(sn.toUnsignedShort())
        buf.writeShort(cmd.toUnsignedShort())
        buf.writeShort(size.toUnsignedShort())
        buf.writeBytes(body)
//        buf.writeByte(crc.toInt())
//        buf.writeByte(entTag.toInt())

//        println(Arrays.toString(buf.array()))
        for (x in buf.array()) {
            nRet *= 2
//            nRet=nRet.toUnsignedByte()
            nRet += x
//            nRet=nRet.toUnsignedByte()
        }

        crc = (nRet and 0xFF).toByte().toShort()
        buf.release()

        /*buf.forEachByteDesc() {
            nRet *= 2;
            nRet += pData[i];
        }*/

        /*crc=CRC8Util.calcCrc8(buf.array()).toShort()
        buf.release()*/


        /*for(int i=0; i<nSize; i++)
        {
            nRet *= 2;
            nRet += pData[i];
        }

        return nRet;*/
    }

    private fun getCRC(): Short {
        var nRet = 0
        val buf = ByteBufAllocator.DEFAULT.buffer(getPackageSize() - 2)
        buf.writeByte(beginTag.toByte().toInt())
        buf.writeByte(type.toInt().toUnsignedByte())
        buf.writeShort(sn.toUnsignedShort())
        buf.writeShort(cmd.toUnsignedShort())
        buf.writeShort(size.toUnsignedShort())
        buf.writeBytes(body)
        for (x in buf.array()) {
            nRet *= 2
            nRet += x
        }
        return (nRet and 0xFF).toByte().toShort()
    }

    fun isValid(): Boolean {
        return this.crc == getCRC()
    }
}


