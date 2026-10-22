package core;

import java.time.LocalDate;

/**
 * Имена файлов результата: {@code <вид документа>_<FSRAR_ID>_<ДД-ММ-ГГГГ>.xml}.
 * <p>
 * Например: {@code ActWriteOff_v3(S)_020000253206_24-09-2024.xml},
 * {@code ActWriteOff_v3(B)_020000253206_24-09-2024.xml},
 * {@code TransferToShop_020000253206_24-09-2024.xml}.
 */
public final class OutputNames {

    /** Начало имени документа переноса остатков в магазин. */
    public static final String TRANSFER_PREFIX = "TransferToShop_";

    /** Начало имени акта списания по крепкому алкоголю. */
    public static final String STRONG_PREFIX = "ActWriteOff_v3(S)_";

    /** Начало имени акта списания по пиву и пивным напиткам. */
    public static final String BEER_PREFIX = "ActWriteOff_v3(B)_";

    /** Расширение файлов результата. */
    private static final String EXTENSION = ".xml";

    private OutputNames() {
    }

    /**
     * Имя файла документа переноса остатков в магазин.
     *
     * @param fsrarId FSRAR_ID организации
     * @param date    дата переноса
     * @return имя файла
     */
    public static String transfer(String fsrarId, LocalDate date) {
        return build(TRANSFER_PREFIX, fsrarId, date);
    }

    /**
     * Имя файла акта списания по крепкому алкоголю.
     *
     * @param fsrarId FSRAR_ID организации
     * @param date    дата списания
     * @return имя файла
     */
    public static String writeOffStrong(String fsrarId, LocalDate date) {
        return build(STRONG_PREFIX, fsrarId, date);
    }

    /**
     * Имя файла акта списания по пиву и пивным напиткам.
     *
     * @param fsrarId FSRAR_ID организации
     * @param date    дата списания
     * @return имя файла
     */
    public static String writeOffBeer(String fsrarId, LocalDate date) {
        return build(BEER_PREFIX, fsrarId, date);
    }

    private static String build(String prefix, String fsrarId, LocalDate date) {
        String id = fsrarId == null ? "" : fsrarId.trim();
        String day = date == null ? "" : date.format(ActHeader.USER_DATE);
        return prefix + id + "_" + day + EXTENSION;
    }
}
