package transfer;

/**
 * Позиция документа переноса остатков в магазин ({@code tts:Position}).
 * <p>
 * Позиция берётся из выгрузки остатков склада ({@code ReplyRests_v2.xml}):
 * у неё уже есть код продукции и регистрационный номер справки 2, поэтому
 * перенести остаток в магазин можно без дополнительных данных.
 * <p>
 * Номер позиции ({@code tts:Identity}) — это номер позиции в самой выгрузке
 * склада, поэтому он совпадает с номером, который видит пользователь в ЕГАИС.
 */
public final class TransferPosition {

    private final int number;
    private final String productCode;
    private final String quantity;
    private final String f2RegId;

    public TransferPosition(int number, String productCode, String quantity, String f2RegId) {
        this.number = number;
        this.productCode = productCode;
        this.quantity = quantity;
        this.f2RegId = f2RegId;
    }

    /** Номер позиции в выгрузке остатков склада; он же {@code tts:Identity}. */
    public int getNumber() {
        return number;
    }

    /** Код продукции; он же {@code tts:ProductCode}. */
    public String getProductCode() {
        return productCode;
    }

    /** Количество, переносимое в магазин, — как оно записано в выгрузке склада. */
    public String getQuantity() {
        return quantity;
    }

    /** Регистрационный номер справки 2 ({@code pref:F2RegId}). */
    public String getF2RegId() {
        return f2RegId;
    }
}
