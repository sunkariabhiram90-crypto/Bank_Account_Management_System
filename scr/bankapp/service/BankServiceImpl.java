package bankapp.service;

import bankapp.exceptions.BankException;
import bankapp.model.Account;
import bankapp.model.AccountType;
import bankapp.model.Transaction;
import bankapp.model.TransactionType;
import bankapp.security.AuthProvider;
import bankapp.persistence.Persistence;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Base64;

public class BankServiceImpl implements BankService {
    private static final long serialVersionUID = 4L;

    private final Map<Long, Account> accounts = new HashMap<>();
    private final AtomicLong nextAccount = new AtomicLong(1_000_000_000L);
    private double minOpeningDeposit = 100.0;
    private double minBalanceSavings = 100.0;
    private double minBalanceCurrent = 0.0;
    private double dailyWithdrawalLimit = 50_000.0;
    private String adminUser = "admin";
    private String adminPassHashBase64;
    private String adminSaltBase64;

    // Transient dependencies
    private transient AuthProvider authProvider;
    private transient Persistence persistence;

    public BankServiceImpl(AuthProvider authProvider) {
        if (authProvider == null)
            throw new IllegalArgumentException("AuthProvider required");
        this.authProvider = authProvider;
        var salt = authProvider.generateSalt();
        this.adminSaltBase64 = Base64.getEncoder().encodeToString(salt);
        this.adminPassHashBase64 = authProvider.hashPin("admin123", salt);
    }

    public void initTransients(AuthProvider authProvider, Persistence persistence) {
        if (authProvider == null)
            throw new IllegalArgumentException("AuthProvider required");
        this.authProvider = authProvider;
        this.persistence = persistence;
    }

    @Override
    public synchronized Account createAccount(String owner, AccountType type, String pin, double openingDeposit)
            throws BankException {
        if (owner == null || owner.trim().isEmpty())
            throw new BankException("Owner required");
        if (pin == null || !pin.matches("\\d{4}"))
            throw new BankException("PIN must be 4 digits");
        if (openingDeposit < minOpeningDeposit)
            throw new BankException("Opening deposit below minimum");
        byte[] salt = authProvider.generateSalt();
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hash = authProvider.hashPin(pin, salt);
        long accNo = nextAccount.getAndIncrement();
        Account acc = new Account(accNo, owner.trim(), type, hash, saltB64);
        if (openingDeposit > 0)
            acc.deposit(openingDeposit, "Opening deposit");
        accounts.put(accNo, acc);
        return acc;
    }

    @Override
    public Account getAccount(long accountNumber) {
        return accounts.get(accountNumber);
    }

    @Override
    public synchronized void deposit(long accountNumber, double amount, String narration) throws BankException {
        if (amount <= 0)
            throw new BankException("Amount must be positive");
        Account a = accounts.get(accountNumber);
        if (a == null)
            throw new BankException("Account not found");
        if (!a.isActive())
            throw new BankException("Account frozen");
        a.deposit(amount, narration == null ? "Deposit" : narration);
    }

    @Override
    public synchronized void withdraw(long accountNumber, double amount, String narration) throws BankException {
        if (amount <= 0)
            throw new BankException("Amount must be positive");
        Account a = accounts.get(accountNumber);
        if (a == null)
            throw new BankException("Account not found");
        if (!a.isActive())
            throw new BankException("Account frozen");
        double withdrawn = a.withdrawnToday();
        if (withdrawn + amount > dailyWithdrawalLimit)
            throw new BankException("Daily withdrawal limit exceeded");
        double minBal = (a.getType() == AccountType.SAVINGS) ? minBalanceSavings : minBalanceCurrent;
        if (a.getBalance() - amount < minBal)
            throw new BankException("Insufficient funds to maintain minimum balance");
        a.withdraw(amount, narration == null ? "Withdrawal" : narration);
    }

