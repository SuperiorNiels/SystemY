package Network;

import java.io.*;
import java.net.Socket;

/**
 * Class that is used to send a file over a tcp connection
 *
 */
public class SendTCP extends Thread {
    private Socket clientSocket;
    private DataOutputStream out;
    private String filePath;
    private String fileName;

    /**
     *
     * @param aClientSocket socket to send to
     * @param filePath path of the file
     * @param fileName name of the file
     */
    public SendTCP(Socket aClientSocket,String filePath, String fileName) {
        try {
            this.filePath = filePath;
            this.fileName = fileName;
            clientSocket = aClientSocket;
            //We wrap everything in bufferedstream to fasten up the transaction
            //The reason for using datastream is because we can write/read primitive types straigth from/to the input/output
            out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
            //starts the thread
            this.start();
        } catch (IOException e) {
            System.out.println("Connection:" + e.getMessage());
        }
    }

    /**
     * First sends the name of the file and then the file over a tcp connection
     */
    public void run() {
        try {
            //first send the file name to the receiver
            out.flush();
            out.writeChars(fileName);
            //array of bytes that holds the file bytes
            byte[] file = readFile(filePath+"/"+fileName);
            System.out.println("Sending file:"+filePath);
            out.flush();        //flushes the buffer
            //sends the file
            out.write(file,0,file.length);
        } catch (IOException e) {
            System.out.println("readline:" + e.getMessage());
        }finally{
            try{
                //closes the socket
                clientSocket.close();
            }catch(IOException e){
                System.out.println("problem closing the socket: "+e.getMessage());
            }
        }
    }

    /**
     * reads a file into a byte array and returns that array
     * @param fileName
     * @return array of bytes
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
