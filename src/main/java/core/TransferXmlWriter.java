package core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import helpers.Constants;

/**
 * Записывает документ переноса остатков в магазин {@code ns:TransferToShop}
 * в стандартном оформлении XML (см. {@link XmlOutput}).
 * <p>
 * Документ собирается как дерево элементов, поэтому экранирование значений,
 * отступы, объявления пространств имён и вид пустых элементов обеспечивает
 * стандартный сериализатор.
 */
public final class TransferXmlWriter {

    private final TransferHeader header;
    private final List<Position> positions = new ArrayList<>();

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
        positions.add(new Position(number, productCode, quantity, f2RegId));
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
        XmlOutput.declareNamespace(root, "xmlns:ns", Constants.NAMESPACE_DOCUMENTS);
        XmlOutput.declareNamespace(root, "xmlns:xs", Constants.NAMESPACE_XS);
        XmlOutput.declareNamespace(root, "xmlns:c", Constants.NAMESPACE_COMMON);
        XmlOutput.declareNamespace(root, "xmlns:pref", Constants.NAMESPACE_PREF);
        XmlOutput.declareNamespace(root, "xmlns:tts", Constants.NAMESPACE_TTS);

        Element owner = append(root, Constants.NAMESPACE_DOCUMENTS, Constants.TAG_OWNER);
        owner.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_DOCUMENTS,
                Constants.TAG_FSRAR_ID, header.getFsrarId()));

        Element envelope = append(root, Constants.NAMESPACE_DOCUMENTS, Constants.TAG_DOCUMENT);
        // Как и в других документах ЕГАИС, корневой элемент переноса записан
        // с префиксом конверта, а его содержимое — в собственном пространстве имён.
        Element transfer = append(envelope, Constants.NAMESPACE_DOCUMENTS, Constants.TAG_TRANSFER_TO_SHOP);
        transfer.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_TTS,
                Constants.TTS_IDENTITY, header.getIdentity()));

        Element transferHeader = append(transfer, Constants.NAMESPACE_TTS, Constants.TTS_HEADER);
        transferHeader.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_TTS,
                Constants.TTS_TRANSFER_NUMBER, header.getTransferNumber()));
        transferHeader.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_TTS,
                Constants.TTS_TRANSFER_DATE, header.getTransferDateText()));

        Element content = append(transfer, Constants.NAMESPACE_TTS, Constants.TTS_CONTENT);
        for (Position position : positions) {
            appendPosition(document, content, position);
        }
        return document;
    }

    private void appendPosition(Document document, Element content, Position position) {
        Element element = append(content, Constants.NAMESPACE_TTS, Constants.TTS_POSITION);
        element.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_TTS,
                Constants.TTS_IDENTITY, String.valueOf(position.number)));
        element.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_TTS,
                Constants.TTS_PRODUCT_CODE, position.productCode));
        element.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_TTS,
                Constants.TTS_QUANTITY, position.quantity));

        Element informF2 = append(element, Constants.NAMESPACE_TTS, Constants.TTS_INFORM_F2);
        informF2.appendChild(XmlOutput.textElement(document, Constants.NAMESPACE_PREF,
                Constants.TAG_F2_REG_ID, position.f2RegId));
    }

    private static Element append(Element parent, String namespace, String name) {
        Element element = XmlOutput.element(parent.getOwnerDocument(), namespace, name);
        parent.appendChild(element);
        return element;
    }

    /** Позиция документа переноса в том виде, в каком её передал вызывающий код. */
    private static final class Position {

        private final int number;
        private final String productCode;
        private final String quantity;
        private final String f2RegId;

        private Position(int number, String productCode, String quantity, String f2RegId) {
            this.number = number;
            this.productCode = productCode;
            this.quantity = quantity;
            this.f2RegId = f2RegId;
        }
    }
}
