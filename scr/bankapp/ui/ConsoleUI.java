package bankapp.ui;

import bankapp.exceptions.BankException;
import bankapp.model.Account;
import bankapp.model.AccountType;
import bankapp.persistence.SerializationPersistence;
import bankapp.security.AuthProvider;
import bankapp.security.AuthProvider_2;
import bankapp.service.BankService;
import bankapp.service.BankServiceImpl;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {
    private final Scanner sc = new Scanner(System.in);
    private BankServiceImpl bank;
    private final AuthProvider auth = new AuthProvider_2();
    private final SerializationPersistence persistence = new SerializationPersistence();
    private final String saveFile = "bankdata-secure.ser";

    public ConsoleUI() {
        try {
            BankServiceImpl loaded = BankServiceImpl.loadFrom(saveFile, auth, persistence);
            if (loaded != null) {
                bank = loaded;
                bank.setPersistence(persistence);
            } else {
                bank = new BankServiceImpl(auth);
                bank.setAdminPassword("admin123");
                bank.setPersistence(persistence);
            }
        } catch (IOException | ClassNotFoundException e) {
            bank = new BankServiceImpl(auth);
            bank.setAdminPassword("admin123");
            bank.setPersistence(persistence);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                bank.saveTo(saveFile);
                System.out.println("\n[Auto-save] State saved to " + saveFile);
            } catch (IOException ignored) {
            }
        }));
    }

    public void start() {
        banner("SimpleJavaBank - Secure Refactor");
        mainLoop: while (true) {
            System.out.println("\n=== Main Menu ===");
            System.out.println("1. Create account");
            System.out.println("2. Login to account");
            System.out.println("3. Admin login");
            System.out.println("4. Save data now");
            System.out.println("5. Load data from file");
            System.out.println("6. Exit ");
            System.out.print("Choose: ");
            String c = sc.nextLine().trim();
            switch (c) {
                case "1":
                    createAccountFlow();
                    break;
                case "2":
                    loginFlow();
                    break;
                case "3":
                    adminFlow();
                    break;
                case "4":
                    saveNow();
                    break;
                case "5":
                    loadNow();
                    break;
                case "6":
                    askSaveAndExit();
                    break mainLoop;
                default:
                    System.out.println("Invalid.");
            }
        }
    }

    private void createAccountFlow() {
        try {
            System.out.println("\n--- Create Account ---");
            System.out.print("Owner full name: ");
            String owner = nonEmpty();
            System.out.println("Account type: 1. SAVINGS  2. CURRENT");
            AccountType type;
            while (true) {
                System.out.print("Choose (1/2): ");
                String s = sc.nextLine().trim();
                if (s.equals("1")) {
                    type = AccountType.SAVINGS;
                    break;
                }
                if (s.equals("2")) {
                    type = AccountType.CURRENT;
                    break;
                }
                System.out.println("Invalid.");
            }
            String pin;
            while (true) {
                System.out.print("Set 4-digit PIN: ");
                pin = sc.nextLine().trim();
                if (pin.matches("\\d{4}"))
                    break;
                System.out.println("PIN must be 4 digits.");
            }
            double opening;
            while (true) {
                System.out.print("Opening deposit (min " + format(bank.getMinOpeningDeposit()) + "): ");
                try {
                    opening = Double.parseDouble(sc.nextLine().trim());
                    if (opening < bank.getMinOpeningDeposit())
                        System.out.println("Too small.");
                    else
                        break;
                } catch (Exception e) {
                    System.out.println("Enter number.");
                }
            }
            Account acc = bank.createAccount(owner, type, pin, opening);
            System.out.println(
                    "Created. AccountNo: " + acc.getAccountNumber() + " | Balance: " + format(acc.getBalance()));
            saveNow();
        } catch (BankException be) {
            System.out.println("Create failed: " + be.getMessage());
        }
    }

    private void loginFlow() {
        System.out.println("\n--- Account Login ---");
        System.out.print("Account number: ");
        try {
            long accNo = Long.parseLong(sc.nextLine().trim());
            System.out.print("PIN: ");
            String pin = sc.nextLine().trim();
            Account acc = bank.getAccount(accNo);
            if (acc == null) {
                System.out.println("Not found.");
                return;
            }
            var salt = java.util.Base64.getDecoder().decode(acc.getPinSaltBase64());
            if (!auth.verifyPin(pin, acc.getPinHashBase64(), salt)) {
                System.out.println("Auth failed.");
                return;
            }
            if (!acc.isActive()) {
                System.out.println("Frozen.");
                return;
            }
            accountMenu(acc);
        } catch (NumberFormatException e) {
            System.out.println("Invalid account number.");
        }
    }

    private void accountMenu(Account acc) {
        accountLoop: while (true) {
            System.out.println("\n=== Account Menu (" + acc.getAccountNumber() + ") ===");
            System.out.println("1. View balance");
            System.out.println("2. Mini statement (last 10)");
            System.out.println("3. Deposit");
            System.out.println("4. Withdraw");
            System.out.println("5. Transfer");
            System.out.println("6. Export CSV");
            System.out.println("7. Change PIN");
            System.out.println("8. Logout");
            System.out.print("Choose: ");
            String ch = sc.nextLine().trim();
            try {
                switch (ch) {
                    case "1":
                        System.out.println("Balance: " + format(acc.getBalance()));
                        break;
                    case "2":
                        printMini(acc);
                        break;
                    case "3":
                        depositFlow(acc);
                        saveNow();
                        break;
                    case "4":
                        withdrawFlow(acc);
                        saveNow();
                        break;
                    case "5":
                        transferFlow(acc);
                        saveNow();
                        break;
                    case "6":
                        exportAccCSV(acc);
                        break;
                    case "7":
                        changePin(acc);
                        saveNow();
                        break;
                    case "8":
                        System.out.println("Logged out.");
                        break accountLoop;
                    default:
                        System.out.println("Invalid.");
                }
            } catch (BankException be) {
                System.out.println("Error: " + be.getMessage());
            }
        }
    }

    private void adminFlow() {
        System.out.println("\n--- Admin Login ---");
        System.out.print("User: ");
        String u = sc.nextLine().trim();
        System.out.print("Pass: ");
        String p = sc.nextLine().trim();
        if (!bank.isAdminCredentials(u, p)) {
            System.out.println("Auth failed.");
            return;
        }
        adminLoop: while (true) {
            System.out.println("\n=== Admin Menu ===");
            System.out.println("1. List accounts");
            System.out.println("2. Search account");
            System.out.println("3. Freeze/unfreeze");
            System.out.println("4. Reverse tx");
            System.out.println("5. Simple report");
            System.out.println("6. Logout");
            System.out.print("Choose: ");
            String c = sc.nextLine().trim();
            switch (c) {
                case "1":
                    listAll();
                    break;
                case "2":
                    search();
                    break;
                case "3":
                    toggleFreeze();
                    saveNow();
                    break;
                case "4":
                    reverseTx();
                    saveNow();
                    break;
                case "5":
                    simpleReport();
                    break;
                case "6":
                    System.out.println("Admin out.");
                    break adminLoop;
                default:
                    System.out.println("Invalid.");
            }
        }
    }

    private void depositFlow(Account acc) throws BankException {
        System.out.print("Amount: ");
        double amt = Double.parseDouble(sc.nextLine().trim());
        System.out.print("Narration: ");
        String note = sc.nextLine().trim();
        bank.deposit(acc.getAccountNumber(), amt, note.isEmpty() ? "Deposit" : note);
        System.out.println("Deposited. New balance: " + format(acc.getBalance()));
    }

    private void withdrawFlow(Account acc) throws BankException {
        System.out.print("Amount: ");
        double amt = Double.parseDouble(sc.nextLine().trim());
        System.out.print("Narration: ");
        String note = sc.nextLine().trim();
        bank.withdraw(acc.getAccountNumber(), amt, note.isEmpty() ? "Withdrawal" : note);
        System.out.println("Withdrawn. New balance: " + format(acc.getBalance()));
    }

    private void transferFlow(Account acc) throws BankException {
        System.out.print("To account: ");
        long to = Long.parseLong(sc.nextLine().trim());
        System.out.print("Amount: ");
        double amt = Double.parseDouble(sc.nextLine().trim());
        System.out.print("Narration: ");
        String note = sc.nextLine().trim();
        bank.transfer(acc.getAccountNumber(), to, amt, note.isEmpty() ? "Transfer" : note);
        System.out.println("Transfer done. New balance: " + format(acc.getBalance()));
    }

    private void exportAccCSV(Account acc) {
        System.out.print("Filename: ");
        String f = sc.nextLine().trim();
        if (f.isEmpty())
            f = "account_" + acc.getAccountNumber() + ".csv";
        try {
            acc.exportToCSV(f);
            System.out.println("Exported to " + f);
        } catch (IOException e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    private void changePin(Account acc) {
        System.out.print("Current PIN: ");
        String cur = sc.nextLine().trim();
        var salt = java.util.Base64.getDecoder().decode(acc.getPinSaltBase64());
        if (!auth.verifyPin(cur, acc.getPinHashBase64(), salt)) {
            System.out.println("Wrong PIN.");
            return;
        }
        System.out.print("New 4-digit PIN: ");
        String np = sc.nextLine().trim();
        if (!np.matches("\\d{4}")) {
            System.out.println("PIN must be 4 digits.");
            return;
        }
        var newsalt = auth.generateSalt();
        var newHash = auth.hashPin(np, newsalt);
        acc.setPin(newHash, java.util.Base64.getEncoder().encodeToString(newsalt));
        System.out.println("PIN changed.");
    }

    private void listAll() {
        List<Account> list = bank.listAccounts();
        if (list.isEmpty()) {
            System.out.println("No accounts.");
            return;
        }
        System.out.printf("%-15s %-25s %-10s %-12s %-6s%n", "AccountNo", "Owner", "Type", "Balance", "Active");
        for (var a : list) {
            System.out.printf("%-15d %-25s %-10s %-12.2f %-6s%n", a.getAccountNumber(), shrink(a.getOwnerName(), 24),
                    a.getType(), a.getBalance(), a.isActive() ? "Yes" : "No");
        }
    }

    private void search() {
        System.out.print("Enter acc no or owner: ");
        String q = sc.nextLine().trim();
        try {
            long accNo = Long.parseLong(q);
            Account a = bank.getAccount(accNo);
            if (a == null)
                System.out.println("Not found.");
            else
                printAccount(a);
        } catch (NumberFormatException e) {
            List<Account> found = bank.searchByOwner(q);
            if (found.isEmpty())
                System.out.println("No matches.");
            else
                for (var a : found)
                    printAccount(a);
        }
    }

    private void printAccount(Account a) {
        System.out.println("Account: " + a.getAccountNumber());
        System.out.println("Owner: " + a.getOwnerName());
        System.out.println("Type: " + a.getType());
        System.out.println("Balance: " + format(a.getBalance()));
        System.out.println("Active: " + a.isActive());
        System.out.println("Created: " + a.getCreatedAt().toString());
    }

    private void toggleFreeze() {
        System.out.print("Account number: ");
        try {
            long accNo = Long.parseLong(sc.nextLine().trim());
            Account a = bank.getAccount(accNo);
            if (a == null) {
                System.out.println("Not found.");
                return;
            }
            a.setActive(!a.isActive());
            System.out.println("Now active = " + a.isActive());
        } catch (NumberFormatException e) {
            System.out.println("Invalid.");
        }
    }

    private void reverseTx() {
        System.out.print("Account number: ");
        try {
            long accNo = Long.parseLong(sc.nextLine().trim());
            System.out.print("Transaction ID: ");
            String tx = sc.nextLine().trim();
            try {
                bank.reverseTransaction(accNo, tx);
                System.out.println("Reversed (if reversible).");
            } catch (BankException be) {
                System.out.println("Reverse failed: " + be.getMessage());
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid.");
        }
    }

    private void simpleReport() {
        System.out.println("Total accounts: " + bank.totalAccounts());
        System.out.println("Total balances: " + format(bank.totalBalances()));
        System.out.println("Active accounts: " + bank.countActiveAccounts());
    }

    private void saveNow() {
        try {
            bank.saveTo(saveFile);
            System.out.println("Saved to " + saveFile);
        } catch (IOException e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    private void loadNow() {
        try {
            var loaded = BankServiceImpl.loadFrom(saveFile, auth, persistence);
            if (loaded != null) {
                bank = loaded;
                bank.setPersistence(persistence);
                System.out.println("Loaded from " + saveFile);
            } else {
                System.out.println("No saved file found.");
            }
        } catch (Exception e) {
            System.out.println("Load failed: " + e.getMessage());
        }
    }

    private void askSaveAndExit() {
        System.out.print("Save before exit? (y/n): ");
        if (sc.nextLine().trim().equalsIgnoreCase("y"))
            saveNow();
        System.out.println("Bye.");
    }

    /* Utilities */

    private String nonEmpty() {
        while (true) {
            String s = sc.nextLine().trim();
            if (!s.isEmpty())
                return s;
            System.out.print("Cannot be empty. Enter again: ");
        }
    }

    private String shrink(String s, int len) {
        if (s == null)
            return "";
        if (s.length() <= len)
            return s;
        return s.substring(0, len - 3) + "...";
    }

    private String format(double amt) {
        var df = new DecimalFormat("#,##0.00");
        return df.format(amt);
    }

    private void printMini(Account acc) {
        List<bankapp.model.Transaction> list = acc.getLastNTransactions(10);
        if (list.isEmpty()) {
            System.out.println("No txns.");
            return;
        }
        System.out.printf("%-20s %-12s %-10s %-12s %s%n", "Timestamp", "Type", "Amount", "Balance", "Narration");
        for (var t : list)
            System.out.printf("%-20s %-12s %-10.2f %-12.2f %s%n", t.getTimestamp().toString().substring(0, 16),
                    t.getType(), t.getAmount(), t.getBalanceAfter(), shrink(t.getNarration(), 30));
    }

    private void banner(String t) {
        System.out.println("======================================");
        System.out.println(t);
        System.out.println("======================================");
    }
}