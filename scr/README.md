# 🏦 Bank Account Management System (Java Console Project)

A **secure, console-based Bank Account Management System** built entirely in **Java**, demonstrating core **Object-Oriented Programming (OOP)** concepts.

This project simulates a real-world bank’s operations — allowing users to create accounts, deposit/withdraw money, transfer funds, and manage transactions, while providing an admin interface to monitor all activity.

---

## 🧩 Tech Stack

| Technology | Purpose |
|-------------|----------|
| **Java (JDK 11+)** | Core programming language |
| **OOP Principles** | Abstraction, Encapsulation, Interfaces |
| **Serialization** | Data persistence between runs |
| **PBKDF2 (HmacSHA256)** | Secure PIN hashing |
| **CSV File Export** | Transaction history export |
| **Console UI (Scanner)** | Interactive command-line interface |

---

## 🗂️ Folder Structure

bankapp-secure/
└─ src/
|─ bankapp/
├─ BankApp.java
├─ model/
│ ├─ Account.java
│ ├─ AccountType.java
│ ├─ Transaction.java
│ └─ TransactionType.java
├─ exceptions/
│ └─ BankException.java
├─ security/
│ ├─ AuthProvider.java
│ └─ Pbkdf2AuthProvider.java
├─ persistence/
│ ├─ Persistence.java
│ └─ SerializationPersistence.java
├─ service/
│ ├─ BankService.java
│ └─ BankServiceImpl.java
|─ ui/
└─ ConsoleUI.java


---

## 💡 Key Features

### 👤 User Features
- Create new bank accounts (SAVINGS or CURRENT)
- Secure PIN authentication (PBKDF2 hashed)
- Deposit and withdraw funds
- Transfer money between accounts
- Mini-statement (last 10 transactions)
- Export transactions to CSV file
- Change account PIN
- Auto-save data on exit

### 🧑‍💼 Admin Features
- Secure admin login (`admin / admin123`)
- View all accounts and balances
- Search accounts by number or name
- Freeze / Unfreeze accounts
- Reverse erroneous transactions
- View simple summary reports (total accounts, active users, total balances)
- Save or load system data manually

---

## 🧰 Object-Oriented Programming Concepts Used

| Concept | Description |
|----------|--------------|
| **Encapsulation** | All account details (PIN, balance, etc.) are private in `Account.java`. Access only through methods. |
| **Abstraction** | Interfaces like `AuthProvider`, `Persistence`, and `BankService` abstract away implementation details. |
| **Polymorphism** | Different persistence/authentication implementations can be swapped easily. |
| **Interface** | `AuthProvider`, `Persistence`, and `BankService` define contracts for implementations. |
| **Composition** | `BankServiceImpl` uses `AuthProvider` and `Persistence` objects to perform secure operations. |

---

## 🔒 Security Highlights

- User PINs are never stored in plain text.  
- Each PIN is hashed using **PBKDF2WithHmacSHA256** with a **unique salt** per account.  
- Admin password is also hashed.  
- Transactions are serialized securely and auto-saved on program exit.  

---

## 🧑‍💻 How to Compile and Run

### 🧱 Compile
```bash
# Navigate to project root
cd bankapp-secure

# Compile all .java files into /out directory
javac -d out -sourcepath src src/bankapp/BankApp.java

## 📋 Sample Console Output
## Main Menu

=== Main Menu ===
1. Create account
2. Login to account
3. Admin login
4. Save data now
5. Load data from file
6. Exit
Choose:

## Create Account

--- Create Account ---
Owner full name: Alice
Account type: 1. SAVINGS  2. CURRENT
Choose (1/2): 1
Set 4-digit PIN: 1234
Opening deposit (min 100.00): 500
Created. AccountNo: 1000000000 | Balance: 500.00
Saved to bankdata-secure.ser

## Deposit / Withdraw / Transfer

Amount: 100
Narration: Salary Oct
Deposited. New balance: 600.00


| Username | Password   |
| -------- | ---------- |
| `admin`  | `admin123` |

## Admin Menu

=== Admin Menu ===
1. List accounts
2. Search account
3. Freeze/unfreeze
4. Reverse tx
5. Simple report
6. Logout

## Example CSV Export (Transactions)

txId,timestamp,type,amount,balanceAfter,narration
a1b2c3,2025-10-27T12:01:00,DEPOSIT,500.00,500.00,"Opening deposit"
d4e5f6,2025-10-27T12:05:00,DEPOSIT,100.00,600.00,"Salary Oct"
g7h8i9,2025-10-27T12:10:00,WITHDRAWAL,50.00,550.00,"ATM withdrawal"

## Example User Flow

Start program → Main menu
Create account (SAVINGS, PIN 1111, deposit 500)
Login → deposit, withdraw, transfer
Export CSV
Admin login → list accounts, freeze/unfreeze
Exit (auto-save)

## Data Storage

All data automatically saved to file:
bankdata-secure.ser

Transaction exports:
account_<number>.csv