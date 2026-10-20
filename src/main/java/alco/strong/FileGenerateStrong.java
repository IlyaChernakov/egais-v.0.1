package alco.strong;

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import core.GenerationException;
import helpers.XmlSupport;

import static helpers.Constants.NAMESPACE_CE;
import static helpers.Constants.TAG_AMC;
import static helpers.Constants.TAG_CONTENT;
import static helpers.Constants.TAG_F2_REG_ID;
import static helpers.Constants.TAG_IDENTITY;
import static helpers.Constants.TAG_INFORM_F1F2;
import static helpers.Constants.TAG_INFORM_F2;
import static helpers.Constants.TAG_MARK_CODE_INFO;
import static helpers.Constants.TAG_POSITION;
import static helpers.Constants.TAG_QUANTITY;

/**
 * Формирование XML акта списания по крепкому алкоголю.
 */
public class FileGenerateStrong {

    private final MarkGenerate markGenerate = new MarkGenerate();

    /**
     * Записывает акт списания в указанный файл.
     *
     * @param actPositionStrong позиции акта
     * @param outputFile        файл результата (родительские каталоги создаются автоматически)
     * @return количество сформированных марок
     * @throws GenerationException если файл не удалось записать или количество некорректно
     */
    public int generateXmlFile(List<ActPositionStrong> actPositionStrong, File outputFile) throws GenerationException {
        int totalMarks = 0;
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

                int marks;
                try {
                    marks = (int) Double.parseDouble(actPositionsStrong.getQuantity().trim());
                } catch (NumberFormatException e) {
                    throw new GenerationException("Позиция №" + actPositionsStrong.getNumber()
                            + ": количество «" + actPositionsStrong.getQuantity() + "» не является числом.", e);
                }

                for (int i = 0; i < marks; i++) {
                    Element amc = doc.createElement(TAG_AMC);
                    String markPos = markGenerate.numberingMark(actPositionsStrong, i + 1);
                    amc.appendChild(doc.createTextNode(markPos));
                    markCodeInfo.appendChild(amc);

                    Attr attr = doc.createAttribute("xmlns:ce");
                    attr.setValue(NAMESPACE_CE);
                    amc.setAttributeNode(attr);
                }
                totalMarks += marks;
            }

            XmlSupport.write(doc, outputFile);
            return totalMarks;
        } catch (GenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new GenerationException("Не удалось сформировать акт по крепкому алкоголю: " + e.getMessage(), e);
        }
    }
}
