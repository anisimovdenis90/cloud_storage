package NIO;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

public class NIOServer implements Runnable {

    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private static final String CLIENT_DIR_PREFIX = "./common/src/main/resources/serverFiles";
    private static int clientId = 1;
    private Path clientDirectory;
    private ByteBuffer buffer;
    private ServerSocketChannel server;
    private Selector selector;

    public NIOServer() throws IOException {
        server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(8189));
        server.configureBlocking(false);
        selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
        this.buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
    }

    public static void main(String[] args) throws IOException {
        new Thread(new NIOServer()).start();
    }

    @Override
    public void run() {
        try {
            System.out.println("Server started");
            while (server.isOpen()) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        acceptClient(key);
                    } else if (key.isReadable()) {
                        readFromClient(key);
                    }
                }
            }
        } catch (Exception e) {
            clientId--;
            e.printStackTrace();
        }
    }

    private void acceptClient(SelectionKey key) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        sendMessageToClient(String.valueOf(clientId), channel);
        clientDirectory = Paths.get(CLIENT_DIR_PREFIX + "/" + clientId);
        if (!Files.exists(clientDirectory)) {
            Files.createDirectory(clientDirectory);
        }
        clientId++;
    }

    private void readFromClient(SelectionKey key) throws IOException, InterruptedException {
        String message = readMessageFromClient(key);
        if ("end".equals(message)) {
            key.channel().close();
            return;
        }
        String[] data = message.split(" ");
        if ("./download".equals(data[0])) {
            sendFileToClient(data[1], key);
        } else if ("./upload".equals(data[0])) {
            getFileFromClient(data[1], key);
        }
    }

    private void sendMessageToClient(String massage, SocketChannel channel) throws IOException {
        byte[] byteMessage = massage.getBytes();
        ByteBuffer messageLength = ByteBuffer.allocate(Integer.BYTES);
        messageLength.putInt(byteMessage.length);
        messageLength.flip();
        channel.write(messageLength);
        channel.write(ByteBuffer.wrap(byteMessage));
    }

    private String readMessageFromClient(SelectionKey key) throws IOException, InterruptedException {
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder message = new StringBuilder();
        Thread.sleep(100);
        ByteBuffer byteLength = ByteBuffer.allocate(Integer.BYTES);
        int count = channel.read(byteLength);
        if (count == -1) {
            key.channel().close();
            return "end";
        }
        byteLength.flip();
        int messageLength = byteLength.getInt();
        ByteBuffer byteMessage = ByteBuffer.allocate(messageLength);
        channel.read(byteMessage);
        byteMessage.flip();
        while (byteMessage.hasRemaining()) {
            message.append((char) byteMessage.get());
        }
        System.out.println("Сообщение от клиента: " + message);
        return message.toString();
    }

    private void getFileFromClient(String fileName, SelectionKey key) throws IOException, InterruptedException {
        System.out.println("Начало передачи файла от клиента " + fileName);
        SocketChannel channel = (SocketChannel) key.channel();
        sendMessageToClient("OK", channel);
        ByteBuffer byteLength = ByteBuffer.allocate(Long.BYTES);
        Thread.sleep(100);
        channel.read(byteLength);
        byteLength.flip();
        long fileSize = byteLength.getLong();
        int totalBytes = 0;
        int lastBytes;
        int readBytes;
        try (RandomAccessFile file = new RandomAccessFile(clientDirectory + "/" + fileName, "rw")) {
            FileChannel fileWriter = file.getChannel();
            while (totalBytes < fileSize) {
                if ((lastBytes = (int) fileSize - totalBytes) < DEFAULT_BUFFER_SIZE) {
                    ByteBuffer lastBytesBuffer = ByteBuffer.allocate(lastBytes);
                    channel.read(lastBytesBuffer);
                    lastBytesBuffer.flip();
                    while (lastBytesBuffer.hasRemaining()) {
                        fileWriter.write(lastBytesBuffer);
                    }
                    break;
                }
                readBytes = channel.read(buffer);
                buffer.flip();
                while (buffer.hasRemaining()) {
                    fileWriter.write(buffer);
                }
                totalBytes += readBytes;
                buffer.clear();
            }
            System.out.println("Файл загружен на сервер " + fileName);
        }
    }

    private void sendFileToClient(String fileName, SelectionKey key) throws IOException {
        Path file = Paths.get(clientDirectory + "/" + fileName);
        SocketChannel channel = (SocketChannel) key.channel();
        if (Files.exists(file)) {
            System.out.println("Начало отправки файла клиенту " + fileName);
            sendMessageToClient("OK", channel);
            long fileSizeLong = Files.size(file);
            ByteBuffer fileSize = ByteBuffer.allocate(Long.BYTES);
            fileSize.putLong(fileSizeLong);
            fileSize.flip();
            channel.write(fileSize);
            try (RandomAccessFile accessFile = new RandomAccessFile(file.toFile(), "r")) {
                FileChannel fileReader = accessFile.getChannel();
                int readBytes = fileReader.read(buffer);
                while (readBytes > -1) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        channel.write(buffer);
                    }
                    buffer.clear();
                    readBytes = fileReader.read(buffer);
                }
            }
            System.out.println("Файл успешно отправлен клиенту");
            buffer.clear();
        } else {
            sendMessageToClient("NotOK", channel);
        }
    }
}
