package Node;

import java.io.Serializable;

public class Neighbour implements Serializable {
    private static final long serialVersionUID = 3;
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

    /**
     * overrides both hashcode and equals method for comparisions of this object
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Neighbour neighbour = (Neighbour) o;

        if (!ip.equals(neighbour.ip)) return false;
        return name.equals(neighbour.name);
    }

    @Override
    public int hashCode() {
        int result = ip.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}
