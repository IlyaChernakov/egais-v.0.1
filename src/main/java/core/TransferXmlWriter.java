package core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import helpers.Constants;

/**
 * Записывает документ переноса остатков в магазин ровно в том виде, в каком он
 * приведён в образце {@code src/main/java/files-/TransferToShop.xml}: те же теги,
 * порядок объявлений пространств имён, отступы, XML-декларация и перевод строки CRLF.
 * <p>
 * Обычный XML-сериализатор так не сделает: он расставляет отступы по-своему
 * и убирает повторные объявления пространств имён, поэтому документ собирается
 * как текст. Значения экранируются.
 */
public final class TransferXmlWriter {

    /** Перевод строки как в образце — файл рассчитан на Windows. */
    private static final String EOL = "\r\n";

    private static final String ATTR_XMLNS_NS = "xmlns:ns";
    private static final String ATTR_XMLNS_XS = "xmlns:xs";
    private static final String ATTR_XMLNS_C = "xmlns:c";
    private static final String ATTR_XMLNS_PREF = "xmlns:pref";
    private static final String ATTR_XMLNS_TTS = "xmlns:tts";

    private static final String INDENT_ENVELOPE_1 = "    ";
    private static final String INDENT_ENVELOPE_2 = "        ";
    private static final String INDENT_ENVELOPE_3 = "            ";
    private static final String INDENT_ENVELOPE_4 = "                ";

    // В образце блок tts:Content оформлен с отступом в 4 пробела на уровень,
    // а сам открывающий тег стоит в первой колонке — сохраняем это как есть.
    private static final String INDENT_POSITION = "    ";
    private static final String INDENT_FIELD = "        ";
    private static final String INDENT_F2_REG_ID = "            ";

    private final TransferHeader header;
    private final StringBuilder positions = new StringBuilder();

    public TransferXmlWriter(TransferHeader header) {
        this.header = header;
    }

    /**
     * Добавляет позицию документа переноса.
     *
     * @param number      номер позиции; он же {@code tts:Identity}
     * @param productCode код продукции ({@code tts:ProductCode})
     * @param quantity    количество ({@code tts:Quantity})
     * @param f2RegId     регистрационный номер справки 2 ({@code pref:F2RegId})
     */
    public void position(int number, String productCode, String quantity, String f2RegId) {
        openLine(positions, INDENT_POSITION, Constants.TTS_POSITION);
        line(positions, INDENT_FIELD, Constants.TTS_IDENTITY, String.valueOf(number));
        line(positions, INDENT_FIELD, Constants.TTS_PRODUCT_CODE, productCode);
        line(positions, INDENT_FIELD, Constants.TTS_QUANTITY, quantity);
        openLine(positions, INDENT_FIELD, Constants.TTS_INFORM_F2);
        line(positions, INDENT_F2_REG_ID, Constants.TAG_F2_REG_ID, f2RegId);
        closeLine(positions, INDENT_FIELD, Constants.TTS_INFORM_F2);
        closeLine(positions, INDENT_POSITION, Constants.TTS_POSITION);
    }

    /**
     * Записывает готовый документ в файл в кодировке UTF-8 без BOM.
     * Родительские каталоги создаются автоматически.
     *
     * @param outputFile файл результата
     * @throws GenerationException если каталог или файл не удалось создать
     */
    public void writeTo(File outputFile) throws GenerationException {
        if (outputFile == null) {
            throw new GenerationException("Не задан файл для сохранения результата.");
        }

        StringBuilder xml = new StringBuilder(4096);
        xml.append(Constants.XML_DECLARATION_UTF8).append(EOL);
        xml.append('<').append(Constants.TAG_DOCUMENTS)
                .append(' ').append(Constants.ATTR_VERSION).append("=\"").append(Constants.DOCUMENTS_VERSION).append('"')
                .append(EOL);
        xml.append(INDENT_ENVELOPE_1).append(ATTR_XMLNS_NS).append("=\"").append(Constants.NAMESPACE_DOCUMENTS)
                .append('"').append(EOL);
        xml.append(INDENT_ENVELOPE_1).append(ATTR_XMLNS_XS).append("=\"").append(Constants.NAMESPACE_XS)
                .append('"').append(EOL);
        xml.append(INDENT_ENVELOPE_1).append(ATTR_XMLNS_C).append("=\"").append(Constants.NAMESPACE_COMMON)
                .append('"').append(EOL);
        xml.append(INDENT_ENVELOPE_1).append(ATTR_XMLNS_PREF).append("=\"").append(Constants.NAMESPACE_PREF)
                .append('"').append(EOL);
        xml.append(INDENT_ENVELOPE_1).append(ATTR_XMLNS_TTS).append("=\"").append(Constants.NAMESPACE_TTS)
                .append("\">").append(EOL);

        openLine(xml, INDENT_ENVELOPE_1, Constants.TAG_OWNER);
        line(xml, INDENT_ENVELOPE_2, Constants.TAG_FSRAR_ID, header.getFsrarId());
        closeLine(xml, INDENT_ENVELOPE_1, Constants.TAG_OWNER);

        openLine(xml, INDENT_ENVELOPE_1, Constants.TAG_DOCUMENT);
        openLine(xml, INDENT_ENVELOPE_2, Constants.TAG_TRANSFER_TO_SHOP);
        line(xml, INDENT_ENVELOPE_3, Constants.TTS_IDENTITY, header.getIdentity());

        openLine(xml, INDENT_ENVELOPE_3, Constants.TTS_HEADER);
        line(xml, INDENT_ENVELOPE_4, Constants.TTS_TRANSFER_NUMBER, header.getTransferNumber());
        line(xml, INDENT_ENVELOPE_4, Constants.TTS_TRANSFER_DATE, header.getTransferDateText());
        closeLine(xml, INDENT_ENVELOPE_3, Constants.TTS_HEADER);

        // Открывающий тег Content стоит в первой колонке, как в образце.
        if (positions.length() == 0) {
            emptyLine(xml, "", Constants.TTS_CONTENT);
        } else {
            openLine(xml, "", Constants.TTS_CONTENT);
            xml.append(positions);
            closeLine(xml, "", Constants.TTS_CONTENT);
        }

        closeLine(xml, INDENT_ENVELOPE_2, Constants.TAG_TRANSFER_TO_SHOP);
        closeLine(xml, INDENT_ENVELOPE_1, Constants.TAG_DOCUMENT);
        // Завершающий тег — последняя строка файла, перевод строки после него не нужен.
        xml.append("</").append(Constants.TAG_DOCUMENTS).append('>');

        File parent = outputFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new GenerationException("Не удалось создать папку: " + parent.getAbsolutePath());
        }
        try {
            Files.write(outputFile.toPath(), xml.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new GenerationException("Не удалось записать файл " + outputFile.getAbsolutePath()
                    + ": " + e.getMessage(), e);
        }
    }

    private static void openLine(StringBuilder out, String indent, String tag) {
        out.append(indent).append('<').append(tag).append('>').append(EOL);
    }

    private static void closeLine(StringBuilder out, String indent, String tag) {
        out.append(indent).append("</").append(tag).append('>').append(EOL);
    }

    private static void emptyLine(StringBuilder out, String indent, String tag) {
        out.append(indent).append('<').append(tag).append("/>").append(EOL);
    }

    private static void line(StringBuilder out, String indent, String tag, String value) {
        out.append(indent).append('<').append(tag).append('>')
                .append(escape(value))
                .append("</").append(tag).append('>')
                .append(EOL);
    }

    /** Экранирует служебные символы XML. */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    result.append("&amp;");
                    break;
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }
}
