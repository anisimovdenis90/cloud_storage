package util;

import java.io.IOException;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class StageBuilder {
    private final Stage stage;
    private Scene scene;
    private FXMLLoader fxmlLoader;
    private Object controller;
    private double xOffset;
    private double yOffset;
    private String stylesheet;
    private boolean isActiveMouseMoved = false;

    private StageBuilder(Stage stage) {
        this.stage = stage;
    }

    public Scene getScene() {
        return this.scene;
    }

    public Stage getStage() {
        return this.stage;
    }

    public <T> T getController() {
        return this.fxmlLoader.getController();
    }

    public static StageBuilder build(Stage stage) {
        return new StageBuilder(stage);
    }

    public static StageBuilder build() {
        return new StageBuilder(new Stage());
    }

    public StageBuilder addResource(String resource) throws IOException {
        return this.addResource(resource, null);
    }

    public StageBuilder addResource(String resource, Object controller) throws IOException {
        this.fxmlLoader = new FXMLLoader();
        if (controller != null) {
            this.controller = controller;
            this.fxmlLoader.setController(controller);
        }

        if (this.controller != null) {
            this.fxmlLoader.setController(this.controller);
        }

        this.fxmlLoader.setLocation(this.getClass().getResource(resource));
        this.scene = new Scene(this.fxmlLoader.load());
        this.addStylesheet();
        this.setMouseMoved();
        if (this.controller == null) {
            this.controller = this.fxmlLoader.getController();
        }

        this.stage.setScene(this.scene);
        return this;
    }

    public StageBuilder setTitle(String title) {
        this.stage.setTitle(title);
        return this;
    }

    public StageBuilder setIcon(String url) {
        this.stage.getIcons().add(new Image(url));
        return this;
    }

    public StageBuilder setResizable(boolean value) {
        this.stage.setResizable(value);
        return this;
    }

    public StageBuilder addStylesheet(String name) {
        this.stylesheet = this.getClass().getResource(name).toExternalForm();
        this.addStylesheet();
        return this;
    }

    public StageBuilder setMouseMoved(boolean value) {
        this.isActiveMouseMoved = value;
        if (this.scene != null) {
            this.setMouseMoved();
        }

        return this;
    }

    public StageBuilder setOnClosedAction(EventHandler<WindowEvent> value) {
        this.stage.setOnCloseRequest(value);
        return this;
    }

    public <T extends Event> StageBuilder addSceneEventHandler(EventType<T> eventType, EventHandler<? super T> eventHandler) {
        this.scene.addEventHandler(eventType, eventHandler);
        return this;
    }

    public <T extends Event> StageBuilder addStageEventHandler(EventType<T> eventType, EventHandler<? super T> eventHandler) {
        this.stage.addEventHandler(eventType, eventHandler);
        return this;
    }

    public StageBuilder setMinHeight(double value) {
        this.stage.setMinHeight(value);
        return this;
    }

    public StageBuilder setMinWidth(double value) {
        this.stage.setMinWidth(value);
        return this;
    }

    private void addStylesheet() {
        if (this.stylesheet != null) {
            this.scene.getStylesheets().add(this.stylesheet);
        }

    }

    private void setMouseMoved() {
        if (this.isActiveMouseMoved) {
            this.scene.setOnMousePressed((event) -> {
                this.xOffset = this.stage.getX() - event.getScreenX();
                this.yOffset = this.stage.getY() - event.getScreenY();
            });
            this.scene.setOnMouseDragged((event) -> {
                this.stage.setX(event.getScreenX() + this.xOffset);
                this.stage.setY(event.getScreenY() + this.yOffset);
            });
        }

    }
}
