package services;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class DBPooledConnector implements DBConnector {

    private static final int DEFAULT_LIMIT_OF_CONNECTIONS = 5;
    private static final boolean BUSY_CONNECTION = false;
    private static final boolean FREE_CONNECTION = true;

    private final int LIMIT_OF_CONNECTIONS;
    private final ConcurrentMap<Connection, Boolean> connectionsPool;
    private final DBConnection connector;
    private final ReentrantLock lock;
    private final Semaphore semaphore;

    public DBPooledConnector(DBConnection connector, int connectionsLimit) {
        this.connector = connector;
        this.LIMIT_OF_CONNECTIONS = connectionsLimit;
        connectionsPool = new ConcurrentHashMap<>();
        semaphore = new Semaphore(connectionsLimit, true);
        lock = new ReentrantLock();
        start();
    }

    public DBPooledConnector(DBConnection connector) {
        this(connector, DEFAULT_LIMIT_OF_CONNECTIONS);
    }

    public void start() {
        try {
            final Connection connection = createConnection();
            connectionsPool.put(connection, FREE_CONNECTION);
            System.out.println("Количество подключений " + connectionsPool.size());
        } catch (SQLException e) {
            System.out.println("Ошибка запуска сервера подключений к БД. Невозможно создать подключение.");
            e.printStackTrace();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            semaphore.acquire();
            lock.lock();
            for (Map.Entry<Connection, Boolean> entry : connectionsPool.entrySet()) {
                if (entry.getValue().equals(FREE_CONNECTION)) {
                    entry.setValue(BUSY_CONNECTION);
                    return entry.getKey();
                }
            }
            if (connectionsPool.size() < LIMIT_OF_CONNECTIONS) {
                final Connection connection = createConnection();
                connectionsPool.put(connection, BUSY_CONNECTION);
                System.out.println("Отдано подключение к базе");
                checkConnection();
                return connection;
            } else {
                throw new SQLException("Ошибка выделения подключения к базе данных. Нет свободных подключений.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Ошибка выделения подключения к базе данных!");
            throw new SQLException(String.format("Ошибка выделения подключения к базе данных! Поток %s завершен", Thread.currentThread().getName()));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void closeConnection(Connection connection) {
        connectionsPool.replace(connection, FREE_CONNECTION);
        System.out.println("Возвращено подключение к базе");
        checkConnection();
        semaphore.release();
    }

    @Override
    public void close() {
        try {
            for (Connection connection : connectionsPool.keySet()) {
                connection.close();
                connectionsPool.remove(connection);
            }
        } catch (SQLException e) {
            System.err.println("Ошибка закрытия соединения с базой данных");
            e.printStackTrace();
        }
    }

    private void checkConnection() {
        int countOfFreeConnections = 0;
        for (Map.Entry<Connection, Boolean> entry : connectionsPool.entrySet()) {
            if (entry.getValue().equals(FREE_CONNECTION)) {
                countOfFreeConnections++;
            }
        }
        System.out.println("Свободные подключения " + countOfFreeConnections + " из " + connectionsPool.size());
    }

    private Connection createConnection() throws SQLException {
        final Connection connection = connector.createConnection();
        System.out.println("Создано новое подключение к базе данных");
        return connection;
    }
}
