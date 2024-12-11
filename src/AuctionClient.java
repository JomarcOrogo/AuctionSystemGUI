import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class AuctionClient extends JFrame {
    private static final String SERVER_IP = "192.168.2.101";
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private JTextArea itemDisplayArea;
    private JTextField bidInputField;
    private JTextField itemInputField;
    private JTextField bidderInputField;

    public AuctionClient() {
        setTitle("Auction Client");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Display area for auction items
        itemDisplayArea = new JTextArea();
        itemDisplayArea.setEditable(false);
        add(new JScrollPane(itemDisplayArea), BorderLayout.CENTER);

        // Input panel for placing bids
        JPanel inputPanel = new JPanel(new GridLayout(4, 2));
        inputPanel.add(new JLabel("Item:"));
        itemInputField = new JTextField();
        inputPanel.add(itemInputField);
        inputPanel.add(new JLabel("Bid (₱):"));
        bidInputField = new JTextField();
        inputPanel.add(bidInputField);
        inputPanel.add(new JLabel("Your Name:"));
        bidderInputField = new JTextField();
        inputPanel.add(bidderInputField);

        JButton bidButton = new JButton("Place Bid");
        bidButton.addActionListener(e -> placeBid());
        inputPanel.add(bidButton);

        add(inputPanel, BorderLayout.SOUTH);

        connectToServer();
        startItemRefresh();
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            appendToDisplay("Connected to server at " + SERVER_IP + "\n");
        } catch (IOException e) {
            appendToDisplay("Failed to connect to server.\n");
        }
    }

    private void placeBid() {
        String item = itemInputField.getText().trim();
        String bid = bidInputField.getText().trim();
        String bidder = bidderInputField.getText().trim();

        if (item.isEmpty() || bid.isEmpty() || bidder.isEmpty()) {
            appendToDisplay("Item, bid amount, and bidder name must not be empty.\n");
            return;
        }

        try {
            int bidValue = Integer.parseInt(bid);
            out.println("BID:" + item + ":" + bidValue + ":" + bidder);
            appendToDisplay("Placed bid on " + item + " for ₱" + bidValue + " by " + bidder + "\n");
        } catch (NumberFormatException e) {
            appendToDisplay("Invalid bid amount.\n");
        }
    }

    private void startItemRefresh() {
        // Periodically refresh the item list
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::viewItems, 0, 3, TimeUnit.SECONDS);
    }

    private void viewItems() {
        out.println("VIEW");
        try {
            StringBuilder itemList = new StringBuilder("Auction Items:\n");
            String response;

            while ((response = in.readLine()) != null) {
                // If server response includes an end signal, break the loop
                if (response.equals("END")) break;
                itemList.append(response).append("\n");
            }

            // Update the display area
            SwingUtilities.invokeLater(() -> itemDisplayArea.setText(itemList.toString()));
        } catch (IOException e) {
            appendToDisplay("Error retrieving items.\n");
        }
    }

    private void appendToDisplay(String message) {
        SwingUtilities.invokeLater(() -> itemDisplayArea.append(message + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AuctionClient().setVisible(true));
    }
}
