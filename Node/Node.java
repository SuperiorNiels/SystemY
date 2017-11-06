package Node;

public class Node {
    private String previous = null;
    private String next = null;
    private String ip = null;
    private String name = null;
    public Node(String ip, String name) {
        this.ip = ip;
        this.name = name;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public void setPrevious(String previous) {
        this.previous = previous;
    }

    public String getNext() {
        return this.next;
    }

    public String getPrevious() {
        return this.previous;
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

    /**
     *
     * @param new_hash
     */
    public void updateNodes(Integer new_hash) {
        int my_hash = Math.abs(this.name.hashCode() % 32768);

    }

}
