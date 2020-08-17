package services;

import java.sql.*;

public class AuthService {

    private String dbDriver = "com.mysql.cj.jdbc.Driver";
    private String dbUrl = "jdbc:mysql://localhost:3306/";
    private String dbUsername = "root";
    private String dbPassword = "gtr120519";
    private String dbName = "cloud_users";

    private String timeZoneConfiguration = "?serverTimezone=Europe/Moscow&useSSL=false";

    public void start() {
        try {
            Class.forName(dbDriver);
            System.out.println("Сервер авторизации запущен");
            resetIsLogin();
//            netty.NetworkServer.getInfoLogger().info("Подключение к базе данных установлено");
        } catch (ClassNotFoundException e) {
            System.err.println("Ошибка загрузки драйвера базы данных!");
            e.printStackTrace();
//            netty.NetworkServer.getFatalLogger().fatal("Ошибка загрузки драйвера базы данных!", e);
        }
    }

    private Connection getConnection() {
        try {
            return DriverManager.getConnection(dbUrl + dbName + timeZoneConfiguration, dbUsername, dbPassword);
        } catch (SQLException e) {
            System.err.println("Ошибка подключения к базе данных!");
            e.printStackTrace();
        }
        return null;
    }

    public void closeConnection(Connection connection) {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("Ошибка закрытия соединения с базой данных");
            e.printStackTrace();
//            netty.NetworkServer.getFatalLogger().fatal("Ошибка закрытия соединения с базой данных", e);
        }
    }

    public synchronized String getUserIDByLoginAndPassword(String login, String password) {
        String userID = null;
        try {
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM users WHERE login = ? AND password = ?"
            );
            statement.setString(1, login);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                userID = resultSet.getString("id");
            }
            closeConnection(connection);
        } catch (SQLException e) {
            System.err.println("Ошибка получения данных из базы!");
            e.printStackTrace();
//            netty.NetworkServer.getFatalLogger().fatal("Ошибка получения данных из базы!", e);
        }
        return userID;
    }

    public void setIsLogin(String id, boolean isLogin) {
        try {
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users SET isLogin = ? WHERE id = ?"
            );
            statement.setInt(1, isLogin ? 1 : 0);
            statement.setString(2, id);
            statement.execute();
            closeConnection(connection);
        } catch (SQLException e) {
            System.err.println("Ошибка изменения данных в базе!");
            e.printStackTrace();
        }
    }

    public boolean isLogin(String id) {
        try {
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT login FROM users WHERE id = ? AND isLogin = 1"
            );
            statement.setString(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
            closeConnection(connection);
        } catch (SQLException e) {
            System.err.println("Ошибка получения данных из базы!");
            e.printStackTrace();
        }
        return false;
    }

    private void resetIsLogin() {
        try {
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users SET isLogin = 0"
            );
            statement.execute();
            closeConnection(connection);
        } catch (SQLException e) {
            System.err.println("Ошибка изменения данных в базе!");
            e.printStackTrace();
        }
    }
}
