package netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import netty.handlers.AuthHandler;
import services.AuthService;
import services.DBConnectionImpl;
import services.DBHikariConnector;
import services.DBPooledConnector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class NetworkServer {

    private static final String SERVER_DIR = "./server";
    private final Properties properties;
    private final int port;

    public NetworkServer(int port, Properties properties) {
        this.port = port;
        this.properties = properties;
    }

    public void run() {
        EventLoopGroup mainGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(mainGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline().addLast(
                                    new ObjectDecoder(1024 * 1024 * 100, ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new AuthHandler(SERVER_DIR)
                            );
                        }
                    });
            ChannelFuture future = bootstrap.bind(port).sync();
            System.out.println("Сервер успешно запущен на порту: " + port);
//            AuthService.getInstance().start(new DBPooledConnector(new DBConnectionImpl(properties), 5));
            AuthService.getInstance().start(new DBHikariConnector("datasource.properties"));
            createMainDirectory();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            System.out.println("Ошибка в работе сервера");
            e.printStackTrace();
        } finally {
            AuthService.getInstance().stop();
            System.out.println("Сервер остановлен");
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private void createMainDirectory() throws IOException {
        if (!Files.exists(Paths.get(SERVER_DIR))) {
            Files.createDirectories(Paths.get(SERVER_DIR));
        }
    }
}
