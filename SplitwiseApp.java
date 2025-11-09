/*
 * SplitwiseApp.java
 * A single-file, runnable Java application that demonstrates core Splitwise functionality:
 * - Users, Groups, Expenses, Splits
 * - Balance maintenance between user pairs (canonical ordering)
 * - Create equal/exact/percentage splits
 * - Record settlements (payments)
 * - View user and group balances
 *
 * This is an in-memory demo intended for learning and interviews. Compile and run with:
 *   javac SplitwiseApp.java
 *   java SplitwiseApp
 *
 * Java version: 11+
 */

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class SplitwiseApp {

    // ---------- Models ----------
    static class User {
        final long id;
        String name;
        String email;
        User(long id, String name, String email) { this.id = id; this.name = name; this.email = email; }
        public String toString() { return String.format("User{id=%d, name=%s}", id, name); }
    }

    static class Group {
        final long id;
        String name;
        Set<Long> members = new LinkedHashSet<>();
        Group(long id, String name) { this.id = id; this.name = name; }
        public String toString() { return String.format("Group{id=%d, name=%s, members=%s}", id, name, members); }
    }

    static class Split {
        final long userId;
        final BigDecimal amount; // positive amount this user owes (or is allocated)
        Split(long userId, BigDecimal amount) { this.userId = userId; this.amount = amount; }
    }

    static class Expense {
        final long id;
        final Long groupId; // nullable
        final long paidBy;
        final BigDecimal amount;
        final String note;
        final Instant createdAt;
        final List<Split> splits;

        Expense(long id, Long groupId, long paidBy, BigDecimal amount, String note, List<Split> splits) {
            this.id = id; this.groupId = groupId; this.paidBy = paidBy; this.amount = amount; this.note = note; this.createdAt = Instant.now(); this.splits = List.copyOf(splits);
        }
    }

    // Canonical balance representation for unordered pair (a,b) with a < b
    static class BalanceKey {
        final long a, b;
        BalanceKey(long a, long b) {
            if (a <= b) { this.a = a; this.b = b; } else { this.a = b; this.b = a; }
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BalanceKey)) return false;
            BalanceKey k = (BalanceKey)o;
            return a == k.a && b == k.b;
        }
        @Override public int hashCode() { return Objects.hash(a, b); }
        @Override public String toString() { return String.format("(%d,%d)", a, b); }
    }

    // ---------- Repositories (in-memory) ----------
    static class InMemoryStore {
        final Map<Long, User> users = new LinkedHashMap<>();
        final Map<Long, Group> groups = new LinkedHashMap<>();
        final Map<Long, Expense> expenses = new LinkedHashMap<>();
        final Map<BalanceKey, BigDecimal> balances = new HashMap<>();
    }

    // ---------- Services ----------
    static class UserService {
        private final InMemoryStore store;
        private final AtomicLong idGen;
        UserService(InMemoryStore s, AtomicLong g) { store = s; idGen = g; }
        User createUser(String name, String email) {
            long id = idGen.getAndIncrement();
            User u = new User(id, name, email); store.users.put(id, u); return u;
        }
        Optional<User> get(long id) { return Optional.ofNullable(store.users.get(id)); }
    }

    static class GroupService {
        private final InMemoryStore store;
        private final AtomicLong idGen;
        GroupService(InMemoryStore s, AtomicLong g) { store = s; idGen = g; }
        Group createGroup(String name, Collection<Long> memberIds) {
            long id = idGen.getAndIncrement(); Group g = new Group(id, name); g.members.addAll(memberIds); store.groups.put(id, g); return g;
        }
        Optional<Group> get(long id) { return Optional.ofNullable(store.groups.get(id)); }
    }

    static class BalanceService {
        private final InMemoryStore store;
        BalanceService(InMemoryStore s) { store = s; }

        // convention: stored value = amount such that amount > 0 means userA (a) is owed by userB (b)
        synchronized void adjustBalance(long creditor, long debtor, BigDecimal delta) {
            if (creditor == debtor) return;
            BalanceKey key = new BalanceKey(creditor, debtor);
            boolean creditorIsA = (key.a == creditor);
            BigDecimal signedDelta = creditorIsA ? delta : delta.negate();
            BigDecimal prev = store.balances.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal updated = prev.add(signedDelta).setScale(2, RoundingMode.HALF_UP);
            if (updated.compareTo(BigDecimal.ZERO) == 0) {
                store.balances.remove(key);
            } else {
                store.balances.put(key, updated);
            }
        }

        // get balances for a user (net view)
        Map<Long, BigDecimal> getBalancesForUser(long userId) {
            Map<Long, BigDecimal> res = new HashMap<>();
            for (var e : store.balances.entrySet()) {
                long a = e.getKey().a, b = e.getKey().b; BigDecimal amt = e.getValue();
                if (userId == a) {
                    // a is owed amt by b
                    res.put(b, amt);
                } else if (userId == b) {
                    // stored amt means a is owed by b, so user b owes a amount => user owes negative
                    res.put(a, amt.negate());
                }
            }
            return res;
        }

        // pretty print all balances
        void printAllBalances() {
            if (store.balances.isEmpty()) { System.out.println("No balances."); return; }
            System.out.println("Balances (canonical pair -> amount where positive means first is owed by second):");
            store.balances.forEach((k,v)-> System.out.println(k + " -> " + v));
        }
    }

    static class ExpenseService {
        private final InMemoryStore store;
        private final AtomicLong idGen;
        private final BalanceService balanceService;
        ExpenseService(InMemoryStore s, AtomicLong g, BalanceService b) { store = s; idGen = g; balanceService = b; }

        Expense createExpense(Long groupId, long paidBy, BigDecimal amount, String note, List<Split> splits) {
            // basic validation
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("amount must be > 0");
            if (!store.users.containsKey(paidBy)) throw new IllegalArgumentException("payer unknown");
            if (groupId != null && !store.groups.containsKey(groupId)) throw new IllegalArgumentException("group unknown");

            // ensure splits sum = amount
            BigDecimal sum = splits.stream().map(s -> s.amount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
            if (sum.compareTo(amount.setScale(2, RoundingMode.HALF_UP)) != 0) {
                throw new IllegalArgumentException("splits do not sum to amount: sum=" + sum + " amount=" + amount);
            }

            long id = idGen.getAndIncrement();
            Expense exp = new Expense(id, groupId, paidBy, amount.setScale(2, RoundingMode.HALF_UP), note, splits);
            store.expenses.put(id, exp);

            // update balances: for each participant != payer, creditor is paidBy, debtor is participant
            for (Split s : splits) {
                if (s.userId == paidBy) continue; // payer's own share
                balanceService.adjustBalance(paidBy, s.userId, s.amount);
            }
            return exp;
        }

        Expense createEqualExpense(Long groupId, long paidBy, BigDecimal amount, String note, Collection<Long> participants) {
            if (!participants.contains(paidBy)) {
                // typically payer is also included
            }
            int n = participants.size();
            BigDecimal share = amount.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
            // distribute rounding remainder to first members
            BigDecimal totalAssigned = share.multiply(BigDecimal.valueOf(n));
            BigDecimal diff = amount.subtract(totalAssigned);
            List<Split> splits = new ArrayList<>();
            int i = 0;
            for (Long u : participants) {
                BigDecimal s = share;
                if (i < diff.multiply(BigDecimal.valueOf(100)).intValue()) { // tiny hack if diff exists (should be small)
                    s = s.add(new BigDecimal("0.01"));
                }
                splits.add(new Split(u, s));
                i++;
            }
            return createExpense(groupId, paidBy, amount, note, splits);
        }
    }

    // ---------- Simple CLI demo ----------
    public static void main(String[] args) throws Exception {
        InMemoryStore store = new InMemoryStore();
        AtomicLong idGen = new AtomicLong(1);
        UserService userSvc = new UserService(store, idGen);
        GroupService groupSvc = new GroupService(store, idGen);
        BalanceService balSvc = new BalanceService(store);
        ExpenseService expSvc = new ExpenseService(store, idGen, balSvc);

        // create sample users
        User a = userSvc.createUser("Alice", "alice@example.com");
        User b = userSvc.createUser("Bob", "bob@example.com");
        User c = userSvc.createUser("Charlie", "charlie@example.com");

        // create group
        Group g = groupSvc.createGroup("Trip", Arrays.asList(a.id, b.id, c.id));

        System.out.println("Users: " + store.users.values());
        System.out.println("Group: " + g);

        // Alice pays 300 and it's split equally among A,B,C
        BigDecimal amt = new BigDecimal("300.00");
        Expense e1 = expSvc.createEqualExpense(g.id, a.id, amt, "Hotel", g.members);
        System.out.println("Created expense id=" + e1.id + " paidBy=" + e1.paidBy + " amount=" + e1.amount);

        // show balances
        balSvc.printAllBalances();

        // Bob settles 50 to Alice
        System.out.println("Bob pays Alice 50.00 (settlement)");
        balSvc.adjustBalance(a.id, b.id, new BigDecimal("-50.00")); // negative delta reduces what Bob owes Alice
        balSvc.printAllBalances();

        // Show per-user net balances
        System.out.println("Net balances for Alice: " + balSvc.getBalancesForUser(a.id));
        System.out.println("Net balances for Bob: " + balSvc.getBalancesForUser(b.id));
        System.out.println("Net balances for Charlie: " + balSvc.getBalancesForUser(c.id));

        // Create an exact split expense: Bob pays 120, Charlie owes 70, Bob owes 50
        List<Split> exactSplits = Arrays.asList(new Split(b.id, new BigDecimal("50.00")), new Split(c.id, new BigDecimal("70.00")), new Split(b.id, new BigDecimal("0.00")));
        // Note: splits must sum to amount; here amount should be 120
        Expense e2 = expSvc.createExpense(g.id, b.id, new BigDecimal("120.00"), "Dinner", Arrays.asList(new Split(a.id, new BigDecimal("40.00")), new Split(b.id, new BigDecimal("40.00")), new Split(c.id, new BigDecimal("40.00"))));
        System.out.println("Created expense id=" + e2.id + " paidBy=" + e2.paidBy + " amount=" + e2.amount);
        balSvc.printAllBalances();

        System.out.println("Demo finished.");
    }
}

