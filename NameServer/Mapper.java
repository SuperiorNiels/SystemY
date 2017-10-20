package NameServer;

import java.util.TreeMap;

public class Mapper {

    TreeMap<Integer, String> map = new TreeMap<>();

    public Mapper() {}

    public void updateMap(Integer hash, String ip, String name) {
        map.put(hash, ip);
    }

    /**
     * @return String (path of xml file)
     */
    public String createXML() {
        return null;
    }
}
