package core;

/**
 * Ошибка при разборе файла остатков или формировании акта списания.
 * <p>
 * Сообщение рассчитано на показ пользователю, поэтому формулируется по-русски
 * и содержит конкретные данные (номер позиции, имя тега, путь к файлу).
 */
public class GenerationException extends Exception {

    private static final long serialVersionUID = 1L;

    public GenerationException(String message) {
        super(message);
    }

    public GenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
