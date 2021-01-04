package services;

import java.sql.Connection;
import java.sql.SQLException;

public interface DBConnector {

    Connection getConnection() throws SQLException;

    void closeConnection(Connection connection);

    void close();
}
