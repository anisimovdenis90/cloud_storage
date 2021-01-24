package netty.handlers;

import commands.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import services.AuthService;
import util.FileInfo;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    private static final String CLIENT_DIR_PREFIX = "client";
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 10;
    private final ExecutorService executor;
    private final String serverDir;
    private final String userId;
    private final String rootClientDirectory;
    private final String clientDir;

    private FileOutputStream fileWriter;
    private Path wroteFilePath;

    public ClientHandler(String userId, String serverDir) {
        this.userId = userId;
        this.serverDir = serverDir;
        this.executor = Executors.newSingleThreadExecutor();
        clientDir = CLIENT_DIR_PREFIX + userId;
        rootClientDirectory = serverDir + "/" + clientDir;
        createClientDirectory();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.printf("Клиент отключился по адресу %s%n", ctx.channel().remoteAddress().toString());
        AuthService.getInstance().setIsLogin(userId, false);
        checkFileWriter();
        executor.shutdownNow();
        ctx.close();
    }

    private void checkFileWriter() {
        try {
            if (fileWriter != null) {
                fileWriter.close();
                System.out.println("Выполнено удаление частично полученного файла от клиента " + wroteFilePath);
                Files.delete(wroteFilePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            System.out.printf("Соединение с клиентом %s по адресу %s%n", userId, ctx.channel().remoteAddress().toString());
        } else {
            System.out.printf("Ошибка обработчика клиента %s по адресу %s%n", userId, ctx.channel().remoteAddress().toString());
            cause.printStackTrace();
        }
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FileRequestCommand) {
            sendFileToClient(ctx, (FileRequestCommand) msg);
        } else if (msg instanceof FileMessageCommand) {
            getFileFromClient((FileMessageCommand) msg);
        } else if (msg instanceof DeleteFileCommand) {
            deleteFile(ctx, (DeleteFileCommand) msg);
        } else if (msg instanceof RenameFileCommand) {
            renameFile(ctx, (RenameFileCommand) msg);
        } else if (msg instanceof CreateFolderCommand) {
            createNewFolder(ctx, (CreateFolderCommand) msg);
        } else if (msg instanceof GetFilesListCommand) {
            sendFilesListToClient(ctx, (GetFilesListCommand) msg);
        } else if (msg instanceof FilesListInDirRequest) {
            sendFilesListInSelectDir(ctx, (FilesListInDirRequest) msg);
        } else {
            System.out.printf("Получен неизвестный объект %s от клиента %s%n", msg.toString(), userId);
        }
    }

    private void sendFilesListInSelectDir(ChannelHandlerContext ctx, FilesListInDirRequest command) {
        System.out.println("Запрос на список файлов в папке: " + command.getServerPath());
        final Path dirPath = Paths.get(serverDir, command.getServerPath());
        try {
            final List<FileInfo> list = new LinkedList<>();
            Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    System.out.println(file.toString());
                    final Path fileDirPath = Paths.get(dirPath.getFileName().toString(), dirPath.relativize(file.getParent()).toString());
                    System.out.println("Полученный путь: " + fileDirPath);
                    final FileInfo fileInfo = new FileInfo(file);
                    fileInfo.setFileDir(fileDirPath.toString());
                    list.add(fileInfo);
                    return FileVisitResult.CONTINUE;
                }
            });
            if (list.size() == 0) {
                final String message = "Отсутствуют файлы для скачивания в папке \"%s\" на сервере";
                ctx.writeAndFlush(new ErrorCommand(String.format(message, command.getServerPath())));
            } else {
                command.setFilesList(list);
                ctx.writeAndFlush(command);
                System.out.println("Список файлов в папке: " + dirPath + " успешно отправлен");
            }
        } catch (IOException e) {
            final String message = "Ошибка получения списка файлов в папке: \"%s\" на сервере";
            System.out.printf(message + "%n", dirPath);
            ctx.writeAndFlush(new ErrorCommand(String.format(message, command.getServerPath())));
            e.printStackTrace();
        }
    }

    private void sendFilesListToClient(ChannelHandlerContext ctx, GetFilesListCommand command) {
        System.out.println("Запрос на список файлов");
        final FilesListCommand filesList;
        try {
            if (command.getCurrentPath() == null) {
                final Path rootClientPath = Paths.get(clientDir);
                final Path folder = Paths.get(rootClientDirectory);
                filesList = new FilesListCommand(Files.list(folder)
                        .map(FileInfo::new)
                        .collect(Collectors.toList()), rootClientPath);
                filesList.setRootServerPath(rootClientPath);
            } else {
                final String currentClientDir = command.getCurrentPath();
                final Path currentClientPath = Paths.get(serverDir, currentClientDir);
                filesList = new FilesListCommand(Files.list(currentClientPath)
                        .map(FileInfo::new)
                        .collect(Collectors.toList()), Paths.get(currentClientDir));
            }
            ctx.writeAndFlush(filesList);
            System.out.println("Список файлов отправлен");
        } catch (IOException e) {
            System.out.printf("Ошибка получения списка файлов клиента %s по пути %s%n", userId, command.getCurrentPath());
            System.out.println("Переход на каталог выше");
            final Path newPath = Paths.get(command.getCurrentPath()).getParent();
            if (newPath != null) {
                sendFilesListToClient(ctx, new GetFilesListCommand(newPath));
            } else {
                ctx.writeAndFlush(new ErrorCommand("Невозможно получить список файлов с сервера, попробуйте повторить позже."));
            }
        }
    }

    private void deleteFile(ChannelHandlerContext ctx, DeleteFileCommand command) {
        System.out.printf("Команда на удаление файла %s от клиента %s%n", command.getFileName(), userId);
        final Path deletePath = Paths.get(serverDir, command.getFileName());
        if (Files.isDirectory(deletePath)) {
            deleteDirectory(ctx, deletePath);
        } else {
            try {
                Files.delete(deletePath);
                ctx.writeAndFlush(new MessageCommand("Файл \"" + deletePath.getFileName() + "\" успешно удален с сервера"));
                System.out.println("Файл " + deletePath.getFileName() + " успешно удален с сервера");
            } catch (IOException e) {
                final String message = "Невозможно удалить файл \"%s\" с сервера, попробуйте повторить позже.";
                System.out.println("Ошибка удаления файла с сервера " + command.getFileName());
                ctx.writeAndFlush(new ErrorCommand(String.format(message, deletePath.getFileName())));
                e.printStackTrace();
            }
        }
    }

    private void deleteDirectory(ChannelHandlerContext ctx, Path deletePath) {
        try {
            Files.walkFileTree(deletePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    System.out.println("Удален файл: " + file.toString());
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    System.out.println("Удален каталог: " + dir.toString());
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            ctx.writeAndFlush(new MessageCommand("Папка " + deletePath.getFileName() + " успешно удалена с сервера"));
        } catch (IOException e) {
            final String message = "Невозможно удалить папку \"%s\" с сервера, попробуйте повторить позже.";
            System.out.println("Ошибка удаления папки " + deletePath);
            ctx.writeAndFlush(new ErrorCommand(String.format(message, deletePath.getFileName())));
            e.printStackTrace();
        }
    }

    private void renameFile(ChannelHandlerContext ctx, RenameFileCommand command) {
        System.out.printf("Команда на переименование файла %s на %s от клиента %s%n", command.getOldFileName(), command.getNewFileName(), userId);
        try {
            final Path oldFile = Paths.get(serverDir, command.getOldFileName());
            final Path newFile = Paths.get(serverDir, command.getNewFileName());
            Files.move(oldFile.toAbsolutePath().normalize(), newFile.toAbsolutePath().normalize());
            System.out.println("Файл " + oldFile + " успешно переименован на " + newFile);
            ctx.writeAndFlush(new MessageCommand("Файл " + oldFile.getFileName() + " успешно переименован на " + newFile.getFileName()));
        } catch (IOException e) {
            System.out.println("Ошибка переименования файла на сервере " + command.getOldFileName());
            ctx.writeAndFlush(new ErrorCommand("Невозможно переименовать файл на сервере, попробуйте повторить позже."));
            e.printStackTrace();
        }
    }

    private void createNewFolder(ChannelHandlerContext ctx, CreateFolderCommand command) {
        System.out.printf("Команда на создание папки %s на от клиента %s%n", command.getFolderName(), userId);
        try {
            final Path newFolderPath = Paths.get(serverDir, command.getFolderName());
            Files.createDirectories(newFolderPath);
            final String message = "Папка \"" + newFolderPath.toString() + "\" успешно создана";
            ctx.writeAndFlush(new MessageCommand(message));
            System.out.println(message);
        } catch (IOException e) {
            final String message = "Ошибка создания на сервере папки \"%s\"";
            System.out.printf(message + "%n", command.getFolderName());
            ctx.writeAndFlush(new ErrorCommand(String.format(message, command.getFolderName())));
            e.printStackTrace();
        }
    }

    private void getFileFromClient(FileMessageCommand command) {
        try {
            if (fileWriter == null) {
                System.out.printf("Начало получения файла %s от клиента %s%n", command.getFileName(), userId);
                final Path destPath = Paths.get(serverDir, command.getDestPath(), command.getFileName());
                if (Files.exists(destPath)) {
                    System.out.printf("Файл %s уже существует на сервере, выполняется удаление%n", command.getFileName());
                    Files.delete(destPath);
                    System.out.println("Выполнено удаление");
                }
                createFileWriter(destPath);
                System.out.println("Абсолютный путь загрузки " + destPath);
            }
            fileWriter.write(command.getData());
            System.out.printf("Получена часть %d из %d%n", command.getPartNumber(), command.getPartsOfFile());
            if (command.getPartNumber() == command.getPartsOfFile()) {
                fileWriter.close();
                fileWriter = null;
                wroteFilePath = null;
                System.out.printf("Файл %s успешно загружен на сервер от клиента %s%n", command.getFileName(), userId);
            }
        } catch (IOException e) {
            System.out.printf("Ошибка записи файла %s на сервер от клиента %s%n", command.getFileName(), userId);
            e.printStackTrace();
        }
    }

    private void createFileWriter(Path destPath) {
        try {
            Files.createDirectories(destPath.getParent());
            fileWriter = new FileOutputStream(destPath.toFile(), true);
            wroteFilePath = destPath;
        } catch (IOException e) {
            System.out.printf("Невозможно начать запись файла %s на диск%n", destPath);
            e.printStackTrace();
        }
    }

    private void sendFileToClient(ChannelHandlerContext ctx, FileRequestCommand command) {
        executor.execute(() -> {
            final Path fileToSend = Paths.get(serverDir, command.getFileToDownload());
            System.out.printf("Начало передачи файла %s клиенту %s%n", fileToSend, userId);
            final long fileSize = fileToSend.toFile().length();
            long partsOfFile = fileSize / DEFAULT_BUFFER_SIZE;
            if (fileToSend.toFile().length() % DEFAULT_BUFFER_SIZE != 0 || fileSize == 0) {
                partsOfFile++;
            }
            System.out.printf("Размер файла %d, количество пакетов %d при размере буфера %d%n", fileSize, partsOfFile, DEFAULT_BUFFER_SIZE);
            final FileMessageCommand fileToClientCommand = new FileMessageCommand(
                    fileToSend.getFileName().toString(),
                    null,
                    fileSize,
                    partsOfFile,
                    0,
                    fileSize < DEFAULT_BUFFER_SIZE ? new byte[(int) fileSize] : new byte[DEFAULT_BUFFER_SIZE]
            );
            try (FileInputStream fileReader = new FileInputStream(fileToSend.toFile())) {
                int readBytes;
                long partsSend = 0;
                do {
                    readBytes = fileReader.read(fileToClientCommand.getData());
                    fileToClientCommand.setPartNumber(partsSend + 1);
                    if (readBytes < fileToClientCommand.getData().length) {
                        fileToClientCommand.setData(Arrays.copyOfRange(fileToClientCommand.getData(), 0, Math.max(readBytes, 0)));
                    }
                    ctx.writeAndFlush(fileToClientCommand);
                    partsSend++;
                    System.out.printf("Отправлена часть %d из %d%n", partsSend, partsOfFile);
                    Thread.sleep(10);
                } while (partsSend < partsOfFile);
                System.out.printf("Файл %s успешно передан клиенту %s%n", command.getFileToDownload(), userId);
            } catch (IOException | InterruptedException e) {
                System.out.printf("Ошибка передачи файла %s клиенту %s%n", command.getFileToDownload(), userId);
                e.printStackTrace();
            }
        });
    }

    private void createClientDirectory() {
        final Path path = Paths.get(rootClientDirectory);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                System.out.printf("Ошибка создания папки клиента %s%n", userId);
                e.printStackTrace();
            }
        }
    }
}
