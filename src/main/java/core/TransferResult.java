package core;

import java.io.File;

/**
 * Итог формирования документа переноса остатков в магазин.
 */
public class TransferResult {

    private final int positions;
    private final int negativeCodes;
    private final int matchedCodes;
    private final File writtenFile;

    public TransferResult(int positions, int negativeCodes, int matchedCodes, File writtenFile) {
        this.positions = positions;
        this.negativeCodes = negativeCodes;
        this.matchedCodes = matchedCodes;
        this.writtenFile = writtenFile;
    }

    /** Количество позиций в документе переноса. */
    public int getPositions() {
        return positions;
    }

    /** Количество кодов продукции с отрицательным остатком в магазине. */
    public int getNegativeCodes() {
        return negativeCodes;
    }

    /** Количество таких кодов, для которых нашлась подходящая партия на складе. */
    public int getMatchedCodes() {
        return matchedCodes;
    }

    /** Записанный файл документа переноса. */
    public File getWrittenFile() {
        return writtenFile;
    }
}
