package netty.handlers;

import commands.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import services.AuthService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    private static final String CLIENT_DIR_PREFIX = "/client";
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 10;
    private final String serverDir;
    private final AuthService authService;
    private String nickName;
    private String userId;
    private String clientDirectory;


    public ClientHandler(AuthService authService, String nickName, String userId, String serverDir) {
        this.authService = authService;
        this.nickName = nickName;
        this.userId = userId;
        this.serverDir = serverDir;
        clientDirectory = serverDir + CLIENT_DIR_PREFIX + userId;
        createClientDirectory();
    }

//    @Override
//    public void channelActive(ChannelHandlerContext ctx) {
//        System.out.println(String.format("Клиент %s подключился по адресу %s", nickName, ctx.channel().remoteAddress().toString()));
//    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println(String.format("Клиент отключился по адресу %s", ctx.channel().remoteAddress().toString()));
        authService.setIsLogin(userId, false);
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FileRequestCommand) {
            sendFileToClient(ctx, (FileRequestCommand) msg);
        } else if (msg instanceof FileMessageCommand) {
            getFileFromClient((FileMessageCommand) msg);
        } else if (msg instanceof DeleteFileCommand) {
            deleteFile((DeleteFileCommand) msg);
        } else if (msg instanceof GetFilesListCommand) {
            sendFilesListToClient(ctx, (GetFilesListCommand) msg);
        } else {
            System.out.println("Получен неизвестный объект от клиента " + userId);
        }
    }

    private void sendFilesListToClient(ChannelHandlerContext ctx, GetFilesListCommand msg) {
        File path = new File(clientDirectory);
        FilesListCommand files = new FilesListCommand(new ArrayList<String>(Arrays.asList(Objects.requireNonNull(path.list()))));
        ctx.writeAndFlush(files);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            System.out.println(String.format("Соединение с клиентом %s разорвано", userId));
        } else {
            System.out.println("Ошибка обработчика клиента!");
            cause.printStackTrace();
        }
        ctx.close();
    }

    private void deleteFile(DeleteFileCommand command) {
        try {
            Files.delete(Paths.get(clientDirectory, command.getFileName()));
        } catch (IOException e) {
            System.out.println("Ошибка удаления файла с сервера " + command.getFileName());
            e.printStackTrace();
        }
    }

    // НАДО ПЕРЕДЕЛАТЬ МЕТОД!!!
    private void getFileFromClient(FileMessageCommand command) {
        try (FileOutputStream fileWriter = new FileOutputStream(clientDirectory + "/" + command.getFileName(), true)) {
            fileWriter.write(command.getData());
            fileWriter.close();
            if (command.getPartNumber() == command.getPartsOfFile()) {
                if (command.getFileSize() == Files.size(Paths.get(clientDirectory, command.getFileName())))
                System.out.println(String.format("Файл %s успешно загружен на сервер от клиента %s", command.getFileName(), userId));
            } else {
                System.out.println(String.format("Ошибка передачи файла %s от клиента %s, получены не все данные", command.getFileName(), userId));
            }
        } catch (IOException e) {
            System.out.println(String.format("Ошибка записи файла %s на сервер", command.getFileName()));
            e.printStackTrace();
        }
    }


    private void sendFileToClient(ChannelHandlerContext ctx, FileRequestCommand command) {
        new Thread(() -> {
            final File file = new File(clientDirectory + "/" + command.getFileName());
            int partsOfFile = (int) file.length() / DEFAULT_BUFFER_SIZE;
            if (file.length() % DEFAULT_BUFFER_SIZE != 0) {
                partsOfFile++;
            }
            FileMessageCommand fileToClient = new FileMessageCommand(file.getName(), file.length(), partsOfFile, 0, new byte[DEFAULT_BUFFER_SIZE]);
            try (FileInputStream fileReader = new FileInputStream(file)) {
                int readBytes;
                for (int i = 0; i < partsOfFile; i++) {
                    readBytes = fileReader.read(fileToClient.getData());
                    fileToClient.setPartNumber(i + 1);
                    if (readBytes < DEFAULT_BUFFER_SIZE) {
                        fileToClient.setData(Arrays.copyOfRange(fileToClient.getData(), 0, readBytes));
                    }
                    ctx.writeAndFlush(fileToClient);
                    Thread.sleep(100);
                }
                System.out.println(String.format("Файл %s успешно передан клиенту %s", command.getFileName(), userId));
            } catch (IOException | InterruptedException e) {
                System.out.println(String.format("Ошибка передачи файла %s клиенту %s", command.getFileName(), userId));
                e.printStackTrace();
            }
        }) .start();
    }

    private void createClientDirectory() {
        if (!Files.exists(Paths.get(clientDirectory))) {
            try {
                Files.createDirectories(Paths.get(clientDirectory));
            } catch (IOException e) {
                System.out.println("Ошибка создания папки клиента");
                e.printStackTrace();
            }
        }
    }
}
