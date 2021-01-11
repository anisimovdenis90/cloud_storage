package services;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class DBPooledConnector implements DBConnector {

    private final int LIMIT_OF_CONNECTIONS;
    private final Map<Connection, Boolean> connectionsPool;
    private final ReentrantLock lock;
    private final DBConnection connector;
    private final Semaphore semaphore;
    private static final boolean FREE_CONNECTION = true;
    private static final boolean BUSY_CONNECTION = false;

    public DBPooledConnector(DBConnection connector, int LIMIT_OF_CONNECTIONS) {
        this.connector = connector;
        this.LIMIT_OF_CONNECTIONS = LIMIT_OF_CONNECTIONS;
        connectionsPool = new ConcurrentHashMap<>();
        semaphore = new Semaphore(LIMIT_OF_CONNECTIONS, true);
        lock = new ReentrantLock();
        start();
    }

    public void start() {
        Optional<Connection> connectionOptional = createConnection();
        connectionOptional.ifPresent(connection -> connectionsPool.put(connection, FREE_CONNECTION));
        System.out.println("Количество подключений " + connectionsPool.size());
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            semaphore.acquire();
            lock.lock();
            Connection connection = null;
            for (Map.Entry<Connection, Boolean> entry : connectionsPool.entrySet()) {
                if (entry.getValue().equals(FREE_CONNECTION)) {
                    entry.setValue(BUSY_CONNECTION);
                    connection = entry.getKey();
                }
            }
            if (connection == null && connectionsPool.size() < LIMIT_OF_CONNECTIONS) {
                Optional<Connection> connectionOptional = createConnection();
                if (connectionOptional.isPresent()) {
                    connection = connectionOptional.get();
                    connectionsPool.put(connection, BUSY_CONNECTION);
                }
            }
            if (connection != null) {
                System.out.println("Отдано подключение к базе");
                checkConnection();
                return connection;
            } else {
                throw new SQLException("Ошибка выделения подключения к базе данных. Нет свободных подключений.");
            }
        } catch (InterruptedException e) {
            System.out.println("Ошибка выделения подключения к базе данных!");
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        throw new SQLException("Ошибка подключения к БД.");
    }

    @Override
    public void closeConnection(Connection connection) {
        for (Map.Entry<Connection, Boolean> entry : connectionsPool.entrySet()) {
            if (entry.getKey().equals(connection)) {
                entry.setValue(true);
                System.out.println("Возвращено подключение к базе");
                checkConnection();
                semaphore.release();
            }
        }
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

    private Optional<Connection> createConnection() {
        try {
            Connection connection = connector.createConnection();
            System.out.println("Создано новое подключение к базе данных");
            return Optional.of(connection);
        } catch (SQLException e) {
            System.err.println("Ошибка создания нового подключения к базе данных!");
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
