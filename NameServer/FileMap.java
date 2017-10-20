package NameServer;

import java.util.TreeMap;

public class FileMap implements FileMapRemote {
    TreeMap<Integer,String> files;

    public FileMap(){
        this.files = new TreeMap<>();
    }

    public void addFile(String fileName, String location){
        int hash = fileNameHash(fileName);
        files.put(hash,location);
    }

    public String getLocation(String fileName){
        int hash = fileNameHash(fileName);
        return files.get(hash);
    }

    public int fileNameHash(String fileName){
        return Math.abs(fileName.hashCode() % 32768);
    }

    public getOwner(){
        
    }
}
