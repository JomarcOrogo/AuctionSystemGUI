import java.io.*;
import java.net.*;
import java.util.*;

public class AuctionServer {
    private static final int PORT = 12345;
    private static Map<String, Integer> items = new HashMap<>();
    private static Map<String, String> highestBidders = new HashMap<>();
    private static List<ObjectOutputStream> clientStreams = new ArrayList<>();

    public static void main(String[] args) {
        items.put("Laptop", 5000); // Initial item prices
        items.put("Smartphone", 3000);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Auction server started. Waiting for clients...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected.");
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                clientStreams.add(out);
                new ClientHandler(clientSocket, out).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void processBid(String item, int bid, String bidderName) {
        if (items.containsKey(item) && bid > items.get(item)) {
            items.put(item, bid);
            highestBidders.put(item, bidderName);
            broadcastUpdate(item, bid, bidderName);
        } else {
            System.out.println("Invalid bid: " + bidderName + " bid ₱" + bid + " on " + item);
        }
    }

    private static void broadcastUpdate(String item, int bid, String bidderName) {
        String message = "New bid: " + item + " - ₱" + bid + " by " + bidderName;
        System.out.println(message);
        for (ObjectOutputStream out : clientStreams) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private final ObjectOutputStream out;

        ClientHandler(Socket socket, ObjectOutputStream out) {
            this.socket = socket;
            this.out = out;
        }

        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                while (true) {
                    String input = (String) in.readObject();
                    String[] parts = input.split(",");
                    String item = parts[0].trim();
                    int bid = Integer.parseInt(parts[1].trim());
                    String bidderName = parts[2].trim();
                    processBid(item, bid, bidderName);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Client disconnected.");
                clientStreams.remove(out);
            }
        }
    }
}
