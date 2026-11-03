package bank;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.bson.Document;

/**
 * All bank work happens here.
 *
 * Two collections (like tables) are used:
 *   accounts     -> one document per customer account
 *   transactions -> one document per deposit / withdrawal / transfer
 */
public class BankService {

    private final MongoCollection<Document> accounts;
    private final MongoCollection<Document> transactions;
    private final Random random = new Random();

    public BankService() {
        MongoDatabase db = DbConnection.getDatabase();
        accounts = db.getCollection("accounts");
        transactions = db.getCollection("transactions");

        // No two accounts can have the same account number
        accounts.createIndex(Indexes.ascending("accountNumber"),
                new IndexOptions().unique(true));
        transactions.createIndex(Indexes.ascending("accountNumber"));
    }

    // ---------- Create / Read ----------

    public Document createAccount(String name, String type, double initialDeposit) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (initialDeposit < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative.");
        }
        initialDeposit = round(initialDeposit);

        String number = newAccountNumber();
        Document account = new Document("accountNumber", number)
                .append("name", name.trim())
                .append("accountType", type)
                .append("balance", initialDeposit)
                .append("createdAt", new Date());
        accounts.insertOne(account);

        if (initialDeposit > 0) {
            logTransaction(number, "DEPOSIT", initialDeposit, initialDeposit, "Opening deposit");
        }
        return account;
    }

    public Document findAccount(String number) {
        return accounts.find(eq("accountNumber", number)).first();
    }

    public List<Document> listAccounts() {
        return accounts.find().sort(Sorts.ascending("accountNumber")).into(new ArrayList<>());
    }

    public List<Document> history(String number) {
        return transactions.find(eq("accountNumber", number))
                .sort(Sorts.descending("date"))
                .limit(20)
                .into(new ArrayList<>());
    }

    // ---------- Update ----------

    public void updateName(String number, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        long changed = accounts.updateOne(eq("accountNumber", number),
                set("name", newName.trim())).getMatchedCount();
        if (changed == 0) {
            throw new IllegalArgumentException("Account not found.");
        }
    }

    public Document deposit(String number, double amount) {
        return doDeposit(number, checkAmount(amount), "Cash deposit");
    }

    public Document withdraw(String number, double amount) {
        return doWithdraw(number, checkAmount(amount), "Cash withdrawal");
    }

    public void transfer(String from, String to, double amount) {
        amount = checkAmount(amount);
        if (from.equals(to)) {
            throw new IllegalArgumentException("Cannot transfer to the same account.");
        }
        if (findAccount(to) == null) {
            throw new IllegalArgumentException("Receiver account not found.");
        }

        doWithdraw(from, amount, "Transfer to " + to);
        try {
            doDeposit(to, amount, "Transfer from " + from);
        } catch (RuntimeException e) {
            // Something went wrong: give the money back to the sender
            doDeposit(from, amount, "Refund (transfer failed)");
            throw new IllegalArgumentException("Transfer failed. Money was returned.");
        }
    }

    // ---------- Delete ----------

    public void deleteAccount(String number) {
        long deleted = accounts.deleteOne(eq("accountNumber", number)).getDeletedCount();
        if (deleted == 0) {
            throw new IllegalArgumentException("Account not found.");
        }
    }

    // ---------- Helpers ----------

    private Document doDeposit(String number, double amount, String note) {
        Document updated = accounts.findOneAndUpdate(
                eq("accountNumber", number),
                inc("balance", amount),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
        if (updated == null) {
            throw new IllegalArgumentException("Account not found.");
        }
        logTransaction(number, "DEPOSIT", amount, balanceOf(updated), note);
        return updated;
    }

    private Document doWithdraw(String number, double amount, String note) {
        // The filter "balance >= amount" makes sure the balance never goes below zero
        Document updated = accounts.findOneAndUpdate(
                and(eq("accountNumber", number), gte("balance", amount)),
                inc("balance", -amount),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
        if (updated == null) {
            if (findAccount(number) == null) {
                throw new IllegalArgumentException("Account not found.");
            }
            throw new IllegalArgumentException("Not enough balance.");
        }
        logTransaction(number, "WITHDRAW", amount, balanceOf(updated), note);
        return updated;
    }

    private void logTransaction(String number, String type, double amount,
                                double balanceAfter, String note) {
        transactions.insertOne(new Document("accountNumber", number)
                .append("type", type)
                .append("amount", amount)
                .append("balanceAfter", balanceAfter)
                .append("note", note)
                .append("date", new Date()));
    }

    public static double balanceOf(Document account) {
        return ((Number) account.get("balance")).doubleValue();
    }

    private double checkAmount(double amount) {
        amount = round(amount);
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be more than 0.");
        }
        return amount;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String newAccountNumber() {
        String number;
        do {
            number = "AC" + (100000 + random.nextInt(900000));
        } while (findAccount(number) != null);
        return number;
    }
}
