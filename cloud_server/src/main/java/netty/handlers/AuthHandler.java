package netty.handlers;

import commands.AuthCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import services.AuthService;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    private final String serverDir;
    private static boolean authOk = false;
    private AuthService authService;
    private String nickName;
    private String userId;

    public AuthHandler(AuthService authService, String serverDir) {
        this.authService = authService;
        this.serverDir = serverDir;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println(String.format("Клиент подключился по адресу %s", ctx.channel().remoteAddress().toString()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println(String.format("Клиент отключился по адресу %s", ctx.channel().remoteAddress().toString()));
        authService.setIsLogin(userId, false);
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof AuthCommand) {
            authProcessing(ctx, (AuthCommand) msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void authProcessing(ChannelHandlerContext ctx, AuthCommand command) {
        String login = command.getLogin();
        String password = command.getPassword();
        userId = authService.getUserIDByLoginAndPassword(login, password);
        if (userId != null) {
            if (checkAlreadyLogin(userId)) {
                command.setMessage("Клиент с таким логином уже авторизован");
                ctx.writeAndFlush(command);
                return;
            }
            command.setAuthorized(true);
            command.setUserID(userId);
            authService.setIsLogin(userId, true);
            ctx.pipeline().addLast(new ClientHandler(authService, nickName, userId, serverDir));
        } else {
            command.setMessage("Неверный логин или пароль");
        }
        ctx.writeAndFlush(command);
    }

    private boolean checkAlreadyLogin(String userId) {
        return authService.isLogin(userId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        authService.setIsLogin(userId, false);
        cause.printStackTrace();
        ctx.close();
    }
}
