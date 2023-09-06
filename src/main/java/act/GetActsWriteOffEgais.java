package act;

import alco.beer.ActPositionBeer;
import alco.strong.ActPositionStrong;
import helpers.GetClass;

import java.util.List;

import static helpers.Constants.FILE_PATH_IN;

public class GetActsWriteOffEgais {

    public static void main(String[] args) {

        GetClass getClass = new GetClass();
        List<ActPositionStrong> actPositionStrong = getClass.getParser(FILE_PATH_IN);
        List<ActPositionBeer> actPositionsBeer = getClass.getParserBeer(FILE_PATH_IN);
        getClass.getFile(actPositionStrong);
        getClass.getFileBeer(actPositionsBeer);
    }
}