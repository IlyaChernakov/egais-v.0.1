package core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import helpers.Constants;

/**
 * Записывает акт списания {@code ns:ActWriteOff_v3} в стандартном оформлении XML
 * (см. {@link XmlOutput}).
 * <p>
 * Документ собирается как дерево элементов, поэтому экранирование значений,
 * отступы, объявления пространств имён и вид пустых элементов обеспечивает
 * стандартный сериализатор.
 */
public final class ActXmlWriter {

    private final ActHeader header;
    private final List<Position> positions = new ArrayList<>();

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
        positions.add(new Position(number, quantity, f2RegId,
                marks == null ? Collections.emptyList() : new ArrayList<>(marks)));
    }

    /**
     * Записывает готовый документ в файл в кодировке UTF-8 без BOM.
     * Родительские каталоги создаются автоматически.
     *
     * @param outputFile файл результата
     * @throws GenerationException если документ или файл не удалось записать
     */
    public void writeTo(File outputFile) throws GenerationException {
        XmlOutput.write(buildDocument(), outputFile);
    }

    private Document buildDocument() throws GenerationException {
        Document document = XmlOutput.newDocument();

        Element root = XmlOutput.element(document, Constants.NAMESPACE_DOCUMENTS, Constants.TAG_DOCUMENTS);
        document.appendChild(root);
        root.setAttribute(Constants.ATTR_VERSION, Constants.DOCUMENTS_VERSION);
        XmlOutput.declareNamespace(root, "xmlns:xsi", Constants.NAMESPACE_XSI);
        XmlOutput.declareNamespace(root, "xmlns:ns", Constants.NAMESPACE_DOCUMENTS);
        XmlOutput.declareNamespace(root, "xmlns:pref", Constants.NAMESPACE_PREF);
        XmlOutput.declareNamespace(root, "xmlns:awr", Constants.NAMESPACE_AWR);
        XmlOutput.declareNamespace(root, "xmlns:ce", Constants.NAMESPACE_CE);

        Element owner = append(root, Constants.NAMESPACE_DOCUMENTS, Constants.TAG_OWNER);
        owner.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_DOCUMENTS,
                Constants.TAG_FSRAR_ID, header.getFsrarId()));

        Element envelope = append(root, Constants.NAMESPACE_DOCUMENTS, Constants.TAG_DOCUMENT);
        Element act = append(envelope, Constants.NAMESPACE_DOCUMENTS, Constants.TAG_ACT_WRITE_OFF);
        act.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_AWR,
                Constants.TAG_IDENTITY, header.getActNumber()));

        Element actHeader = append(act, Constants.NAMESPACE_AWR, Constants.TAG_HEADER);
        actHeader.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_AWR,
                Constants.TAG_ACT_NUMBER, header.getActNumber()));
        actHeader.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_AWR,
                Constants.TAG_ACT_DATE, header.getActDateText()));
        if (!header.getTypeWriteOff().isEmpty()) {
            actHeader.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_AWR,
                    Constants.TAG_TYPE_WRITE_OFF, header.getTypeWriteOff()));
        }
        if (!header.getNote().isEmpty()) {
            actHeader.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_AWR,
                    Constants.TAG_NOTE, header.getNote()));
        }

        Element content = append(act, Constants.NAMESPACE_AWR, Constants.TAG_CONTENT);
        for (Position position : positions) {
            appendPosition(document, content, position);
        }
        return document;
    }

    private void appendPosition(Document document, Element content, Position position) {
        Element element = append(content, Constants.NAMESPACE_AWR, Constants.TAG_POSITION);
        element.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_AWR,
                Constants.TAG_IDENTITY, String.valueOf(position.number)));
        element.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_AWR,
                Constants.TAG_QUANTITY, position.quantity));

        Element informF1F2 = append(element, Constants.NAMESPACE_AWR, Constants.TAG_INFORM_F1F2);
        Element informF2 = append(informF1F2, Constants.NAMESPACE_AWR, Constants.TAG_INFORM_F2);
        informF2.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_PREF,
                Constants.TAG_F2_REG_ID, position.f2RegId));

        if (!position.marks.isEmpty()) {
            Element markCodeInfo = append(element, Constants.NAMESPACE_AWR, Constants.TAG_MARK_CODE_INFO);
            for (String mark : position.marks) {
                markCodeInfo.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_CE,
                        Constants.TAG_AMC, mark));
            }
        }
    }

    private static Element append(Element parent, String namespace, String name) {
        Element element = XmlOutput.element(parent.getOwnerDocument(), namespace, name);
        parent.appendChild(element);
        return element;
    }

    /** Позиция акта в том виде, в каком её передал вызывающий код. */
    private static final class Position {

        private final int number;
        private final String quantity;
        private final String f2RegId;
        private final List<String> marks;

        private Position(int number, String quantity, String f2RegId, List<String> marks) {
            this.number = number;
            this.quantity = quantity;
            this.f2RegId = f2RegId;
            this.marks = marks;
        }
    }
}
