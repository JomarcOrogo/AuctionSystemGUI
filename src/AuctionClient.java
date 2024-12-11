import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

//Jomarc Orogo

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
    private UserAuthentication userAuth;
    private String loggedInUser;

    public AuctionClient() {
        userAuth = new UserAuthentication();
        setTitle("Auction Client");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Show login screen before initializing the auction interface
        if (!login()) {
            JOptionPane.showMessageDialog(this, "Login failed. Closing application.");
            System.exit(0);
        }

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
        bidderInputField = new JTextField(loggedInUser); // Set the logged-in user's name
        bidderInputField.setEditable(false); // Disable editing
        inputPanel.add(bidderInputField);

        JButton bidButton = new JButton("Place Bid");
        bidButton.addActionListener(e -> placeBid());
        inputPanel.add(bidButton);

        add(inputPanel, BorderLayout.SOUTH);

        connectToServer();
        startItemRefresh();
    }

    private boolean login() {
        while (true) {
            String username = JOptionPane.showInputDialog(this, "Enter Username:");
            String password = JOptionPane.showInputDialog(this, "Enter Password:");
            if (username == null || password == null) return false; // User canceled

            if (userAuth.authenticate(username, password)) {
                loggedInUser = username;
                JOptionPane.showMessageDialog(this, "Login successful! Welcome, " + username);
                return true;
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password. Try again.");
            }
        }
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

        if (item.isEmpty() || bid.isEmpty()) {
            appendToDisplay("Item and bid amount must not be empty.\n");
            return;
        }

        try {
            int bidValue = Integer.parseInt(bid);
            out.println("BID:" + item + ":" + bidValue + ":" + loggedInUser);
            appendToDisplay("Placed bid on " + item + " for ₱" + bidValue + " by " + loggedInUser + "\n");
        } catch (NumberFormatException e) {
            appendToDisplay("Invalid bid amount.\n");
        }
    }

    private void startItemRefresh() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::viewItems, 0, 3, TimeUnit.SECONDS);
    }

    private void viewItems() {
        out.println("VIEW");
        try {
            StringBuilder itemList = new StringBuilder("Auction Items:\n");
            String response;

            while ((response = in.readLine()) != null) {
                if (response.equals("END")) break;
                itemList.append(response).append("\n");
            }

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

