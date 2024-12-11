import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class AuctionServer {
    private static final int PORT = 12345;
    private static final String SERVER_IP = "192.168.2.101";
    private static Map<String, AuctionItem> auctionItems = new HashMap<>();
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        auctionItems.put("Sofa", new AuctionItem(1000, "none", 60000)); // 5 minutes auction duration
        auctionItems.put("Table", new AuctionItem(2000, "none", 60000)); // 5 minutes auction duration

        // Schedule a task to check auction status every 1 second
        scheduler.scheduleAtFixedRate(AuctionServer::checkAuctions, 0, 1, TimeUnit.SECONDS);

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

    private static void checkAuctions() {
        Iterator<Map.Entry<String, AuctionItem>> iterator = auctionItems.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, AuctionItem> entry = iterator.next();
            AuctionItem item = entry.getValue();

            if (item.isAuctionOver()) {
                // Save the result only if there's a valid bidder
                if (!item.isResultSaved() && !item.getBidder().equals("none")) {
                    AuctionResultSaver.saveAuctionResult(entry.getKey(), item.getBidder(), item.getBidPrice());
                    item.setResultSaved(true); // Mark that result has been saved
                }
                // Remove the item after auction ends
                iterator.remove();
                System.out.println("Auction ended for " + entry.getKey() + ". Item removed.");
            }
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
            } else if (action.equals("ADD_ITEM")) {
                String itemName = messageParts[1];
                int startPrice = Integer.parseInt(messageParts[2]);
                int auctionTime = Integer.parseInt(messageParts[3]);
                addAuctionItem(itemName, startPrice, auctionTime);
            }
        }

        private void processBid(String item, int bid, String bidder) {
            if (auctionItems.containsKey(item)) {
                AuctionItem currentItem = auctionItems.get(item);
                if (currentItem.isAuctionOver()) {
                    System.out.println("Auction ended for " + item + ". Final bid: ₱" + currentItem.getBidPrice());
                } else {
                    if (bid > currentItem.getBidPrice()) {
                        currentItem.setBidPrice(bid);
                        currentItem.setBidder(bidder);
                        System.out.println("New bid for " + item + ": ₱" + bid + " by " + bidder);
                    } else {
                        System.out.println("Rejected bid for " + item + ": ₱" + bid);
                    }
                }
            } else {
                System.out.println("Item not found: " + item);
            }
        }

        private void sendAuctionItems() {
            try {
                for (Map.Entry<String, AuctionItem> entry : auctionItems.entrySet()) {
                    AuctionItem item = entry.getValue();
                    String remainingTime = item.getRemainingTime(); // Get remaining time for the auction
                    out.println(entry.getKey() + " | ₱" + item.getBidPrice() + " | " + item.getBidder() + " | " + remainingTime);
                }
                out.println("END"); // End of item list
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addAuctionItem(String itemName, int startPrice, int auctionTime) {
            if (!auctionItems.containsKey(itemName)) {
                AuctionItem newItem = new AuctionItem(startPrice, "none", auctionTime * 60000); // Convert minutes to milliseconds
                auctionItems.put(itemName, newItem);
                System.out.println("New item added to auction: " + itemName);
                sendAuctionItems(); // Send the updated item list to all clients
            } else {
                out.println("Item already exists: " + itemName);
            }
        }
    }

    static class AuctionItem {
        private int bidPrice;
        private String bidder;
        private long auctionEndTime;
        private boolean resultSaved = false; // Track whether result has been saved

        public AuctionItem(int bidPrice, String bidder, long auctionDurationInMillis) {
            this.bidPrice = bidPrice;
            this.bidder = bidder;
            this.auctionEndTime = System.currentTimeMillis() + auctionDurationInMillis;
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

        public long getAuctionEndTime() {
            return auctionEndTime;
        }

        public String getRemainingTime() {
            long remainingTime = auctionEndTime - System.currentTimeMillis();
            if (remainingTime <= 0) {
                return "Auction Ended";
            }
            return String.format("%02d:%02d", (remainingTime / 60000), (remainingTime / 1000) % 60);
        }

        public boolean isAuctionOver() {
            return System.currentTimeMillis() >= auctionEndTime;
        }

        public boolean isResultSaved() {
            return resultSaved;
        }

        public void setResultSaved(boolean resultSaved) {
            this.resultSaved = resultSaved;
        }
    }
}
