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
        try {
            resetIsLogin();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public SystemUser getSystemUserByLogin(String login) throws SQLException {
        final Connection connection = dbConnector.getConnection();
        final String sql = "SELECT id, password FROM users WHERE login = ?";
        SystemUser systemUser = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            final ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                systemUser = new SystemUser();
                systemUser.setId(resultSet.getString("id"));
                systemUser.setHashedPassword(resultSet.getString("password"));
            }
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
        }
        return systemUser;
    }

    public synchronized void setIsLogin(String id, boolean isLogin) throws SQLException {
        final Connection connection = dbConnector.getConnection();
        final String sql = "UPDATE users SET isLogin = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, isLogin ? 1 : 0);
            statement.setString(2, id);
            statement.executeUpdate();
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
        }
    }

    public boolean isLogin(String id) throws SQLException {
        final Connection connection = dbConnector.getConnection();
        final String sql = "SELECT login FROM users WHERE id = ? AND isLogin = 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            final ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
        }
        return false;
    }

    public boolean checkNotUsedUserId(String login) throws SQLException {
        final Connection connection = dbConnector.getConnection();
        final String sql = "SELECT id FROM users WHERE login = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
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

    public synchronized void registerNewUser(String login, String password) throws SQLException {
        final Connection connection = dbConnector.getConnection();
        final String sql = "INSERT INTO users (login, password) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            statement.setString(2, password);
            statement.executeUpdate();
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

    private synchronized void resetIsLogin() throws SQLException {
        final Connection connection = dbConnector.getConnection();
        final String sql = "UPDATE users SET isLogin = 0";
        try (PreparedStatement statement = connection.prepareStatement(sql);) {
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка изменения данных в базе!");
            e.printStackTrace();
        } finally {
            if (connection != null) dbConnector.closeConnection(connection);
        }
    }
}
