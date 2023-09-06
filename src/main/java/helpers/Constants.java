package helpers;

import java.util.Arrays;
import java.util.List;

public class Constants {

    public static final List<String> ALLOWED_PRODUCT_CODES = Arrays.asList("500", "510", "520", "263", "261", "262");

    public static final String RST_POSITION = "rst:StockPosition";
    public static final String PREF_CODE = "pref:ProductVCode";
    public static final String PREF_ALC_CODE = "pref:AlcCode";
    public static final String RST_QUANTITY = "rst:Quantity";
    public static final String RST_F2 = "rst:InformF2RegId";

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

    public static final String FILE_PATH_IN = "src/main/java/files/ReplyRests_v2.xml";
    public static final String FILE_PATH_OUT_STRONG = "src/main/java/files/ActWriteOffStrong.xml";
    public static final String FILE_PATH_OUT_BEER = "src/main/java/files/ActWriteOffBeer.xml";

    public static final String MID_MARK = "1NKN31105001000188NQQMS5VP4HTF5SB46ZSQQJD8";
}