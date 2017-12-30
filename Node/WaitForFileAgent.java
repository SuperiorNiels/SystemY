package Node;

public class WaitForFileAgent implements Runnable {
    private Node node;
    private String filename;

    public WaitForFileAgent(Node rootNode, String filename){
        this.node = rootNode;
        this.filename = filename;
    }

    @Override
    public void run() {
        while(node.getFilesToRemove().contains(filename)){
            //Do nothing == waiting for the file agent
        }
    }
}
