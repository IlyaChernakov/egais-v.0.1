package alco.beer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import core.GenerationException;
import helpers.XmlSupport;

import static helpers.Constants.ALLOWED_PRODUCT_CODES;
import static helpers.Constants.PREF_CODE;
import static helpers.Constants.RST_F2;
import static helpers.Constants.RST_POSITION;
import static helpers.Constants.RST_QUANTITY;

/**
 * Разбор выгрузки остатков ЕГАИС: позиции пива и пивных напитков.
 * <p>
 * К пиву относятся позиции, код вида продукции которых указан
 * в {@link helpers.Constants#ALLOWED_PRODUCT_CODES}.
 */
public class XmlParserBeer {

    /**
     * Читает файл остатков и возвращает позиции пива.
     *
     * @param xmlFile файл выгрузки остатков
     * @return список позиций (возможно пустой)
     * @throws GenerationException если файл не читается или в позиции нет обязательных данных
     */
    public List<ActPositionBeer> parseXmlFileBeer(File xmlFile) throws GenerationException {
        return parseDocument(XmlSupport.parse(xmlFile));
    }

    /**
     * Извлекает позиции пива из уже разобранного документа.
     *
     * @param document разобранный XML выгрузки остатков
     * @return список позиций (возможно пустой)
     * @throws GenerationException если в позиции нет обязательных данных
     */
    public List<ActPositionBeer> parseDocument(Document document) throws GenerationException {
        List<ActPositionBeer> actPositionsBeer = new ArrayList<>();
        NodeList nodeList = document.getElementsByTagName(RST_POSITION);

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) node;
            int number = i + 1;

            String productCode = XmlSupport.requiredText(element, PREF_CODE, number);
            if (!ALLOWED_PRODUCT_CODES.contains(productCode)) {
                // Это крепкий алкоголь — позиция обрабатывается парсером крепкого алкоголя.
                continue;
            }

            String quantity = XmlSupport.requiredText(element, RST_QUANTITY, number);
            String fb = XmlSupport.requiredText(element, RST_F2, number);
            actPositionsBeer.add(new ActPositionBeer(number, quantity, fb));
        }
        return actPositionsBeer;
    }
}
