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
    public static final String APP_NAME = "Генерация документов ЕГАИС";

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

    // --- Конверт документа акта списания ---

    public static final String TAG_DOCUMENTS = "ns:Documents";
    public static final String TAG_OWNER = "ns:Owner";
    public static final String TAG_FSRAR_ID = "ns:FSRAR_ID";
    public static final String TAG_DOCUMENT = "ns:Document";
    public static final String TAG_ACT_WRITE_OFF = "ns:ActWriteOff_v3";
    public static final String TAG_HEADER = "awr:Header";
    public static final String TAG_ACT_NUMBER = "awr:ActNumber";
    public static final String TAG_ACT_DATE = "awr:ActDate";
    public static final String TAG_TYPE_WRITE_OFF = "awr:TypeWriteOff";
    public static final String TAG_NOTE = "awr:Note";

    /** Атрибут версии формата у корневого элемента. */
    public static final String ATTR_VERSION = "Version";

    /** Значение версии формата у корневого элемента. */
    public static final String DOCUMENTS_VERSION = "1.0";

    // --- Пространства имён ---

    /** Пространство имён для атрибута формируемых марок. */
    public static final String NAMESPACE_CE = "http://fsrar.ru/WEGAIS/CommonV3";

    /** Служебное пространство имён XML-схемы. */
    public static final String NAMESPACE_XSI = "http://www.w3.org/2001/XMLSchema-instance";

    /** Основное пространство имён документов ЕГАИС. */
    public static final String NAMESPACE_DOCUMENTS = "http://fsrar.ru/WEGAIS/WB_DOC_SINGLE_01";

    /** Пространство имён ссылок на продукцию. */
    public static final String NAMESPACE_PREF = "http://fsrar.ru/WEGAIS/ProductRef_v2";

    /** Пространство имён акта списания. */
    public static final String NAMESPACE_AWR = "http://fsrar.ru/WEGAIS/ActWriteOff_v3";

    // --- Значения по умолчанию для шапки акта ---

    /** Причина списания по умолчанию. */
    public static final String DEFAULT_TYPE_WRITE_OFF = "Порча";

    /** Шаблон примечания по умолчанию; %s — дата в формате ДД-ММ-ГГГГ. */
    public static final String DEFAULT_NOTE_TEMPLATE = "Списание по инвентаризации 1 от %s";

    /** Фиксированная (серединная) часть марки. */
    public static final String MID_MARK = "1NKN31105001000188NQQMS5VP4HTF5SB46ZSQQJD8";

    // --- Отправка акта в УТМ ---

    /** Адрес УТМ по умолчанию (поле «Адрес УТМ» предзаполняется этим значением). */
    public static final String DEFAULT_UTM_ADDRESS = "http://localhost:8080";

    /** Путь приёма актов списания в УТМ; дописывается к адресу УТМ. */
    public static final String UTM_ACT_WRITE_OFF_PATH = "/opt/in/ActWriteOff_v3";

    /** Имя файла, под которым акт передаётся в УТМ. */
    public static final String UTM_UPLOAD_FILE_NAME = "client.xml";

    /** Имя поля формы, в котором УТМ ожидает файл акта. */
    public static final String UTM_FORM_FIELD = "xml_file";

    /** Сколько секунд ждать ответа УТМ, прежде чем прекратить отправку. */
    public static final int UTM_TIMEOUT_SECONDS = 60;

    // --- Документ переноса остатков в магазин (TransferToShop) ---

    /** Пространство имён документа переноса остатков в магазин. */
    public static final String NAMESPACE_TTS = "http://fsrar.ru/WEGAIS/TransferToShop";

    /** Пространство имён общей части документов ЕГАИС. */
    public static final String NAMESPACE_COMMON = "http://fsrar.ru/WEGAIS/Common";

    /** Служебное пространство имён XML-схемы ({@code xmlns:xs}). */
    public static final String NAMESPACE_XS = "http://www.w3.org/2001/XMLSchema";

    /** Корневой элемент документа переноса внутри конверта {@code ns:Document}. */
    public static final String TAG_TRANSFER_TO_SHOP = "ns:TransferToShop";

    /** Элементы документа переноса остатков в магазин. */
    public static final String TTS_CONTENT = "tts:Content";
    public static final String TTS_POSITION = "tts:Position";
    public static final String TTS_IDENTITY = "tts:Identity";
    public static final String TTS_PRODUCT_CODE = "tts:ProductCode";
    public static final String TTS_QUANTITY = "tts:Quantity";
    public static final String TTS_INFORM_F2 = "tts:InformF2";
    public static final String TTS_HEADER = "tts:Header";
    public static final String TTS_TRANSFER_NUMBER = "tts:TransferNumber";
    public static final String TTS_TRANSFER_DATE = "tts:TransferDate";

    /** Путь приёма документов переноса остатков в магазин в УТМ. */
    public static final String UTM_TRANSFER_TO_SHOP_PATH = "/opt/in/TransferToShop";
}
