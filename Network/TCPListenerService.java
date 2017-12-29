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
    private String rootPath;
    private static final int port = 6000;
    public TCPListenerService(String rootPath){
        try {
            this.rootPath = rootPath;
            listeningSocket = new ServerSocket(port);
            System.out.println("Opened port: "+port);
            this.start();
        } catch (IOException e) {
            System.err.println("Problem opening serverSocket");        }
    }
    public void run(){
        while(true){
            try {
                //System.out.println("Listening for connections");
                Socket connection = listeningSocket.accept();
                //System.out.println("Connection has been accepeted");
                ReceiveTCP receiveHandler = new ReceiveTCP(connection,rootPath);
                //System.out.println("Connection forwarded to receive thread");
            } catch (IOException e) {
                System.err.println("Error opening the connection");
            }
        }
    }
}
