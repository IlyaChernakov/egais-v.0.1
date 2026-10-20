package helpers;

import java.util.Arrays;
import java.util.List;

/**
 * Константы формата ЕГАИС и параметры приложения.
 */
public final class Constants {

    private Constants() {
    }

    /** Название программы (заголовок окна и диалог «О программе»). */
    public static final String APP_NAME = "Генерация актов списания ЕГАИС";

    /** Версия программы. */
    public static final String APP_VERSION = "1.0";

    /** Коды вида продукции, которые относятся к пиву и пивным напиткам. */
    public static final List<String> ALLOWED_PRODUCT_CODES =
            Arrays.asList("500", "510", "520", "263", "261", "262");

    // --- Элементы входного файла остатков (ReplyRests_v2) ---

    public static final String RST_POSITION = "rst:StockPosition";
    public static final String PREF_CODE = "pref:ProductVCode";
    public static final String PREF_ALC_CODE = "pref:AlcCode";
    public static final String RST_QUANTITY = "rst:Quantity";
    public static final String RST_F2 = "rst:InformF2RegId";

    // --- Элементы формируемого акта списания ---

    public static final String TAG_CONTENT = "awr:Content";
    public static final String TAG_POSITION = "awr:Position";
    public static final String TAG_IDENTITY = "awr:Identity";
    public static final String TAG_QUANTITY = "awr:Quantity";
    public static final String TAG_INFORM_F1F2 = "awr:InformF1F2";
    public static final String TAG_INFORM_F2 = "awr:InformF2";
    public static final String TAG_F2_REG_ID = "pref:F2RegId";
    public static final String TAG_MARK_CODE_INFO = "awr:MarkCodeInfo";
    public static final String TAG_AMC = "ce:amc";

    public static final String TAG_SHOP_POSITION = "rst:ShopPosition";

    /** Пространство имён для атрибута формируемых марок. */
    public static final String NAMESPACE_CE = "http://fsrar.ru/WEGAIS/CommonV3";

    /** Фиксированная (серединная) часть марки. */
    public static final String MID_MARK = "1NKN31105001000188NQQMS5VP4HTF5SB46ZSQQJD8";

    // --- Имена файлов результата ---

    public static final String OUTPUT_FILE_STRONG = "ActWriteOffStrong.xml";
    public static final String OUTPUT_FILE_BEER = "ActWriteOffBeer.xml";
}
