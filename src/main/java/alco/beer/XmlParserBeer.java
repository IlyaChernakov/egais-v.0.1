package alco.beer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static helpers.Constants.*;

public class XmlParserBeer {

    public List<ActPositionBeer> parseXmlFileBeer(String filePath) {

        List<ActPositionBeer> actPositionsBeer = new ArrayList<>();

        try {
            File xmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName(RST_POSITION);

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String productCode = element.getElementsByTagName(PREF_CODE).item(0).getTextContent();
                    if (ALLOWED_PRODUCT_CODES.contains(productCode)) {
                        int number = i + 1;
                        String quantity = element.getElementsByTagName(RST_QUANTITY).item(0).getTextContent();
                        String fb = element.getElementsByTagName(RST_F2).item(0).getTextContent();
                        ActPositionBeer actPositionBeer = new ActPositionBeer(number, quantity, fb);
                        actPositionsBeer.add(actPositionBeer);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return actPositionsBeer;
    }
}
