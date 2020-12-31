package services;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;

public class DBPooledConnector {

    private final int LIMIT_OF_CONNECTIONS;
    private final Map<Connection, Boolean> connectionsPool = new HashMap<>();
    private final DBConnection connector;
    private final Semaphore semaphore;

    public DBPooledConnector(DBConnection connector, int LIMIT_OF_CONNECTIONS) {
        this.connector = connector;
        this.LIMIT_OF_CONNECTIONS = LIMIT_OF_CONNECTIONS;
        semaphore = new Semaphore(LIMIT_OF_CONNECTIONS, true);
        start();
    }

    public void start() {
        Optional<Connection> connectionOptional;
        if ((connectionOptional = createConnection()).isPresent()) {
            connectionsPool.put(connectionOptional.get(), true);
        }
        System.out.println("Количество подключений " + connectionsPool.size());
    }

    public Connection getConnection() {
        Connection connection = null;
        try {
            semaphore.acquire();
            for (Map.Entry<Connection, Boolean> entry : connectionsPool.entrySet()) {
                if (entry.getValue().equals(true)) {
                    entry.setValue(false);
                    connection = entry.getKey();
                } else if (connectionsPool.size() <= LIMIT_OF_CONNECTIONS) {
                    Optional<Connection> connectionOptional;
                    if ((connectionOptional = createConnection()).isPresent()) {
                        connectionsPool.put(connectionOptional.get(), false);
                    }
                }
                System.out.println("Отдано подключение к базе");
                checkConnection();
                return connection;
            }
        } catch (InterruptedException e) {
            System.out.println("Ошибка выделения подключения к базе данных!");
            e.printStackTrace();
        }
        return null;
    }

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

    public void stop() {
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
            if (entry.getValue().equals(true)) {
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
