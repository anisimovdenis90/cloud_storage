package services;

import java.sql.Connection;
import java.sql.SQLException;

public interface DBConnection {

    String PROPERTIES_DB_DRIVER = "server.db.driver";
    String PROPERTIES_DB_URL = "server.db.url";
    String PROPERTIES_DB_USERNAME = "server.db.username";
    String PROPERTIES_DB_PASSWORD = "server.db.password";

    Connection createConnection() throws SQLException;
}
