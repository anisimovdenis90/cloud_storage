package netty.handlers;

import commands.AuthCommand;
import commands.SignUpCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import services.AuthService;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    private final String serverDir;
    private String userId;

    public AuthHandler(String serverDir) {
        this.serverDir = serverDir;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.printf("Клиент подключился по адресу %s%n", ctx.channel().remoteAddress().toString());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.printf("Клиент отключился по адресу %s%n", ctx.channel().remoteAddress().toString());
        AuthService.getInstance().setIsLogin(userId, false);
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof AuthCommand) {
            authProcessing(ctx, (AuthCommand) msg);
        } else if (msg instanceof SignUpCommand) {
            signUpProcessing(ctx, (SignUpCommand) msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void signUpProcessing(ChannelHandlerContext ctx, SignUpCommand command) {
        final String login = command.getLogin();
        if (AuthService.getInstance().checkIsUsedUserId(login)) {
            final String password = command.getPassword();
            AuthService.getInstance().registerNewUser(login, password);
            command.setSignUp(true);
        } else {
            command.setMessage("Указанный логин уже используется");
        }
        ctx.writeAndFlush(command);
    }

    private void authProcessing(ChannelHandlerContext ctx, AuthCommand command) {
        final String login = command.getLogin();
        final String password = command.getPassword();
        userId = AuthService.getInstance().getUserIDByLoginAndPassword(login, password);
        if (userId != null) {
            if (checkAlreadyLogin(userId)) {
                command.setMessage("Клиент с таким логином уже авторизован");
                ctx.writeAndFlush(command);
                return;
            }
            command.setAuthorized(true);
            command.setUserID(userId);
            AuthService.getInstance().setIsLogin(userId, true);
            ctx.pipeline().addLast(new ClientHandler(userId, serverDir));
            ctx.pipeline().remove(this);
            System.out.println("Добавлен обработчик для нового клиента с ID: " + userId);
        } else {
            command.setMessage("Неверный логин или пароль");
        }
        ctx.writeAndFlush(command);
    }

    private boolean checkAlreadyLogin(String userId) {
        return AuthService.getInstance().isLogin(userId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        AuthService.getInstance().setIsLogin(userId, false);
        cause.printStackTrace();
        ctx.close();
    }
}
