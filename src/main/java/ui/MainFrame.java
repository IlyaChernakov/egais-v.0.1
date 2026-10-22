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
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
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
import core.OutputNames;
import core.TransferHeader;
import core.TransferResult;
import core.TransferService;
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
    private static final String PREF_ACT_NUMBER_STRONG = "actNumberStrong";
    private static final String PREF_ACT_NUMBER_BEER = "actNumberBeer";
    /** Прежний ключ номера: пока номер акта был один на оба акта. */
    private static final String PREF_ACT_NUMBER_LEGACY = "actNumber";
    private static final String PREF_TYPE_WRITE_OFF = "typeWriteOff";
    private static final String PREF_UTM_ADDRESS = "utmAddress";
    private static final String PREF_LAST_SHOP_FILE = "lastShopRestsFile";
    private static final String PREF_LAST_STOCK_FILE = "lastStockRestsFile";
    private static final String PREF_TRANSFER_NUMBER = "transferNumber";
    private static final String PREF_TRANSFER_IDENTITY = "transferIdentity";
    private static final String PREF_TRANSFER_MODE = "transferMode";

    private final Preferences preferences = Preferences.userNodeForPackage(MainFrame.class);

    /** Идёт длительная операция: поля и кнопки заблокированы. */
    private boolean busy;

    private final JRadioButton writeOffModeRadio = new JRadioButton("Акты списания", true);
    private final JRadioButton transferModeRadio = new JRadioButton("Перенос остатков в магазин");

    private final JTextField inputField = new JTextField();
    private final JTextField outputField = new JTextField();
    private final JTextField fsrarIdField = new JTextField();
    private final JTextField actNumberStrongField = new JTextField();
    private final JTextField actNumberBeerField = new JTextField();
    private final JTextField actDateField = new JTextField();
    private final JTextField typeWriteOffField = new JTextField();
    private final JTextField noteField = new JTextField();
    private final JTextField utmAddressField = new JTextField();
    private final JCheckBox strongCheckBox = new JCheckBox("Крепкий алкоголь — ActWriteOff_v3(S)", true);
    private final JCheckBox beerCheckBox = new JCheckBox("Пиво и пивные напитки — ActWriteOff_v3(B)", true);
    private final JTextArea logArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Готово к работе");
    private final JProgressBar progressBar = new JProgressBar();

    // Поля режима переноса остатков в магазин.
    private final JTextField transferShopField = new JTextField();
    private final JTextField transferStockField = new JTextField();
    private final JTextField transferFsrarIdField = new JTextField();
    private final JTextField transferIdentityField = new JTextField();
    private final JTextField transferNumberField = new JTextField();
    private final JTextField transferDateField = new JTextField();
    private final JLabel transferShopLabel = new JLabel("Остатки магазина:");
    private final JLabel transferStockLabel = new JLabel("Остатки склада:");
    private final JLabel transferFsrarIdLabel = new JLabel("FSRAR_ID организации:");
    private final JLabel transferIdentityLabel = new JLabel("Identity документа:");
    private final JLabel transferNumberLabel = new JLabel("Номер переноса:");
    private final JLabel transferDateLabel = new JLabel("Дата переноса:");

    private final JButton browseInputButton = new JButton("Обзор…");
    private final JButton browseOutputButton = new JButton("Обзор…");
    private final JButton browseTransferShopButton = new JButton("Обзор…");
    private final JButton browseTransferStockButton = new JButton("Обзор…");

    /** Компоненты, которые работают только в режиме актов списания. */
    private final List<JComponent> writeOffComponents = new ArrayList<>();
    /** Компоненты, которые работают только в режиме переноса остатков. */
    private final List<JComponent> transferComponents = new ArrayList<>();

    // Блоки полей, которые целиком относятся к одному из режимов: неактивный
    // режим не только блокируется, но и скрывается, чтобы окно не разрасталось.
    private JPanel actHeaderPanel;
    private JPanel optionsPanel;
    private JPanel transferFilesPanel;
    private JPanel transferHeaderPanel;
    private final JButton generateButton = new JButton("Сформировать акты");
    private final JButton sendToUtmButton = new JButton("Отправить в УТМ");
    private final JButton openFolderButton = new JButton("Открыть папку");
    private final JMenuItem chooseInputItem = new JMenuItem("Выбрать файл остатков…");
    private final JMenuItem chooseShopInputItem = new JMenuItem("Выбрать остатки магазина…");
    private final JMenuItem chooseOutputItem = new JMenuItem("Выбрать папку результата…");

    /** Файлы, сформированные последним запуском: их и предлагается отправить в УТМ. */
    private final List<File> lastGeneratedFiles = new ArrayList<>();

    public MainFrame() {
        super(Constants.APP_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 700));
        buildUi();
        initActHeaderDefaults();
        initTransferDefaults();
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

    /**
     * Заполняет поля документа переноса значениями по умолчанию: датой переноса
     * становится сегодняшний день. Номер и Identity запоминаются между запусками,
     * поэтому здесь они не задаются.
     */
    private void initTransferDefaults() {
        transferDateField.setText(LocalDate.now().format(TransferHeader.USER_DATE));
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
        fileMenu.add(chooseShopInputItem);
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

        // Режим работы: или акты списания, или перенос остатков в магазин.
        JPanel modePanel = buildModePanel();

        GridBagConstraints modeConstraints = new GridBagConstraints();
        modeConstraints.gridx = 0;
        modeConstraints.gridy = 0;
        modeConstraints.gridwidth = 3;
        modeConstraints.fill = GridBagConstraints.HORIZONTAL;
        modeConstraints.insets = new Insets(0, 0, 0, 0);
        form.add(modePanel, modeConstraints);

        // Файл остатков
        addRow(form, new JLabel("Файл остатков ЕГАИС:"), inputField, browseInputButton, 1,
                labelConstraints, fieldConstraints, buttonConstraints, writeOffComponents);
        inputField.setToolTipText("Выгрузка остатков из ЕГАИС, файл вида ReplyRests_v2.xml");

        // Папка результата — общая для обоих режимов, поэтому не блокируется.
        addRow(form, new JLabel("Папка для результата:"), outputField, browseOutputButton, 2,
                labelConstraints, fieldConstraints, buttonConstraints);
        outputField.setToolTipText("Каталог, в который будут сохранены акты списания"
                + " или документ переноса остатков");

        // Данные акта
        actHeaderPanel = buildActHeaderPanel();

        GridBagConstraints actHeaderConstraints = new GridBagConstraints();
        actHeaderConstraints.gridx = 0;
        actHeaderConstraints.gridy = 3;
        actHeaderConstraints.gridwidth = 3;
        actHeaderConstraints.fill = GridBagConstraints.HORIZONTAL;
        actHeaderConstraints.insets = new Insets(10, 0, 0, 0);
        form.add(actHeaderPanel, actHeaderConstraints);

        // Что формировать
        optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Что формировать"));
        strongCheckBox.setToolTipText("Файл ActWriteOff_v3(S)_<FSRAR_ID>_<ДД-ММ-ГГГГ>.xml");
        beerCheckBox.setToolTipText("Файл ActWriteOff_v3(B)_<FSRAR_ID>_<ДД-ММ-ГГГГ>.xml");
        optionsPanel.add(strongCheckBox);
        optionsPanel.add(beerCheckBox);

        GridBagConstraints optionsConstraints = new GridBagConstraints();
        optionsConstraints.gridx = 0;
        optionsConstraints.gridy = 4;
        optionsConstraints.gridwidth = 3;
        optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
        optionsConstraints.insets = new Insets(10, 0, 0, 0);
        form.add(optionsPanel, optionsConstraints);
        writeOffComponents.add(strongCheckBox);
        writeOffComponents.add(beerCheckBox);

        // Файлы для переноса остатков: остатки магазина и остатки склада
        transferFilesPanel = buildTransferFilesPanel();

        GridBagConstraints transferFilesConstraints = new GridBagConstraints();
        transferFilesConstraints.gridx = 0;
        transferFilesConstraints.gridy = 5;
        transferFilesConstraints.gridwidth = 3;
        transferFilesConstraints.fill = GridBagConstraints.HORIZONTAL;
        transferFilesConstraints.insets = new Insets(10, 0, 0, 0);
        form.add(transferFilesPanel, transferFilesConstraints);

        // Данные документа переноса остатков в магазин
        transferHeaderPanel = buildTransferHeaderPanel();

        GridBagConstraints transferHeaderConstraints = new GridBagConstraints();
        transferHeaderConstraints.gridx = 0;
        transferHeaderConstraints.gridy = 6;
        transferHeaderConstraints.gridwidth = 3;
        transferHeaderConstraints.fill = GridBagConstraints.HORIZONTAL;
        transferHeaderConstraints.insets = new Insets(10, 0, 0, 0);
        form.add(transferHeaderPanel, transferHeaderConstraints);

        // Отправка в УТМ
        JPanel utmPanel = buildUtmPanel();

        GridBagConstraints utmConstraints = new GridBagConstraints();
        utmConstraints.gridx = 0;
        utmConstraints.gridy = 7;
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
        buttonsConstraints.gridy = 8;
        buttonsConstraints.gridwidth = 3;
        buttonsConstraints.anchor = GridBagConstraints.WEST;
        buttonsConstraints.insets = new Insets(10, 0, 0, 0);
        form.add(buttons, buttonsConstraints);

        return form;
    }

    /**
     * Панель выбора режима работы. Режимы несовместимы: либо формируются акты
     * списания, либо документ переноса остатков в магазин, поэтому поля одного
     * режима блокируются при выборе другого.
     */
    private JPanel buildModePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setBorder(BorderFactory.createTitledBorder("Режим работы"));

        ButtonGroup group = new ButtonGroup();
        group.add(writeOffModeRadio);
        group.add(transferModeRadio);

        writeOffModeRadio.setToolTipText("Формирование актов списания по выгрузке остатков ЕГАИС");
        transferModeRadio.setToolTipText("Формирование документа переноса остатков в магазин"
                + " по остаткам магазина и остаткам склада");

        panel.add(writeOffModeRadio);
        panel.add(transferModeRadio);
        return panel;
    }

    /**
     * Панель с файлами для режима переноса остатков в магазин: выгрузка остатков
     * магазина и выгрузка остатков склада.
     */
    private JPanel buildTransferFilesPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Файлы для переноса остатков в магазин"));

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

        addRow(panel, transferShopLabel, transferShopField, browseTransferShopButton, 0,
                labelConstraints, fieldConstraints, buttonConstraints, transferComponents);
        transferShopField.setToolTipText("Выгрузка остатков магазина, файл вида ReplyRestsShop_v2.xml");

        addRow(panel, transferStockLabel, transferStockField, browseTransferStockButton, 1,
                labelConstraints, fieldConstraints, buttonConstraints, transferComponents);
        transferStockField.setToolTipText("Выгрузка остатков склада, файл вида ReplyRests_v2.xml");

        return panel;
    }

    /**
     * Панель с данными документа переноса остатков в магазин: FSRAR_ID организации,
     * Identity и номер переноса, дата переноса.
     */
    private JPanel buildTransferHeaderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Данные документа переноса"));

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(3, 0, 3, 8);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(3, 0, 3, 0);

        addHeaderRow(panel, transferFsrarIdLabel, transferFsrarIdField, 0,
                labelConstraints, fieldConstraints, transferComponents);
        // Код всегда берётся из выбранного файла остатков магазина, поэтому поле только для чтения.
        transferFsrarIdField.setEditable(false);
        transferFsrarIdField.setToolTipText("Заполняется автоматически из файла остатков"
                + " и не редактируется.");

        addHeaderRow(panel, transferIdentityLabel, transferIdentityField, 1,
                labelConstraints, fieldConstraints, transferComponents);
        transferIdentityField.setToolTipText("Identity документа переноса; по умолчанию совпадает"
                + " с номером переноса");

        addHeaderRow(panel, transferNumberLabel, transferNumberField, 2,
                labelConstraints, fieldConstraints, transferComponents);
        transferNumberField.setToolTipText("Значение tts:TransferNumber в шапке документа");

        addHeaderRow(panel, transferDateLabel, transferDateField, 3,
                labelConstraints, fieldConstraints, transferComponents);
        transferDateField.setToolTipText("Дата переноса; принимается и формат ГГГГ-ММ-ДД");

        return panel;
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
     * {@code ns:Documents}. Номера актов по крепкому алкоголю и по пиву
     * задаются отдельно.
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

        addHeaderRow(panel, new JLabel("FSRAR_ID организации:"), fsrarIdField, 0,
                labelConstraints, fieldConstraints, writeOffComponents);
        // Код всегда берётся из выбранного файла остатков, поэтому поле только для чтения:
        // так в акт не попадёт код, не совпадающий с самой выгрузкой.
        fsrarIdField.setEditable(false);
        fsrarIdField.setToolTipText("Заполняется автоматически из файла остатков ЕГАИС и не редактируется.");

        addHeaderRow(panel, new JLabel("Номер акта (крепкий алкоголь):"), actNumberStrongField, 1,
                labelConstraints, fieldConstraints, writeOffComponents);
        actNumberStrongField.setToolTipText("Номер акта по крепкому алкоголю; он же awr:Identity этого акта");

        addHeaderRow(panel, new JLabel("Номер акта (пиво):"), actNumberBeerField, 2,
                labelConstraints, fieldConstraints, writeOffComponents);
        actNumberBeerField.setToolTipText("Номер акта по пиву; он же awr:Identity этого акта");

        addHeaderRow(panel, new JLabel("Дата списания (ДД-ММ-ГГГГ):"), actDateField, 3,
                labelConstraints, fieldConstraints, writeOffComponents);
        actDateField.setToolTipText("Дата акта; принимается и формат ГГГГ-ММ-ДД");

        addHeaderRow(panel, new JLabel("Причина списания:"), typeWriteOffField, 4,
                labelConstraints, fieldConstraints, writeOffComponents);
        typeWriteOffField.setToolTipText("Значение awr:TypeWriteOff. Пустое поле — тег в акт не записывается.");

        addHeaderRow(panel, new JLabel("Примечание:"), noteField, 5,
                labelConstraints, fieldConstraints, writeOffComponents);
        noteField.setToolTipText("Значение awr:Note. Пустое поле — тег в акт не записывается.");

        return panel;
    }

    private void addHeaderRow(JPanel panel, String labelText, JTextField field, int row,
                              GridBagConstraints labelConstraints, GridBagConstraints fieldConstraints) {
        addHeaderRow(panel, new JLabel(labelText), field, row, labelConstraints, fieldConstraints, null);
    }

    private void addHeaderRow(JPanel panel, JLabel label, JTextField field, int row,
                              GridBagConstraints labelConstraints, GridBagConstraints fieldConstraints,
                              List<JComponent> registry) {
        labelConstraints.gridy = row;
        fieldConstraints.gridy = row;
        panel.add(label, labelConstraints);
        panel.add(field, fieldConstraints);
        register(registry, label);
        register(registry, field);
    }

    private void addRow(JPanel form, JLabel label, JTextField field, JButton button, int row,
                        GridBagConstraints labelConstraints, GridBagConstraints fieldConstraints,
                        GridBagConstraints buttonConstraints) {
        addRow(form, label, field, button, row, labelConstraints, fieldConstraints, buttonConstraints, null);
    }

    private void addRow(JPanel form, JLabel label, JTextField field, JButton button, int row,
                        GridBagConstraints labelConstraints, GridBagConstraints fieldConstraints,
                        GridBagConstraints buttonConstraints, List<JComponent> registry) {
        labelConstraints.gridy = row;
        fieldConstraints.gridy = row;
        buttonConstraints.gridy = row;
        form.add(label, labelConstraints);
        form.add(field, fieldConstraints);
        form.add(button, buttonConstraints);
        register(registry, label);
        register(registry, field);
        register(registry, button);
    }

    private static void register(List<JComponent> registry, JComponent component) {
        if (registry != null) {
            registry.add(component);
        }
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
        browseTransferShopButton.addActionListener(this::chooseTransferShopFile);
        browseTransferStockButton.addActionListener(this::chooseTransferStockFile);
        chooseInputItem.addActionListener(this::chooseInputFile);
        chooseShopInputItem.addActionListener(this::chooseTransferShopFile);
        chooseOutputItem.addActionListener(this::chooseOutputDirectory);
        generateButton.addActionListener(e -> generate());
        sendToUtmButton.addActionListener(e -> sendToUtm());
        openFolderButton.addActionListener(e -> openOutputFolder());
        strongCheckBox.addActionListener(e -> updateButtons());
        beerCheckBox.addActionListener(e -> updateButtons());
        // Выбор режима блокирует поля другого режима — они несовместимы.
        writeOffModeRadio.addActionListener(e -> updateMode());
        transferModeRadio.addActionListener(e -> updateMode());
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
        // То же самое в режиме переноса: FSRAR_ID берётся из выбранных файлов.
        FocusAdapter transferFocusListener = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateTransferFsrarId();
            }
        };
        transferShopField.addFocusListener(transferFocusListener);
        transferStockField.addFocusListener(transferFocusListener);

        DocumentListener refreshListener = new DocumentListener() {
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
        };
        transferShopField.getDocument().addDocumentListener(refreshListener);
        transferStockField.getDocument().addDocumentListener(refreshListener);
        transferIdentityField.getDocument().addDocumentListener(refreshListener);
        transferNumberField.getDocument().addDocumentListener(refreshListener);
        transferDateField.getDocument().addDocumentListener(refreshListener);

        updateMode();
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

    /**
     * Выбор файла остатков магазина: именно в нём ищутся коды с отрицательным
     * остатком, которые нужно перенести на магазин.
     */
    private void chooseTransferShopFile(ActionEvent event) {
        File selected = chooseTransferFile(currentTransferShopFile(),
                "Выберите файл остатков магазина (ReplyRestsShop_v2.xml)");
        if (selected == null) {
            return;
        }
        transferShopField.setText(selected.getAbsolutePath());
        writePreference(PREF_LAST_SHOP_FILE, selected);
        if (outputField.getText().trim().isEmpty() && selected.getParentFile() != null) {
            outputField.setText(selected.getParentFile().getAbsolutePath());
        }
        log("Выбран файл остатков магазина: " + selected.getAbsolutePath());
        updateTransferFsrarId();
        updateButtons();
    }

    /** Выбор файла остатков склада: в нём ищутся партии для переноса. */
    private void chooseTransferStockFile(ActionEvent event) {
        File selected = chooseTransferFile(currentTransferStockFile(),
                "Выберите файл остатков склада (ReplyRests_v2.xml)");
        if (selected == null) {
            return;
        }
        transferStockField.setText(selected.getAbsolutePath());
        writePreference(PREF_LAST_STOCK_FILE, selected);
        if (outputField.getText().trim().isEmpty() && selected.getParentFile() != null) {
            outputField.setText(selected.getParentFile().getAbsolutePath());
        }
        log("Выбран файл остатков склада: " + selected.getAbsolutePath());
        updateTransferFsrarId();
        updateButtons();
    }

    private File chooseTransferFile(File preferred, String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Выгрузка остатков ЕГАИС (*.xml)", "xml"));
        setCurrentDirectory(chooser, preferred, readPreference(PREF_LAST_INPUT_DIR));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return chooser.getSelectedFile();
    }

    /**
     * Обновляет интерфейс при смене режима работы: активирует поля выбранного
     * режима и деактивирует поля другого, потому что режимы несовместимы.
     */
    private void updateMode() {
        boolean transfer = transferModeRadio.isSelected();
        // Режим запоминается: при следующем запуске окно откроется в нём же.
        writePreference(PREF_TRANSFER_MODE, transfer ? "true" : "false");

        // Поля неактивного режима не только блокируются, но и скрываются: иначе окно
        // разрастается почти вдвое и журналу работы не остаётся места.
        setComponentsVisible(writeOffComponents, !transfer);
        setComponentsVisible(transferComponents, transfer);
        setPanelVisible(actHeaderPanel, !transfer);
        setPanelVisible(optionsPanel, !transfer);
        setPanelVisible(transferFilesPanel, transfer);
        setPanelVisible(transferHeaderPanel, transfer);

        generateButton.setText(transfer ? "Сформировать перенос" : "Сформировать акты");
        sendToUtmButton.setToolTipText(transfer
                ? "Отправить документ переноса остатков в магазин в УТМ"
                : "Отправить сформированные акты в УТМ");
        updateButtons();
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private static void setComponentsVisible(List<JComponent> components, boolean visible) {
        for (JComponent component : components) {
            component.setVisible(visible);
        }
    }

    private static void setPanelVisible(JPanel panel, boolean visible) {
        if (panel != null) {
            panel.setVisible(visible);
        }
    }

    private void updateEnabledState(boolean isBusy) {
        boolean transfer = transferModeRadio.isSelected();
        setComponentsEnabled(writeOffComponents, !isBusy && !transfer);
        setComponentsEnabled(transferComponents, !isBusy && transfer);

        generateButton.setEnabled(!isBusy && canGenerate());
        sendToUtmButton.setEnabled(!isBusy && canSend());
        openFolderButton.setEnabled(!isBusy && currentOutputDirectory() != null
                && currentOutputDirectory().isDirectory());
        setComponentsEnabled(java.util.Collections.singletonList(outputField), !isBusy);
        setComponentsEnabled(java.util.Collections.singletonList(browseOutputButton), !isBusy);
        chooseOutputItem.setEnabled(!isBusy);
        chooseInputItem.setEnabled(!isBusy && !transfer);
        chooseShopInputItem.setEnabled(!isBusy && transfer);
        utmAddressField.setEnabled(!isBusy);
        writeOffModeRadio.setEnabled(!isBusy);
        transferModeRadio.setEnabled(!isBusy);

        progressBar.setVisible(isBusy);
        progressBar.setIndeterminate(isBusy);
        setCursor(Cursor.getPredefinedCursor(isBusy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        if (isBusy) {
            setStatus(transfer ? "Идёт формирование документа переноса…" : "Идёт формирование актов…");
        }
    }

    private static void setComponentsEnabled(List<JComponent> components, boolean enabled) {
        for (JComponent component : components) {
            component.setEnabled(enabled);
        }
    }

    private void generate() {
        if (transferModeRadio.isSelected()) {
            generateTransfer();
        } else {
            generateWriteOff();
        }
    }

    private void generateWriteOff() {
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

        if (strong && actNumberStrongField.getText().trim().isEmpty()) {
            showWarning("Укажите номер акта по крепкому алкоголю.");
            return;
        }
        if (beer && actNumberBeerField.getText().trim().isEmpty()) {
            showWarning("Укажите номер акта по пиву.");
            return;
        }

        final ActHeader strongHeader;
        final ActHeader beerHeader;
        try {
            strongHeader = strong
                    ? ActHeader.of(fsrarIdField.getText(), actNumberStrongField.getText(),
                            actDateField.getText(), typeWriteOffField.getText(), noteField.getText())
                    : null;
            beerHeader = beer
                    ? ActHeader.of(fsrarIdField.getText(), actNumberBeerField.getText(),
                            actDateField.getText(), typeWriteOffField.getText(), noteField.getText())
                    : null;
            if (strongHeader != null) {
                strongHeader.validate();
            }
            if (beerHeader != null) {
                beerHeader.validate();
            }
        } catch (GenerationException e) {
            showWarning(e.getMessage());
            return;
        }

        writePreference(PREF_LAST_INPUT_FILE, inputFile);
        writePreference(PREF_LAST_INPUT_DIR, inputFile.getParentFile());
        writePreference(PREF_LAST_OUTPUT_DIR, outputDirectory);
        writePreference(PREF_ACT_NUMBER_STRONG, actNumberStrongField.getText());
        writePreference(PREF_ACT_NUMBER_BEER, actNumberBeerField.getText());
        writePreference(PREF_TYPE_WRITE_OFF, typeWriteOffField.getText());

        setBusy(true);
        log("――― Начало формирования актов ―――");

        new SwingWorker<GenerationResult, String>() {
            @Override
            protected GenerationResult doInBackground() throws Exception {
                WriteOffService service = new WriteOffService().withLogger(this::publish);
                return service.generate(inputFile, outputDirectory, strongHeader, beerHeader);
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

    /**
     * Формирует документ переноса остатков в магазин по двум выгрузкам:
     * остаткам магазина (отрицательные остатки) и остаткам склада (партии для переноса).
     */
    private void generateTransfer() {
        File shopFile = currentTransferShopFile();
        File stockFile = currentTransferStockFile();
        File outputDirectory = currentOutputDirectory();

        if (shopFile == null) {
            showWarning("Выберите файл остатков магазина (ReplyRestsShop_v2.xml).");
            return;
        }
        if (!shopFile.isFile()) {
            showWarning("Файл остатков магазина не найден:\n" + shopFile.getAbsolutePath());
            return;
        }
        if (stockFile == null) {
            showWarning("Выберите файл остатков склада (ReplyRests_v2.xml).");
            return;
        }
        if (!stockFile.isFile()) {
            showWarning("Файл остатков склада не найден:\n" + stockFile.getAbsolutePath());
            return;
        }
        if (outputDirectory == null) {
            showWarning("Выберите папку для сохранения результата.");
            return;
        }
        // Поле FSRAR_ID недоступно для правки, поэтому объясняем, что именно не так с файлом.
        if (transferFsrarIdField.getText().trim().isEmpty()) {
            showWarning("В файле остатков магазина не найден FSRAR_ID организации,"
                    + " поэтому документ переноса сформировать нельзя.\n\nФайл: " + shopFile.getAbsolutePath());
            return;
        }
        // Если Identity не заполнен, он совпадает с номером переноса.
        if (transferIdentityField.getText().trim().isEmpty()) {
            transferIdentityField.setText(transferNumberField.getText().trim());
        }

        TransferHeader header;
        try {
            header = TransferHeader.of(transferFsrarIdField.getText(), transferIdentityField.getText(),
                    transferNumberField.getText(), transferDateField.getText());
            header.validate();
        } catch (GenerationException e) {
            showWarning(e.getMessage());
            return;
        }

        writePreference(PREF_LAST_SHOP_FILE, shopFile);
        writePreference(PREF_LAST_STOCK_FILE, stockFile);
        writePreference(PREF_LAST_INPUT_DIR, shopFile.getParentFile());
        writePreference(PREF_LAST_OUTPUT_DIR, outputDirectory);
        writePreference(PREF_TRANSFER_NUMBER, header.getTransferNumber());
        writePreference(PREF_TRANSFER_IDENTITY, header.getIdentity());

        setBusy(true);
        log("――― Начало формирования документа переноса ―――");

        new SwingWorker<TransferResult, String>() {
            @Override
            protected TransferResult doInBackground() throws Exception {
                TransferService service = new TransferService().withLogger(this::publish);
                return service.generate(shopFile, stockFile, outputDirectory, header);
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
                    TransferResult result = get();
                    // Документ переноса один, поэтому в отправку попадает именно он.
                    lastGeneratedFiles.clear();
                    lastGeneratedFiles.add(result.getWrittenFile());
                    updateButtons();
                    log(String.format("Готово. Позиций в документе переноса: %d"
                                    + " (кодов с отрицательным остатком: %d, найдено на складе: %d).",
                            result.getPositions(), result.getNegativeCodes(), result.getMatchedCodes()));
                    setStatus("Готово: позиций в документе переноса — " + result.getPositions());
                    JOptionPane.showMessageDialog(MainFrame.this,
                            buildTransferSuccessMessage(result),
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

    private String buildTransferSuccessMessage(TransferResult result) {
        StringBuilder builder = new StringBuilder("Документ переноса остатков в магазин сформирован.\n\n");
        builder.append(result.getWrittenFile().getName())
                .append("  —  ")
                .append(result.getWrittenFile().getAbsolutePath())
                .append('\n');
        builder.append("\nПозиций в документе: ").append(result.getPositions());
        builder.append("\nКодов с отрицательным остатком: ").append(result.getNegativeCodes());
        builder.append("\nНайдено подходящих партий на складе: ").append(result.getMatchedCodes());
        return builder.toString();
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
     * Отправляет сформированный документ в УТМ утилитой curl.
     * <p>
     * Тип документа зависит от режима работы: акты списания передаются по адресу
     * {@code <адрес УТМ>/opt/in/ActWriteOff_v3}, документ переноса остатков в магазин —
     * по адресу {@code <адрес УТМ>/opt/in/TransferToShop}:
     * <pre>
     * curl -F "xml_file=@client.xml" http://localhost:8080/opt/in/TransferToShop
     * </pre>
     */
    private void sendToUtm() {
        final boolean transfer = transferModeRadio.isSelected();
        final List<File> files = filesToSend();
        if (files.isEmpty()) {
            showWarning(transfer
                    ? "Отправлять пока нечего: сначала сформируйте документ переноса остатков."
                    : "Отправлять пока нечего: сначала сформируйте акты.");
            return;
        }

        final String address = utmAddressField.getText().trim();
        final String documentPath = transfer
                ? Constants.UTM_TRANSFER_TO_SHOP_PATH
                : Constants.UTM_ACT_WRITE_OFF_PATH;
        final String url;
        try {
            url = UtmSender.endpoint(address, documentPath);
        } catch (GenerationException e) {
            showWarning(e.getMessage());
            return;
        }

        if (lastGeneratedFiles.isEmpty() && !confirmSendingFoundFiles(files)) {
            return;
        }

        writePreference(PREF_UTM_ADDRESS, address);

        setBusy(true);
        log(transfer ? "――― Отправка документа переноса в УТМ ―――" : "――― Отправка актов в УТМ ―――");

        new SwingWorker<List<UtmSender.SendResult>, String>() {
            @Override
            protected List<UtmSender.SendResult> doInBackground() throws Exception {
                UtmSender sender = new UtmSender();
                List<UtmSender.SendResult> results = new ArrayList<>();
                publish("Команда: curl -F \"" + Constants.UTM_FORM_FIELD + "=@"
                        + Constants.UTM_UPLOAD_FILE_NAME + "\" " + url);
                for (File file : files) {
                    publish("Отправляю " + file.getName() + " …");
                    UtmSender.SendResult result = sender.send(file, address, documentPath);
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
     * найденные в папке результата. В режиме переноса это документ TransferToShop,
     * в режиме списания — акты по крепкому алкоголю и пиву.
     * <p>
     * Имена файлов содержат FSRAR_ID и дату, поэтому в папке ищется самый свежий
     * файл каждого вида.
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
        String[] prefixes = transferModeRadio.isSelected()
                ? new String[]{OutputNames.TRANSFER_PREFIX}
                : new String[]{OutputNames.STRONG_PREFIX, OutputNames.BEER_PREFIX};
        File[] found = directory.listFiles();
        if (found != null) {
            for (String prefix : prefixes) {
                File newest = null;
                for (File file : found) {
                    if (!file.isFile() || !file.getName().startsWith(prefix)) {
                        continue;
                    }
                    if (newest == null || file.lastModified() > newest.lastModified()) {
                        newest = file;
                    }
                }
                if (newest != null) {
                    files.add(newest);
                }
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
                        + "Программа работает в двух режимах, которые несовместимы:\n\n"
                        + "1. Акты списания по крепкому алкоголю и пиву\n"
                        + "   на основании выгрузки текущих остатков из ЕГАИС (ReplyRests_v2.xml).\n"
                        + "   Результат: ActWriteOff_v3(S)_ и ActWriteOff_v3(B)_\n"
                        + "   с FSRAR_ID и датой в имени файла.\n\n"
                        + "2. Перенос остатков в магазин (TransferToShop)\n"
                        + "   по выгрузке остатков магазина (ReplyRestsShop_v2.xml)\n"
                        + "   и выгрузке остатков склада (ReplyRests_v2.xml).\n"
                        + "   Результат: TransferToShop_<FSRAR_ID>_<ДД-ММ-ГГГГ>.xml.\n\n"
                        + "FSRAR_ID организации берётся из выбранного файла остатков.\n"
                        + "Остальные данные документа задаются в окне.\n\n"
                        + "Готовый документ можно отправить в УТМ утилитой curl\n"
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
                    } else if (transferModeRadio.isSelected()) {
                        // В режиме переноса нужны два файла: сначала заполняем остатки
                        // магазина, затем — остатки склада.
                        if (transferShopField.getText().trim().isEmpty()) {
                            transferShopField.setText(file.getAbsolutePath());
                            log("Выбран файл остатков магазина: " + file.getAbsolutePath());
                        } else {
                            transferStockField.setText(file.getAbsolutePath());
                            log("Выбран файл остатков склада: " + file.getAbsolutePath());
                        }
                        if (outputField.getText().trim().isEmpty() && file.getParentFile() != null) {
                            outputField.setText(file.getParentFile().getAbsolutePath());
                        }
                        updateTransferFsrarId();
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
        this.busy = busy;
        updateButtons();
    }

    private void updateButtons() {
        updateEnabledState(busy);
    }

    /**
     * Формирование возможно, когда заполнены поля того режима, который выбран.
     * Акты требуют файла остатков, отметки вида продукции и папки результата;
     * перенос — файла остатков магазина, файла остатков склада и папки результата.
     */
    private boolean canGenerate() {
        if (outputField.getText().trim().isEmpty()) {
            return false;
        }
        if (transferModeRadio.isSelected()) {
            return !transferShopField.getText().trim().isEmpty()
                    && !transferStockField.getText().trim().isEmpty();
        }
        return !inputField.getText().trim().isEmpty()
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

    private File currentTransferShopFile() {
        String text = transferShopField.getText().trim();
        return text.isEmpty() ? null : new File(text);
    }

    private File currentTransferStockFile() {
        String text = transferStockField.getText().trim();
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
     * Обновляет FSRAR_ID организации в режиме переноса остатков.
     * <p>
     * Код берётся из файла остатков магазина, а если там его нет — из файла остатков
     * склада. Поле только для чтения: значение всегда соответствует выбранным файлам.
     */
    private void updateTransferFsrarId() {
        String owner = readOwnerQuietly(currentTransferShopFile());
        if (owner.isEmpty()) {
            owner = readOwnerQuietly(currentTransferStockFile());
        }
        if (owner.equals(transferFsrarIdField.getText())) {
            // Значение не изменилось — не засоряем журнал повторными записями.
            return;
        }
        transferFsrarIdField.setText(owner);
        if (owner.isEmpty()) {
            log("В выбранных файлах не найден FSRAR_ID организации.");
        } else {
            log("FSRAR_ID организации взят из файла остатков: " + owner);
        }
        updateButtons();
    }

    private String readOwnerQuietly(File file) {
        if (file == null || !file.isFile()) {
            return "";
        }
        try {
            return XmlSupport.ownerFsrarId(file);
        } catch (GenerationException e) {
            log("Не удалось прочитать FSRAR_ID организации из файла "
                    + file.getName() + ": " + e.getMessage());
            return "";
        }
    }

    /**
     * Восстанавливает пути, выбранные при прошлом запуске, если они ещё существуют,
     * а также номер и причину списания, номер и Identity переноса и выбранный режим.
     * Дата и примечание не запоминаются — они привязаны к текущему дню, а FSRAR_ID
     * всегда берётся из файла остатков.
     */
    private void restorePreferences() {
        String lastInput = readPreference(PREF_LAST_INPUT_FILE);
        if (lastInput != null) {
            File file = new File(lastInput);
            if (file.isFile()) {
                inputField.setText(file.getAbsolutePath());
            }
        }
        String lastShop = readPreference(PREF_LAST_SHOP_FILE);
        if (lastShop != null) {
            File file = new File(lastShop);
            if (file.isFile()) {
                transferShopField.setText(file.getAbsolutePath());
            }
        }
        String lastStock = readPreference(PREF_LAST_STOCK_FILE);
        if (lastStock != null) {
            File file = new File(lastStock);
            if (file.isFile()) {
                transferStockField.setText(file.getAbsolutePath());
            }
        }
        String lastOutput = readPreference(PREF_LAST_OUTPUT_DIR);
        if (lastOutput != null) {
            File directory = new File(lastOutput);
            if (directory.isDirectory()) {
                outputField.setText(directory.getAbsolutePath());
            }
        }
        String savedStrongNumber = readPreference(PREF_ACT_NUMBER_STRONG);
        if (savedStrongNumber == null || savedStrongNumber.trim().isEmpty()) {
            // Значение из версии, где номер акта был один на оба акта.
            savedStrongNumber = readPreference(PREF_ACT_NUMBER_LEGACY);
        }
        if (savedStrongNumber != null && !savedStrongNumber.trim().isEmpty()) {
            actNumberStrongField.setText(savedStrongNumber);
        }
        String savedBeerNumber = readPreference(PREF_ACT_NUMBER_BEER);
        if (savedBeerNumber != null && !savedBeerNumber.trim().isEmpty()) {
            actNumberBeerField.setText(savedBeerNumber);
        }
        String savedTypeWriteOff = readPreference(PREF_TYPE_WRITE_OFF);
        if (savedTypeWriteOff != null && !savedTypeWriteOff.trim().isEmpty()) {
            typeWriteOffField.setText(savedTypeWriteOff);
        }
        String savedUtmAddress = readPreference(PREF_UTM_ADDRESS);
        if (savedUtmAddress != null && !savedUtmAddress.trim().isEmpty()) {
            utmAddressField.setText(savedUtmAddress);
        }
        String savedTransferNumber = readPreference(PREF_TRANSFER_NUMBER);
        if (savedTransferNumber != null && !savedTransferNumber.trim().isEmpty()) {
            transferNumberField.setText(savedTransferNumber);
        }
        String savedTransferIdentity = readPreference(PREF_TRANSFER_IDENTITY);
        if (savedTransferIdentity != null && !savedTransferIdentity.trim().isEmpty()) {
            transferIdentityField.setText(savedTransferIdentity);
        }
        // Режим запоминается последним: он определяет, какие поля останутся активными.
        boolean transferMode = "true".equals(readPreference(PREF_TRANSFER_MODE));
        writeOffModeRadio.setSelected(!transferMode);
        transferModeRadio.setSelected(transferMode);

        updateFsrarIdFromInput(currentInputFile());
        updateTransferFsrarId();
        updateMode();
    }
}
