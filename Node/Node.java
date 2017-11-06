package Node;

public class Node {
    private Node previous = null;
    private Node next = null;
    private String ip = null;
    private String name = null;
    public Node(String ip, String name) {
        this.ip = ip;
        this.name = name;
    }

    public void setNext(Node next) {
        this.next = next;
    }

    public void setPrevious(Node previous) {
        this.previous = previous;
    }

    public Node getNext() {
        return this.next;
    }

    public Node getPrevious() {
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
        if(this.ip.equals(node.ip)) {
            //System.out.println("IP address already in use.");
            error = true;
        }
        if(this.name.equals(node.name)) {
            //System.out.println("Name already in use.");
            error = true;
        }
        return error;
    }

    /**
     * This method updates the nodes next en previous ip addresses
     * @param new_name, String name of the new node (recieved via multicast)
     * @param new_ip, String ip address of the new node
     */
    public void updateNodes(String new_name, String new_ip) throws NodeAlreadyExistsException {
        int my_hash = calculateHash(name);
        int new_hash = calculateHash(new_name);

        if(my_hash == new_hash) throw new NodeAlreadyExistsException();

        if(my_hash < new_hash && new_hash < calculateHash(next.name)) {
            next = new Node(new_ip, new_name);
            //update new node
        } else if(calculateHash(previous.name) < new_hash && new_hash < my_hash) {
            previous = new Node(new_ip, new_name);
        }
    }

    

    private int calculateHash(String name) {
        return Math.abs(name.hashCode() % 32768);
    }
}
