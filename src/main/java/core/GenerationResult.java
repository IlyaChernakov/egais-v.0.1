package core;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Итог формирования актов списания.
 */
public class GenerationResult {

    private final int strongPositions;
    private final int strongMarks;
    private final int beerPositions;
    private final List<File> writtenFiles;

    public GenerationResult(int strongPositions, int strongMarks, int beerPositions, List<File> writtenFiles) {
        this.strongPositions = strongPositions;
        this.strongMarks = strongMarks;
        this.beerPositions = beerPositions;
        this.writtenFiles = Collections.unmodifiableList(writtenFiles);
    }

    /** Количество позиций крепкого алкоголя. */
    public int getStrongPositions() {
        return strongPositions;
    }

    /** Количество сформированных марок крепкого алкоголя. */
    public int getStrongMarks() {
        return strongMarks;
    }

    /** Количество позиций пива. */
    public int getBeerPositions() {
        return beerPositions;
    }

    /** Файлы, записанные в результате работы. */
    public List<File> getWrittenFiles() {
        return writtenFiles;
    }

    /** Общее количество обработанных позиций. */
    public int getTotalPositions() {
        return strongPositions + beerPositions;
    }
}
