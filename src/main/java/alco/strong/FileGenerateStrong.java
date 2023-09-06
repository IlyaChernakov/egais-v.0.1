package alco.strong;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.File;
import java.util.List;

import static helpers.Constants.*;

public class FileGenerateStrong {

    public void generateXmlFile(List<ActPositionStrong> actPositionStrong) {

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            Element rootElement = doc.createElement(TAG_CONTENT);
            doc.appendChild(rootElement);

            for (ActPositionStrong actPositionsStrong : actPositionStrong) {

                Element position = doc.createElement(TAG_POSITION);
                rootElement.appendChild(position);

                Element identity = doc.createElement(TAG_IDENTITY);
                identity.appendChild(doc.createTextNode(String.valueOf(actPositionsStrong.getNumber())));
                position.appendChild(identity);

                Element quantity = doc.createElement(TAG_QUANTITY);
                quantity.appendChild(doc.createTextNode(actPositionsStrong.getQuantity()));
                position.appendChild(quantity);

                Element informF1F2 = doc.createElement(TAG_INFORM_F1F2);
                position.appendChild(informF1F2);

                Element markCodeInfo = doc.createElement(TAG_MARK_CODE_INFO);
                position.appendChild(markCodeInfo);

                Element informF2 = doc.createElement(TAG_INFORM_F2);
                informF1F2.appendChild(informF2);

                Element f2RegId = doc.createElement(TAG_F2_REG_ID);
                f2RegId.appendChild(doc.createTextNode(actPositionsStrong.getFb()));
                informF2.appendChild(f2RegId);

                for (int i = 0; i < Double.parseDouble(actPositionsStrong.getQuantity()); i++) {
                    Element amc = doc.createElement(TAG_AMC);
                    int number = i + 1;
                    String markPos = new MarkGenerate().numberingMark(actPositionsStrong, number);

                    amc.appendChild(doc.createTextNode(markPos));
                    markCodeInfo.appendChild(amc);

                    Attr attr = doc.createAttribute("xmlns:ce");
                    attr.setValue("http://fsrar.ru/WEGAIS/CommonV3");
                    amc.setAttributeNode(attr);
                }
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);

            StreamResult result = new StreamResult(new File(FILE_PATH_OUT_STRONG));
            transformer.transform(source, result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}