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
import services.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class NetworkServer {

    private static final int DEFAULT_PORT = 8189;

    private static final String SERVER_DIR = "./server";
    private static final String PROPERTIES_SERVER_PORT = "server.port";
    private final DBConnector dbConnector;
    private int port;

    public NetworkServer(Properties properties) {
        try {
            port = Integer.parseInt(properties.getProperty(PROPERTIES_SERVER_PORT));
        } catch (NumberFormatException e) {
            port = DEFAULT_PORT;
            System.err.println("Ошибка чтения порта из файла конфигурации, использован порт по умолчанию: " + DEFAULT_PORT);
        }
        dbConnector = new DBHikariConnector(properties);
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
            AuthService.getInstance().start(dbConnector);
            createMainDirectory();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            System.err.println("Ошибка в работе сервера");
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
