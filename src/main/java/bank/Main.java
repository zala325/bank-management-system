package bank;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Scanner;
import org.bson.Document;

/** Menu screen of the Bank Management System. */
public class Main {

    private static final Scanner in = new Scanner(System.in);
    private static BankService bank;

    public static void main(String[] args) {
        try {
            bank = new BankService(); // also tests the database connection
        } catch (Exception e) {
            System.out.println("Cannot connect to MongoDB.");
            System.out.println("Please start MongoDB (port 27017) and run again.");
            DbConnection.close();
            return;
        }

        boolean running = true;
        while (running) {
            printMenu();
            String choice = ask("Choose (1-9): ");
            try {
                switch (choice) {
                    case "1" -> createAccount();
                    case "2" -> viewAccount();
                    case "3" -> listAccounts();
                    case "4" -> deposit();
                    case "5" -> withdraw();
                    case "6" -> transfer();
                    case "7" -> history();
                    case "8" -> deleteAccount();
                    case "9" -> running = false;
                    default -> System.out.println("Please enter a number from 1 to 9.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Something went wrong: " + e.getMessage());
            }
        }

        DbConnection.close();
        System.out.println("Thank you. Goodbye!");
    }

    // ---------- Menu actions ----------

    private static void printMenu() {
        System.out.println();
        System.out.println("===== BANK MANAGEMENT SYSTEM =====");
        System.out.println("1. Create account");
        System.out.println("2. View account");
        System.out.println("3. List all accounts");
        System.out.println("4. Deposit money");
        System.out.println("5. Withdraw money");
        System.out.println("6. Transfer money");
        System.out.println("7. Transaction history");
        System.out.println("8. Delete account");
        System.out.println("9. Exit");
    }

    private static void createAccount() {
        String name = ask("Customer name: ");
        String type = ask("Account type (1 = Savings, 2 = Current): ").equals("2")
                ? "Current" : "Savings";
        double amount = askAmount("Opening deposit (0 for none): ");
        Document acc = bank.createAccount(name, type, amount);
        System.out.println("Account created! Account number: " + acc.getString("accountNumber"));
    }

    private static void viewAccount() {
        Document acc = bank.findAccount(ask("Account number: ").toUpperCase());
        if (acc == null) {
            throw new IllegalArgumentException("Account not found.");
        }
        printAccount(acc);
    }

    private static void listAccounts() {
        List<Document> all = bank.listAccounts();
        if (all.isEmpty()) {
            System.out.println("No accounts yet.");
            return;
        }
        System.out.printf("%-10s %-20s %-10s %12s%n", "Number", "Name", "Type", "Balance");
        System.out.println("-------------------------------------------------------");
        for (Document a : all) {
            System.out.printf("%-10s %-20s %-10s %12.2f%n",
                    a.getString("accountNumber"), a.getString("name"),
                    a.getString("accountType"), BankService.balanceOf(a));
        }
    }

    private static void deposit() {
        String number = ask("Account number: ").toUpperCase();
        double amount = askAmount("Amount to deposit: ");
        Document acc = bank.deposit(number, amount);
        System.out.printf("Done. New balance: %.2f%n", BankService.balanceOf(acc));
    }

    private static void withdraw() {
        String number = ask("Account number: ").toUpperCase();
        double amount = askAmount("Amount to withdraw: ");
        Document acc = bank.withdraw(number, amount);
        System.out.printf("Done. New balance: %.2f%n", BankService.balanceOf(acc));
    }

    private static void transfer() {
        String from = ask("From account number: ").toUpperCase();
        String to = ask("To account number: ").toUpperCase();
        double amount = askAmount("Amount to transfer: ");
        bank.transfer(from, to, amount);
        System.out.println("Transfer successful.");
    }

    private static void history() {
        String number = ask("Account number: ").toUpperCase();
        if (bank.findAccount(number) == null) {
            throw new IllegalArgumentException("Account not found.");
        }
        List<Document> list = bank.history(number);
        if (list.isEmpty()) {
            System.out.println("No transactions yet.");
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        System.out.println("Last 20 transactions (newest first):");
        for (Document t : list) {
            System.out.printf("%s | %-8s | %10.2f | Balance: %10.2f | %s%n",
                    fmt.format(t.getDate("date")), t.getString("type"),
                    ((Number) t.get("amount")).doubleValue(),
                    ((Number) t.get("balanceAfter")).doubleValue(),
                    t.getString("note"));
        }
    }

    private static void deleteAccount() {
        String number = ask("Account number to delete: ").toUpperCase();
        String sure = ask("Type YES to confirm delete: ");
        if (!sure.equals("YES")) {
            System.out.println("Cancelled.");
            return;
        }
        bank.deleteAccount(number);
        System.out.println("Account deleted.");
    }

    // ---------- Small helpers ----------

    private static void printAccount(Document a) {
        System.out.println("Account number : " + a.getString("accountNumber"));
        System.out.println("Name           : " + a.getString("name"));
        System.out.println("Type           : " + a.getString("accountType"));
        System.out.printf("Balance        : %.2f%n", BankService.balanceOf(a));
    }

    private static String ask(String text) {
        System.out.print(text);
        return in.nextLine().trim();
    }

    private static double askAmount(String text) {
        String value = ask(text);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid number.");
        }
    }
}
