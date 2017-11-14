package Node;

public class Test {
    public static void main(String[] args) {
        FileManager manager = new FileManager("./files/");
        manager.initialize();
        manager.run();
    }
}