    @Override
    public synchronized void transfer(long fromAcc, long toAcc, double amount, String narration) throws BankException {
        if (amount <= 0)
            throw new BankException("Amount must be positive");
        if (fromAcc == toAcc)
            throw new BankException("Cannot transfer to same account");
        Account from = accounts.get(fromAcc);
        Account to = accounts.get(toAcc);
        if (from == null || to == null)
            throw new BankException("Account not found");
        if (!from.isActive() || !to.isActive())
            throw new BankException("One of the accounts is frozen");
        Account first = (fromAcc < toAcc) ? from : to;
        Account second = (first == from) ? to : from;
        synchronized (first) {
            synchronized (second) {
                double minBal = (from.getType() == AccountType.SAVINGS) ? minBalanceSavings : minBalanceCurrent;
                if (from.getBalance() - amount < minBal)
                    throw new BankException("Insufficient funds");
                double withdrawn = from.withdrawnToday();
                if (withdrawn + amount > dailyWithdrawalLimit)
                    throw new BankException("Daily withdrawal limit exceeded for source");
                from.withdraw(amount, "Transfer to " + toAcc + (narration == null ? "" : " | " + narration));
                to.deposit(amount, "Transfer from " + fromAcc + (narration == null ? "" : " | " + narration));
            }
        }
    }

    @Override
    public synchronized void reverseTransaction(long accountNumber, String txId) throws BankException {
        Account a = accounts.get(accountNumber);
        if (a == null)
            throw new BankException("Account not found");
        var tx = a.findTransaction(txId);
        if (tx == null)
            throw new BankException("Transaction not found");
        if (!tx.isReversible())
            throw new BankException("Transaction not reversible");
        synchronized (a) {
            if (tx.getType() == TransactionType.DEPOSIT) {
                if (a.getBalance() - tx.getAmount() < 0)
                    throw new BankException("Cannot reverse deposit due to insufficient balance");
                a.withdraw(tx.getAmount(), "Reversal of " + txId);
            } else if (tx.getType() == TransactionType.WITHDRAWAL) {
                a.deposit(tx.getAmount(), "Reversal of " + txId);
            } else {
                throw new BankException("Only simple deposits/withdrawals reversible");
            }
        }
    }

    @Override
    public List<Account> listAccounts() {
        return new ArrayList<>(accounts.values());
    }

    @Override
    public List<Account> searchByOwner(String query) {
        String q = query == null ? "" : query.toLowerCase();
        var res = new ArrayList<Account>();
        for (Account a : accounts.values())
            if (a.getOwnerName().toLowerCase().contains(q))
                res.add(a);
        return res;
    }

    @Override
    public int totalAccounts() {
        return accounts.size();
    }

    @Override
    public double totalBalances() {
        return Math.round(accounts.values().stream().mapToDouble(Account::getBalance).sum() * 100.0) / 100.0;
    }

    @Override
    public int countActiveAccounts() {
        return (int) accounts.values().stream().filter(Account::isActive).count();
    }

    @Override
    public double getMinOpeningDeposit() {
        return minOpeningDeposit;
    }

    @Override
    public double getDailyWithdrawalLimit() {
        return dailyWithdrawalLimit;
    }

    @Override
    public boolean isAdminCredentials(String user, String pass) {
        if (user == null || pass == null)
            return false;
        if (!adminUser.equals(user))
            return false;
        var salt = Base64.getDecoder().decode(adminSaltBase64);
        return authProvider.verifyPin(pass, adminPassHashBase64, salt);
    }

    @Override
    public void setAdminPassword(String pass) {
        var salt = authProvider.generateSalt();
        this.adminSaltBase64 = Base64.getEncoder().encodeToString(salt);
        this.adminPassHashBase64 = authProvider.hashPin(pass == null ? "admin123" : pass, salt);
    }

    public void setPersistence(Persistence p) {
        this.persistence = p;
    }

    public void saveTo(String filename) throws IOException {
        if (persistence == null)
            throw new IOException("No persistence configured");
        persistence.save(this, filename);
    }

    public static BankServiceImpl loadFrom(String filename, AuthProvider authProvider, Persistence persistence)
            throws IOException, ClassNotFoundException {
        if (persistence == null)
            throw new IOException("No persistence provided");
        Object obj = persistence.load(filename);
        if (obj == null) {
            return null;
        }
        if (!(obj instanceof BankServiceImpl)) {
            throw new ClassNotFoundException("Saved object is not BankServiceImpl");
        }
        BankServiceImpl loaded = (BankServiceImpl) obj;
        loaded.initTransients(authProvider, persistence);
        return loaded;
    }
}
