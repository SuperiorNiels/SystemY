package Multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class Node {
    public Node(){
        try {
            MulticastSocket socket = new MulticastSocket(4446);
            InetAddress groupAddress = InetAddress.getByName("10.");
            socket.joinGroup(groupAddress);

            DatagramPacket packet;
            byte[] buf = new byte[256];
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            String received = new String(packet.getData());
            System.out.println("RECEIVED PACKET: " + received);

            socket.leaveGroup(groupAddress);
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
