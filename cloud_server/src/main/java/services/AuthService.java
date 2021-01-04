package services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {

    private static volatile AuthService instance;

    private DBConnector dbConnector;

    private AuthService() {
    }

    public static AuthService getInstance() {
        if (instance == null) {
            synchronized (AuthService.class) {
                if (instance == null) {
                    instance = new AuthService();
                }
            }
        }
        return instance;
    }

    public void start(DBConnector dbConnector) {
        this.dbConnector = dbConnector;
        resetIsLogin();
    }

    public String getUserIDByLoginAndPassword(String login, String password) {
        String userID = null;
        Connection connection = null;
        try {
            connection = dbConnector.getConnection();
            final PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM users WHERE login = ? AND password = ?"
            );
            statement.setString(1, login);
            statement.setString(2, password);
            final ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                userID = resultSet.getString("id");
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения данных из базы!");
            e.printStackTrace();
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
        }
        return userID;
    }

    public synchronized void setIsLogin(String id, boolean isLogin) {
        Connection connection = null;
        try {
            connection = dbConnector.getConnection();
            final PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users SET isLogin = ? WHERE id = ?"
            );
            statement.setInt(1, isLogin ? 1 : 0);
            statement.setString(2, id);
            statement.execute();
        } catch (SQLException e) {
            System.err.println("Ошибка изменения данных в базе!");
            e.printStackTrace();
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
        }
    }

    public boolean isLogin(String id) {
        Connection connection = null;
        try {
            connection = dbConnector.getConnection();
            final PreparedStatement statement = connection.prepareStatement(
                    "SELECT login FROM users WHERE id = ? AND isLogin = 1"
            );
            statement.setString(1, id);
            final ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения данных из базы!");
            e.printStackTrace();
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
        }
        return false;
    }

    public boolean checkIsUsedUserId(String login) {
        Connection connection = null;
        try {
            connection = dbConnector.getConnection();
            final PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM users WHERE login = ?"
            );
            statement.setString(1, login);
            final ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения данных из базы!");
            e.printStackTrace();
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
        }
        return true;
    }

    public synchronized void registerNewUser(String login, String password) {
        Connection connection = null;
        try {
            connection = dbConnector.getConnection();
            final PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO users (login, password) VALUES (?, ?)"
            );
            statement.setString(1, login);
            statement.setString(2, password);
            statement.execute();
        } catch (SQLException e) {
            System.err.println("Ошибка изменения данных в базе!");
            e.printStackTrace();
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
        }
    }

    public void stop() {
        dbConnector.close();
    }

    private synchronized void resetIsLogin() {
        Connection connection = null;
        try {
            connection = dbConnector.getConnection();
            final PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users SET isLogin = 0"
            );
            statement.execute();
        } catch (SQLException e) {
            System.err.println("Ошибка изменения данных в базе!");
            e.printStackTrace();
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
        }
    }
}
