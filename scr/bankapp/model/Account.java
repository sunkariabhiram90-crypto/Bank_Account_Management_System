package bankapp.model;

import bankapp.exceptions.BankException;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Account implements Serializable {
    private static final long serialVersionUID = 2L;

    private final long accountNumber;
    private final String ownerName;
    private final AccountType type;
    private double balance;
    private String pinHashBase64;
    private String pinSaltBase64;
    private boolean active = true;
    private final List<Transaction> transactions = new ArrayList<>();
    private final LocalDateTime createdAt;

    public Account(long accountNumber, String ownerName, AccountType type, String pinHashBase64, String pinSaltBase64) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.type = type;
        this.pinHashBase64 = pinHashBase64;
        this.pinSaltBase64 = pinSaltBase64;
        this.balance = 0.0;
        this.createdAt = LocalDateTime.now();
    }

    public synchronized void deposit(double amount, String narration) {
        amount = round2(amount);
        balance = round2(balance + amount);
        Transaction t = new Transaction(TransactionType.DEPOSIT, amount, balance, narration);
        transactions.add(t);
    }

    public synchronized void withdraw(double amount, String narration) throws BankException {
        amount = round2(amount);
        if (amount > balance)
            throw new BankException("Insufficient funds");
        balance = round2(balance - amount);
        Transaction t = new Transaction(TransactionType.WITHDRAWAL, amount, balance, narration);
        transactions.add(t);
    }

    synchronized void addTransactionInternal(Transaction t) {
        transactions.add(t);
        this.balance = t.getBalanceAfter();
    }

    public Transaction findTransaction(String txId) {
        for (Transaction t : transactions)
            if (t.getTxId().equals(txId))
                return t;
        return null;
    }

    public List<Transaction> getLastNTransactions(int n) {
        int size = transactions.size();
        if (size == 0)
            return Collections.emptyList();
        int from = Math.max(0, size - n);
        return new ArrayList<>(transactions.subList(from, size));
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    public double getBalance() {
        return round2(balance);
    }

    public long getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public AccountType getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean v) {
        this.active = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getPinHashBase64() {
        return pinHashBase64;
    }

    public String getPinSaltBase64() {
        return pinSaltBase64;
    }

    public void setPin(String newHashBase64, String newSaltBase64) {
        this.pinHashBase64 = newHashBase64;
        this.pinSaltBase64 = newSaltBase64;
    }

    public double withdrawnToday() {
        var startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        double sum = 0.0;
        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.WITHDRAWAL && t.getTimestamp().isAfter(startOfDay))
                sum += t.getAmount();
        }
        return round2(sum);
    }

    public void exportToCSV(String filename) throws IOException {
        Path path = Paths.get(filename);
        try (var writer = Files.newBufferedWriter(path)) {
            writer.write("txId,timestamp,type,amount,balanceAfter,narration");
            writer.newLine();
            for (Transaction t : transactions) {
                String safeNarr = "\"" + t.getNarration().replace("\"", "\"\"") + "\"";
                String line = String.format("%s,%s,%s,%.2f,%.2f,%s",
                        t.getTxId(), t.getTimestamp().toString(), t.getType(), t.getAmount(), t.getBalanceAfter(),
                        safeNarr);
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
