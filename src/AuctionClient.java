import javax.swing.*;
import javax.swing.table.DefaultTableModel;
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
    private JTable itemTable;
    private DefaultTableModel tableModel;
    private JButton bidButton;
    private UserAuthentication userAuth;
    private String loggedInUser;

    public AuctionClient() {
        userAuth = new UserAuthentication();
        setTitle("Auction Client");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        if (!login()) {
            JOptionPane.showMessageDialog(this, "Login failed. Closing application.");
            System.exit(0);
        }

        // Table for auction items
        tableModel = new DefaultTableModel(new String[]{"Item", "Price (₱)", "Bidder", "Time Left"}, 0);
        itemTable = new JTable(tableModel);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(itemTable), BorderLayout.CENTER);

        // Bid button
        bidButton = new JButton("Bid");
        bidButton.addActionListener(e -> handleBid());
        JPanel bidPanel = new JPanel();
        bidPanel.add(bidButton);
        add(bidPanel, BorderLayout.SOUTH);

        connectToServer();
        startItemRefresh();
    }

    private boolean login() {
        while (true) {
            String username = JOptionPane.showInputDialog(this, "Enter Username:");
            String password = JOptionPane.showInputDialog(this, "Enter Password:");
            if (username == null || password == null) return false;

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

    private void handleBid() {
        int selectedRow = itemTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to bid on.");
            return;
        }

        String item = tableModel.getValueAt(selectedRow, 0).toString();
        String bidInput = JOptionPane.showInputDialog(this, "Enter your bid for " + item + " (₱):");
        if (bidInput == null || bidInput.trim().isEmpty()) {
            return;
        }

        try {
            int bidValue = Integer.parseInt(bidInput.trim());
            out.println("BID:" + item + ":" + bidValue + ":" + loggedInUser);
            appendToDisplay("Placed bid on " + item + " for ₱" + bidValue + " by " + loggedInUser + "\n");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid bid amount.");
        }
    }

    private void startItemRefresh() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::viewItems, 0, 3, TimeUnit.SECONDS);
    }

    private void viewItems() {
        out.println("VIEW");
        try {
            String response;
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0)); // Clear the table

            while ((response = in.readLine()) != null) {
                if (response.equals("END")) break;

                String[] parts = response.split("\\|");
                String item = parts[0].trim();
                String price = parts[1].trim();
                String bidder = parts[2].trim();
                String remainingTime = parts[3].trim(); // Get remaining time

                SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{item, price, bidder, remainingTime}));
            }
        } catch (IOException e) {
            appendToDisplay("Error retrieving items.\n");
        }
    }

    private void appendToDisplay(String message) {
        System.out.println(message); // Log to console for debugging
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AuctionClient().setVisible(true));
    }
}
