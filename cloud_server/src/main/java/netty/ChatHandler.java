package netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;


import java.util.concurrent.ConcurrentLinkedDeque;

public class ChatHandler extends SimpleChannelInboundHandler<String> {

    private static ConcurrentLinkedDeque<SocketChannel> clients = new ConcurrentLinkedDeque<SocketChannel>();
    private String name;
    private static int cnt = 0;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client connected");
        cnt++;
        name = "user#" + cnt;
        clients.add((SocketChannel) ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client leave");
        clients.remove((SocketChannel) ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        System.out.println("message from client " + name + ": " + s);
        for (SocketChannel channel : clients) {
            channel.writeAndFlush(s);
        }
    }
}
