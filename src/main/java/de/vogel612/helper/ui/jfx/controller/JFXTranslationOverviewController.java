package de.vogel612.helper.ui.jfx.controller;

import de.vogel612.helper.data.NotableData;
import de.vogel612.helper.data.Translation;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import de.vogel612.helper.ui.jfx.TranslationPair;
import javafx.application.Platform;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

/**
 * Created by vogel612 on 01.03.16.
 */
public class JFXTranslationOverviewController implements Initializable {
    private final Set<Consumer<String>> translationRequestListeners = new HashSet<>();
    private final Set<Runnable> langChoiceRequestListeners = new HashSet<>();
    private final Set<Runnable> fileChoiceRequestListeners = new HashSet<>();
    private final Set<Runnable> saveRequestListeners = new HashSet<>();

    @FXML
    private Label fileset;

    @FXML
    private Button save;

    @FXML
    private Button chooseFile;

    @FXML
    private Button chooseLang;

    @FXML
    private TableView<TranslationPair> table;

    public void rebuildWith(List<Translation> left, List<Translation> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return;
        }
        table.setItems(buildObservableList(left, right));
        table.getColumns().get(0).setText(left.get(0).getLocale());
        table.getColumns().get(1).setText(right.get(0).getLocale());
    }

    private static ObservableList<TranslationPair> buildObservableList(List<Translation> left,
                                                                       List<Translation> right) {
        List<TranslationPair> result = new ArrayList<>();
        final int limit = Math.min(left.size(), right.size());
        for (int i = 0; i < limit; i++) {
            result.add(new TranslationPair(left.get(i), right.get(i)));
        }
        return FXCollections.observableList(result);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Objects.requireNonNull(save, "save was not FXML-injected correctly");
        Objects.requireNonNull(table, "table was not FXML-injected correctly");
        Objects.requireNonNull(chooseLang, "chooseLang was not FXML-injected correctly");
        Objects.requireNonNull(fileset, "fileset was not FXML-injected correctly");
        Objects.requireNonNull(chooseFile, "chooseFile was not FXML-injected correctly");

        save.setOnAction(evt -> Platform.runLater(() ->  saveRequestListeners.forEach(Runnable::run)));
        chooseLang.setOnAction(evt -> Platform.runLater(() -> langChoiceRequestListeners.forEach(Runnable::run)));
        chooseFile.setOnAction(evt -> Platform.runLater(() -> fileChoiceRequestListeners.forEach(Runnable::run)));

        table.setEditable(false);
        table.getColumns().clear();
        Callback<TableColumn<TranslationPair, String>, TableCell<TranslationPair, String>> cellRenderer = createTableCellRenderer();
        table.getColumns().add(createTableColumn(cellRenderer, data -> data.getLeft().getValue()));
        table.getColumns().add(createTableColumn(cellRenderer, data -> data.getRight().getValue()));
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && table.getSelectionModel().getSelectedItem() != null) {
                translationRequestListeners.forEach(listener -> {
                    // so many assumptions :/
                    listener.accept(selectedKey(table));
                });
            }
        });
    }

    private Callback<TableColumn<TranslationPair, String>, TableCell<TranslationPair, String>> createTableCellRenderer() {
        return column -> {
            TableCell<TranslationPair, String> cell = new TableCell<TranslationPair, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(item);
                    TranslationPair rowValue = (TranslationPair) getTableRow().getItem();
                    if (rowValue != null) {
                        assignHighlightClasses(rowValue);
                    }
                }

                private void assignHighlightClasses(TranslationPair rowValue) {
                    getStyleClass().remove("default");
                    getStyleClass().remove("warn");
                    getStyleClass().remove("error");

                    switch (NotableData.assessNotability(rowValue)) {
                        case INFO:
                        case DEFAULT:
                            getStyleClass().add("default");
                            break;
                        case WARNING:
                            getStyleClass().add("warn");
                            break;
                        case ERROR:
                            getStyleClass().add("error");
                            break;
                    }
                }
            };
            cell.addEventFilter(MouseEvent.MOUSE_CLICKED, evt -> {
                // assume the double-click selected the relevant row....
                if (evt.getButton() != MouseButton.PRIMARY || evt.getClickCount() != 2) {
                    return;
                }
                translationRequestListeners.forEach(listener -> listener.accept(selectedKey(table)));
            });
            return cell;
        };
    }

    private static String selectedKey(final TableView<TranslationPair> table) {
        return table.getSelectionModel().getSelectedItem().getRight().getKey();
    }

    private static <S, T> TableColumn<S, T> createTableColumn(final Callback<TableColumn<S, T>, TableCell<S, T>> renderer,
                                                              final Function<S, T> valueFactory) {
        TableColumn<S, T> result = new TableColumn<>("");
        result.setCellValueFactory(data -> new ObservableValueBase<T>() {
            @Override
            public T getValue() {
                return valueFactory.apply(data.getValue());
            }
        });
        result.setCellFactory(renderer);
        return result;
    }

    public final void addLangChoiceListener(Runnable listener) {
        langChoiceRequestListeners.add(listener);
    }

    public final void addFileRequestListener(Runnable listener) {
        fileChoiceRequestListeners.add(listener);
    }

    public void addSaveRequestListener(Runnable listener) {
        saveRequestListeners.add(listener);
    }

    public void addTranslationRequestListener(Consumer<String> listener) {
        translationRequestListeners.add(listener);
    }
}
