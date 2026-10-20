package helpers;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import core.GenerationException;

/**
 * Вспомогательные операции чтения XML, общие для парсеров крепкого алкоголя и пива.
 */
public final class XmlSupport {

    private XmlSupport() {
    }

    /**
     * Читает XML-файл и возвращает разобранный документ.
     *
     * @param xmlFile файл выгрузки остатков ЕГАИС
     * @return разобранный документ
     * @throws GenerationException если файл недоступен или содержит некорректный XML
     */
    public static Document parse(File xmlFile) throws GenerationException {
        if (xmlFile == null) {
            throw new GenerationException("Не выбран файл остатков.");
        }
        if (!xmlFile.isFile()) {
            throw new GenerationException("Файл остатков не найден: " + xmlFile.getAbsolutePath());
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);
            document.getDocumentElement().normalize();
            return document;
        } catch (Exception e) {
            throw new GenerationException(
                    "Не удалось прочитать файл остатков " + xmlFile.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Возвращает текстовое содержимое первого вложенного элемента с указанным именем.
     *
     * @param parent  элемент, внутри которого выполняется поиск
     * @param tagName имя тега вместе с префиксом, например {@code rst:Quantity}
     * @param positionNumber номер позиции — для понятного сообщения об ошибке
     * @return текст элемента (без обрезки пробелов)
     * @throws GenerationException если элемент отсутствует
     */
    public static String requiredText(Element parent, String tagName, int positionNumber) throws GenerationException {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            throw new GenerationException("Позиция №" + positionNumber + ": не найден обязательный элемент <" + tagName + ">.");
        }
        return nodes.item(0).getTextContent();
    }

    /**
     * Читает FSRAR_ID организации из файла выгрузки остатков.
     * <p>
     * Код нужен для шапки акта, поэтому программа подставляет его автоматически,
     * если пользователь ещё не заполнил это поле.
     *
     * @param xmlFile файл выгрузки остатков
     * @return код организации или пустая строка, если его в файле нет
     * @throws GenerationException если файл недоступен или содержит некорректный XML
     */
    public static String ownerFsrarId(File xmlFile) throws GenerationException {
        NodeList nodes = parse(xmlFile).getElementsByTagName(Constants.TAG_FSRAR_ID);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }

    /**
     * Записывает документ в файл в кодировке UTF-8 с отступами.
     * Родительские каталоги создаются автоматически.
     *
     * @param document   документ для записи
     * @param outputFile файл результата
     * @throws GenerationException если каталог или файл не удалось создать
     */
    public static void write(Document document, File outputFile) throws GenerationException {
        if (outputFile == null) {
            throw new GenerationException("Не задан файл для сохранения результата.");
        }
        try {
            File parent = outputFile.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new GenerationException("Не удалось создать папку: " + parent.getAbsolutePath());
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(outputFile));
        } catch (GenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new GenerationException("Не удалось записать файл " + outputFile.getAbsolutePath()
                    + ": " + e.getMessage(), e);
        }
    }
}
