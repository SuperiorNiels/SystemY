package Network;

import java.io.*;
import java.net.Socket;

/**
 * This class is used to receive a tcp packet
 * The receiving happens in a thread that runs parallel with the main process
 */
public class receiveTCP extends Thread {
    private DataInputStream in;
    private String filePath;
    private Socket clientSocket;

    public receiveTCP(Socket aClientSocket,String filePath){
        try {
            clientSocket = aClientSocket;
            this.filePath = filePath;
            //initializes the inputstream
            in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            //starts the thread
            this.start();
        } catch (IOException e) {
            System.err.println("Problem opening a tcp connection to receive: "+e.getMessage());
        }

    }

    public void run(){
        try {
            //intiliazes the buffer to read the file
            byte[] byteBuffer = new byte[8192];
            //first reads the name of the file
            String fileName = in.readUTF();
            //opens the stream to save the file to a filepath
            OutputStream outputStream = new FileOutputStream(filePath+"/"+fileName);
            //amount of bytes already read
            int bytesRead;
            while ((bytesRead = in.read(byteBuffer, 0, byteBuffer.length)) != -1) {
                outputStream.write(byteBuffer,0,bytesRead);
            }
            //close the outputstream
            outputStream.close();

        } catch (FileNotFoundException e) {
            System.err.println("Problem opening the FileOutPut stream :"+e.getMessage());
        } catch (IOException e){
            System.err.println("Problem reading from the socket"+e.getMessage());
        }finally {
            try{
                clientSocket.close();
            }catch(IOException e){
                System.out.println("problem closing the socket: "+e.getMessage());
            }
        }


    }
}
