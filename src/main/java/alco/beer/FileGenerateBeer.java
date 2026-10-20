package alco.beer;

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import core.GenerationException;
import helpers.XmlSupport;

import static helpers.Constants.TAG_CONTENT;
import static helpers.Constants.TAG_F2_REG_ID;
import static helpers.Constants.TAG_IDENTITY;
import static helpers.Constants.TAG_INFORM_F1F2;
import static helpers.Constants.TAG_INFORM_F2;
import static helpers.Constants.TAG_POSITION;
import static helpers.Constants.TAG_QUANTITY;

/**
 * Формирование XML акта списания по пиву и пивным напиткам.
 */
public class FileGenerateBeer {

    /**
     * Записывает акт списания в указанный файл.
     *
     * @param actPositionBeer позиции акта
     * @param outputFile      файл результата (родительские каталоги создаются автоматически)
     * @throws GenerationException если файл не удалось записать
     */
    public void generateXmlFileBeer(List<ActPositionBeer> actPositionBeer, File outputFile) throws GenerationException {
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

            XmlSupport.write(doc, outputFile);
        } catch (GenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new GenerationException("Не удалось сформировать акт по пиву: " + e.getMessage(), e);
        }
    }
}
