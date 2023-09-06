package helpers;

import alco.beer.FileGenerateBeer;
import alco.beer.ActPositionBeer;
import alco.beer.XmlParserBeer;
import alco.strong.ActPositionStrong;
import alco.strong.FileGenerateStrong;
import alco.strong.XmlParserStrong;

import java.util.List;

public class GetClass {

    public List<ActPositionStrong> getParser (String filePath){
        XmlParserStrong xmlParserStrongService = new XmlParserStrong();
        return xmlParserStrongService.parseXmlFile(filePath);
    }

    public List<ActPositionBeer> getParserBeer (String filePath){
        XmlParserBeer xmlParserServiceBeer = new XmlParserBeer();
        return xmlParserServiceBeer.parseXmlFileBeer(filePath);
    }

    public void getFile (List<ActPositionStrong> actPositionStrong){
        FileGenerateStrong fileGenerateStrong = new FileGenerateStrong();
        fileGenerateStrong.generateXmlFile(actPositionStrong);
    }

    public void getFileBeer (List<ActPositionBeer> actPositionsBeer){
        FileGenerateBeer fileGenerateBeer = new FileGenerateBeer();
        fileGenerateBeer.generateXmlFileBeer(actPositionsBeer);
    }
}