package de.vogel612.helper;

import de.vogel612.helper.data.OverviewModel;
import de.vogel612.helper.ui.OverviewPresenter;
import de.vogel612.helper.ui.OverviewView;
import de.vogel612.helper.ui.jfx.JFXOverviewView;
import de.vogel612.helper.ui.jfx.JFXResxChooser;
import de.vogel612.helper.ui.swing.SwingOverviewView;
import de.vogel612.helper.ui.TranslationPresenter;

import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class TranslationHelper extends Application {

    static final String ARGUMENT_MISMATCH = "Arguments do not match up. Please provide no more than a Path to the intended fileset";


    public static void main(String[] args) {

        if (args.length > 1) {
            // don't even bother!
            System.out.println(ARGUMENT_MISMATCH);
            return;
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // FIXME: Make this the DI Context root and wire up stuff here instead of in the OverviewPresenter!
        FXMLLoader resxLoader = new FXMLLoader(getClass().getResource("/ResxChooser.fxml"));

        GridPane rcPane = resxLoader.load();
        JFXResxChooser rc = resxLoader.getController();
        Parameters params = getParameters();
        if (params.getUnnamed().size() != 0) { // should be 1..
            final Path resxFile = Paths.get(params.getUnnamed().get(0));
            rc.setFileset(resxFile);
        }

        // That's a view though.. duh.
        TranslationPresenter tp = new TranslationPresenter();

        FXMLLoader overviewLoader = new FXMLLoader(getClass().getResource("/OverviewView.fxml"));
        StackPane overviewPane = overviewLoader.load();
        JFXOverviewView v = overviewLoader.getController();

        OverviewModel m = new OverviewModel();
//        OverviewPresenter p = new OverviewPresenter(primaryStage, rcPane, overviewPane);
        OverviewPresenter p = new OverviewPresenter(null, null, null, null); // just to stop the compiler from screaming at me

        // Wire up all the crap
        v.addLanguageRequestListener(p::fileChoosing);
        v.addSaveRequestListener(p::onSaveRequest);
        v.addTranslationRequestListener(p::onTranslateRequest);
        v.addWindowClosingListener(p::onWindowCloseRequest);

        m.addParseCompletionListener(p::onParseCompletion);

        tp.addTranslationAbortListener(p::onTranslationAbort);
        tp.addTranslationSubmitListener(p::onTranslationSubmit);

        rc.addCompletionListener(p::fileChoiceCompletion);

        p.show();
        p.fileChoosing();

        Stage resxStage = new Stage(StageStyle.UNDECORATED);
        resxStage.setScene(new Scene(rcPane));
        resxStage.setTitle("Choose Your Resx");

        primaryStage.setTitle("Translation Helper");
        primaryStage.setScene(new Scene(overviewPane));
        primaryStage.show();
        resxStage.show();
    }
}