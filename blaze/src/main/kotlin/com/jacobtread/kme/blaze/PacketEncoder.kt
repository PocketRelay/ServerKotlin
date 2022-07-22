package com.jacobtread.kme.blaze

import com.jacobtread.kme.blaze.packet.Packet
import com.jacobtread.kme.utils.logging.Logger
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.EncoderException
import io.netty.util.ReferenceCountUtil

object PacketEncoder : ChannelOutboundHandlerAdapter() {

    override fun isSharable(): Boolean = true

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        var buffer: ByteBuf? = null
        try {
            if (msg is Packet) {
                buffer = Packet.allocateBuffer(ctx.alloc(), msg)
                try {
                    if (Logger.logPackets) {
                        try {
                            Logger.debug("SENT PACKET ===========\n" + packetToBuilder(msg) + "\n======================")
                        } catch (e: Throwable) {
                            logPacketException("Failed to decode sent packet contents for debugging: ", msg, e)
                        }
                    }
                    msg.writeTo(buffer)
                    ctx.flush()
                } finally {
                    ReferenceCountUtil.release(msg)
                }
                if (buffer.isReadable) {
                    ctx.write(buffer, promise)
                } else {
                    buffer.release()
                    ctx.write(Unpooled.EMPTY_BUFFER, promise)
                }
                buffer = null
            } else {
                ctx.write(msg, promise)
            }
        } catch (e: EncoderException) {
            throw e
        } catch (e: Throwable) {
            throw EncoderException(e)
        } finally {
            buffer?.release()
        }
    }

}