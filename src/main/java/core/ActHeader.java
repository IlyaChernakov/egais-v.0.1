package core;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import helpers.Constants;

/**
 * Данные шапки акта списания: они одинаковы для всех позиций документа
 * и попадают в конверт {@code ns:Documents}.
 * <p>
 * Значения, которые вводит пользователь:
 * <ul>
 *   <li>{@code FSRAR_ID} организации — узел {@code ns:Owner/ns:FSRAR_ID};</li>
 *   <li>номер акта — он же {@code awr:ActNumber} и {@code awr:Identity}; для крепкого
 *       алкоголя и для пива номера задаются отдельно, поэтому шапка собирается
 *       на каждый акт своя;</li>
 *   <li>дата списания — {@code awr:ActDate};</li>
 *   <li>причина списания — {@code awr:TypeWriteOff};</li>
 *   <li>примечание — {@code awr:Note}.</li>
 * </ul>
 */
public final class ActHeader {

    /** Формат даты, который видит пользователь: ДД-ММ-ГГГГ. */
    public static final DateTimeFormatter USER_DATE = DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT);

    /** Формат даты внутри документа ЕГАИС: ГГГГ-ММ-ДД. */
    public static final DateTimeFormatter XML_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ROOT);

    private final String fsrarId;
    private final String actNumber;
    private final LocalDate actDate;
    private final String typeWriteOff;
    private final String note;

    public ActHeader(String fsrarId, String actNumber, LocalDate actDate, String typeWriteOff, String note) {
        this.fsrarId = trim(fsrarId);
        this.actNumber = trim(actNumber);
        this.actDate = actDate;
        this.typeWriteOff = trim(typeWriteOff);
        this.note = trim(note);
    }

    /**
     * Собирает шапку из значений, введённых пользователем как текст.
     *
     * @param fsrarId       FSRAR_ID организации
     * @param actNumber     номер акта
     * @param actDateText   дата списания в формате ДД-ММ-ГГГГ (принимается и ГГГГ-ММ-ДД)
     * @param typeWriteOff  причина списания
     * @param note          примечание
     * @return готовая шапка акта
     * @throws GenerationException если дату не удалось разобрать
     */
    public static ActHeader of(String fsrarId, String actNumber, String actDateText,
                               String typeWriteOff, String note) throws GenerationException {
        return new ActHeader(fsrarId, actNumber, parseDate(actDateText), typeWriteOff, note);
    }

    /** Шапка со значениями по умолчанию: сегодняшняя дата, причина «Порча» и типовое примечание. */
    public static ActHeader defaults(String fsrarId, String actNumber) {
        LocalDate today = LocalDate.now();
        return new ActHeader(fsrarId, actNumber, today, Constants.DEFAULT_TYPE_WRITE_OFF, defaultNote(today));
    }

    /** Примечание по умолчанию для указанной даты. */
    public static String defaultNote(LocalDate date) {
        LocalDate value = date != null ? date : LocalDate.now();
        return String.format(Constants.DEFAULT_NOTE_TEMPLATE, value.format(USER_DATE));
    }

    /**
     * Разбирает дату списания. Принимаются форматы ДД-ММ-ГГГГ и ГГГГ-ММ-ДД,
     * разделителем может быть дефис, точка или дробь.
     *
     * @param text введённый текст
     * @return разобранная дата
     * @throws GenerationException если текст не является датой
     */
    public static LocalDate parseDate(String text) throws GenerationException {
        String value = trim(text).replace('.', '-').replace('/', '-');
        if (value.isEmpty()) {
            throw new GenerationException("Укажите дату списания в формате ДД-ММ-ГГГГ.");
        }
        try {
            return LocalDate.parse(value, USER_DATE);
        } catch (DateTimeParseException ignored) {
            // возможно, дата записана как в самом документе: ГГГГ-ММ-ДД
        }
        try {
            return LocalDate.parse(value, XML_DATE);
        } catch (DateTimeParseException ignored) {
            throw new GenerationException("Дата списания «" + trim(text)
                    + "» непонятна. Укажите её в формате ДД-ММ-ГГГГ, например "
                    + LocalDate.now().format(USER_DATE) + ".");
        }
    }

    /**
     * Проверяет, что шапка заполнена достаточно для формирования акта.
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
        if (actNumber.isEmpty()) {
            throw new GenerationException("Укажите номер акта.");
        }
        if (actDate == null) {
            throw new GenerationException("Укажите дату списания.");
        }
    }

    /** FSRAR_ID организации. */
    public String getFsrarId() {
        return fsrarId;
    }

    /** Номер акта; он же используется как {@code awr:Identity} документа. */
    public String getActNumber() {
        return actNumber;
    }

    /** Дата списания. */
    public LocalDate getActDate() {
        return actDate;
    }

    /** Дата списания в формате документа ЕГАИС (ГГГГ-ММ-ДД). */
    public String getActDateText() {
        return actDate == null ? "" : actDate.format(XML_DATE);
    }

    /** Дата списания в привычном пользователю формате (ДД-ММ-ГГГГ). */
    public String getActDateUserText() {
        return actDate == null ? "" : actDate.format(USER_DATE);
    }

    /** Причина списания; может быть пустой — тогда тег не записывается. */
    public String getTypeWriteOff() {
        return typeWriteOff;
    }

    /** Примечание; может быть пустым — тогда тег не записывается. */
    public String getNote() {
        return note;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
