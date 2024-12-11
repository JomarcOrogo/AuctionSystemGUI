import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AuctionResultSaver {
    private static final String AUCTION_RESULTS_FOLDER = "AuctionBidderFolder"; // Folder for saving auction results

    static {
        // Create the auction results folder if it doesn't exist
        File folder = new File(AUCTION_RESULTS_FOLDER);
        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    public static void saveAuctionResult(String itemName, String winningBidder, int finalBid) {
        // Construct the filename
        String fileName = AUCTION_RESULTS_FOLDER + File.separator + itemName + "_winner.txt";
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName))) {
            writer.write("Item: " + itemName + "\n");
            writer.write("Winning Bidder: " + winningBidder + "\n");
            writer.write("Final Bid: ₱" + finalBid + "\n");
            writer.write("Congratulations for Winning!" + "\n");
            System.out.println("Auction result saved: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
