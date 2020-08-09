import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("logInScreen.fxml"));
        primaryStage.setTitle("Cloud Drive");
        primaryStage.setScene(new Scene(root));

        InputStream iconStream = getClass().getResourceAsStream("network_drive.png");
        Image image = new Image(iconStream);
        primaryStage.getIcons().add(image);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
