package ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import core.GenerationException;
import core.GenerationResult;
import core.WriteOffService;
import helpers.Constants;

/**
 * Главное окно программы: выбор файла остатков, выбор папки результата
 * и запуск формирования актов списания.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String PREF_LAST_INPUT_FILE = "lastInputFile";
    private static final String PREF_LAST_INPUT_DIR = "lastInputDir";
    private static final String PREF_LAST_OUTPUT_DIR = "lastOutputDir";

    private final Preferences preferences = Preferences.userNodeForPackage(MainFrame.class);

    private final JTextField inputField = new JTextField();
    private final JTextField outputField = new JTextField();
    private final JCheckBox strongCheckBox = new JCheckBox("Крепкий алкоголь — ActWriteOffStrong.xml", true);
    private final JCheckBox beerCheckBox = new JCheckBox("Пиво и пивные напитки — ActWriteOffBeer.xml", true);
    private final JTextArea logArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Готово к работе");
    private final JProgressBar progressBar = new JProgressBar();

    private final JButton browseInputButton = new JButton("Обзор…");
    private final JButton browseOutputButton = new JButton("Обзор…");
    private final JButton generateButton = new JButton("Сформировать акты");
    private final JButton openFolderButton = new JButton("Открыть папку");
    private final JMenuItem chooseInputItem = new JMenuItem("Выбрать файл остатков…");
    private final JMenuItem chooseOutputItem = new JMenuItem("Выбрать папку результата…");

    public MainFrame() {
        super(Constants.APP_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 520));
        buildUi();
        wireActions();
        installDropTarget();
        restorePreferences();
        pack();
        setSize(860, 620);
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------
    // Построение интерфейса
    // ------------------------------------------------------------------

    private void buildUi() {
        setJMenuBar(buildMenuBar());
        setLayout(new BorderLayout(0, 0));
        add(buildFormPanel(), BorderLayout.NORTH);
        add(buildLogPanel(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Файл");
        fileMenu.add(chooseInputItem);
        fileMenu.add(chooseOutputItem);
        fileMenu.add(new JSeparator());
        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK));
        exitItem.addActionListener(e -> dispose());
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        JMenu helpMenu = new JMenu("Справка");
        JMenuItem aboutItem = new JMenuItem("О программе");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(4, 0, 4, 8);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(4, 0, 4, 8);

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridx = 2;
        buttonConstraints.insets = new Insets(4, 0, 4, 0);

        // Файл остатков
        addRow(form, new JLabel("Файл остатков ЕГАИС:"), inputField, browseInputButton, 0,
                labelConstraints, fieldConstraints, buttonConstraints);
        inputField.setToolTipText("Выгрузка остатков из ЕГАИС, файл вида ReplyRests_v2.xml");

        // Папка результата
        addRow(form, new JLabel("Папка для результата:"), outputField, browseOutputButton, 1,
                labelConstraints, fieldConstraints, buttonConstraints);
        outputField.setToolTipText("Каталог, в который будут сохранены акты списания");

        // Что формировать
        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        options.setBorder(BorderFactory.createTitledBorder("Что формировать"));
        options.add(strongCheckBox);
        options.add(beerCheckBox);

        GridBagConstraints optionsConstraints = new GridBagConstraints();
        optionsConstraints.gridx = 0;
        optionsConstraints.gridy = 2;
        optionsConstraints.gridwidth = 3;
        optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
        optionsConstraints.insets = new Insets(10, 0, 0, 0);
        form.add(options, optionsConstraints);

        // Кнопки
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(generateButton);
        buttons.add(openFolderButton);
        generateButton.setPreferredSize(new Dimension(180, generateButton.getPreferredSize().height));

        GridBagConstraints buttonsConstraints = new GridBagConstraints();
        buttonsConstraints.gridx = 0;
        buttonsConstraints.gridy = 3;
        buttonsConstraints.gridwidth = 3;
        buttonsConstraints.anchor = GridBagConstraints.WEST;
        buttonsConstraints.insets = new Insets(10, 0, 0, 0);
        form.add(buttons, buttonsConstraints);

        return form;
    }

    private void addRow(JPanel form, JLabel label, JTextField field, JButton button, int row,
                        GridBagConstraints labelConstraints, GridBagConstraints fieldConstraints,
                        GridBagConstraints buttonConstraints) {
        labelConstraints.gridy = row;
        fieldConstraints.gridy = row;
        buttonConstraints.gridy = row;
        form.add(label, labelConstraints);
        form.add(field, fieldConstraints);
        form.add(button, buttonConstraints);
    }

    private JPanel buildLogPanel() {
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setMargin(new Insets(6, 6, 6, 6));
        logArea.setText("");

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Журнал работы"));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout(8, 0));
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));

        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(160, 16));

        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(progressBar, BorderLayout.EAST);
        return statusBar;
    }

    // ------------------------------------------------------------------
    // Действия
    // ------------------------------------------------------------------

    private void wireActions() {
        browseInputButton.addActionListener(this::chooseInputFile);
        browseOutputButton.addActionListener(this::chooseOutputDirectory);
        chooseInputItem.addActionListener(this::chooseInputFile);
        chooseOutputItem.addActionListener(this::chooseOutputDirectory);
        generateButton.addActionListener(e -> generate());
        openFolderButton.addActionListener(e -> openOutputFolder());
        strongCheckBox.addActionListener(e -> updateButtons());
        beerCheckBox.addActionListener(e -> updateButtons());
        updateButtons();
    }

    private void chooseInputFile(ActionEvent event) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Выберите файл выгрузки остатков ЕГАИС");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Выгрузка остатков ЕГАИС (*.xml)", "xml"));
        setCurrentDirectory(chooser, currentInputFile(), readPreference(PREF_LAST_INPUT_DIR));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File selected = chooser.getSelectedFile();
        inputField.setText(selected.getAbsolutePath());
        writePreference(PREF_LAST_INPUT_FILE, selected);
        writePreference(PREF_LAST_INPUT_DIR, selected.getParentFile());
        if (outputField.getText().trim().isEmpty() && selected.getParentFile() != null) {
            outputField.setText(selected.getParentFile().getAbsolutePath());
        }
        log("Выбран файл остатков: " + selected.getAbsolutePath());
        updateButtons();
    }

    private void chooseOutputDirectory(ActionEvent event) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Выберите папку для сохранения актов");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        setCurrentDirectory(chooser, currentOutputDirectory(), readPreference(PREF_LAST_OUTPUT_DIR));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File selected = chooser.getSelectedFile();
        outputField.setText(selected.getAbsolutePath());
        writePreference(PREF_LAST_OUTPUT_DIR, selected);
        log("Выбрана папка результата: " + selected.getAbsolutePath());
        updateButtons();
    }

    private void generate() {
        File inputFile = currentInputFile();
        File outputDirectory = currentOutputDirectory();
        boolean strong = strongCheckBox.isSelected();
        boolean beer = beerCheckBox.isSelected();

        if (inputFile == null || inputField.getText().trim().isEmpty()) {
            showWarning("Выберите файл выгрузки остатков ЕГАИС.");
            return;
        }
        if (!inputFile.isFile()) {
            showWarning("Файл остатков не найден:\n" + inputFile.getAbsolutePath());
            return;
        }
        if (outputDirectory == null || outputField.getText().trim().isEmpty()) {
            showWarning("Выберите папку для сохранения результата.");
            return;
        }
        if (!strong && !beer) {
            showWarning("Отметьте хотя бы один акт: крепкий алкоголь или пиво.");
            return;
        }

        writePreference(PREF_LAST_INPUT_FILE, inputFile);
        writePreference(PREF_LAST_INPUT_DIR, inputFile.getParentFile());
        writePreference(PREF_LAST_OUTPUT_DIR, outputDirectory);

        setBusy(true);
        log("――― Начало формирования актов ―――");

        new SwingWorker<GenerationResult, String>() {
            @Override
            protected GenerationResult doInBackground() throws Exception {
                WriteOffService service = new WriteOffService().withLogger(this::publish);
                return service.generate(inputFile, outputDirectory, strong, beer);
            }

            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    log(message);
                }
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    GenerationResult result = get();
                    log(String.format("Готово. Позиций обработано: %d (крепкий алкоголь — %d, пиво — %d).",
                            result.getTotalPositions(), result.getStrongPositions(), result.getBeerPositions()));
                    setStatus("Готово: обработано позиций — " + result.getTotalPositions());
                    JOptionPane.showMessageDialog(MainFrame.this,
                            buildSuccessMessage(result),
                            "Формирование завершено",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log("Операция прервана.");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String message = cause instanceof GenerationException && cause.getMessage() != null
                            ? cause.getMessage()
                            : String.valueOf(cause);
                    log("ОШИБКА: " + message);
                    setStatus("Ошибка");
                    JOptionPane.showMessageDialog(MainFrame.this, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private String buildSuccessMessage(GenerationResult result) {
        StringBuilder builder = new StringBuilder("Акты списания сформированы.\n\n");
        for (File file : result.getWrittenFiles()) {
            builder.append(file.getName())
                    .append("  —  ")
                    .append(file.getAbsolutePath())
                    .append('\n');
        }
        if (result.getStrongMarks() > 0) {
            builder.append("\nМарок по крепкому алкоголю: ").append(result.getStrongMarks());
        }
        return builder.toString();
    }

    private void openOutputFolder() {
        File directory = currentOutputDirectory();
        if (directory == null || !directory.isDirectory()) {
            showWarning("Папка результата ещё не выбрана или не существует.");
            return;
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            showWarning("Открытие папки не поддерживается в этой системе.");
            return;
        }
        try {
            Desktop.getDesktop().open(directory);
        } catch (Exception e) {
            showWarning("Не удалось открыть папку:\n" + e.getMessage());
        }
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                Constants.APP_NAME + "\nВерсия " + Constants.APP_VERSION + "\n\n"
                        + "Формирует акты списания по крепкому алкоголю и пиву\n"
                        + "на основании выгрузки текущих остатков из ЕГАИС (ReplyRests_v2.xml).\n\n"
                        + "Результат: ActWriteOffStrong.xml и ActWriteOffBeer.xml\n"
                        + "в выбранной папке.",
                "О программе",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ------------------------------------------------------------------
    // Перетаскивание файла в окно
    // ------------------------------------------------------------------

    private void installDropTarget() {
        setTransferHandler(new TransferHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                try {
                    Object data = support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!(data instanceof List) || ((List<?>) data).isEmpty()) {
                        return false;
                    }
                    Object first = ((List<?>) data).get(0);
                    if (!(first instanceof File)) {
                        return false;
                    }
                    File file = (File) first;
                    if (file.isDirectory()) {
                        outputField.setText(file.getAbsolutePath());
                        log("Выбрана папка результата: " + file.getAbsolutePath());
                    } else {
                        inputField.setText(file.getAbsolutePath());
                        if (outputField.getText().trim().isEmpty() && file.getParentFile() != null) {
                            outputField.setText(file.getParentFile().getAbsolutePath());
                        }
                        log("Выбран файл остатков: " + file.getAbsolutePath());
                    }
                    updateButtons();
                    return true;
                } catch (Exception e) {
                    log("Не удалось принять перетаскиваемый объект: " + e.getMessage());
                    return false;
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // Вспомогательные методы
    // ------------------------------------------------------------------

    private void setBusy(boolean busy) {
        generateButton.setEnabled(!busy && canGenerate());
        browseInputButton.setEnabled(!busy);
        browseOutputButton.setEnabled(!busy);
        chooseInputItem.setEnabled(!busy);
        chooseOutputItem.setEnabled(!busy);
        strongCheckBox.setEnabled(!busy);
        beerCheckBox.setEnabled(!busy);
        progressBar.setVisible(busy);
        progressBar.setIndeterminate(busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        if (busy) {
            setStatus("Идёт формирование актов…");
        }
    }

    private void updateButtons() {
        generateButton.setEnabled(canGenerate());
        openFolderButton.setEnabled(currentOutputDirectory() != null
                && currentOutputDirectory().isDirectory());
    }

    private boolean canGenerate() {
        return !inputField.getText().trim().isEmpty()
                && !outputField.getText().trim().isEmpty()
                && (strongCheckBox.isSelected() || beerCheckBox.isSelected());
    }

    private File currentInputFile() {
        String text = inputField.getText().trim();
        return text.isEmpty() ? null : new File(text);
    }

    private File currentOutputDirectory() {
        String text = outputField.getText().trim();
        return text.isEmpty() ? null : new File(text);
    }

    private void setCurrentDirectory(JFileChooser chooser, File preferred, String remembered) {
        File directory = null;
        if (preferred != null) {
            directory = preferred.isDirectory() ? preferred : preferred.getParentFile();
        }
        if (directory == null && remembered != null) {
            File rememberedDirectory = new File(remembered);
            if (rememberedDirectory.isDirectory()) {
                directory = rememberedDirectory;
            }
        }
        if (directory != null) {
            chooser.setCurrentDirectory(directory);
        }
    }

    private void log(String message) {
        logArea.append("[" + LocalTime.now().format(TIME_FORMAT) + "] " + message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Внимание", JOptionPane.WARNING_MESSAGE);
    }

    private String readPreference(String key) {
        try {
            return preferences.get(key, null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void writePreference(String key, File value) {
        if (value == null) {
            return;
        }
        try {
            preferences.put(key, value.getAbsolutePath());
        } catch (RuntimeException e) {
            // Настройки не критичны: если их не удалось сохранить, программа продолжает работу.
        }
    }

    /**
     * Восстанавливает пути, выбранные при прошлом запуске, если они ещё существуют.
     */
    private void restorePreferences() {
        String lastInput = readPreference(PREF_LAST_INPUT_FILE);
        if (lastInput != null) {
            File file = new File(lastInput);
            if (file.isFile()) {
                inputField.setText(file.getAbsolutePath());
            }
        }
        String lastOutput = readPreference(PREF_LAST_OUTPUT_DIR);
        if (lastOutput != null) {
            File directory = new File(lastOutput);
            if (directory.isDirectory()) {
                outputField.setText(directory.getAbsolutePath());
            }
        }
        updateButtons();
    }
}
