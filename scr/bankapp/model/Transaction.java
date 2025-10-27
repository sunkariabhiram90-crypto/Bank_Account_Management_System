package bankapp.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public final class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String txId;
    private final LocalDateTime timestamp;
    private final TransactionType type;
    private final double amount;
    private final double balanceAfter;
    private final String narration;

    public Transaction(TransactionType type, double amount, double balanceAfter, String narration) {
        this.txId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.type = type;
        this.amount = Math.round(amount * 100.0) / 100.0;
        this.balanceAfter = Math.round(balanceAfter * 100.0) / 100.0;
        this.narration = narration == null ? "" : narration;
    }

    public String getTxId() {
        return txId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public String getNarration() {
        return narration;
    }

    public boolean isReversible() {
        return type == TransactionType.DEPOSIT || type == TransactionType.WITHDRAWAL;
    }
}
