package core;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Данные шапки документа переноса остатков в магазин ({@code tts:Header}).
 * <p>
 * Значения, которые вводит пользователь:
 * <ul>
 *   <li>{@code FSRAR_ID} организации — узел {@code ns:Owner/ns:FSRAR_ID};</li>
 *   <li>{@code Identity} документа — номер документа переноса;</li>
 *   <li>{@code TransferNumber} — номер переноса в шапке документа;</li>
 *   <li>{@code TransferDate} — дата переноса.</li>
 * </ul>
 * Как и в акте списания, дата вводится в привычном виде ДД-ММ-ГГГГ, а в документ
 * записывается в формате ЕГАИС ГГГГ-ММ-ДД.
 */
public final class TransferHeader {

    /** Формат даты, который видит пользователь: ДД-ММ-ГГГГ. */
    public static final DateTimeFormatter USER_DATE = ActHeader.USER_DATE;

    /** Формат даты внутри документа ЕГАИС: ГГГГ-ММ-ДД. */
    public static final DateTimeFormatter XML_DATE = ActHeader.XML_DATE;

    private final String fsrarId;
    private final String identity;
    private final String transferNumber;
    private final LocalDate transferDate;

    public TransferHeader(String fsrarId, String identity, String transferNumber, LocalDate transferDate) {
        this.fsrarId = trim(fsrarId);
        this.identity = trim(identity);
        this.transferNumber = trim(transferNumber);
        this.transferDate = transferDate;
    }

    /**
     * Собирает шапку из значений, введённых пользователем как текст.
     *
     * @param fsrarId          FSRAR_ID организации
     * @param identity         Identity документа переноса
     * @param transferNumber   номер переноса
     * @param transferDateText дата переноса в формате ДД-ММ-ГГГГ (принимается и ГГГГ-ММ-ДД)
     * @return готовая шапка документа переноса
     * @throws GenerationException если дату не удалось разобрать
     */
    public static TransferHeader of(String fsrarId, String identity, String transferNumber, String transferDateText)
            throws GenerationException {
        return new TransferHeader(fsrarId, identity, transferNumber, parseDate(transferDateText));
    }

    /** Шапка со значениями по умолчанию: сегодняшняя дата переноса. */
    public static TransferHeader defaults(String fsrarId, String identity, String transferNumber) {
        return new TransferHeader(fsrarId, identity, transferNumber, LocalDate.now());
    }

    /**
     * Разбирает дату переноса. Принимаются форматы ДД-ММ-ГГГГ и ГГГГ-ММ-ДД,
     * разделителем может быть дефис, точка или дробь.
     *
     * @param text введённый текст
     * @return разобранная дата
     * @throws GenerationException если текст не является датой
     */
    public static LocalDate parseDate(String text) throws GenerationException {
        String value = trim(text).replace('.', '-').replace('/', '-');
        if (value.isEmpty()) {
            throw new GenerationException("Укажите дату переноса в формате ДД-ММ-ГГГГ.");
        }
        try {
            return LocalDate.parse(value, USER_DATE);
        } catch (DateTimeParseException ignored) {
            // возможно, дата записана как в самом документе: ГГГГ-ММ-ДД
        }
        try {
            return LocalDate.parse(value, XML_DATE);
        } catch (DateTimeParseException ignored) {
            throw new GenerationException("Дата переноса «" + trim(text)
                    + "» непонятна. Укажите её в формате ДД-ММ-ГГГГ, например "
                    + LocalDate.now().format(USER_DATE) + ".");
        }
    }

    /**
     * Проверяет, что шапка заполнена достаточно для формирования документа.
     *
     * @throws GenerationException с понятным пользователю текстом
     */
    public void validate() throws GenerationException {
        if (fsrarId.isEmpty()) {
            throw new GenerationException("Укажите FSRAR_ID организации.");
        }
        if (!fsrarId.chars().allMatch(Character::isDigit)) {
            throw new GenerationException("FSRAR_ID организации должен состоять только из цифр, а указано «"
                    + fsrarId + "».");
        }
        if (identity.isEmpty()) {
            throw new GenerationException("Укажите Identity документа переноса.");
        }
        if (transferNumber.isEmpty()) {
            throw new GenerationException("Укажите номер переноса (TransferNumber).");
        }
        if (transferDate == null) {
            throw new GenerationException("Укажите дату переноса.");
        }
    }

    /** FSRAR_ID организации. */
    public String getFsrarId() {
        return fsrarId;
    }

    /** Identity документа переноса. */
    public String getIdentity() {
        return identity;
    }

    /** Номер переноса ({@code tts:TransferNumber}). */
    public String getTransferNumber() {
        return transferNumber;
    }

    /** Дата переноса. */
    public LocalDate getTransferDate() {
        return transferDate;
    }

    /** Дата переноса в формате документа ЕГАИС (ГГГГ-ММ-ДД). */
    public String getTransferDateText() {
        return transferDate == null ? "" : transferDate.format(XML_DATE);
    }

    /** Дата переноса в привычном пользователю формате (ДД-ММ-ГГГГ). */
    public String getTransferDateUserText() {
        return transferDate == null ? "" : transferDate.format(USER_DATE);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
