import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("1. Host and play");
        System.out.println("2. Play");

        Client client;
        Server server;
        switch (sc.nextInt()){
            case 1:
                server = new Server();
                client = new Client();
                break;
            case 2:
                client = new Client();
        }
    }
}
