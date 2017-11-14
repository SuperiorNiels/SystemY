package Network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class is used to listen for other nodes that want to connect to this node.
 * It runs in a seperated thread so it doesn't stale the main process
 * After receiving a request to make a connection with this node, the request is accepted and the connection is handed
 * to another thread
 */
public class TCPListenerService extends Thread{
    private ServerSocket listeningSocket;
    private String filePath;
    private int port = 8000;
    public TCPListenerService(String filePath){
        try {
            this.filePath = filePath;
            listeningSocket = new ServerSocket(port);
            this.start();
        } catch (IOException e) {
            System.err.println("Problem opening serverSocket");        }
    }
    public void run(){
        try {
            Socket connection = listeningSocket.accept();
            receiveTCP receiveHandler = new receiveTCP(connection,filePath);
        } catch (IOException e) {
            System.err.println("Error opening the connection");
        }


    }
    private void listen(){

    }
}
