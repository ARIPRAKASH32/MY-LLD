// File: Main.java
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Bank bank = new Bank();
        // create sample accounts
        Account acc1 = new Account("A1001", "John Doe", 5000);
        acc1.getCard().setCardNumber("1111-2222-3333-4444");
        acc1.getCard().setPin("1234");
        bank.addAccount(acc1);

        Account acc2 = new Account("A1002", "Jane Smith", 15000);
        acc2.getCard().setCardNumber("4444-3333-2222-1111");
        acc2.getCard().setPin("4321");
        bank.addAccount(acc2);

        ATM atm = new ATM("ATM-01", bank);
        atm.loadCash(50, 2000); // 50 notes of 2000
        atm.loadCash(100, 500); // 100 notes of 500
        atm.loadCash(200, 200); // 200 notes of 200
        atm.loadCash(500, 100); // 500 notes of 100

        atm.start();
    }
}

/* --------------------- ATM.java --------------------- */
class ATM {
    private final String atmId;
    private final Bank bank;
    private final CashDispenser cashDispenser;
    private final Scanner scanner = new Scanner(System.in);
    private final ATMService atmService;

    public ATM(String atmId, Bank bank) {
        this.atmId = atmId;
        this.bank = bank;
        this.cashDispenser = new CashDispenser();
        this.atmService = new ATMService(bank, cashDispenser);
    }

    public void loadCash(int denomination, int count) {
        cashDispenser.load(denomination, count);
    }

    public void start() {
        System.out.println("Welcome to " + atmId);
        while (true) {
            System.out.print("\nInsert card number (or 'exit'): ");
            String cardNumber = scanner.nextLine().trim();
            if (cardNumber.equalsIgnoreCase("exit")) {
                System.out.println("Thank you. Goodbye!");
                break;
            }
            System.out.print("Enter PIN: ");
            String pin = scanner.nextLine().trim();

            Optional<Account> opt = bank.authenticate(cardNumber, pin);
            if (!opt.isPresent()) {
                System.out.println("Authentication failed. Try again.");
                continue;
            }
            Account user = opt.get();
            System.out.println("Hello, " + user.getName());

            boolean session = true;
            while (session) {
                System.out.println("\nChoose operation: 1-Balance 2-Withdraw 3-Deposit 4-MiniStatement 5-ChangePIN 6-Eject");
                String choice = scanner.nextLine().trim();
                switch (choice) {
                    case "1":
                        System.out.printf("Balance: %.2f%n", atmService.checkBalance(user));
                        break;
                    case "2":
                        System.out.print("Enter amount to withdraw: ");
                        String amtStr = scanner.nextLine().trim();
                        try {
                            int amt = Integer.parseInt(amtStr);
                            atmService.withdraw(user, amt);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid amount.");
                        } catch (ATMException e) {
                            System.out.println("Withdraw failed: " + e.getMessage());
                        }
                        break;
                    case "3":
                        System.out.print("Enter amount to deposit: ");
                        String depStr = scanner.nextLine().trim();
                        try {
                            double dep = Double.parseDouble(depStr);
                            atmService.deposit(user, dep);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid amount.");
                        }
                        break;
                    case "4":
                        List<Transaction> txs = atmService.getMiniStatement(user, 5);
                        System.out.println("Last transactions:");
                        txs.forEach(System.out::println);
                        break;
                    case "5":
                        System.out.print("Enter new PIN: ");
                        String newPin = scanner.nextLine().trim();
                        try {
                            atmService.changePin(user, newPin);
                            System.out.println("PIN changed successfully.");
                        } catch (ATMException e) {
                            System.out.println("PIN change failed: " + e.getMessage());
                        }
                        break;
                    case "6":
                        session = false;
                        System.out.println("Card ejected. Goodbye " + user.getName());
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            }
        }
    }
}

/* --------------------- Bank.java --------------------- */
class Bank {
    private final Map<String, Account> accountsById = new HashMap<>();
    private final Map<String, Account> accountsByCardNumber = new HashMap<>();

    public void addAccount(Account account) {
        accountsById.put(account.getAccountId(), account);
        if (account.getCard().getCardNumber() != null) {
            accountsByCardNumber.put(account.getCard().getCardNumber(), account);
        }
    }

    public Optional<Account> authenticate(String cardNumber, String pin) {
        Account acc = accountsByCardNumber.get(cardNumber);
        if (acc != null && acc.getCard().validatePin(pin)) {
            return Optional.of(acc);
        }
        return Optional.empty();
    }

    public Account getAccountById(String id) {
        return accountsById.get(id);
    }
}

/* --------------------- Account.java --------------------- */
class Account {
    private final String accountId;
    private final String name;
    private double balance;
    private final Card card = new Card();
    private final Deque<Transaction> transactions = new ArrayDeque<>(); // recent first

    public Account(String accountId, String name, double initialBalance) {
        this.accountId = accountId;
        this.name = name;
        this.balance = initialBalance;
    }

    public synchronized void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit must be positive");
        balance += amount;
        addTransaction(new Transaction(TransactionType.DEPOSIT, amount, "Deposit"));
    }

