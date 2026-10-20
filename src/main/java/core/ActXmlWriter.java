package core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import helpers.Constants;

/**
 * Записывает акт списания ровно в том виде, в каком он приведён в образце
 * {@code src/main/java/files/1.xml}: те же теги, порядок атрибутов, отступы,
 * отсутствие XML-декларации и перевод строки CRLF.
 * <p>
 * Обычный XML-сериализатор так не сделает: он добавляет декларацию, убирает
 * повторные объявления пространства имён и расставляет отступы по-своему,
 * поэтому документ собирается как текст. Значения экранируются.
 */
public final class ActXmlWriter {

    /** Перевод строки как в образце — файл рассчитан на Windows. */
    private static final String EOL = "\r\n";

    private static final String ATTR_XMLNS_XSI = "xmlns:xsi";
    private static final String ATTR_XMLNS_NS = "xmlns:ns";
    private static final String ATTR_XMLNS_PREF = "xmlns:pref";
    private static final String ATTR_XMLNS_AWR = "xmlns:awr";
    private static final String ATTR_XMLNS_CE = "xmlns:ce";

    private static final String INDENT_ENVELOPE_1 = "  ";
    private static final String INDENT_ENVELOPE_2 = "    ";
    private static final String INDENT_ENVELOPE_3 = "      ";
    private static final String INDENT_ENVELOPE_4 = "        ";

    // В образце блок awr:Content оформлен с отступом в 4 пробела на уровень,
    // а сам открывающий тег стоит в первой колонке — сохраняем это как есть.
    private static final String INDENT_POSITION = "    ";
    private static final String INDENT_FIELD = "        ";
    private static final String INDENT_INFORM_F2 = "            ";
    private static final String INDENT_F2_REG_ID = "                ";
    private static final String INDENT_AMC = "            ";

    private final ActHeader header;
    private final StringBuilder positions = new StringBuilder();

    public ActXmlWriter(ActHeader header) {
        this.header = header;
    }

    /**
     * Добавляет позицию акта.
     *
     * @param number   номер позиции
     * @param quantity количество
     * @param f2RegId  регистрационный номер справки 2
     * @param marks    марки бутылок; пустой список — блок {@code awr:MarkCodeInfo}
     *                 не записывается (так формируется акт по пиву)
     */
    public void position(int number, String quantity, String f2RegId, List<String> marks) {
        openLine(positions, INDENT_POSITION, Constants.TAG_POSITION);
        line(positions, INDENT_FIELD, Constants.TAG_IDENTITY, String.valueOf(number));
        line(positions, INDENT_FIELD, Constants.TAG_QUANTITY, quantity);
        openLine(positions, INDENT_FIELD, Constants.TAG_INFORM_F1F2);
        openLine(positions, INDENT_INFORM_F2, Constants.TAG_INFORM_F2);
        line(positions, INDENT_F2_REG_ID, Constants.TAG_F2_REG_ID, f2RegId);
        closeLine(positions, INDENT_INFORM_F2, Constants.TAG_INFORM_F2);
        closeLine(positions, INDENT_FIELD, Constants.TAG_INFORM_F1F2);

        if (marks != null && !marks.isEmpty()) {
            openLine(positions, INDENT_FIELD, Constants.TAG_MARK_CODE_INFO);
            for (String mark : marks) {
                positions.append(INDENT_AMC)
                        .append('<').append(Constants.TAG_AMC)
                        .append(' ').append(ATTR_XMLNS_CE).append("=\"").append(Constants.NAMESPACE_CE).append("\">")
                        .append(escape(mark))
                        .append("</").append(Constants.TAG_AMC).append('>')
                        .append(EOL);
            }
            closeLine(positions, INDENT_FIELD, Constants.TAG_MARK_CODE_INFO);
        }

        closeLine(positions, INDENT_POSITION, Constants.TAG_POSITION);
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

        StringBuilder xml = new StringBuilder(2048);
        xml.append('<').append(Constants.TAG_DOCUMENTS)
                .append(' ').append(Constants.ATTR_VERSION).append("=\"").append(Constants.DOCUMENTS_VERSION).append('"')
                .append(' ').append(ATTR_XMLNS_XSI).append("=\"").append(Constants.NAMESPACE_XSI).append('"')
                .append(' ').append(ATTR_XMLNS_NS).append("=\"").append(Constants.NAMESPACE_DOCUMENTS).append('"')
                .append(' ').append(ATTR_XMLNS_PREF).append("=\"").append(Constants.NAMESPACE_PREF).append('"')
                .append(' ').append(ATTR_XMLNS_AWR).append("=\"").append(Constants.NAMESPACE_AWR).append('"')
                .append(' ').append(ATTR_XMLNS_CE).append("=\"").append(Constants.NAMESPACE_CE).append("\">")
                .append(EOL);

        openLine(xml, INDENT_ENVELOPE_1, Constants.TAG_OWNER);
        line(xml, INDENT_ENVELOPE_2, Constants.TAG_FSRAR_ID, header.getFsrarId());
        closeLine(xml, INDENT_ENVELOPE_1, Constants.TAG_OWNER);

        openLine(xml, INDENT_ENVELOPE_1, Constants.TAG_DOCUMENT);
        openLine(xml, INDENT_ENVELOPE_2, Constants.TAG_ACT_WRITE_OFF);
        line(xml, INDENT_ENVELOPE_3, Constants.TAG_IDENTITY, header.getActNumber());

        openLine(xml, INDENT_ENVELOPE_3, Constants.TAG_HEADER);
        line(xml, INDENT_ENVELOPE_4, Constants.TAG_ACT_NUMBER, header.getActNumber());
        line(xml, INDENT_ENVELOPE_4, Constants.TAG_ACT_DATE, header.getActDateText());
        if (!header.getTypeWriteOff().isEmpty()) {
            line(xml, INDENT_ENVELOPE_4, Constants.TAG_TYPE_WRITE_OFF, header.getTypeWriteOff());
        }
        if (!header.getNote().isEmpty()) {
            line(xml, INDENT_ENVELOPE_4, Constants.TAG_NOTE, header.getNote());
        }
        closeLine(xml, INDENT_ENVELOPE_3, Constants.TAG_HEADER);

        // Открывающий тег Content стоит в первой колонке, как в образце.
        if (positions.length() == 0) {
            emptyLine(xml, "", Constants.TAG_CONTENT);
        } else {
            openLine(xml, "", Constants.TAG_CONTENT);
            xml.append(positions);
            closeLine(xml, "", Constants.TAG_CONTENT);
        }

        closeLine(xml, INDENT_ENVELOPE_2, Constants.TAG_ACT_WRITE_OFF);
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
