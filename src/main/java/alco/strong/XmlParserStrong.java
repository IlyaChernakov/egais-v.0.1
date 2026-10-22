package alco.strong;

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
import static helpers.Constants.PREF_ALC_CODE;
import static helpers.Constants.PREF_CODE;
import static helpers.Constants.RST_F2;
import static helpers.Constants.RST_POSITION;
import static helpers.Constants.RST_QUANTITY;

/**
 * Разбор выгрузки остатков ЕГАИС: позиции крепкого алкоголя.
 * <p>
 * К крепкому алкоголю относятся все позиции, код вида продукции которых
 * отсутствует в {@link helpers.Constants#ALLOWED_PRODUCT_CODES}.
 */
public class XmlParserStrong {

    private final MarkGenerate markGenerate = new MarkGenerate();

    /**
     * Читает файл остатков и возвращает позиции крепкого алкоголя.
     *
     * @param xmlFile файл выгрузки остатков
     * @return список позиций (возможно пустой)
     * @throws GenerationException если файл не читается или в позиции нет обязательных данных
     */
    public List<ActPositionStrong> parseXmlFile(File xmlFile) throws GenerationException {
        return parseDocument(XmlSupport.parse(xmlFile));
    }

    /**
     * Извлекает позиции крепкого алкоголя из уже разобранного документа.
     *
     * @param document разобранный XML выгрузки остатков
     * @return список позиций (возможно пустой)
     * @throws GenerationException если в позиции нет обязательных данных
     */
    public List<ActPositionStrong> parseDocument(Document document) throws GenerationException {
        List<ActPositionStrong> actPositionsStrong = new ArrayList<>();
        NodeList nodeList = document.getElementsByTagName(RST_POSITION);

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) node;
            int number = i + 1;

            String productCode = XmlSupport.requiredText(element, PREF_CODE, number);
            if (ALLOWED_PRODUCT_CODES.contains(productCode)) {
                // Это пиво — позиция обрабатывается парсером пива.
                continue;
            }

            String quantity = XmlSupport.requiredText(element, RST_QUANTITY, number);
            String fb = XmlSupport.requiredText(element, RST_F2, number);
            String code10 = XmlSupport.requiredText(element, PREF_ALC_CODE, number);

            String mark;
            try {
                mark = markGenerate.markGenerate(code10, number);
            } catch (NumberFormatException e) {
                throw new GenerationException("Позиция №" + number + ": код продукции «" + code10
                        + "» не является числом, марка не может быть построена.", e);
            }

            actPositionsStrong.add(new ActPositionStrong(number, quantity, fb, mark));
        }
        return actPositionsStrong;
    }
}