    public synchronized void withdraw(double amount) throws ATMException {
        if (amount <= 0) throw new IllegalArgumentException("Withdraw must be positive");
        if (amount > balance) throw new ATMException("Insufficient funds");
        balance -= amount;
        addTransaction(new Transaction(TransactionType.WITHDRAW, amount, "Withdraw"));
    }

    private void addTransaction(Transaction tx) {
        transactions.addFirst(tx);
        if (transactions.size() > 50) transactions.removeLast();
    }

    public synchronized double getBalance() {
        return balance;
    }

    public String getAccountId() { return accountId; }
    public String getName() { return name; }
    public Card getCard() { return card; }

    public List<Transaction> getRecentTransactions(int n) {
        List<Transaction> list = new ArrayList<>();
        int i = 0;
        for (Transaction t : transactions) {
            if (i++ == n) break;
            list.add(t);
        }
        return list;
    }
}

/* --------------------- Card.java --------------------- */
class Card {
    private String cardNumber;
    private String pin; // store hashed in production

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public void setPin(String pin) { this.pin = pin; }
    public boolean validatePin(String inputPin) {
        return pin != null && pin.equals(inputPin);
    }
}

/* --------------------- ATMService.java --------------------- */
class ATMService {
    private final Bank bank;
    private final CashDispenser dispenser;

    public ATMService(Bank bank, CashDispenser dispenser) {
        this.bank = bank;
        this.dispenser = dispenser;
    }

    public double checkBalance(Account acc) {
        return acc.getBalance();
    }

    public void deposit(Account acc, double amount) {
        acc.deposit(amount);
        System.out.printf("Deposited %.2f. New balance: %.2f%n", amount, acc.getBalance());
    }

    public void withdraw(Account acc, int amount) throws ATMException {
        if (amount <= 0) throw new ATMException("Amount must be positive");
        if (!dispenser.canDispense(amount)) throw new ATMException("ATM cannot dispense requested amount with available denominations");
        // check account balance
        if (acc.getBalance() < amount) throw new ATMException("Insufficient funds in account");
        // deduct from account
        acc.withdraw(amount);
        // dispense cash (this updates dispenser internal state)
        Map<Integer, Integer> dispensed = dispenser.dispense(amount);
        System.out.println("Please collect cash:");
        dispensed.forEach((denom, cnt) -> System.out.println(denom + " x " + cnt));
        System.out.printf("Withdrawal successful. Remaining balance: %.2f%n", acc.getBalance());
    }

    public List<Transaction> getMiniStatement(Account acc, int n) {
        return acc.getRecentTransactions(n);
    }

    public void changePin(Account acc, String newPin) throws ATMException {
        if (newPin == null || newPin.length() < 4) throw new ATMException("PIN must be at least 4 digits");
        acc.getCard().setPin(newPin);
    }
}

/* --------------------- CashDispenser.java --------------------- */
class CashDispenser {
    // maintain denominations: key=denomination, value=count
    private final NavigableMap<Integer, Integer> denominations = new TreeMap<>(Comparator.reverseOrder());

    // denominations allowed (example): 2000,500,200,100
    public CashDispenser() {
        denominations.put(2000, 0);
        denominations.put(500, 0);
        denominations.put(200, 0);
        denominations.put(100, 0);
    }

    public synchronized void load(int denomination, int count) {
        if (!denominations.containsKey(denomination)) {
            denominations.put(denomination, count);
        } else {
            denominations.put(denomination, denominations.get(denomination) + count);
        }
    }

    public synchronized boolean canDispense(int amount) {
        try {
            calculateDispensePlan(amount);
            return true;
        } catch (ATMException e) {
            return false;
        }
    }

    public synchronized Map<Integer, Integer> dispense(int amount) throws ATMException {
        Map<Integer, Integer> plan = calculateDispensePlan(amount);
        // commit
        plan.forEach((denom, cnt) -> denominations.put(denom, denominations.get(denom) - cnt));
        return plan;
    }

    private Map<Integer, Integer> calculateDispensePlan(int amount) throws ATMException {
        if (amount % 100 != 0) throw new ATMException("Amount must be multiple of 100");
        int remaining = amount;
        Map<Integer, Integer> plan = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> e : denominations.entrySet()) {
            int denom = e.getKey();
            int avail = e.getValue();
            if (avail <= 0) continue;
            int take = Math.min(avail, remaining / denom);
            if (take > 0) {
                plan.put(denom, take);
                remaining -= take * denom;
            }
        }
        if (remaining != 0) throw new ATMException("ATM cannot dispense the requested amount with current denominations");
        return plan;
    }

    public synchronized void printStatus() {
        System.out.println("ATM Cash status:");
        denominations.forEach((d, c) -> System.out.println(d + " -> " + c));
    }
}

/* --------------------- Transaction.java --------------------- */
enum TransactionType { WITHDRAW, DEPOSIT, TRANSFER }

class Transaction {
    private final TransactionType type;
    private final double amount;
    private final Date timestamp;
    private final String note;

    public Transaction(TransactionType type, double amount, String note) {
        this.type = type;
        this.amount = amount;
        this.timestamp = new Date();
        this.note = note;
    }

    public String toString() {
        return String.format("[%tF %tT] %s: %.2f - %s", timestamp, timestamp, type, amount, note);
    }
}

/* --------------------- ATMException.java --------------------- */
class ATMException extends Exception {
    public ATMException(String message) {
        super(message);
    }
}
