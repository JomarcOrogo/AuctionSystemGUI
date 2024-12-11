import java.io.*;
import java.net.*;
import java.util.*;

public class AuctionServer {
    private static final int PORT = 12345;
    private static final String SERVER_IP = "192.168.2.101";
    private static Map<String, AuctionItem> auctionItems = new HashMap<>();

    public static void main(String[] args) {
        auctionItems.put("Item1", new AuctionItem(1000, "none"));
        auctionItems.put("Item2", new AuctionItem(2000, "none"));

        try (ServerSocket serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName(SERVER_IP))) {
            System.out.println("Auction Server started on " + SERVER_IP + ":" + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
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

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    handleClientMessage(clientMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleClientMessage(String message) {
            String[] messageParts = message.split(":");
            String action = messageParts[0];

            if (action.equals("BID")) {
                String item = messageParts[1];
                int bid = Integer.parseInt(messageParts[2]);
                String bidder = messageParts[3];
                processBid(item, bid, bidder);
            } else if (action.equals("VIEW")) {
                sendAuctionItems();
            }
        }

        private void processBid(String item, int bid, String bidder) {
            if (auctionItems.containsKey(item)) {
                AuctionItem currentItem = auctionItems.get(item);
                if (bid > currentItem.getBidPrice()) {
                    currentItem.setBidPrice(bid);
                    currentItem.setBidder(bidder);
                    System.out.println("New bid for " + item + ": ₱" + bid + " by " + bidder);
                } else {
                    System.out.println("Rejected bid for " + item + ": ₱" + bid);
                }
            } else {
                System.out.println("Item not found: " + item);
            }
        }

        private void sendAuctionItems() {
            try {
                for (Map.Entry<String, AuctionItem> entry : auctionItems.entrySet()) {
                    AuctionItem item = entry.getValue();
                    out.println(entry.getKey() + " | ₱" + item.getBidPrice() + " | " + item.getBidder());
                }
                out.println("END"); // End of item list
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class AuctionItem {
        private int bidPrice;
        private String bidder;

        public AuctionItem(int bidPrice, String bidder) {
            this.bidPrice = bidPrice;
            this.bidder = bidder;
        }

        public int getBidPrice() {
            return bidPrice;
        }

        public void setBidPrice(int bidPrice) {
            this.bidPrice = bidPrice;
        }

        public String getBidder() {
            return bidder;
        }

        public void setBidder(String bidder) {
            this.bidder = bidder;
        }
    }
}
