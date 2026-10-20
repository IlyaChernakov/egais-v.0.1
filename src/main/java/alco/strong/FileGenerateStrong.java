package alco.strong;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import core.ActHeader;
import core.ActXmlWriter;
import core.GenerationException;

/**
 * Формирование XML акта списания по крепкому алкоголю.
 */
public class FileGenerateStrong {

    private final MarkGenerate markGenerate = new MarkGenerate();

    /**
     * Записывает акт списания в указанный файл.
     *
     * @param actPositionStrong позиции акта
     * @param header            данные шапки акта (номер, дата, причина, примечание, FSRAR_ID)
     * @param outputFile        файл результата (родительские каталоги создаются автоматически)
     * @return количество сформированных марок
     * @throws GenerationException если файл не удалось записать или количество некорректно
     */
    public int generateXmlFile(List<ActPositionStrong> actPositionStrong, ActHeader header, File outputFile)
            throws GenerationException {
        int totalMarks = 0;
        ActXmlWriter writer = new ActXmlWriter(header);

        for (ActPositionStrong actPositionsStrong : actPositionStrong) {
            int marks;
            try {
                marks = (int) Double.parseDouble(actPositionsStrong.getQuantity().trim());
            } catch (NumberFormatException e) {
                throw new GenerationException("Позиция №" + actPositionsStrong.getNumber()
                        + ": количество «" + actPositionsStrong.getQuantity() + "» не является числом.", e);
            }

            List<String> marksList = new ArrayList<>(marks);
            for (int i = 0; i < marks; i++) {
                marksList.add(markGenerate.numberingMark(actPositionsStrong, i + 1));
            }

            writer.position(actPositionsStrong.getNumber(), actPositionsStrong.getQuantity(),
                    actPositionsStrong.getFb(), marksList);
            totalMarks += marks;
        }

        writer.writeTo(outputFile);
        return totalMarks;
    }
}
