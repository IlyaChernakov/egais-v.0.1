package core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import helpers.Constants;

/**
 * Отправляет сформированный документ ЕГАИС в УТМ (универсальный транспортный модуль)
 * утилитой {@code curl}, как это описано в документации ЕГАИС:
 * <pre>
 * curl -F "xml_file=@client.xml" http://localhost:8080/opt/in/ActWriteOff_v3
 * </pre>
 * <p>
 * Путь приёма зависит от типа документа: акт списания отправляется по пути
 * {@code /opt/in/ActWriteOff_v3}, документ переноса остатков в магазин — по пути
 * {@code /opt/in/TransferToShop}.
 * <p>
 * УТМ принимает документ под именем {@code client.xml}, поэтому файл результата
 * копируется во временный каталог под этим именем, а исходный файл не меняется.
 * Ответ УТМ (обычно XML с квитанцией или текст ошибки) возвращается вызывающему
 * коду для показа пользователю.
 */
public class UtmSender {

    /** Итог одной отправки: адрес, код возврата curl и его вывод. */
    public static final class SendResult {

        private final File file;
        private final String url;
        private final int exitCode;
        private final String output;

        SendResult(File file, String url, int exitCode, String output) {
            this.file = file;
            this.url = url;
            this.exitCode = exitCode;
            this.output = output;
        }

        /** Файл, который отправлялся. */
        public File getFile() {
            return file;
        }

        /** Полный адрес приёма, по которому отправлялся файл. */
        public String getUrl() {
            return url;
        }

        /** Код возврата curl: 0 — отправка принята. */
        public int getExitCode() {
            return exitCode;
        }

        /** Вывод curl (ответ УТМ или сообщение об ошибке), без лишних пробелов. */
        public String getOutput() {
            return output;
        }

        /** Принят ли акт УТМ. */
        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    /** Команда запуска curl; вынесена в поле, чтобы подменять её в проверках. */
    private String curlCommand = resolveCurl();

    /**
     * Задаёт команду запуска curl вместо найденной автоматически.
     * Нужна для проверок, где curl подменяется заглушкой.
     *
     * @param command путь к исполняемому файлу или имя команды
     * @return этот же отправитель — для цепочки вызовов
     */
    public UtmSender withCurlCommand(String command) {
        if (command != null && !command.trim().isEmpty()) {
            this.curlCommand = command;
        }
        return this;
    }

    /** Команда запуска curl, которая будет использована при отправке. */
    public String getCurlCommand() {
        return curlCommand;
    }

    /**
     * Отправляет акт списания в УТМ по пути {@code /opt/in/ActWriteOff_v3}.
     *
     * @param xmlFile файл акта, сформированный программой
     * @param address адрес УТМ из поля «Адрес УТМ», например {@code http://localhost:8080}
     * @return итог отправки с кодом возврата и выводом curl
     * @throws GenerationException если файла нет, адрес пуст или curl не удалось запустить
     */
    public SendResult send(File xmlFile, String address) throws GenerationException {
        return send(xmlFile, address, Constants.UTM_ACT_WRITE_OFF_PATH);
    }

