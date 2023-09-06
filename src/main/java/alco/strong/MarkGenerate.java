package alco.strong;

import static helpers.Constants.MID_MARK;

public class MarkGenerate {

    public String markGenerate(String code10, int number){
        String code36 = String.format("%15s",(Long.toString(Long.parseLong(code10), 36).toUpperCase()));
        String markNum = String.format("%4s", number).replace(' ', '0');
        return ("20N0" + code36 + MID_MARK + markNum).replace(' ', '0');
    }

    public String numberingMark(ActPositionStrong actPositionsStrong, int number){
        String markPos = actPositionsStrong.getCode();
        String markNum = String.format("%3s", number).replace(' ', '0');
        return (markPos + markNum);
    }
}