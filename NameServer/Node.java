package NameServer;

public class Node {
    String ip = null;
    String name = null;

    public Node(String ip, String name) {
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

    /**
     * Function compares 2 nodes and returns Boolean if either name or ip are same
     * @param node the node to compare to
     * @return error, true when ip or name are equal
     */
    public Boolean equals(Node node) {
        Boolean error = false;
        if(this.ip == node.ip) {
            //System.out.println("IP address already in use.");
            error = true;
        }
        if(this.name == node.name) {
            //System.out.println("Name already in use.");
            error = true;
        }
        return error;
    }
}
