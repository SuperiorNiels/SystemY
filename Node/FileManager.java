package Node;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.TreeMap;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class FileManager extends Thread {
    private Path root;
    private WatchService watcher;
    private WatchKey key;
    private Neighbour root_node; // Root node is node name that created the filemanager

    private TreeMap<Integer, FileEntry> map;

    public FileManager(String root, Neighbour root_node) {
        this.root = Paths.get(root);
        this.root_node = root_node;
        this.map = new TreeMap<Integer, FileEntry>();
    }

    public void initialize() {
        try {
            watcher = FileSystems.getDefault().newWatchService();
            registerRecursive(root);
            File folder = new File(root+"/local");
            File [] fileList = folder.listFiles();
            for(File file : fileList) {
                FileEntry new_entry = new FileEntry(root_node);
                map.put(calculateHash(file.getName()), new_entry);
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void printMap() {
        System.out.println("FileManager Map of node: "+root_node.toString());
        for(Integer i : map.keySet()) {
            System.out.println("Hash: "+i+" ; \nReplicated nodes: ");
            FileEntry entry = map.get(i);
            for(Neighbour node : entry.getReplicatedNodes()) {
                System.out.println(node.getName());
            }
        }
    }

    private void registerRecursive(final Path root) throws IOException {
        // register all subfolders
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void run() {
        try {
            while ((key = watcher.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    switch (event.kind().toString()) {
                        case "ENTRY_CREATE":

                            System.out.println("File created.");
                            break;
                        case "ENTRY_MODIFY":
                            System.out.println("File modified.");
                            break;
                        case "ENTRY_DELETE":
                            System.out.println("File deleted.");
                            break;
                    }
                }
                key.reset();
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculates the hash of a given String
     * @param name
     * @return
     */
    private int calculateHash(String name) {
        return Math.abs(name.hashCode() % 32768);
    }

}
