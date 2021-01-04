package services;

import java.sql.Connection;
import java.sql.SQLException;

public interface DBConnection {

    Connection createConnection() throws SQLException;
}
