package alco.strong;

import java.io.File;
import java.util.List;

import org.w3c.dom.Element;

import core.ActDocumentBuilder;
import core.ActHeader;
import core.GenerationException;
import helpers.XmlSupport;

import static helpers.Constants.TAG_AMC;
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
     * @param header            данные шапки акта (номер, дата, причина, примечание, FSRAR_ID)
     * @param outputFile        файл результата (родительские каталоги создаются автоматически)
     * @return количество сформированных марок
     * @throws GenerationException если файл не удалось записать или количество некорректно
     */
    public int generateXmlFile(List<ActPositionStrong> actPositionStrong, ActHeader header, File outputFile)
            throws GenerationException {
        int totalMarks = 0;
        try {
            ActDocumentBuilder builder = new ActDocumentBuilder(header);
            Element content = builder.content();

            for (ActPositionStrong actPositionsStrong : actPositionStrong) {

                Element position = builder.appendChild(content, TAG_POSITION);

                builder.appendText(position, TAG_IDENTITY, String.valueOf(actPositionsStrong.getNumber()));
                builder.appendText(position, TAG_QUANTITY, actPositionsStrong.getQuantity());

                Element informF1F2 = builder.appendChild(position, TAG_INFORM_F1F2);
                Element markCodeInfo = builder.appendChild(position, TAG_MARK_CODE_INFO);

                Element informF2 = builder.appendChild(informF1F2, TAG_INFORM_F2);
                builder.appendText(informF2, TAG_F2_REG_ID, actPositionsStrong.getFb());

                int marks;
                try {
                    marks = (int) Double.parseDouble(actPositionsStrong.getQuantity().trim());
                } catch (NumberFormatException e) {
                    throw new GenerationException("Позиция №" + actPositionsStrong.getNumber()
                            + ": количество «" + actPositionsStrong.getQuantity() + "» не является числом.", e);
                }

                for (int i = 0; i < marks; i++) {
                    String markPos = markGenerate.numberingMark(actPositionsStrong, i + 1);
                    // Пространство имён ce объявлено один раз в корне документа
                    // (ActDocumentBuilder), поэтому на каждом ce:amc оно не повторяется.
                    builder.appendText(markCodeInfo, TAG_AMC, markPos);
                }
                totalMarks += marks;
            }

            XmlSupport.write(builder.document(), outputFile);
            return totalMarks;
        } catch (GenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new GenerationException("Не удалось сформировать акт по крепкому алкоголю: " + e.getMessage(), e);
        }
    }
}
