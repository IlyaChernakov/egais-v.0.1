package core;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import helpers.Constants;

/**
 * Собирает полный документ акта списания по образцу {@code files/1.xml}:
 * <pre>
 * &lt;ns:Documents Version="1.0"&gt;
 *   &lt;ns:Owner&gt;&lt;ns:FSRAR_ID&gt;…&lt;/ns:FSRAR_ID&gt;&lt;/ns:Owner&gt;
 *   &lt;ns:Document&gt;
 *     &lt;ns:ActWriteOff_v3&gt;
 *       &lt;awr:Identity&gt;…&lt;/awr:Identity&gt;
 *       &lt;awr:Header&gt;…&lt;/awr:Header&gt;
 *       &lt;awr:Content&gt;позиции&lt;/awr:Content&gt;
 *     &lt;/ns:ActWriteOff_v3&gt;
 *   &lt;/ns:Document&gt;
 * &lt;/ns:Documents&gt;
 * </pre>
 * Позиции добавляются вызывающим кодом в элемент {@link #content()}.
 */
public final class ActDocumentBuilder {

    private static final String ATTR_XMLNS_XSI = "xmlns:xsi";
    private static final String ATTR_XMLNS_NS = "xmlns:ns";
    private static final String ATTR_XMLNS_PREF = "xmlns:pref";
    private static final String ATTR_XMLNS_AWR = "xmlns:awr";

    private final Document document;
    private final Element content;

    /**
     * Создаёт документ с заполненной шапкой.
     *
     * @param header данные шапки акта
     * @throws GenerationException если не удалось создать XML-документ
     */
    public ActDocumentBuilder(ActHeader header) throws GenerationException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            this.document = builder.newDocument();
        } catch (Exception e) {
            throw new GenerationException("Не удалось создать XML-документ акта: " + e.getMessage(), e);
        }

        Element root = document.createElement(Constants.TAG_DOCUMENTS);
        document.appendChild(root);
        root.setAttribute(Constants.ATTR_VERSION, Constants.DOCUMENTS_VERSION);
        root.setAttribute(ATTR_XMLNS_XSI, Constants.NAMESPACE_XSI);
        root.setAttribute(ATTR_XMLNS_NS, Constants.NAMESPACE_DOCUMENTS);
        root.setAttribute(ATTR_XMLNS_PREF, Constants.NAMESPACE_PREF);
        root.setAttribute(ATTR_XMLNS_AWR, Constants.NAMESPACE_AWR);
        root.setAttribute(Constants.ATTR_XMLNS_CE, Constants.NAMESPACE_CE);

        Element owner = append(root, Constants.TAG_OWNER);
        appendText(owner, Constants.TAG_FSRAR_ID, header.getFsrarId());

        Element documentElement = append(root, Constants.TAG_DOCUMENT);
        Element act = append(documentElement, Constants.TAG_ACT_WRITE_OFF);
        appendText(act, Constants.TAG_IDENTITY, header.getActNumber());

        Element actHeader = append(act, Constants.TAG_HEADER);
        appendText(actHeader, Constants.TAG_ACT_NUMBER, header.getActNumber());
        appendText(actHeader, Constants.TAG_ACT_DATE, header.getActDateText());
        optionalText(actHeader, Constants.TAG_TYPE_WRITE_OFF, header.getTypeWriteOff());
        optionalText(actHeader, Constants.TAG_NOTE, header.getNote());

        this.content = append(act, Constants.TAG_CONTENT);
    }

    /** Готовый документ для записи в файл. */
    public Document document() {
        return document;
    }

    /** Элемент {@code awr:Content}, в который добавляются позиции акта. */
    public Element content() {
        return content;
    }

    /** Создаёт вложенный элемент с указанным именем тега. */
    public Element appendChild(Element parent, String tagName) {
        return append(parent, tagName);
    }

    /** Создаёт вложенный элемент и записывает в него текст. */
    public Element appendText(Element parent, String tagName, String value) {
        Element element = append(parent, tagName);
        element.appendChild(document.createTextNode(value == null ? "" : value));
        return element;
    }

    private void optionalText(Element parent, String tagName, String value) {
        if (value != null && !value.isEmpty()) {
            appendText(parent, tagName, value);
        }
    }

    private Element append(Element parent, String tagName) {
        Element element = document.createElement(tagName);
        parent.appendChild(element);
        return element;
    }
}
