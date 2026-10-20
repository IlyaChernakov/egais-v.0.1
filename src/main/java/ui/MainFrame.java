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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import core.ActHeader;
import core.GenerationException;
import core.GenerationResult;
import core.UtmSender;
import core.WriteOffService;
import helpers.Constants;
import helpers.XmlSupport;

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
    private static final String PREF_ACT_NUMBER = "actNumber";
    private static final String PREF_TYPE_WRITE_OFF = "typeWriteOff";
    private static final String PREF_UTM_ADDRESS = "utmAddress";

    private final Preferences preferences = Preferences.userNodeForPackage(MainFrame.class);

    private final JTextField inputField = new JTextField();
    private final JTextField outputField = new JTextField();
    private final JTextField fsrarIdField = new JTextField();
    private final JTextField actNumberField = new JTextField();
    private final JTextField actDateField = new JTextField();
    private final JTextField typeWriteOffField = new JTextField();
    private final JTextField noteField = new JTextField();
    private final JTextField utmAddressField = new JTextField();
    private final JCheckBox strongCheckBox = new JCheckBox("Крепкий алкоголь — ActWriteOffStrong.xml", true);
    private final JCheckBox beerCheckBox = new JCheckBox("Пиво и пивные напитки — ActWriteOffBeer.xml", true);
    private final JTextArea logArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Готово к работе");
    private final JProgressBar progressBar = new JProgressBar();

    private final JButton browseInputButton = new JButton("Обзор…");
    private final JButton browseOutputButton = new JButton("Обзор…");
    private final JButton generateButton = new JButton("Сформировать акты");
    private final JButton sendToUtmButton = new JButton("Отправить в УТМ");
    private final JButton openFolderButton = new JButton("Открыть папку");
    private final JMenuItem chooseInputItem = new JMenuItem("Выбрать файл остатков…");
    private final JMenuItem chooseOutputItem = new JMenuItem("Выбрать папку результата…");

    /** Файлы, сформированные последним запуском: их и предлагается отправить в УТМ. */
    private final List<File> lastGeneratedFiles = new ArrayList<>();

    public MainFrame() {
        super(Constants.APP_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 700));
        buildUi();
        initActHeaderDefaults();
        wireActions();
        installDropTarget();
        restorePreferences();
        pack();
        setSize(900, 780);
        setLocationRelativeTo(null);
    }

    /**
     * Заполняет поля шапки акта значениями по умолчанию: сегодняшняя дата,
     * причина «Порча» и типовое примечание вместе с той же датой.
     */
    private void initActHeaderDefaults() {
        LocalDate today = LocalDate.now();
        actDateField.setText(today.format(ActHeader.USER_DATE));
        typeWriteOffField.setText(Constants.DEFAULT_TYPE_WRITE_OFF);
        noteField.setText(ActHeader.defaultNote(today));
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

        // Данные акта
        JPanel actHeaderPanel = buildActHeaderPanel();

        GridBagConstraints actHeaderConstraints = new GridBagConstraints();
        actHeaderConstraints.gridx = 0;
        actHeaderConstraints.gridy = 2;
        actHeaderConstraints.gridwidth = 3;
        actHeaderConstraints.fill = GridBagConstraints.HORIZONTAL;
        actHeaderConstraints.insets = new Insets(10, 0, 0, 0);
        form.add(actHeaderPanel, actHeaderConstraints);

        // Что формировать
        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        options.setBorder(BorderFactory.createTitledBorder("Что формировать"));
        options.add(strongCheckBox);
        options.add(beerCheckBox);

        GridBagConstraints optionsConstraints = new GridBagConstraints();
        optionsConstraints.gridx = 0;
        optionsConstraints.gridy = 3;
        optionsConstraints.gridwidth = 3;
        optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
        optionsConstraints.insets = new Insets(10, 0, 0, 0);
        form.add(options, optionsConstraints);

        // Отправка в УТМ
        JPanel utmPanel = buildUtmPanel();

        GridBagConstraints utmConstraints = new GridBagConstraints();
        utmConstraints.gridx = 0;
        utmConstraints.gridy = 4;
        utmConstraints.gridwidth = 3;
        utmConstraints.fill = GridBagConstraints.HORIZONTAL;
        utmConstraints.insets = new Insets(10, 0, 0, 0);
        form.add(utmPanel, utmConstraints);

        // Кнопки
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(generateButton);
        buttons.add(openFolderButton);
        generateButton.setPreferredSize(new Dimension(180, generateButton.getPreferredSize().height));

        GridBagConstraints buttonsConstraints = new GridBagConstraints();
        buttonsConstraints.gridx = 0;
        buttonsConstraints.gridy = 5;
        buttonsConstraints.gridwidth = 3;
        buttonsConstraints.anchor = GridBagConstraints.WEST;
        buttonsConstraints.insets = new Insets(10, 0, 0, 0);
        form.add(buttons, buttonsConstraints);

        return form;
    }

    /**
     * Панель отправки акта в УТМ: адрес модуля и кнопка отправки.
     * <p>
     * Акт передаётся утилитой curl по адресу
     * {@code <адрес УТМ>/opt/in/ActWriteOff_v3} — так же, как в документации ЕГАИС:
     * <pre>
     * curl -F "xml_file=@client.xml" http://localhost:8080/opt/in/ActWriteOff_v3
     * </pre>
     */
    private JPanel buildUtmPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Отправка в УТМ"));

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(3, 0, 3, 8);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(3, 0, 3, 8);

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridx = 2;
        buttonConstraints.insets = new Insets(3, 0, 3, 0);

        utmAddressField.setText(Constants.DEFAULT_UTM_ADDRESS);
        utmAddressField.setToolTipText("Адрес УТМ, например " + Constants.DEFAULT_UTM_ADDRESS
                + ". Путь " + Constants.UTM_ACT_WRITE_OFF_PATH + " дописывается автоматически.");

        addRow(panel, new JLabel("Адрес УТМ:"), utmAddressField, sendToUtmButton, 0,
                labelConstraints, fieldConstraints, buttonConstraints);
        sendToUtmButton.setToolTipText("Отправить сформированные акты в УТМ");

        return panel;
    }

    /**
     * Панель с данными шапки акта — они попадают в конверт документа
     * {@code ns:Documents} (см. образец {@code files/1.xml}).
     */
    private JPanel buildActHeaderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Данные акта"));

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(3, 0, 3, 8);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(3, 0, 3, 0);

        addHeaderRow(panel, "FSRAR_ID организации:", fsrarIdField, 0, labelConstraints, fieldConstraints);
        // Код всегда берётся из выбранного файла остатков, поэтому поле только для чтения:
        // так в акт не попадёт код, не совпадающий с самой выгрузкой.
        fsrarIdField.setEditable(false);
        fsrarIdField.setToolTipText("Заполняется автоматически из файла остатков ЕГАИС и не редактируется.");

        addHeaderRow(panel, "Номер акта:", actNumberField, 1, labelConstraints, fieldConstraints);
        actNumberField.setToolTipText("Номер акта; он же записывается в awr:Identity документа");

        addHeaderRow(panel, "Дата списания (ДД-ММ-ГГГГ):", actDateField, 2, labelConstraints, fieldConstraints);
        actDateField.setToolTipText("Дата акта; принимается и формат ГГГГ-ММ-ДД");

        addHeaderRow(panel, "Причина списания:", typeWriteOffField, 3, labelConstraints, fieldConstraints);
        typeWriteOffField.setToolTipText("Значение awr:TypeWriteOff. Пустое поле — тег в акт не записывается.");

        addHeaderRow(panel, "Примечание:", noteField, 4, labelConstraints, fieldConstraints);
        noteField.setToolTipText("Значение awr:Note. Пустое поле — тег в акт не записывается.");

        return panel;
    }

    private void addHeaderRow(JPanel panel, String labelText, JTextField field, int row,
                              GridBagConstraints labelConstraints, GridBagConstraints fieldConstraints) {
        labelConstraints.gridy = row;
        fieldConstraints.gridy = row;
        panel.add(new JLabel(labelText), labelConstraints);
        panel.add(field, fieldConstraints);
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
        sendToUtmButton.addActionListener(e -> sendToUtm());
        openFolderButton.addActionListener(e -> openOutputFolder());
        strongCheckBox.addActionListener(e -> updateButtons());
        beerCheckBox.addActionListener(e -> updateButtons());
        // Адрес УТМ можно править вручную, поэтому кнопку отправки обновляем по мере ввода.
        utmAddressField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateButtons();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateButtons();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateButtons();
            }
        });
        // Путь можно не только выбрать в диалоге, но и вписать руками,
        // поэтому FSRAR_ID обновляем ещё и при уходе фокуса с поля файла.
        inputField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateFsrarIdFromInput(currentInputFile());
            }
        });
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
        updateFsrarIdFromInput(selected);
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

        // Поле FSRAR_ID недоступно для правки, поэтому объясняем, что именно не так с файлом.
        if (fsrarIdField.getText().trim().isEmpty()) {
            showWarning("В файле остатков не найден FSRAR_ID организации,"
                    + " поэтому акт сформировать нельзя.\n\nФайл: " + inputFile.getAbsolutePath());
            return;
        }

        ActHeader header;
        try {
            header = ActHeader.of(fsrarIdField.getText(), actNumberField.getText(),
                    actDateField.getText(), typeWriteOffField.getText(), noteField.getText());
            header.validate();
        } catch (GenerationException e) {
            showWarning(e.getMessage());
            return;
        }

        writePreference(PREF_LAST_INPUT_FILE, inputFile);
        writePreference(PREF_LAST_INPUT_DIR, inputFile.getParentFile());
        writePreference(PREF_LAST_OUTPUT_DIR, outputDirectory);
        writePreference(PREF_ACT_NUMBER, header.getActNumber());
        writePreference(PREF_TYPE_WRITE_OFF, header.getTypeWriteOff());

        setBusy(true);
        log("――― Начало формирования актов ―――");

        new SwingWorker<GenerationResult, String>() {
            @Override
            protected GenerationResult doInBackground() throws Exception {
                WriteOffService service = new WriteOffService().withLogger(this::publish);
                return service.generate(inputFile, outputDirectory, header, strong, beer);
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
                    // Запоминаем, что сформировано в этот раз: именно эти файлы предлагается отправить в УТМ.
                    lastGeneratedFiles.clear();
                    lastGeneratedFiles.addAll(result.getWrittenFiles());
                    updateButtons();
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

    // ------------------------------------------------------------------
    // Отправка в УТМ
    // ------------------------------------------------------------------

    /**
     * Отправляет сформированные акты в УТМ утилитой curl.
     * <p>
     * Каждый файл передаётся отдельной командой вида
     * {@code curl -F "xml_file=@client.xml" <адрес УТМ>/opt/in/ActWriteOff_v3}.
     */
    private void sendToUtm() {
        final List<File> files = filesToSend();
        if (files.isEmpty()) {
            showWarning("Отправлять пока нечего: сначала сформируйте акты.");
            return;
        }

        final String url;
        try {
            url = UtmSender.endpoint(utmAddressField.getText());
        } catch (GenerationException e) {
            showWarning(e.getMessage());
            return;
        }

        if (lastGeneratedFiles.isEmpty() && !confirmSendingFoundFiles(files)) {
            return;
        }

        writePreference(PREF_UTM_ADDRESS, utmAddressField.getText().trim());

        setBusy(true);
        log("――― Отправка актов в УТМ ―――");

        new SwingWorker<List<UtmSender.SendResult>, String>() {
            @Override
            protected List<UtmSender.SendResult> doInBackground() throws Exception {
                UtmSender sender = new UtmSender();
                List<UtmSender.SendResult> results = new ArrayList<>();
                publish("Команда: curl -F \"" + Constants.UTM_FORM_FIELD + "=@"
                        + Constants.UTM_UPLOAD_FILE_NAME + "\" " + url);
                for (File file : files) {
                    publish("Отправляю " + file.getName() + " …");
                    UtmSender.SendResult result = sender.send(file, url);
                    results.add(result);
                    if (result.getOutput().isEmpty()) {
                        publish("  ответ УТМ: пусто, код возврата " + result.getExitCode() + ".");
                    } else {
                        publish("  ответ УТМ (код возврата " + result.getExitCode() + "):");
                        for (String line : result.getOutput().split("\\R")) {
                            if (!line.trim().isEmpty()) {
                                publish("    " + line.trim());
                            }
                        }
                    }
                }
                return results;
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
                    List<UtmSender.SendResult> results = get();
                    List<String> rejected = new ArrayList<>();
                    for (UtmSender.SendResult result : results) {
                        if (!result.isSuccess()) {
                            rejected.add(result.getFile().getName());
                        }
                    }
                    if (rejected.isEmpty()) {
                        log("Отправка в УТМ завершена: файлов — " + results.size() + ".");
                        setStatus("Отправлено в УТМ: " + results.size());
                        JOptionPane.showMessageDialog(MainFrame.this,
                                buildUtmSuccessMessage(results), "Отправка в УТМ",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        log("ОШИБКА: curl не смог отправить файлы: " + String.join(", ", rejected));
                        setStatus("Ошибка отправки в УТМ");
                        JOptionPane.showMessageDialog(MainFrame.this,
                                buildUtmFailureMessage(results, rejected), "Отправка в УТМ",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log("Отправка в УТМ прервана.");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String message = cause instanceof GenerationException && cause.getMessage() != null
                            ? cause.getMessage()
                            : String.valueOf(cause);
                    log("ОШИБКА: " + message);
                    setStatus("Ошибка отправки");
                    JOptionPane.showMessageDialog(MainFrame.this, message, "Отправка в УТМ",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Спрашивает подтверждение, если отправлять предстоит файлы, найденные в папке
     * результата, а не сформированные в этом запуске: там мог остаться акт от другой
     * организации или другой даты.
     */
    private boolean confirmSendingFoundFiles(List<File> files) {
        StringBuilder question = new StringBuilder("Эти файлы найдены в папке результата,"
                + " но сформированы не в этом запуске:\n\n");
        for (File file : files) {
            question.append(file.getName()).append("  —  ").append(file.getAbsolutePath()).append('\n');
        }
        question.append("\nОтправить их в УТМ?");
        return JOptionPane.showConfirmDialog(this, question.toString(), "Отправка в УТМ",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }

    /**
     * Файлы для отправки: сформированные последним запуском, а если их нет —
     * найденные в папке результата.
     */
    private List<File> filesToSend() {
        List<File> files = new ArrayList<>();
        for (File file : lastGeneratedFiles) {
            if (file.isFile()) {
                files.add(file);
            }
        }
        if (!files.isEmpty()) {
            return files;
        }
        File directory = currentOutputDirectory();
        if (directory == null || !directory.isDirectory()) {
            return files;
        }
        for (String name : new String[]{Constants.OUTPUT_FILE_STRONG, Constants.OUTPUT_FILE_BEER}) {
            File file = new File(directory, name);
            if (file.isFile()) {
                files.add(file);
            }
        }
        return files;
    }

    private String buildUtmSuccessMessage(List<UtmSender.SendResult> results) {
        StringBuilder builder = new StringBuilder("Файлы отправлены в УТМ.\n\n");
        for (UtmSender.SendResult result : results) {
            builder.append(result.getFile().getName())
                    .append("  —  curl вернул ").append(result.getExitCode()).append('\n');
        }
        builder.append("\nАдрес приёма: ").append(results.get(0).getUrl());
        builder.append("\n\nОтвет УТМ показан в журнале работы.");
        return builder.toString();
    }

    private String buildUtmFailureMessage(List<UtmSender.SendResult> results, List<String> rejected) {
        StringBuilder builder = new StringBuilder("curl не смог отправить файлы: ");
        builder.append(String.join(", ", rejected)).append("\n\n");
        for (UtmSender.SendResult result : results) {
            if (!result.isSuccess()) {
                builder.append(result.getFile().getName())
                        .append("  —  код возврата ").append(result.getExitCode()).append('\n');
                if (!result.getOutput().isEmpty()) {
                    builder.append(result.getOutput()).append('\n');
                }
            }
        }
        builder.append("\nПроверьте адрес УТМ и доступность сервера: ").append(results.get(0).getUrl());
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
                        + "Данные акта (FSRAR_ID, номер, дата, причина, примечание)\n"
                        + "задаются в окне и попадают в шапку документа.\n\n"
                        + "Результат: ActWriteOffStrong.xml и ActWriteOffBeer.xml\n"
                        + "в выбранной папке.\n\n"
                        + "Готовые акты можно отправить в УТМ утилитой curl\n"
                        + "по адресу из поля «Адрес УТМ».",
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
                        updateFsrarIdFromInput(file);
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
        sendToUtmButton.setEnabled(!busy && canSend());
        browseInputButton.setEnabled(!busy);
        browseOutputButton.setEnabled(!busy);
        chooseInputItem.setEnabled(!busy);
        chooseOutputItem.setEnabled(!busy);
        strongCheckBox.setEnabled(!busy);
        beerCheckBox.setEnabled(!busy);
        fsrarIdField.setEnabled(!busy);
        actNumberField.setEnabled(!busy);
        actDateField.setEnabled(!busy);
        typeWriteOffField.setEnabled(!busy);
        noteField.setEnabled(!busy);
        utmAddressField.setEnabled(!busy);
        progressBar.setVisible(busy);
        progressBar.setIndeterminate(busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        if (busy) {
            setStatus("Идёт формирование актов…");
        }
    }

    private void updateButtons() {
        generateButton.setEnabled(canGenerate());
        sendToUtmButton.setEnabled(canSend());
        openFolderButton.setEnabled(currentOutputDirectory() != null
                && currentOutputDirectory().isDirectory());
    }

    private boolean canGenerate() {
        return !inputField.getText().trim().isEmpty()
                && !outputField.getText().trim().isEmpty()
                && (strongCheckBox.isSelected() || beerCheckBox.isSelected());
    }

    /** Отправка возможна, когда задан адрес УТМ и есть что отправлять. */
    private boolean canSend() {
        return !utmAddressField.getText().trim().isEmpty() && !filesToSend().isEmpty();
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

    private void writePreference(String key, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            preferences.put(key, value);
        } catch (RuntimeException e) {
            // Настройки не критичны: если их не удалось сохранить, программа продолжает работу.
        }
    }

    /**
     * Обновляет FSRAR_ID организации по выбранному файлу остатков.
     * <p>
     * Поле только для чтения: значение всегда берётся из файла, поэтому при выборе
     * другого файла оно заменяется, а если кода в файле нет — очищается.
     *
     * @param inputFile выбранный файл остатков
     */
    private void updateFsrarIdFromInput(File inputFile) {
        if (inputFile == null || !inputFile.isFile()) {
            return;
        }
        String owner;
        try {
            owner = XmlSupport.ownerFsrarId(inputFile);
        } catch (GenerationException e) {
            owner = "";
            if (!fsrarIdField.getText().isEmpty()) {
                fsrarIdField.setText("");
                log("Не удалось прочитать FSRAR_ID организации: " + e.getMessage());
            }
            return;
        }
        if (owner.equals(fsrarIdField.getText())) {
            // Значение не изменилось — не засоряем журнал повторными записями.
            return;
        }
        fsrarIdField.setText(owner);
        if (owner.isEmpty()) {
            log("В файле остатков не найден FSRAR_ID организации.");
        } else {
            log("FSRAR_ID организации взят из файла остатков: " + owner);
        }
    }

    /**
     * Восстанавливает пути, выбранные при прошлом запуске, если они ещё существуют,
     * а также номер и причину списания. Дата и примечание не запоминаются — они
     * привязаны к текущему дню, а FSRAR_ID всегда берётся из файла остатков.
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
        String savedActNumber = readPreference(PREF_ACT_NUMBER);
        if (savedActNumber != null && !savedActNumber.trim().isEmpty()) {
            actNumberField.setText(savedActNumber);
        }
        String savedTypeWriteOff = readPreference(PREF_TYPE_WRITE_OFF);
        if (savedTypeWriteOff != null && !savedTypeWriteOff.trim().isEmpty()) {
            typeWriteOffField.setText(savedTypeWriteOff);
        }
        String savedUtmAddress = readPreference(PREF_UTM_ADDRESS);
        if (savedUtmAddress != null && !savedUtmAddress.trim().isEmpty()) {
            utmAddressField.setText(savedUtmAddress);
        }
        updateFsrarIdFromInput(currentInputFile());
        updateButtons();
    }
}
