package de.vogel612.helper.ui.jfx;

import de.vogel612.helper.data.Translation;
import de.vogel612.helper.ui.TranslationView;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Created by vogel612 on 02.03.16.
 */
public class JFXTranslationView implements TranslationView {

    private final Stage stage;
    private final Scene ui;
    private final JFXTranslationController controller;

    public JFXTranslationView(Stage stage, URL fxml) throws IOException {
        this.stage = stage;
        FXMLLoader loader = new FXMLLoader(fxml);
        ui = new Scene(loader.load());
        controller = loader.getController();
    }

    @Override
    public void addTranslationSubmitListener(Consumer<Translation> listener) {
        controller.addTranslationSubmitListener(listener);
    }

    @Override
    public void addTranslationAbortListener(Runnable listener) {
        controller.addTranslationAbortListener(listener);
    }

    @Override
    public void show() {
        stage.setScene(ui);
        stage.show();
    }

    @Override
    public void hide() {
        stage.hide();
    }

    @Override
    public void setRequestedTranslation(Translation left, Translation right) {
        controller.setRequestedTranslation(left, right);
    }
}