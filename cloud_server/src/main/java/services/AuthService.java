package services;

import util.SystemUser;

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

    public SystemUser getSystemUserByLogin(String login) {
        SystemUser systemUser = null;
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dbConnector.getConnection();
            statement = connection.prepareStatement(
                    "SELECT id, password FROM users WHERE login = ?"
            );
            statement.setString(1, login);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                systemUser = new SystemUser();
                systemUser.setId(resultSet.getString("id"));
                systemUser.setHashedPassword(resultSet.getString("password"));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения данных из базы!");
            e.printStackTrace();
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
            try {
                if (statement != null) statement.close();
                if (resultSet != null) resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return systemUser;
    }

    public synchronized void setIsLogin(String id, boolean isLogin) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dbConnector.getConnection();
            statement = connection.prepareStatement(
                    "UPDATE users SET isLogin = ? WHERE id = ?"
            );
            statement.setInt(1, isLogin ? 1 : 0);
            statement.setString(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка изменения данных в базе!");
            e.printStackTrace();
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isLogin(String id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dbConnector.getConnection();
            statement = connection.prepareStatement(
                    "SELECT login FROM users WHERE id = ? AND isLogin = 1"
            );
            statement.setString(1, id);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения данных из базы!");
            e.printStackTrace();
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
            try {
                if (statement != null) statement.close();
                if (resultSet != null) resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean checkNotUsedUserId(String login) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dbConnector.getConnection();
            statement = connection.prepareStatement(
                    "SELECT id FROM users WHERE login = ?"
            );
            statement.setString(1, login);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения данных из базы!");
            e.printStackTrace();
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
            try {
                if (statement != null) statement.close();
                if (resultSet != null) resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public synchronized void registerNewUser(String login, String password) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dbConnector.getConnection();
            statement = connection.prepareStatement(
                    "INSERT INTO users (login, password) VALUES (?, ?)"
            );
            statement.setString(1, login);
            statement.setString(2, password);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка изменения данных в базе!");
            e.printStackTrace();
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        dbConnector.close();
    }

    private synchronized void resetIsLogin() {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dbConnector.getConnection();
            statement = connection.prepareStatement(
                    "UPDATE users SET isLogin = 0"
            );
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка изменения данных в базе!");
            e.printStackTrace();
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
