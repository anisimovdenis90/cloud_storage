package netty.handlers;

import commands.AuthCommand;
import commands.CheckLoginCommand;
import commands.SignUpCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import services.AuthService;
import util.SystemUser;

import java.sql.SQLException;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    private final String serverDir;
    private String userId;

    public AuthHandler(String serverDir) {
        this.serverDir = serverDir;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.printf("Клиент подключился по адресу %s%n", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.printf("Клиент %s отключился по адресу %s%n", userId, ctx.channel().remoteAddress());
        try {
            AuthService.getInstance().setIsLogin(userId, false);
        } catch (SQLException e) {
            System.out.printf("Ошибка изменения данных в БД в процессе отключения пользователя %s по адресу %s%n", userId, ctx.channel().remoteAddress());
            e.printStackTrace();
        }
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof AuthCommand) {
            authProcessing(ctx, (AuthCommand) msg);
        } else if (msg instanceof SignUpCommand) {
            signUpProcessing(ctx, (SignUpCommand) msg);
        } else if (msg instanceof CheckLoginCommand) {
            checkNotUsedLogin(ctx, (CheckLoginCommand) msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void checkNotUsedLogin(ChannelHandlerContext ctx, CheckLoginCommand command) {
        try {
            final String login = command.getLogin();
            if (AuthService.getInstance().checkNotUsedUserId(login)) {
                command.setFree(true);
            } else {
                command.setMessage("Указанный логин уже используется");
            }
            ctx.writeAndFlush(command);
        } catch (SQLException e) {
            System.out.printf("Ошибка чтения данных из БД в процессе проверки логина пользователя %s по адресу %s%n", userId, ctx.channel().remoteAddress());
            e.printStackTrace();
        }
    }

    private void signUpProcessing(ChannelHandlerContext ctx, SignUpCommand command) {
        try {
            final String login = command.getLogin();
            if (AuthService.getInstance().checkNotUsedUserId(login)) {
                final String password = command.getPassword();
                AuthService.getInstance().registerNewUser(login, password);
                command.setSignUp(true);
            } else {
                command.setMessage("Указанный логин уже используется");
            }
            ctx.writeAndFlush(command);
        } catch (SQLException e) {
            System.out.printf("Ошибка сохранения данных в БД в процессе регистрации нового пользователя %s по адресу %s%n", userId, ctx.channel().remoteAddress());
            e.printStackTrace();
        }
    }

    private void authProcessing(ChannelHandlerContext ctx, AuthCommand command) {
        if (command.isAuthorized()) {
            userId = command.getId();
            try {
                AuthService.getInstance().setIsLogin(userId, true);
            } catch (SQLException e) {
                System.out.printf("Ошибка изменения данных в БД в процессе аутентификации пользователя %s по адресу %s%n", userId, ctx.channel().remoteAddress());
                e.printStackTrace();
            }
            ctx.pipeline().addLast(new ClientHandler(userId, serverDir));
            ctx.pipeline().remove(this);
            System.out.println("Добавлен обработчик для нового клиента с ID: " + userId);
            return;
        }
        final String login = command.getLogin();
        SystemUser systemUser = null;
        try {
            systemUser = AuthService.getInstance().getSystemUserByLogin(login);
        } catch (SQLException e) {
            System.out.print("Ошибка получения данных из БД в процессе аутентификации");
            e.printStackTrace();
        }
        if (systemUser != null) {
            if (checkAlreadyLogin(systemUser.getId())) {
                command.setAuthorized(true);
                command.setMessage("Клиент с таким логином уже авторизован");
                ctx.writeAndFlush(command);
                return;
            }
            command.setPassword(systemUser.getHashedPassword());
            command.setId(systemUser.getId());
        } else {
            command.setAuthorized(true);
            command.setMessage("Неверный логин или пароль");
        }
        ctx.writeAndFlush(command);
    }

    private boolean checkAlreadyLogin(String userId) {
        try {
            return AuthService.getInstance().isLogin(userId);
        } catch (SQLException e) {
            System.out.printf("Ошибка чтения данных из БД в процессе аутентификации пользователя %s%n", userId);
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            AuthService.getInstance().setIsLogin(userId, false);
        } catch (SQLException e) {
            System.out.printf("Ошибка изменения данных в БД в процессе отключения пользователя %s по адресу %s%n%n", userId, ctx.channel().remoteAddress());
            e.printStackTrace();
        }
        cause.printStackTrace();
        ctx.close();
    }
}
