package Node;

import java.io.Serializable;

public class Neighbour implements Serializable {
    private String ip;
    private String name;

    public Neighbour(String name, String ip) {
        this.ip = ip;
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return "Name: "+name+" IP: "+ip;
    }
}
