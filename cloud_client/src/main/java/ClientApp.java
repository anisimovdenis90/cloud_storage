import controllers.AuthWindowsController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import services.NetworkClient;
import util.StageBuilder;

public class ClientApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        final AuthWindowsController controller = new AuthWindowsController();
        NetworkClient.getInstance().setAuthWindowsController(controller);
        NetworkClient.getInstance().start();

        final StageBuilder stageBuilder = StageBuilder.build(primaryStage)
                .addResource("/fxml/logInScreen.fxml", controller)
                .addSceneEventHandler(KeyEvent.KEY_RELEASED, event -> {
                    if (event.getCode().equals(KeyCode.ENTER)) {
                        controller.startAuthentication();
                    }
                })
                .addStylesheet("/css/style.css")
                .setTitle("Авторизация")
                .setIcon("img/network_drive.png")
                .setResizable(false)
                .setMouseMoved(true)
                .setOnClosedAction(event -> {
                    NetworkClient.getInstance().stop();
                    Platform.exit();
                });
        controller.setStageBuilder(stageBuilder);
        stageBuilder.getStage().show();
    }
}
