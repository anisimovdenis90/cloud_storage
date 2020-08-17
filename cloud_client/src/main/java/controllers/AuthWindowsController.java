package controllers;

import commands.AuthCommand;
import commands.SignUpCommand;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.NetworkClient;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;

public class AuthWindowsController implements Initializable {

    @FXML
    private AnchorPane signInPane;

    @FXML
    private TextField loginText;

    @FXML
    private PasswordField passwordText;

    @FXML
    private Button signUpScreenButton;

    @FXML
    private Button logInButton;

    @FXML
    private Label signInLabel;

    @FXML
    private Button signInScreenButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        NetworkClient.getInstance().start();
    }

    private void openMainWindow() {
        try {
            Stage mainWindow = new Stage();
            Scene mainScene = new Scene(FXMLLoader.load(getClass().getResource("/fxml/mainWindow.fxml")));
            mainScene.getStylesheets().add((getClass().getResource("/css/action_style.css")).toExternalForm());
            mainWindow.setTitle("Cloud Drive");
            mainWindow.setScene(mainScene);
            mainWindow.setOnCloseRequest(event -> {
                NetworkClient.getInstance().stop();
                Platform.exit();
            });
            Image image = new Image("img/network_drive.png");
            mainWindow.getIcons().add(image);
            mainWindow.setResizable(false);
            logInButton.getScene().getWindow().hide();
            mainWindow.show();
        } catch (IOException e) {
            System.out.println("Ошибка загрузки главного экрана приложения");
            e.printStackTrace();
        }

    }

    public void startAuthentication() {
        String login = loginText.getText().trim();
        String password = passwordText.getText().trim();
        if (login.isEmpty() || password.isEmpty()) {
            signInLabel.setText("Некорректный ввод данных");
            signInLabel.setTextFill(Color.TOMATO);
            loginText.setText("");
            passwordText.setText("");
            return;
        }
        NetworkClient.getInstance().sendCommandToServer(new AuthCommand(login, password));
        AuthCommand command = (AuthCommand) NetworkClient.getInstance().readCommandFromServer();
        if (command.isAuthorized()) {
            logInButton.setDisable(true);
            signInLabel.setText("Вход выполнен");
            signInLabel.setTextFill(Color.GREEN);
            NetworkClient.getInstance().setUserId(command.getUserID());
            PauseTransition ps = new PauseTransition();
            ps.setDuration(Duration.millis(200));
            ps.setOnFinished(event -> openMainWindow());
            ps.play();
        } else {
            signInLabel.setText(command.getMessage());
            signInLabel.setTextFill(Color.TOMATO);
        }
    }

    public void startSignUp() {
        String login = loginText.getText().trim();
        String password = passwordText.getText().trim();
        if (login.isEmpty() || password.isEmpty()) {
            signInLabel.setText("Некорректный ввод данных");
            signInLabel.setTextFill(Color.TOMATO);
            loginText.setText("");
            passwordText.setText("");
            return;
        }
        NetworkClient.getInstance().sendCommandToServer(new SignUpCommand(login, password));
        SignUpCommand command = (SignUpCommand) NetworkClient.getInstance().readCommandFromServer();
        if (command.isSignUp()) {
            signInLabel.setText("Регистрация выполнена успешно. Переход на окно авторизации");
            signInLabel.setTextFill(Color.GREEN);
            PauseTransition ps = new PauseTransition();
            ps.setDuration(Duration.millis(1500));
            ps.setOnFinished(event -> openSignInScreen());
            ps.play();
        } else {
            signInLabel.setText(command.getMessage());
            signInLabel.setTextFill(Color.TOMATO);
        }
    }

    public void openSignUpScreen() {
        try {
            Scene mainScene = new Scene(FXMLLoader.load(getClass().getResource("/fxml/signUpScreen.fxml")));
            mainScene.getStylesheets().add((getClass().getResource("/css/action_style.css")).toExternalForm());
            Stage signInWindow = (Stage) signUpScreenButton.getScene().getWindow();
            signInWindow.setTitle("Регистрация");
            signInWindow.setScene(mainScene);
            signInWindow.setOnCloseRequest(event -> {
                NetworkClient.getInstance().stop();
                Platform.exit();
            });
            signInWindow.show();
        } catch (IOException e) {
            System.out.println("Ошибка загрузки экрана регистрации");
            e.printStackTrace();
        }
    }

    public void openSignInScreen() {
        try {
            Scene mainScene = new Scene(FXMLLoader.load(getClass().getResource("/fxml/logInScreen.fxml")));
            mainScene.getStylesheets().add((getClass().getResource("/css/action_style.css")).toExternalForm());
            Stage signUpWindow = (Stage) signInScreenButton.getScene().getWindow();
            signUpWindow.setTitle("Авторизация");
            signUpWindow.setScene(mainScene);
            signUpWindow.setOnCloseRequest(event -> {
                NetworkClient.getInstance().stop();
                Platform.exit();
            });
            signUpWindow.show();
        } catch (IOException e) {
            System.out.println("Ошибка загрузки экрана авторизации");
            e.printStackTrace();
        }
    }
}