    /**
     * Отправляет документ ЕГАИС в УТМ по указанному пути приёма.
     *
     * @param xmlFile      файл документа, сформированный программой
     * @param address      адрес УТМ из поля «Адрес УТМ», например {@code http://localhost:8080};
     *                     путь приёма дописывается автоматически, если его ещё нет в адресе
     * @param documentPath путь приёма в УТМ, например {@code /opt/in/TransferToShop}
     * @return итог отправки с кодом возврата и выводом curl
     * @throws GenerationException если файла нет, адрес пуст или curl не удалось запустить
     */
    public SendResult send(File xmlFile, String address, String documentPath) throws GenerationException {
        if (xmlFile == null || !xmlFile.isFile()) {
            throw new GenerationException("Файл для отправки не найден: "
                    + (xmlFile == null ? "путь не задан" : xmlFile.getAbsolutePath()));
        }
        final String url = endpoint(address, documentPath);

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("utm-send");
            // УТМ принимает акт под именем client.xml, поэтому копируем файл под этим
            // именем: исходный акт в папке результата остаётся нетронутым.
            Path upload = tempDir.resolve(Constants.UTM_UPLOAD_FILE_NAME);
            Files.copy(xmlFile.toPath(), upload, StandardCopyOption.REPLACE_EXISTING);

            File curlOutput = tempDir.resolve("curl-output.txt").toFile();
            List<String> command = new ArrayList<>();
            command.add(curlCommand);
            command.add("-F");
            command.add(Constants.UTM_FORM_FIELD + "=@" + upload.toAbsolutePath());
            command.add(url);

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            // Вывод пишется в файл, а не в поток: так чтение не блокируется, если УТМ зависнет.
            builder.redirectOutput(curlOutput);

            Process process = builder.start();
            if (!process.waitFor(Constants.UTM_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new GenerationException("УТМ не ответил за " + Constants.UTM_TIMEOUT_SECONDS
                        + " с.\nПроверьте адрес УТМ и доступность сервера: " + url);
            }

            String output = new String(Files.readAllBytes(curlOutput.toPath()), StandardCharsets.UTF_8).trim();
            return new SendResult(xmlFile, url, process.exitValue(), output);
        } catch (IOException e) {
            throw new GenerationException("Не удалось выполнить curl («" + curlCommand + "»).\n"
                    + "Проверьте, что утилита curl доступна в системе.\n" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GenerationException("Отправка в УТМ прервана.");
        } finally {
            deleteQuietly(tempDir);
        }
    }

    /**
     * Собирает полный адрес приёма акта списания из адреса УТМ.
     *
     * @param address адрес УТМ из поля «Адрес УТМ»
     * @return полный адрес для curl
     * @throws GenerationException если адрес пуст
     */
    public static String endpoint(String address) throws GenerationException {
        return endpoint(address, Constants.UTM_ACT_WRITE_OFF_PATH);
    }

    /**
     * Собирает полный адрес приёма документа из адреса УТМ.
     * <p>
     * Если схема не указана, подставляется {@code http://}. Если в адресе уже
     * указан путь приёма, он заменяется на нужный: так адрес, скопированный для
     * одного документа, не превращается в {@code .../ActWriteOff_v3/opt/in/TransferToShop}.
     *
     * @param address      адрес УТМ из поля «Адрес УТМ»
     * @param documentPath путь приёма в УТМ, например {@code /opt/in/TransferToShop}
     * @return полный адрес для curl
     * @throws GenerationException если адрес пуст
     */
    public static String endpoint(String address, String documentPath) throws GenerationException {
        if (address == null || address.trim().isEmpty()) {
            throw new GenerationException("Укажите адрес УТМ.");
        }
        String value = address.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty()) {
            throw new GenerationException("Укажите адрес УТМ.");
        }
        if (!value.contains("://")) {
            value = "http://" + value;
        }
        String path = documentPath == null ? "" : documentPath.trim();
        if (path.isEmpty() || value.endsWith(path)) {
            return value;
        }
        // Адрес мог быть скопирован вместе с путём приёма другого документа —
        // например, с «/opt/in/ActWriteOff_v3» или с оборванным «/opt/in».
        // Такой хвост отбрасывается, иначе путь продублировался бы.
        int marker = indexOfUploadRoot(value);
        if (marker >= 0) {
            value = value.substring(0, marker);
        }
        return value + path;
    }

    /**
     * Ищет в адресе начало пути приёма УТМ ({@code /opt/in}).
     *
     * @param value адрес УТМ без завершающих косых черт
     * @return позиция первого вхождения {@code /opt/in} как отдельного сегмента
     *         или {@code -1}, если его нет
     */
    private static int indexOfUploadRoot(String value) {
        String root = "/opt/in";
        int marker = value.indexOf(root);
        while (marker >= 0) {
            int after = marker + root.length();
            if (after == value.length() || value.charAt(after) == '/') {
                return marker;
            }
            marker = value.indexOf(root, marker + 1);
        }
        return -1;
    }

    /** Ищет curl: сначала штатный в System32, затем в PATH. */
    private static String resolveCurl() {
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot != null && !systemRoot.isEmpty()) {
            File bundled = new File(systemRoot, "System32" + File.separator + "curl.exe");
            if (bundled.isFile()) {
                return bundled.getAbsolutePath();
            }
        }
        return "curl";
    }

    private static void deleteQuietly(Path directory) {
        if (directory == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Временные файлы уберёт система.
                }
            });
        } catch (IOException ignored) {
            // Временные файлы уберёт система.
        }
    }
}
