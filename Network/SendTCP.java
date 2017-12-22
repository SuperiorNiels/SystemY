package Network;

import Node.Neighbour;
import Node.NodeInterface;

import java.io.*;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.NotBoundException;

/**
 * Class that is used to send a file over a tcp connection
 *
 */
public class SendTCP extends Thread {
    private Socket clientSocket;
    private DataOutputStream out;
    private String srcFilePath;
    private String destFolder;
    private String fileName;
    private boolean notifDownloader;

    /**
     * @param aClientSocket socket to send to
     * @param srcFilePath source path of the file
     * @param fileName name of the file
     * @param destFolder name of the folder where the file has to be sent
     * @param notifDownloader set this boolean true to notify the downloader when the file has been sent through RMI
     */
    public SendTCP(Socket aClientSocket, String srcFilePath, String fileName,String destFolder,boolean notifDownloader) {
        try {
            this.srcFilePath = srcFilePath;
            this.destFolder = destFolder;
            this.fileName = fileName;
            this.notifDownloader = notifDownloader;
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
            out.writeUTF(fileName);
            out.flush();
            //sends the destination folder name
            out.writeUTF(destFolder);
            out.flush();        //flushes the buffer
            //array of bytes that holds the file bytes
            byte[] file = readFile(srcFilePath +"/"+fileName);
            System.out.println("Sending file: "+fileName+" to folder: "+destFolder);
            //sends the file
            out.write(file,0,file.length);
            out.flush();
            if(notifDownloader){
                NodeInterface stub = (NodeInterface) Naming.lookup("//"+clientSocket.getInetAddress()+"/Node");
                //TODO notify downloader
            }
        } catch (IOException e) {
            System.out.println("readline: " + e.getMessage());
        } catch (NotBoundException e) {
            e.printStackTrace();
            System.err.println("There was an error notifying the downloader"+e.getMessage());
        } finally{
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
     * @param fileLocation whole location of the file(path + filename)
     * @return array of bytes
     */
    private byte[] readFile(String fileLocation){
        File myFile = new File(fileLocation);
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
