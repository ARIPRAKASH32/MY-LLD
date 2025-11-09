import java.util.*;

enum OrderStatus {
    PLACED, PREPARING, PICKED_UP, DELIVERED
}

class MenuItem {
    String name;
    double price;

    MenuItem(String name, double price) {
        this.name = name;
        this.price = price;
    }
}

class Order {
    static int idCounter = 1;
    int id;
    Customer customer;
    Restaurant restaurant;
    List<MenuItem> items;
    OrderStatus status;
    DeliveryPartner partner;

    Order(Customer customer, Restaurant restaurant, List<MenuItem> items) {
        this.id = idCounter++;
        this.customer = customer;
        this.restaurant = restaurant;
        this.items = items;
        this.status = OrderStatus.PLACED;
    }

    void updateStatus(OrderStatus status) {
        this.status = status;
    }
}

class Customer {
    String name;

    Customer(String name) {
        this.name = name;
    }

    Order placeOrder(Restaurant restaurant, List<MenuItem> items) {
        System.out.println(name + " placed an order at " + restaurant.name);
        return new Order(this, restaurant, items);
    }
}

class Restaurant {
    String name;
    List<MenuItem> menu = new ArrayList<>();

    Restaurant(String name) {
        this.name = name;
    }

    void addMenuItem(String name, double price) {
        menu.add(new MenuItem(name, price));
    }

    boolean acceptOrder(Order order) {
        System.out.println(name + " accepted order #" + order.id);
        order.updateStatus(OrderStatus.PREPARING);
        return true;
    }
}

class DeliveryPartner {
    String name;

    DeliveryPartner(String name) {
        this.name = name;
    }

    void pickOrder(Order order) {
        order.partner = this;
        order.updateStatus(OrderStatus.PICKED_UP);
        System.out.println(name + " picked up order #" + order.id);
    }

    void deliverOrder(Order order) {
        order.updateStatus(OrderStatus.DELIVERED);
        System.out.println(name + " delivered order #" + order.id);
    }
}

class FoodDeliverySystem {
    List<DeliveryPartner> partners = new ArrayList<>();

    void addPartner(DeliveryPartner partner) {
        partners.add(partner);
    }

    DeliveryPartner assignDeliveryPartner(Order order) {
        if (partners.isEmpty()) {
            System.out.println("No delivery partner available.");
            return null;
        }
        DeliveryPartner partner = partners.get(new Random().nextInt(partners.size()));
        System.out.println("Assigned " + partner.name + " to order #" + order.id);
        return partner;
    }
}

public class FoodDeliverySystem {
    public static void main(String[] args) {
        FoodDeliverySystem system = new FoodDeliverySystem();
        Restaurant rest = new Restaurant("Ari's Dine");
        rest.addMenuItem("Pizza", 250);
        rest.addMenuItem("Pasta", 180);

        Customer cust = new Customer("Ariprakash");
        DeliveryPartner dp = new DeliveryPartner("Ravi");
        system.addPartner(dp);

        Order order = cust.placeOrder(rest, Arrays.asList(rest.menu.get(0)));
        rest.acceptOrder(order);

        DeliveryPartner assigned = system.assignDeliveryPartner(order);
        if (assigned != null) {
            assigned.pickOrder(order);
            assigned.deliverOrder(order);
        }

        System.out.println("Order Status: " + order.status);
    }
}

