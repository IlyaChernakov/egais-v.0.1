package alco.strong;

import static helpers.Constants.MID_MARK;

/**
 * Формирование марок для акта списания крепкого алкоголя.
 */
public class MarkGenerate {

    /**
     * Строит базовую марку позиции.
     *
     * @param code10 код продукции ЕГАИС (10 знаков, только цифры)
     * @param number номер позиции в документе
     * @return строка марки
     * @throws NumberFormatException если код продукции не является числом
     */
    public String markGenerate(String code10, int number) {
        String code36 = String.format("%15s", Long.toString(Long.parseLong(code10.trim()), 36).toUpperCase());
        String markNum = String.format("%4s", number).replace(' ', '0');
        return ("20N0" + code36 + MID_MARK + markNum).replace(' ', '0');
    }

    /**
     * Дописывает к марке позиции порядковый номер бутылки.
     *
     * @param actPositionsStrong позиция акта
     * @param number             порядковый номер бутылки внутри позиции
     * @return марка конкретной бутылки
     */
    public String numberingMark(ActPositionStrong actPositionsStrong, int number) {
        String markPos = actPositionsStrong.getCode();
        String markNum = String.format("%3s", number).replace(' ', '0');
        return (markPos + markNum);
    }
}
