package core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Сборка документов ЕГАИС и их запись в стандартном оформлении XML.
 * <p>
 * Оформление задаёт стандартный сериализатор Java: каждый элемент — на своей
 * строке, все атрибуты элемента — в одной строке, вложенность — четыре пробела,
 * пустые элементы — вида {@code <tag/>}. XML-декларация пишется вручную, потому
 * что сериализатор добавляет к ней {@code standalone="no"}.
 * <p>
 * Перевод строки приводится к CRLF: документы рассчитаны на Windows.
 */
public final class XmlOutput {

    /** XML-декларация документов. */
    public static final String DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    /** Величина отступа вложенных элементов. */
    private static final int INDENT_AMOUNT = 4;

    /** Свойство сериализатора, задающее величину отступа. */
    private static final String INDENT_AMOUNT_PROPERTY = "{http://xml.apache.org/xslt}indent-amount";

    /** Адрес служебного пространства имён объявлений {@code xmlns}. */
    private static final String NAMESPACE_XMLNS = "http://www.w3.org/2000/xmlns/";

    /** Перевод строки документа. */
    private static final String EOL = "\r\n";

    private XmlOutput() {
    }

    /**
     * Создаёт пустой документ, готовый к сборке дерева элементов.
     *
     * @return пустой документ с поддержкой пространств имён
     * @throws GenerationException если парсер не удалось настроить
     */
    public static Document newDocument() throws GenerationException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new GenerationException("Не удалось подготовить XML-документ: " + e.getMessage(), e);
        }
    }

    /**
     * Создаёт элемент в указанном пространстве имён.
     *
     * @param document  документ-владелец
     * @param namespace пространство имён элемента
     * @param name      имя элемента вместе с префиксом, например {@code ns:Owner}
     * @return новый элемент
     */
    public static Element element(Document document, String namespace, String name) {
        return document.createElementNS(namespace, name);
    }

    /**
     * Создаёт элемент с текстовым содержимым.
     *
     * @param document  документ-владелец
     * @param namespace пространство имён элемента
     * @param name      имя элемента вместе с префиксом
     * @param value     текстовое значение; служебные символы экранирует сериализатор
     * @return новый элемент
     */
    public static Element textElement(Document document, String namespace, String name, String value) {
        Element element = element(document, namespace, name);
        element.setTextContent(value == null ? "" : value);
        return element;
    }

    /**
     * Объявляет пространство имён у корневого элемента.
     *
     * @param element элемент, в котором делается объявление (обычно корневой)
     * @param name    имя объявления, например {@code xmlns:ns}
     * @param value   адрес пространства имён
     */
    public static void declareNamespace(Element element, String name, String value) {
        element.setAttributeNS(NAMESPACE_XMLNS, name, value);
    }

    /**
     * Записывает документ в файл в кодировке UTF-8 без BOM.
     * Родительские каталоги создаются автоматически.
     *
     * @param document   документ
     * @param outputFile файл результата
     * @throws GenerationException если каталог или файл не удалось создать
     */
    public static void write(Document document, File outputFile) throws GenerationException {
        if (outputFile == null) {
            throw new GenerationException("Не задан файл для сохранения результата.");
        }

        File parent = outputFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new GenerationException("Не удалось создать папку: " + parent.getAbsolutePath());
        }

        String xml = DECLARATION + EOL + windowsLineEndings(serialize(document));
        try {
            Files.write(outputFile.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new GenerationException("Не удалось записать файл " + outputFile.getAbsolutePath()
                    + ": " + e.getMessage(), e);
        }
    }

    private static String serialize(Document document) throws GenerationException {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(INDENT_AMOUNT_PROPERTY, String.valueOf(INDENT_AMOUNT));

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(document), new StreamResult(buffer));
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (TransformerException e) {
            throw new GenerationException("Не удалось собрать XML-документ: " + e.getMessage(), e);
        }
    }

    /** Приводит переводы строк к CRLF, чтобы вид файла не зависел от системы. */
    private static String windowsLineEndings(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n').replace("\n", EOL);
    }
}
