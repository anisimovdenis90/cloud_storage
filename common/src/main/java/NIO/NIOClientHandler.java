package NIO;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

public class NIOClientHandler {

    private static int clientId = 1;
    private String clientDirPrefix = "./common/src/main/resources/serverFiles";
    private Path clientDirectory;

    private SocketChannel channel;
    private Selector selector;
    private SelectionKey key;


    public NIOClientHandler(SocketChannel channel) {
        System.out.println("Client accepted");
        this.channel = channel;
    }

    public void start() {
        new Thread(() -> {
            try {
                this.selector = Selector.open();
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_READ);
                channel.write(ByteBuffer.wrap(String.valueOf(clientId).getBytes()));
                clientDirectory = Paths.get(clientDirPrefix + "/" + clientId);
                if (!Files.exists(clientDirectory)) {
                    Files.createDirectory(clientDirectory);
                }
                clientId++;
                runReadMessage();
            } catch (IOException e) {
                clientId--;
                e.printStackTrace();
            }
        }).start();
    }

    private void runReadMessage() throws IOException {
        while (true) {
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                key = iterator.next();
                iterator.remove();
                if (key.isReadable()) {
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    int count = ((SocketChannel) key.channel()).read(buffer);
                    if (count == -1) {
                        key.channel().close();
                        break;
                    }
                    buffer.flip();
                    StringBuilder s = new StringBuilder();
                    while (buffer.hasRemaining()) {
                        s.append((char) buffer.get());
                    }
                    String[] data = s.toString().split(" ");
                    if ("./download".equals(data[0])) {
                        sendFileToClient(data[1]);
                    } else if ("./upload".equals(data[0])) {
                        getFileFromClient(data[1], Long.parseLong(data[2]));
                    }
                }
            }
        }
    }

    private void getFileFromClient(String fileName, Long fileSize) throws IOException {
        System.out.println("Начало передачи файла от клиента " + fileName);
        channel.write(ByteBuffer.wrap("OK".getBytes()));
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        SocketChannel channel = (SocketChannel) key.channel();
        RandomAccessFile file = new RandomAccessFile(clientDirectory + "/" + fileName, "rw");
        FileChannel fileWriter = file.getChannel();
        for (int i = 0; i < (fileSize / 1024) + 1; i++) {
            channel.read(buffer);
            buffer.flip();
            while (buffer.hasRemaining()) {
                fileWriter.write(buffer);
            }
            buffer.clear();
        }
        file.close();
        System.out.println("Файл загружен на сервер " + fileName);

    }

    private void sendFileToClient(String fileName) throws IOException {
        Path file = Paths.get(clientDirectory + "/" + fileName);
        SocketChannel channel = (SocketChannel) key.channel();
        if (Files.exists(file)) {
            channel.write(ByteBuffer.wrap("OK".getBytes()));
            long fileSize = Files.size(file);
            channel.write(ByteBuffer.wrap(String.valueOf(fileSize).getBytes()));
            channel.write(ByteBuffer.wrap(Files.readAllBytes(file)));
        } else {
            channel.write(ByteBuffer.wrap("NotOK".getBytes()));
        }
    }

    private String readMessageFromClient(SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        channel.read(buffer);
        buffer.flip();
        StringBuilder s = new StringBuilder();
        while (buffer.hasRemaining()) {
            s.append((char) buffer.get());
        }
        return s.toString();
    }
}
