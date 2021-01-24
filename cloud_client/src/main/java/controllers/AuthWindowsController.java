package controllers;

import commands.AuthCommand;
import commands.SignUpCommand;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import services.NetworkClient;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AuthWindowsController implements Initializable {

    @FXML
    private TextField loginText;

    @FXML
    private PasswordField passwordText;

    @FXML
    private PasswordField passwordTextRepeat;

    @FXML
    private Button signUpScreenButton;

    @FXML
    private Button logInButton;

    @FXML
    private Button signUpButton;

    @FXML
    private Label signInLabel;

    @FXML
    private Button signInScreenButton;

    private PauseTransition pause;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (pause == null) {
            pause = new PauseTransition();
        }
    }

    public void startAuthentication() {
        if (!checkTextFields()) {
            return;
        }
        if (!checkLengthsTextFields()) {
            return;
        }
        final int PAUSE_TIME = 200;
        final String login = loginText.getText().trim();
        final String password = passwordText.getText().trim();
        NetworkClient.getInstance().sendCommandToServer(new AuthCommand(login, password));
        logInButton.setDisable(true);
        setLabelError("Ожидание ответа от сервера...");
        final AuthCommand command = (AuthCommand) NetworkClient.getInstance().readCommandFromServer();
        if (command.isAuthorized()) {
            setLabelOk("Вход выполнен");
            NetworkClient.getInstance().setUserId(command.getUserID());
            runWithPause(PAUSE_TIME, event -> openMainWindow());
        } else {
            setLabelError(command.getMessage());
            logInButton.setDisable(false);
        }
    }

    public void startRegistration() {
        if (!checkLengthsTextFields()) {
            return;
        }
        if (!checkLengthsTextFields()) {
            return;
        }
        if (!passwordText.getText().equals(passwordTextRepeat.getText())) {
            setLabelError("Пароли не совпадают!");
            return;
        }
        final int PAUSE_TIME = 1500;
        final String login = loginText.getText().trim();
        final String password = passwordText.getText().trim();
        NetworkClient.getInstance().sendCommandToServer(new SignUpCommand(login, password));
        signUpButton.setDisable(true);
        setLabelError("Ожидание ответа от сервера...");
        final SignUpCommand command = (SignUpCommand) NetworkClient.getInstance().readCommandFromServer();
        if (command.isSignUp()) {
            setLabelOk("Регистрация выполнена успешно. Переход на окно авторизации");
            runWithPause(PAUSE_TIME, event -> openSignInScreen());
        } else {
            setLabelError(command.getMessage());
            signUpButton.setDisable(false);
        }
    }

    public void setLabelError(String message) {
        Platform.runLater(() -> {
            signInLabel.setText(message);
            signInLabel.setTextFill(Color.TOMATO);
        });
    }

    public void setLabelOk(String message) {
        Platform.runLater(() -> {
            signInLabel.setText(message);
            signInLabel.setTextFill(Color.GREEN);
        });
    }

    public void openSignUpScreen() {
        try {
            showNewStage((Stage) signUpScreenButton.getScene().getWindow(),
                    "/fxml/signUpScreen.fxml",
                    "Регистрация",
                    event -> {
                        NetworkClient.getInstance().stop();
                        Platform.exit();
                    });
        } catch (IOException e) {
            System.out.println("Ошибка загрузки экрана регистрации");
            e.printStackTrace();
        }
    }

    public void openSignInScreen() {
        try {
            showNewStage((Stage) signInScreenButton.getScene().getWindow(),
                    "/fxml/logInScreen.fxml",
                    "Авторизация",
                    event -> {
                        NetworkClient.getInstance().stop();
                        Platform.exit();
                    });
        } catch (IOException e) {
            System.out.println("Ошибка загрузки экрана авторизации");
            e.printStackTrace();
        }
    }

    private void showNewStage(Stage stage, String FXMLFile, String title, EventHandler<WindowEvent> onCloseEvent) throws IOException {
        final FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource(FXMLFile));
        final Parent root = fxmlLoader.load();
        final AuthWindowsController controller = fxmlLoader.getController();
        NetworkClient.getInstance().setAuthWindowsController(controller);
        final Scene newScene = new Scene(root);
        newScene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                if (title.equals("Регистрация")) {
                    controller.startRegistration();
                } else if (title.equals("Авторизация")) {
                    controller.startAuthentication();
                }
            }
        });
        newScene.getStylesheets().add((getClass().getResource("/css/style.css")).toExternalForm());
        stage.setTitle(title);
        stage.setScene(newScene);
        stage.setOnCloseRequest(onCloseEvent);
    }

    private boolean checkTextFields() {
        if (loginText.getText().trim().isEmpty() || passwordText.getText().trim().isEmpty()) {
            setLabelError("Некорректный ввод данных");
            setTextFieldsZeroLength();
            return false;
        }
        return true;
    }

    private boolean checkLengthsTextFields() {
        final int STRING_MIN_LENGTH = 3;
        if (loginText.getText().length() < STRING_MIN_LENGTH) {
            setLabelError("Слишком короткое имя пользователя. Допустимо не менее 3-х символов.");
            return false;
        } else if (passwordText.getText().length() < STRING_MIN_LENGTH) {
            setLabelError("Слишком короткий пароль. Допустимо не менее 3-х символов.");
            setTextFieldsZeroLength();
            return false;
        }
        return true;
    }

    private void setTextFieldsZeroLength() {
//        loginText.setText("");
        passwordText.setText("");
        if (passwordTextRepeat != null) {
            passwordTextRepeat.setText("");
        }
    }

    private void runWithPause(int duration, EventHandler<ActionEvent> event) {
        pause.setDuration(Duration.millis(duration));
        pause.setOnFinished(event);
        pause.play();
    }

    private void openMainWindow() {
        try {
            final int SCREEN_HEIGHT = 550;
            final int SCREEN_WIDTH = 920;
            final Stage mainWindow = new Stage();
            final FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/mainWindow.fxml"));
            final Scene mainScene = new Scene(loader.load());
            final MainWindowController controller = loader.getController();
            mainScene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
                if (event.getCode().equals(KeyCode.DELETE)) {
                    controller.deleteButtonAction();
                }
                if (event.getCode().equals(KeyCode.SPACE)) {
                    if (controller.getMaximizeOperations().isDisabled()) {
                        controller.minimizeOperationsTable();
                    } else {
                        controller.maximizeOperationsTable();
                    }
                }
            });
            mainScene.getStylesheets().add((getClass().getResource("/css/style.css")).toExternalForm());
            mainWindow.setTitle("Cloud Drive");
            mainWindow.setScene(mainScene);
            mainWindow.setOnCloseRequest(controller::onExitAction);
            mainWindow.getIcons().add(new Image("img/network_drive.png"));

            mainWindow.setMinHeight(SCREEN_HEIGHT);
            mainWindow.setMinWidth(SCREEN_WIDTH);

            logInButton.getScene().getWindow().hide();
            mainWindow.show();
        } catch (IOException e) {
            System.out.println("Ошибка загрузки главного экрана приложения");
            e.printStackTrace();
        }
    }
}
