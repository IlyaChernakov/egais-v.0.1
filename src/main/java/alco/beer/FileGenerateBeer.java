package alco.beer;

import java.io.File;
import java.util.Collections;
import java.util.List;

import core.ActHeader;
import core.ActXmlWriter;
import core.GenerationException;

/**
 * Формирование XML акта списания по пиву и пивным напиткам.
 */
public class FileGenerateBeer {

    /**
     * Записывает акт списания в указанный файл.
     * <p>
     * У пива марки не указываются, поэтому блок {@code awr:MarkCodeInfo} в позициях
     * отсутствует, а при пустом списке позиций записывается {@code <awr:Content/>}.
     *
     * @param actPositionBeer позиции акта
     * @param header          данные шапки акта (номер, дата, причина, примечание, FSRAR_ID)
     * @param outputFile      файл результата (родительские каталоги создаются автоматически)
     * @throws GenerationException если файл не удалось записать
     */
    public void generateXmlFileBeer(List<ActPositionBeer> actPositionBeer, ActHeader header, File outputFile)
            throws GenerationException {
        ActXmlWriter writer = new ActXmlWriter(header);

        for (ActPositionBeer actPositionsBeer : actPositionBeer) {
            writer.position(actPositionsBeer.getNumber(), actPositionsBeer.getQuantity(),
                    actPositionsBeer.getFb(), Collections.emptyList());
        }

        writer.writeTo(outputFile);
    }
}
