import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;

public class AuctionClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private JFrame frame;
    private JTextArea updatesArea;
    private JComboBox<String> itemDropdown;
    private JTextField bidField, nameField;
    private JButton bidButton;

    private ObjectOutputStream out;

    public AuctionClient () {
        setupGUI();
        connectToServer();
    }

    private void setupGUI() {
        frame = new JFrame("Auction Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setLayout(new BorderLayout());

        updatesArea = new JTextArea();
        updatesArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(updatesArea);

        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Place Your Bid"));

        inputPanel.add(new JLabel("Select Item:"));
        itemDropdown = new JComboBox<>(new String[]{"-- Select Item --"});
        inputPanel.add(itemDropdown);

        inputPanel.add(new JLabel("Bid Amount (₱):"));
        bidField = new JTextField();
        inputPanel.add(bidField);

        inputPanel.add(new JLabel("Your Name:"));
        nameField = new JTextField();
        inputPanel.add(nameField);

        bidButton = new JButton("Place Bid");
        inputPanel.add(bidButton);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Initially disable bid fields and button
        bidField.setEnabled(false);
        nameField.setEnabled(false);
        bidButton.setEnabled(false);

        frame.setVisible(true);

        // Add action listener to the dropdown
        itemDropdown.addActionListener(e -> {
            boolean isItemSelected = itemDropdown.getSelectedIndex() > 0;
            bidField.setEnabled(isItemSelected);
            nameField.setEnabled(isItemSelected);
            bidButton.setEnabled(isItemSelected);
        });

        bidButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placeBid();
            }
        });
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            updatesArea.append("Connected to the auction server.\n");

            // Start a thread to listen for updates
            new Thread(() -> {
                try {
                    while (true) {
                        String update = (String) in.readObject();

                        if (update.startsWith("ITEMS:")) {
                            updateItemsDropdown(update.substring(6).split(","));
                        } else {
                            updatesArea.append(update + "\n");
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    updatesArea.append("Disconnected from the server.\n");
                }
            }).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Unable to connect to the server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void updateItemsDropdown(String[] items) {
        SwingUtilities.invokeLater(() -> {
            itemDropdown.removeAllItems();
            itemDropdown.addItem("-- Select Item --");
            for (String item : items) {
                itemDropdown.addItem(item.trim());
            }
        });
    }

    private void placeBid() {
        String item = (String) itemDropdown.getSelectedItem();
        String bidText = bidField.getText().trim();
        String name = nameField.getText().trim();

        if (item == null || item.equals("-- Select Item --") || bidText.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "All fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int bid = Integer.parseInt(bidText);
            String message = item + "," + bid + "," + name;
            out.writeObject(message);
            out.flush();

            updatesArea.append("You placed a bid on " + item + " for ₱" + bid + ".\n");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Bid amount must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            updatesArea.append("Error sending bid to the server.\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AuctionClient::new);
    }
}
