package controllers;

import commands.AuthCommand;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import services.NetworkClient;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private Scene authScene;

    @FXML
    private TextField loginText;

    @FXML
    private PasswordField passwordText;

    @FXML
    private Button signupButton;

    @FXML
    private Button logInButton;

    @FXML
    private Label signInLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        NetworkClient.getInstance().start();
    }

    private void openMainWindow() {
        try {
            Scene mainScene = new Scene(FXMLLoader.load(getClass().getResource("/mainWindow.fxml")));
            Stage window = (Stage) authScene.getWindow();
            window.setScene(mainScene);
            window.show();
        } catch (IOException e) {
            System.out.println("Ошибка загрузки главного экрана приложения");
            e.printStackTrace();
        }

    }

    public void startAuthentication(ActionEvent actionEvent) {
        authScene = ((Node)actionEvent.getSource()).getScene();
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
            signInLabel.setText("Вход выполнен");
            signInLabel.setTextFill(Color.GREEN);
            NetworkClient.getInstance().setUserId(command.getUserID());
            NetworkClient.getInstance().createClientDir();
            openMainWindow();
        } else {
            signInLabel.setText(command.getMessage());
            signInLabel.setTextFill(Color.TOMATO);
        }
    }
}
