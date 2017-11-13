package Network;

import java.io.*;
import java.net.Socket;

/**
 * Class that is used to send a file over a tcp connection
 */
public class SendTCP extends Thread {
    private Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private String filePath;

    /**
     *
     * @param aClientSocket
     */
    public SendTCP(Socket aClientSocket,String path) {
        try {
            filePath = path;
            clientSocket = aClientSocket;
            //We wrap everything in bufferedstream to fasten up the transaction
            //The reason for using datastream is because we can write/read primitive types straigth from/to the input/output
            in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
            //starts the thread
            this.start();
        } catch (IOException e) {
            System.out.println("Connection:" + e.getMessage());
        }
    }

    /**
     *
     */
    public void run() {
        try {
            String data = in.readUTF();
            //array of bytes that holds the file bytes
            byte[] file = readFile(filePath);
            System.out.println("sending file :"+data);
            out.flush();        //flushes the buffer
            out.write(file,0,file.length);
        } catch (IOException e) {
            System.out.println("readline:" + e.getMessage());
        }finally{
            try{
                clientSocket.close();
            }catch(IOException e){
                System.out.println("problem closing the socket: "+e.getMessage());
            }
        }
    }

    /**
     * reads a file into a byte array and returns that array
     * @param fileName
     * @return
     */
    private byte[] readFile(String fileName){
        File myFile = new File(fileName);
        byte myByteArray[] = new byte[(int) myFile.length()];
        try {
            BufferedInputStream reader = new BufferedInputStream(new FileInputStream(myFile));
            reader.read(myByteArray,0,myByteArray.length);
            reader.close();
        }catch(FileNotFoundException e){
            System.out.println("The file has not been found: "+e.getMessage());
        }catch(IOException e){
            System.out.println("problem with reading the file: "+e.getMessage());
        }
        return myByteArray;
    }
}
