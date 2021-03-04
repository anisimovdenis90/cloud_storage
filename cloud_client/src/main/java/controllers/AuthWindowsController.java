package controllers;

import commands.AuthCommand;
import commands.CheckLoginCommand;
import commands.SignUpCommand;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;
import services.NetworkClient;
import util.StageBuilder;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AuthWindowsController implements Initializable {

    @FXML
    private TextField loginText;

    @FXML
    private TextField signUpText;

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

    private StageBuilder stageBuilder;

    public void setStageBuilder(StageBuilder stageBuilder) {
        this.stageBuilder = stageBuilder;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (pause == null) {
            pause = new PauseTransition();
        }
        if (signUpText != null) {
            signUpText.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    checkUserLogin();
                }
            });
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
        NetworkClient.getInstance().sendCommandToServer(new AuthCommand(login));
        logInButton.setDisable(true);
        setLabelError("Ожидание ответа от сервера...");
        final AuthCommand command = (AuthCommand) NetworkClient.getInstance().readCommandFromServer();
        if (!command.isAuthorized()) {
            if (BCrypt.checkpw(password, command.getPassword())) {
                command.setAuthorized(true);
                NetworkClient.getInstance().sendCommandToServer(command);
                setLabelOk("Вход выполнен");
                NetworkClient.getInstance().setUserId(command.getId());
                runWithPause(PAUSE_TIME, event -> openMainWindow());
            } else {
                setLabelError("Неверный логин или пароль");
                logInButton.setDisable(false);
            }
        } else {
            setLabelError(command.getMessage());
            logInButton.setDisable(false);
        }
    }

    public void checkUserLogin() {
        if (signUpText == null) {
            return;
        }
        final String login = signUpText.getText().trim();
        final int MIN_LOGIN_LENGTH = 3;
        if (login.isEmpty()) {
            return;
        }
        if (login.length() < MIN_LOGIN_LENGTH) {
            setLabelError("Слишком короткое имя пользователя. Допустимо не менее 3-х символов.");
            return;
        }
        NetworkClient.getInstance().sendCommandToServer(new CheckLoginCommand(login));
        final CheckLoginCommand command = (CheckLoginCommand) NetworkClient.getInstance().readCommandFromServer();
        if (!command.isFree()) {
            setLabelError(command.getMessage());
        } else {
            setLabelError("");
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
        final String login = signUpText.getText().trim();
        final String password = BCrypt.hashpw(passwordText.getText().trim(), BCrypt.gensalt());
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
        showNewStage("/fxml/signUpScreen.fxml", "Регистрация");
    }

    public void openSignInScreen() {
        showNewStage("/fxml/logInScreen.fxml", "Авторизация");
    }

    private void showNewStage(String FXMLFile, String title) {
        try {
            stageBuilder.addResource(FXMLFile)
                    .addSceneEventHandler(KeyEvent.KEY_RELEASED, event -> {
                        if (event.getCode().equals(KeyCode.ENTER)) {
                            if (title.equals("Регистрация")) {
                                ((AuthWindowsController) stageBuilder.getController()).startRegistration();
                            } else if (title.equals("Авторизация")) {
                                ((AuthWindowsController) stageBuilder.getController()).startAuthentication();
                            }
                        }
                    })
                    .setTitle(title);
        } catch (IOException e) {
            System.out.println("Ошибка загрузки fxml ресурса " + FXMLFile);
            e.printStackTrace();
        }
    }

    private boolean checkTextFields() {
        if ((signUpText != null && signUpText.getText().trim().isEmpty())
                || (loginText != null && loginText.getText().trim().isEmpty())
                || passwordText.getText().trim().isEmpty()) {
            setLabelError("Некорректный ввод данных");
            setTextFieldsZeroLength();
            return false;
        }
        return true;
    }

    private boolean checkLengthsTextFields() {
        final int STRING_MIN_LENGTH = 3;
        if ((loginText != null && loginText.getText().length() < STRING_MIN_LENGTH)
                || (signUpText != null && signUpText.getText().length() < STRING_MIN_LENGTH)) {
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
            final String MAIN_WINDOW_FXML = "/fxml/mainWindow.fxml";
            final MainWindowController controller = new MainWindowController();

            final StageBuilder stageBuilder = StageBuilder.build()
                    .addResource(MAIN_WINDOW_FXML, controller)
                    .addSceneEventHandler(KeyEvent.KEY_RELEASED, event -> {
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
                    })
                    .addStylesheet("/css/style.css")
                    .setTitle("Cloud Drive")
                    .setIcon("img/network_drive.png")
                    .setOnClosedAction(controller::onExitAction)
                    .setMinWidth(SCREEN_WIDTH)
                    .setMinHeight(SCREEN_HEIGHT);

            logInButton.getScene().getWindow().hide();
            stageBuilder.getStage().show();
        } catch (IOException e) {
            System.out.println("Ошибка загрузки главного экрана приложения");
            e.printStackTrace();
        }
    }
}
