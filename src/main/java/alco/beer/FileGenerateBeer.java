package alco.beer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.List;

import static helpers.Constants.*;

public class FileGenerateBeer {

    public void generateXmlFileBeer(List<ActPositionBeer> actPositionBeer) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            Element rootElement = doc.createElement(TAG_CONTENT);
            doc.appendChild(rootElement);

            for (ActPositionBeer actPositionsBeer : actPositionBeer) {

                Element position = doc.createElement(TAG_POSITION);
                rootElement.appendChild(position);

                Element identity = doc.createElement(TAG_IDENTITY);
                identity.appendChild(doc.createTextNode(String.valueOf(actPositionsBeer.getNumber())));
                position.appendChild(identity);

                Element quantity = doc.createElement(TAG_QUANTITY);
                quantity.appendChild(doc.createTextNode(actPositionsBeer.getQuantity()));
                position.appendChild(quantity);

                Element informF1F2 = doc.createElement(TAG_INFORM_F1F2);
                position.appendChild(informF1F2);

                Element informF2 = doc.createElement(TAG_INFORM_F2);
                informF1F2.appendChild(informF2);

                Element f2RegId = doc.createElement(TAG_F2_REG_ID);
                f2RegId.appendChild(doc.createTextNode(actPositionsBeer.getFb()));
                informF2.appendChild(f2RegId);
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StreamResult result = new StreamResult(new File(FILE_PATH_OUT_BEER));
            transformer.transform(source, result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
