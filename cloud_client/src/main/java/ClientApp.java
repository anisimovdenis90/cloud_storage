import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import services.NetworkClient;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/logInScreen.fxml"));
        primaryStage.setTitle("Авторизация");
        Scene scene = new Scene(root);
        scene.getStylesheets().add((getClass().getResource("/css/action_style.css")).toExternalForm());
        primaryStage.setScene(scene);
        Image image = new Image("img/network_drive.png");
        primaryStage.getIcons().add(image);
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(event -> {
            NetworkClient.getInstance().stop();
            Platform.exit();
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
