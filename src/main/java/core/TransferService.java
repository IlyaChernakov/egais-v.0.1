package core;

import java.io.File;
import java.util.function.Consumer;

import helpers.Constants;
import transfer.TransferParser;
import transfer.TransferPosition;

/**
 * Формирование документа переноса остатков в магазин ({@code TransferToShop})
 * на основании двух выгрузок ЕГАИС: остатков магазина ({@code ReplyRestsShop_v2.xml})
 * и остатков склада ({@code ReplyRests_v2.xml}).
 * <p>
 * Класс не зависит от интерфейса: тем же способом пользуются и графическая
 * программа, и консольный запуск.
 */
public class TransferService {

    /** Сообщения о ходе работы; по умолчанию ничего не делается. */
    private Consumer<String> logger = message -> {
    };

    /**
     * Задаёт приёмник сообщений о ходе работы (например, журнал в окне программы).
     *
     * @param logger приёмник сообщений
     * @return этот же сервис — для цепочки вызовов
     */
    public TransferService withLogger(Consumer<String> logger) {
        this.logger = logger != null ? logger : message -> {
        };
        return this;
    }

    /**
     * Формирует документ переноса остатков в магазин.
     *
     * @param shopRestsFile  выгрузка остатков магазина ({@code ReplyRestsShop_v2.xml})
     * @param stockRestsFile выгрузка остатков склада ({@code ReplyRests_v2.xml})
     * @param outputDir      папка для сохранения результата
     * @param header         данные шапки документа: FSRAR_ID, Identity, номер и дата переноса
     * @return итог работы: количество позиций и записанный файл
     * @throws GenerationException если данные некорректны или файл не удалось записать
     */
    public TransferResult generate(File shopRestsFile, File stockRestsFile, File outputDir, TransferHeader header)
            throws GenerationException {

        validate(shopRestsFile, stockRestsFile, outputDir, header);

        logger.accept("Чтение файла остатков магазина: " + shopRestsFile.getAbsolutePath());
        logger.accept("Чтение файла остатков склада: " + stockRestsFile.getAbsolutePath());

        TransferParser.ParseResult parsed = new TransferParser().parse(shopRestsFile, stockRestsFile);
        logger.accept(String.format("Отрицательный остаток найден по %d кодам продукции,"
                        + " подходящие партии со склада — по %d.",
                parsed.getNegativeCodes(), parsed.getMatchedCodes()));
        logger.accept(String.format("Документ переноса №%s, Identity %s, дата %s, FSRAR_ID %s.",
                header.getTransferNumber(), header.getIdentity(), header.getTransferDateUserText(),
                header.getFsrarId()));

        File outputFile = new File(outputDir, Constants.OUTPUT_FILE_TRANSFER);
        TransferXmlWriter writer = new TransferXmlWriter(header);
        for (TransferPosition position : parsed.getPositions()) {
            writer.position(position.getNumber(), position.getProductCode(),
                    position.getQuantity(), position.getF2RegId());
        }
        writer.writeTo(outputFile);

        logger.accept(String.format("Позиций в документе переноса — %d.", parsed.getPositions().size()));
        logger.accept("  записан файл: " + outputFile.getAbsolutePath());
        if (parsed.getPositions().isEmpty()) {
            logger.accept("  внимание: подходящих позиций не найдено, документ переноса пустой.");
        }

        return new TransferResult(parsed.getPositions().size(), parsed.getNegativeCodes(),
                parsed.getMatchedCodes(), outputFile);
    }

    private void validate(File shopRestsFile, File stockRestsFile, File outputDir, TransferHeader header)
            throws GenerationException {
        if (shopRestsFile == null) {
            throw new GenerationException("Не выбран файл остатков магазина.");
        }
        if (!shopRestsFile.isFile()) {
            throw new GenerationException("Файл остатков магазина не найден: " + shopRestsFile.getAbsolutePath());
        }
        if (stockRestsFile == null) {
            throw new GenerationException("Не выбран файл остатков склада.");
        }
        if (!stockRestsFile.isFile()) {
            throw new GenerationException("Файл остатков склада не найден: " + stockRestsFile.getAbsolutePath());
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
        if (header == null) {
            throw new GenerationException("Не заполнены данные документа переноса.");
        }
        header.validate();
    }
}
