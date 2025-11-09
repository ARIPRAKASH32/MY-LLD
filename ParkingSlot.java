import java.util.*;

enum VehicleType {
    BIKE, CAR, TRUCK
}

class Vehicle {
    String number;
    VehicleType type;

    Vehicle(String number, VehicleType type) {
        this.number = number;
        this.type = type;
    }
}

class ParkingSlot {
    int slotId;
    boolean isAvailable;
    VehicleType slotType;
    Vehicle parkedVehicle;

    ParkingSlot(int slotId, VehicleType slotType) {
        this.slotId = slotId;
        this.slotType = slotType;
        this.isAvailable = true;
    }
}

class Ticket {
    Vehicle vehicle;
    ParkingSlot slot;
    long entryTime;

    Ticket(Vehicle vehicle, ParkingSlot slot) {
        this.vehicle = vehicle;
        this.slot = slot;
        this.entryTime = System.currentTimeMillis();
    }
}

class ParkingLot {
    Map<VehicleType, List<ParkingSlot>> slots = new HashMap<>();
    Map<String, Ticket> activeTickets = new HashMap<>();

    ParkingLot(int bikeSlots, int carSlots, int truckSlots) {
        slots.put(VehicleType.BIKE, createSlots(bikeSlots, VehicleType.BIKE));
        slots.put(VehicleType.CAR, createSlots(carSlots, VehicleType.CAR));
        slots.put(VehicleType.TRUCK, createSlots(truckSlots, VehicleType.TRUCK));
    }

    private List<ParkingSlot> createSlots(int count, VehicleType type) {
        List<ParkingSlot> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(new ParkingSlot(i, type));
        }
        return list;
    }

    void parkVehicle(Vehicle vehicle) {
        List<ParkingSlot> typeSlots = slots.get(vehicle.type);
        for (ParkingSlot slot : typeSlots) {
            if (slot.isAvailable) {
                slot.isAvailable = false;
                slot.parkedVehicle = vehicle;
                Ticket ticket = new Ticket(vehicle, slot);
                activeTickets.put(vehicle.number, ticket);
                System.out.println(vehicle.type + " " + vehicle.number + " parked at slot " + slot.slotId);
                return;
            }
        }
        System.out.println("No available slot for " + vehicle.type);
    }

    void removeVehicle(String vehicleNumber) {
        Ticket ticket = activeTickets.get(vehicleNumber);
        if (ticket == null) {
            System.out.println("Vehicle not found.");
            return;
        }

        long duration = (System.currentTimeMillis() - ticket.entryTime) / 1000; // seconds
        int rate = getRate(ticket.vehicle.type);
        double fee = Math.max(1, duration / 3600.0) * rate;

        ParkingSlot slot = ticket.slot;
        slot.isAvailable = true;
        slot.parkedVehicle = null;
        activeTickets.remove(vehicleNumber);

        System.out.println("Vehicle " + vehicleNumber + " exited. Parking fee: ₹" + (int) fee);
    }

    int getRate(VehicleType type) {
        switch (type) {
            case BIKE: return 10;
            case CAR: return 20;
            case TRUCK: return 30;
            default: return 0;
        }
    }

    void displayAvailableSlots() {
        for (VehicleType type : slots.keySet()) {
            long count = slots.get(type).stream().filter(s -> s.isAvailable).count();
            System.out.println(type + " Slots Available: " + count);
        }
    }
}

public class ParkingSlot {
    public static void main(String[] args) throws InterruptedException {
        ParkingLot lot = new ParkingLot(2, 2, 1);

        Vehicle v1 = new Vehicle("TN01AB1234", VehicleType.CAR);
        Vehicle v2 = new Vehicle("TN02CD5678", VehicleType.BIKE);
        Vehicle v3 = new Vehicle("TN03EF9999", VehicleType.TRUCK);

        lot.parkVehicle(v1);
        lot.parkVehicle(v2);
        lot.parkVehicle(v3);

        lot.displayAvailableSlots();

        Thread.sleep(2000); // simulate parked time
        lot.removeVehicle("TN01AB1234");

        lot.displayAvailableSlots();
    }
}

