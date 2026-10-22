package core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.w3c.dom.Document;

import alco.beer.ActPositionBeer;
import alco.beer.FileGenerateBeer;
import alco.beer.XmlParserBeer;
import alco.strong.ActPositionStrong;
import alco.strong.FileGenerateStrong;
import alco.strong.XmlParserStrong;
import helpers.XmlSupport;

/**
 * Формирование актов списания на основании выгрузки текущих остатков ЕГАИС.
 * <p>
 * Класс не зависит от интерфейса: тем же способом пользуются и графическая
 * программа, и консольный запуск.
 */
public class WriteOffService {

    /** Сообщения о ходе работы; по умолчанию ничего не делается. */
    private Consumer<String> logger = message -> {
    };

    /**
     * Задаёт приёмник сообщений о ходе работы (например, журнал в окне программы).
     *
     * @param logger приёмник сообщений
     * @return этот же сервис — для цепочки вызовов
     */
    public WriteOffService withLogger(Consumer<String> logger) {
        this.logger = logger != null ? logger : message -> {
        };
        return this;
    }

    /**
     * Формирует акты списания. Номера актов по крепкому алкоголю и по пиву задаются
     * отдельно, поэтому шапка передаётся для каждого акта своя.
     *
     * @param inputFile    файл выгрузки остатков (ReplyRests_v2.xml)
     * @param outputDir    папка для сохранения результата
     * @param strongHeader шапка акта по крепкому алкоголю; {@code null} — акт не формируется
     * @param beerHeader   шапка акта по пиву и пивным напиткам; {@code null} — акт не формируется
     * @return итог работы: количество позиций и список записанных файлов
     * @throws GenerationException если данные некорректны или файлы не удалось записать
     */
    public GenerationResult generate(File inputFile, File outputDir, ActHeader strongHeader, ActHeader beerHeader)
            throws GenerationException {

        validate(inputFile, outputDir, strongHeader, beerHeader);

        logger.accept("Чтение файла остатков: " + inputFile.getAbsolutePath());
        Document document = XmlSupport.parse(inputFile);

        List<File> writtenFiles = new ArrayList<>();
        int strongPositions = 0;
        int strongMarks = 0;
        int beerPositions = 0;

        if (strongHeader != null) {
            logHeader("Крепкий алкоголь", strongHeader);
            List<ActPositionStrong> positions = new XmlParserStrong().parseDocument(document);
            File outputFile = new File(outputDir,
                    OutputNames.writeOffStrong(strongHeader.getFsrarId(), strongHeader.getActDate()));
            strongMarks = new FileGenerateStrong().generateXmlFile(positions, strongHeader, outputFile);
            strongPositions = positions.size();
            writtenFiles.add(outputFile);
            logger.accept(String.format("Позиций — %d, марок — %d.", strongPositions, strongMarks));
            logger.accept("  записан файл: " + outputFile.getAbsolutePath());
            if (strongPositions == 0) {
                logger.accept("  внимание: подходящих позиций не найдено, акт пустой.");
            }
        }

        if (beerHeader != null) {
            logHeader("Пиво и пивные напитки", beerHeader);
            List<ActPositionBeer> positions = new XmlParserBeer().parseDocument(document);
            File outputFile = new File(outputDir,
                    OutputNames.writeOffBeer(beerHeader.getFsrarId(), beerHeader.getActDate()));
            new FileGenerateBeer().generateXmlFileBeer(positions, beerHeader, outputFile);
            beerPositions = positions.size();
            writtenFiles.add(outputFile);
            logger.accept(String.format("Позиций — %d.", beerPositions));
            logger.accept("  записан файл: " + outputFile.getAbsolutePath());
            if (beerPositions == 0) {
                logger.accept("  внимание: подходящих позиций не найдено, акт пустой.");
            }
        }

        return new GenerationResult(strongPositions, strongMarks, beerPositions, writtenFiles);
    }

    private void logHeader(String kind, ActHeader header) {
        logger.accept(String.format("%s: акт №%s от %s, FSRAR_ID %s, причина: %s.", kind,
                header.getActNumber(), header.getActDateUserText(), header.getFsrarId(),
                header.getTypeWriteOff().isEmpty() ? "не указана" : header.getTypeWriteOff()));
    }

    private void validate(File inputFile, File outputDir, ActHeader strongHeader, ActHeader beerHeader)
            throws GenerationException {
        if (inputFile == null) {
            throw new GenerationException("Не выбран файл остатков.");
        }
        if (!inputFile.isFile()) {
            throw new GenerationException("Файл остатков не найден: " + inputFile.getAbsolutePath());
        }
        if (outputDir == null) {
            throw new GenerationException("Не выбрана папка для сохранения результата.");
        }
        if (!outputDir.isDirectory()) {
            if (outputDir.exists()) {
                throw new GenerationException("Путь для сохранения не является папкой: " + outputDir.getAbsolutePath());
            }
            if (!outputDir.mkdirs()) {
                throw new GenerationException("Не удалось создать папку: " + outputDir.getAbsolutePath());
            }
        }
        if (strongHeader == null && beerHeader == null) {
            throw new GenerationException("Не выбран ни один акт для формирования.");
        }
        if (strongHeader != null) {
            strongHeader.validate();
        }
        if (beerHeader != null) {
            beerHeader.validate();
        }
    }
}
