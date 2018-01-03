package Node;

public class WaitForNameServer extends Thread {

    private Node node;

    public WaitForNameServer(Node rootNode){
        this.node = rootNode;
    }

    @Override
    public void run() {
        // Wait 6 seconds than check if node is accepted by name server
        try {
            sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(!node.getLoggedIn()) {
            // node is not accepted, shutdown the node
            System.err.println("No response from the name server.");
            node.failedToAddNode();
        }
    }
}
