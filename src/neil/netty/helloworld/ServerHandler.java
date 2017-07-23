package neil.netty.helloworld;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerHandler extends ChannelInboundHandlerAdapter{

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        //将msg转换成ByteBuf对象
        ByteBuf buf = ((ByteBuf)msg);
        //通过可读字节数创建byte数组
        byte[] req = new byte[buf.readableBytes()];
        //将缓冲区的字节数组复制到新的byte数组
        buf.readBytes(req);
        String body = new String(req,"utf-8");
        System.out.println("Client:" + body);
        //创建应答消息并返回给客户端
        String resp = "服务端收到您的消息";
        ctx.writeAndFlush(Unpooled.copiedBuffer(resp.getBytes()))
        //添加监听器，关闭通道
        .addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }   
}

