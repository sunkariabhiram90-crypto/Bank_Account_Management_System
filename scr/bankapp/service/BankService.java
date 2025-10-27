package bankapp.service;

import bankapp.exceptions.BankException;
import bankapp.model.Account;
import bankapp.model.AccountType;
import java.util.List;

public interface BankService extends java.io.Serializable {
    Account createAccount(String owner, AccountType type, String pin, double openingDeposit) throws BankException;

    Account getAccount(long accountNumber);

    void deposit(long accountNumber, double amount, String narration) throws BankException;

    void withdraw(long accountNumber, double amount, String narration) throws BankException;

    void transfer(long fromAcc, long toAcc, double amount, String narration) throws BankException;

    void reverseTransaction(long accountNumber, String txId) throws BankException;

    List<Account> listAccounts();

    List<Account> searchByOwner(String query);

    int totalAccounts();

    double totalBalances();

    int countActiveAccounts();

    double getMinOpeningDeposit();

    double getDailyWithdrawalLimit();

    boolean isAdminCredentials(String user, String pass);

    void setAdminPassword(String pass);
}
