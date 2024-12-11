import java.io.*;
import java.net.*;
import java.util.Scanner;

public class AuctionClient {
    private static final String SERVER_IP = "192.168.2.101";
    private static final int SERVER_PORT = 12345;
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void main(String[] args) {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Scanner scanner = new Scanner(System.in);
            String message;

            System.out.println("Connected to auction server at " + SERVER_IP);

            while (true) {
                System.out.println("Options:\n1. Place a bid\n2. View auction items\n3. Exit");
                System.out.print("Enter your choice: ");
                int choice = scanner.nextInt();
                scanner.nextLine();  // Consume newline

                switch (choice) {
                    case 1:
                        System.out.print("Enter item name: ");
                        String item = scanner.nextLine();
                        System.out.print("Enter your bid (₱): ");
                        int bid = scanner.nextInt();
                        out.println("BID:" + item + ":" + bid);
                        break;
                    case 2:
                        out.println("VIEW");
                        break;
                    case 3:
                        socket.close();
                        System.out.println("Disconnected from server.");
                        return;
                    default:
                        System.out.println("Invalid choice, try again.");
                        break;
                }

                while ((message = in.readLine()) != null) {
                    System.out.println("Server: " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
