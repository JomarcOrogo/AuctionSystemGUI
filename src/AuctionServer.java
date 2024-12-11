import java.io.*;
import java.net.*;
import java.util.*;

public class AuctionServer {
    private static final int PORT = 12345;
    private static final String SERVER_IP = "192.168.2.101";
    private static Map<String, Integer> auctionItems = new HashMap<>();
    private static Map<Socket, PrintWriter> clientOutputs = new HashMap<>();

    public static void main(String[] args) {
        auctionItems.put("Item1", 1000);
        auctionItems.put("Item2", 2000);

        try (ServerSocket serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName(SERVER_IP))) {
            System.out.println("Auction Server started on IP " + SERVER_IP + " and port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                clientOutputs.put(clientSocket, out);

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    System.out.println("Received from client: " + clientMessage);
                    handleClientMessage(clientMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                    clientOutputs.remove(clientSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleClientMessage(String message) {
            String[] messageParts = message.split(":");
            String action = messageParts[0];

            if (action.equals("BID")) {
                String item = messageParts[1];
                int bid = Integer.parseInt(messageParts[2]);
                processBid(item, bid);
            } else if (action.equals("VIEW")) {
                sendAuctionItems();
            }
        }

        private void processBid(String item, int bid) {
            if (auctionItems.containsKey(item)) {
                int currentBid = auctionItems.get(item);
                if (bid > currentBid) {
                    auctionItems.put(item, bid);
                    sendToAllClients("BID UPDATE:" + item + ":" + bid);
                    out.println("Your bid of ₱" + bid + " on " + item + " is successful.");
                } else {
                    out.println("Bid failed. Current bid is ₱" + currentBid);
                }
            } else {
                out.println("Item not found.");
            }
        }

        private void sendAuctionItems() {
            for (Map.Entry<String, Integer> entry : auctionItems.entrySet()) {
                out.println(entry.getKey() + ":₱" + entry.getValue());
            }
        }

        private void sendToAllClients(String message) {
            for (PrintWriter writer : clientOutputs.values()) {
                writer.println(message);
            }
        }
    }
}
