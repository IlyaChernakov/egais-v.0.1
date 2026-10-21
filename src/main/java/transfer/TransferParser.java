package transfer;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import core.GenerationException;
import helpers.Constants;
import helpers.XmlSupport;

/**
 * Разбор двух выгрузок ЕГАИС для документа переноса остатков в магазин.
 * <p>
 * Порядок работы перенесён из программы {@code egais-negative}:
 * <ol>
 *   <li>в выгрузке остатков магазина ({@code ReplyRestsShop_v2.xml}) находятся
 *       позиции с отрицательным остатком ({@code rst:ShopPosition});</li>
 *   <li>для каждой такой позиции запоминается код продукции ({@code pref:AlcCode})
 *       и количество без знака «минус»;</li>
 *   <li>в выгрузке остатков склада ({@code ReplyRests_v2.xml}) выбираются позиции
 *       {@code rst:StockPosition} с тем же кодом и тем же количеством — именно они
 *       попадают в документ переноса вместе со своим номером позиции и номером
 *       справки 2 ({@code rst:InformF2RegId}).</li>
 * </ol>
 * Если по одному коду в магазине несколько отрицательных позиций, учитывается
 * каждая: на складе переносятся все партии с подходящим количеством. Количество
 * сравнивается точно так же, как в исходной программе, — по тексту из выгрузки.
 */
public final class TransferParser {

    /** Итог разбора: позиции документа и числа для журнала работы. */
    public static final class ParseResult {

        private final List<TransferPosition> positions;
        private final int negativeCodes;
        private final int matchedCodes;

        ParseResult(List<TransferPosition> positions, int negativeCodes, int matchedCodes) {
            this.positions = positions;
            this.negativeCodes = negativeCodes;
            this.matchedCodes = matchedCodes;
        }

        /** Позиции документа переноса в порядке выгрузки остатков склада. */
        public List<TransferPosition> getPositions() {
            return positions;
        }

        /** Сколько кодов продукции имеют отрицательный остаток в магазине. */
        public int getNegativeCodes() {
            return negativeCodes;
        }

        /** Сколько из этих кодов нашли подходящую партию на складе. */
        public int getMatchedCodes() {
            return matchedCodes;
        }
    }

    /**
     * Разбирает выгрузку остатков магазина и выгрузку остатков склада.
     *
     * @param shopRestsFile  выгрузка остатков магазина ({@code ReplyRestsShop_v2.xml})
     * @param stockRestsFile выгрузка остатков склада ({@code ReplyRests_v2.xml})
     * @return позиции документа переноса и числа для журнала работы
     * @throws GenerationException если файл недоступен, содержит некорректный XML
     *                             или в позиции нет обязательного элемента
     */
    public ParseResult parse(File shopRestsFile, File stockRestsFile) throws GenerationException {
        Map<String, List<String>> negativeQuantities = negativeShopQuantities(shopRestsFile);
        return stockPositions(negativeQuantities, stockRestsFile);
    }

    /**
     * Собирает отрицательные остатки магазина.
     *
     * @param shopRestsFile выгрузка остатков магазина
     * @return код продукции → количества без знака «минус», в порядке выгрузки
     * @throws GenerationException если файл недоступен или содержит некорректный XML
     */
    public Map<String, List<String>> negativeShopQuantities(File shopRestsFile) throws GenerationException {
        Document document = XmlSupport.parse(shopRestsFile);
        Map<String, List<String>> result = new LinkedHashMap<>();
        NodeList nodes = document.getElementsByTagName(Constants.TAG_SHOP_POSITION);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) node;
            String quantity = XmlSupport.requiredText(element, Constants.RST_QUANTITY, i + 1);
            if (!isNegative(quantity)) {
                continue;
            }
            String code = XmlSupport.requiredText(element, Constants.PREF_ALC_CODE, i + 1);
            String absolute = quantity.replace("-", "");
            List<String> quantities = result.computeIfAbsent(code, key -> new ArrayList<>());
            if (!quantities.contains(absolute)) {
                quantities.add(absolute);
            }
        }
        return result;
    }

    /**
     * Отбирает позиции склада, которые нужно перенести в магазин.
     *
     * @param negativeQuantities отрицательные остатки магазина
     * @param stockRestsFile     выгрузка остатков склада
     * @return позиции документа переноса
     * @throws GenerationException если файл недоступен или содержит некорректный XML
     */
    private ParseResult stockPositions(Map<String, List<String>> negativeQuantities, File stockRestsFile)
            throws GenerationException {
        Document document = XmlSupport.parse(stockRestsFile);
        List<TransferPosition> positions = new ArrayList<>();
        List<String> matchedCodes = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName(Constants.RST_POSITION);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) node;
            String code = XmlSupport.requiredText(element, Constants.PREF_ALC_CODE, i + 1);
            List<String> quantities = negativeQuantities.get(code);
            if (quantities == null) {
                continue;
            }
            String quantity = XmlSupport.requiredText(element, Constants.RST_QUANTITY, i + 1);
            if (!quantities.contains(quantity)) {
                continue;
            }
            String f2RegId = XmlSupport.requiredText(element, Constants.RST_F2, i + 1);
            positions.add(new TransferPosition(i + 1, code, quantity, f2RegId));
            if (!matchedCodes.contains(code)) {
                matchedCodes.add(code);
            }
        }
        return new ParseResult(positions, negativeQuantities.size(), matchedCodes.size());
    }

    /**
     * Проверяет, что остаток отрицательный.
     * <p>
     * Количество в выгрузке записано с дробной частью ({@code -72.0000}), поэтому
     * знак определяется как число, а не как текст.
     */
    private static boolean isNegative(String quantity) {
        try {
            return new BigDecimal(quantity.trim()).signum() < 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
