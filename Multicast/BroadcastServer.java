package Multicast;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class BroadcastServer {

    public static void main(String[] args) throws IOException {
        Scanner snc = new Scanner(System.in);
        System.out.println("Type a message: ");
        String in = snc.nextLine();
        new BroadcastServerThread(in).start();
    }

}

class BroadcastServerThread extends Thread{
    private DatagramSocket socket;
    private String message;
    public BroadcastServerThread(String message){
        this.message = message;
        try {
            socket = new DatagramSocket(4446);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    public void run(){
        try {
            byte[] buf;
            buf = message.getBytes();
            InetAddress groupAddress = InetAddress.getByName("10.0.1.0");
            DatagramPacket packet;
            packet = new DatagramPacket(buf, buf.length, groupAddress, 4446);
            socket.send(packet);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            socket.close();
        }
    }
}