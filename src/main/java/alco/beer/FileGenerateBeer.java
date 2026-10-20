package alco.beer;

import java.io.File;
import java.util.List;

import org.w3c.dom.Element;

import core.ActDocumentBuilder;
import core.ActHeader;
import core.GenerationException;
import helpers.XmlSupport;

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
     * @param header          данные шапки акта (номер, дата, причина, примечание, FSRAR_ID)
     * @param outputFile      файл результата (родительские каталоги создаются автоматически)
     * @throws GenerationException если файл не удалось записать
     */
    public void generateXmlFileBeer(List<ActPositionBeer> actPositionBeer, ActHeader header, File outputFile)
            throws GenerationException {
        try {
            ActDocumentBuilder builder = new ActDocumentBuilder(header);
            Element content = builder.content();

            for (ActPositionBeer actPositionsBeer : actPositionBeer) {

                Element position = builder.appendChild(content, TAG_POSITION);

                builder.appendText(position, TAG_IDENTITY, String.valueOf(actPositionsBeer.getNumber()));
                builder.appendText(position, TAG_QUANTITY, actPositionsBeer.getQuantity());

                Element informF1F2 = builder.appendChild(position, TAG_INFORM_F1F2);
                Element informF2 = builder.appendChild(informF1F2, TAG_INFORM_F2);
                builder.appendText(informF2, TAG_F2_REG_ID, actPositionsBeer.getFb());
            }

            XmlSupport.write(builder.document(), outputFile);
        } catch (GenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new GenerationException("Не удалось сформировать акт по пиву: " + e.getMessage(), e);
        }
    }
}
