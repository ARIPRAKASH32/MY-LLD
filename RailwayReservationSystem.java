import java.util.*;

enum BookingStatus {
    CONFIRMED, CANCELED
}

class User {
    private String name;
    private int userId;
    private List<Booking> bookings = new ArrayList<>();

    public User(String name, int userId) {
        this.name = name;
        this.userId = userId;
    }

    public int getUserId() { return userId; }
    public String getName() { return name; }

    public void addBooking(Booking booking) {
        bookings.add(booking);
    }
}

class Train {
    private int trainNumber;
    private String source;
    private String destination;
    private int totalSeats;
    private int availableSeats;

    public Train(int trainNumber, String source, String destination, int totalSeats) {
        this.trainNumber = trainNumber;
        this.source = source;
        this.destination = destination;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
    }

    public int getTrainNumber() { return trainNumber; }
    public String getSource() { return source; }
    public String getDestination() { return destination; }
    public int getAvailableSeats() { return availableSeats; }

    public boolean bookSeats(int seats) {
        if (availableSeats >= seats) {
            availableSeats -= seats;
            return true;
        }
        return false;
    }

    public void cancelSeats(int seats) {
        availableSeats += seats;
    }

    @Override
    public String toString() {
        return "Train " + trainNumber + " (" + source + " → " + destination + "), Seats: " + availableSeats;
    }
}

class Booking {
    private static int idCounter = 1;
    private int bookingId;
    private User user;
    private Train train;
    private int numberOfSeats;
    private BookingStatus status;

    public Booking(User user, Train train, int numberOfSeats) {
        this.bookingId = idCounter++;
        this.user = user;
        this.train = train;
        this.numberOfSeats = numberOfSeats;
        this.status = BookingStatus.CONFIRMED;
    }

    public int getBookingId() { return bookingId; }
    public BookingStatus getStatus() { return status; }
    public Train getTrain() { return train; }

    public void cancel() {
        status = BookingStatus.CANCELED;
        train.cancelSeats(numberOfSeats);
    }

    @Override
    public String toString() {
        return "Booking #" + bookingId + " | Train " + train.getTrainNumber() +
               " | Seats: " + numberOfSeats + " | Status: " + status;
    }
}

class RailwayReservationSystem {
    private List<Train> trains = new ArrayList<>();
    private List<Booking> bookings = new ArrayList<>();

    public void addTrain(Train train) {
        trains.add(train);
    }

    public List<Train> searchTrains(String source, String destination) {
        List<Train> result = new ArrayList<>();
        for (Train t : trains) {
            if (t.getSource().equalsIgnoreCase(source) && t.getDestination().equalsIgnoreCase(destination)) {
                result.add(t);
            }
        }
        return result;
    }

    public Booking bookTicket(User user, int trainNumber, int seats) {
        for (Train t : trains) {
            if (t.getTrainNumber() == trainNumber && t.bookSeats(seats)) {
                Booking booking = new Booking(user, t, seats);
                bookings.add(booking);
                user.addBooking(booking);
                return booking;
            }
        }
        return null;
    }

    public boolean cancelBooking(int bookingId) {
        for (Booking b : bookings) {
            if (b.getBookingId() == bookingId && b.getStatus() == BookingStatus.CONFIRMED) {
                b.cancel();
                return true;
            }
        }
        return false;
    }
}

public class RailwayReservationSystem {
    public static void main(String[] args) {
        RailwayReservationSystem system = new RailwayReservationSystem();

        system.addTrain(new Train(101, "Delhi", "Mumbai", 100));
        system.addTrain(new Train(102, "Delhi", "Kolkata", 80));

        User user1 = new User("Amit", 1);

        System.out.println("🔍 Available Trains from Delhi to Mumbai:");
        for (Train t : system.searchTrains("Delhi", "Mumbai")) {
            System.out.println(t);
        }

        System.out.println("\n Booking Ticket...");
        Booking booking = system.bookTicket(user1, 101, 2);
        if (booking != null)
            System.out.println("Booking Conformed  " + booking);
        else
            System.out.println(" Booking failed!");

        System.out.println("\nCanceling Booking...");
        boolean canceled = system.cancelBooking(booking.getBookingId());
        System.out.println("Canceled: " + canceled);
    }
}

