import controllers.AuthWindowsController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import services.NetworkClient;

public class ClientApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        final FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/fxml/logInScreen.fxml"));
        final Parent root = fxmlLoader.load();
        final AuthWindowsController controller = fxmlLoader.getController();
        NetworkClient.getInstance().setAuthWindowsController(controller);
        NetworkClient.getInstance().start();
        final Scene scene = new Scene(root);
        scene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                controller.startAuthentication();
            }
        });
        scene.getStylesheets().add((getClass().getResource("/css/style.css")).toExternalForm());
        primaryStage.setTitle("Авторизация");
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image("img/network_drive.png"));
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(event -> {
            NetworkClient.getInstance().stop();
            Platform.exit();
        });
        primaryStage.show();
    }
}
