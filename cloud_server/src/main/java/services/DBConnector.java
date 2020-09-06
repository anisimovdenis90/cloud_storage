package services;

import java.sql.Connection;

public interface DBConnector {

    Connection getConnection();

    void closeConnection(Connection connection);

}
