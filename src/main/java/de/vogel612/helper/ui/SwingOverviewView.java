package de.vogel612.helper.ui;

import static de.vogel612.helper.ui.util.UiBuilder.addToGridBag;
import static java.awt.GridBagConstraints.BOTH;
import static javax.swing.JOptionPane.ERROR_MESSAGE;

import de.vogel612.helper.data.Side;
import de.vogel612.helper.data.Translation;
import de.vogel612.helper.data.TranslationTable;
import de.vogel612.helper.data.TranslationTableRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SwingOverviewView implements OverviewView {

    private static final Dimension MINIMUM_WINDOW_SIZE = new Dimension(800, 500);
    private static final Dimension DEFAULT_WINDOW_SIZE = new Dimension(1000, 700);

    private static final Dimension MENU_BAR_DIMENSION = new Dimension(800, 100);
    private static final Dimension BUTTON_DIMENSION = new Dimension(100, 40);

    private final JFrame window;
    private final JTable translationContainer;
    private final JPanel menuBar;
    private final JButton saveButton;
    private final JButton chooseLeft;
    private final JButton chooseRight;

    private final Set<BiConsumer<String, Side>> localeChangeRequestListeners = new HashSet<>();
    private final Set<Consumer<String>> translationRequestListeners = new HashSet<>();
    private final Set<Runnable> saveRequestListeners = new HashSet<>();
    private final Set<Consumer<WindowEvent>> windowCloseListeners = new HashSet<>();

    public SwingOverviewView() {
        window = new JFrame("Rubberduck Translation Helper");
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        translationContainer = new JTable();
        translationContainer.setModel(new TranslationTable());
        menuBar = new JPanel();
        saveButton = new JButton("save");
        chooseLeft = new JButton("choose left");
        chooseRight = new JButton("choose right");

        saveButton.addActionListener(event -> saveRequestListeners.forEach(Runnable::run));
        chooseLeft.addActionListener(event -> chooseAndLoadLanguage(Side.LEFT));
        chooseRight.addActionListener(event -> chooseAndLoadLanguage(Side.RIGHT));

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                windowCloseListeners.forEach(l -> l.accept(windowEvent));
            }
        });
    }

    @Override
    public void addLocaleChangeRequestListener(BiConsumer<String, Side> listener) {
        localeChangeRequestListeners.add(listener);
    }

    @Override
    public void addTranslationRequestListener(Consumer<String> listener) {
        translationRequestListeners.add(listener);
    }
    
    @Override
    public void addSaveRequestListener(Runnable listener) {
        saveRequestListeners.add(listener);
    }

    @Override
    public void addWindowClosingListener(Consumer<WindowEvent> listener) {
        windowCloseListeners.add(listener);
    }

    private void chooseAndLoadLanguage(Side side) {
        String locale = chooseLocale();
        localeChangeRequestListeners.forEach(l -> l.accept(locale, side));
    }

    private String chooseLocale() {
        // FIXME: Request LocaleOptions!?
        String[] localeOptions = {""};//presenter.getLocaleOptions();
        int selectedOption = JOptionPane.showOptionDialog(window,
          "Please choose the Locale out of following options:",
          "Choose Locale",
          JOptionPane.DEFAULT_OPTION,
          JOptionPane.QUESTION_MESSAGE,
          null,
          localeOptions,
          null);
        return localeOptions[selectedOption];
    }

    @Override
    public void initialize() {
        window.setLayout(new GridBagLayout());
        window.setSize(DEFAULT_WINDOW_SIZE);
        window.setMinimumSize(MINIMUM_WINDOW_SIZE);
        window.setBackground(new Color(0.2f, 0.3f, 0.7f, 1.0f));

        addMenuBar();
        addTranslationContainer();
        window.doLayout();
    }

    private void addTranslationContainer() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(15, 15, 15, 15);
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = BOTH;
        constraints.gridx = 0;
        constraints.gridy = 1;

        JScrollPane scroller = new JScrollPane(translationContainer);
        scroller.setMinimumSize(new Dimension(800, 400));
        scroller.setSize(new Dimension(800, 400));
        window.add(scroller, constraints);
        bindEventListener();

        translationContainer.setDefaultRenderer(Object.class,
          new TranslationTableRenderer());
    }

    private void bindEventListener() {
        translationContainer.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent event) {
                if (event.getClickCount() != 2) { // only react to doubleclicks!
                    return;
                }
                final int row = translationContainer.rowAtPoint(event.getPoint());
                final String key = ((TranslationTable) translationContainer
                  .getModel()).getKeyAt(row);
                translationRequestListeners.forEach(c -> c.accept(key));
            }
        });
    }

    private void addMenuBar() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(15, 15, 15, 15);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.fill = BOTH;
        menuBar.setLayout(new GridBagLayout());
        menuBar.setBackground(new Color(0.4f, 0.2f, 0.4f, 0.2f));

        addToGridBag(menuBar, window, MENU_BAR_DIMENSION, constraints);

        GridBagConstraints buttonConstraints = (GridBagConstraints) constraints.clone();
        buttonConstraints.gridx = GridBagConstraints.RELATIVE;
        addToGridBag(chooseLeft, menuBar, BUTTON_DIMENSION, buttonConstraints);
        addToGridBag(chooseRight, menuBar, BUTTON_DIMENSION, buttonConstraints);
        addToGridBag(saveButton, menuBar, BUTTON_DIMENSION, buttonConstraints);
    }

    @Override
    public void rebuildWith(final List<Translation> left, final List<Translation> right) {
        translationContainer.setModel(new TranslationTable(left, right));
    }

    @Override
    public void displayError(final String title, final String errorMessage) {
        JOptionPane.showMessageDialog(window, errorMessage, title, ERROR_MESSAGE);
    }

    @Override
    public void show() {
        window.setVisible(true);
    }

    @Override
    public void hide() {
        window.setVisible(false);
    }
}
