import java.util.*;

class Movie {
    private String title;
    private int duration;

    public Movie(String title, int duration) {
        this.title = title;
        this.duration = duration;
    }

    public String getTitle() { return title; }
    public int getDuration() { return duration; }
}

class Theater {
    private String name;
    private List<Show> shows = new ArrayList<>();

    public Theater(String name) {
        this.name = name;
    }

    public void addShow(Show show) {
        shows.add(show);
    }

    public List<Show> getShows() {
        return shows;
    }

    public String getName() { return name; }
}

class Show {
    private Movie movie;
    private Date showTime;
    private int totalSeats;
    private int availableSeats;

    public Show(Movie movie, Date showTime, int totalSeats) {
        this.movie = movie;
        this.showTime = showTime;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
    }

    public Movie getMovie() { return movie; }
    public Date getShowTime() { return showTime; }
    public int getAvailableSeats() { return availableSeats; }

    public boolean bookSeats(int count) {
        if (count <= availableSeats) {
            availableSeats -= count;
            return true;
        }
        return false;
    }
}

class User {
    private String name;

    public User(String name) {
        this.name = name;
    }

    public String getName() { return name; }
}

class Booking {
    private User user;
    private Show show;
    private int seatCount;
    private boolean confirmed;

    public Booking(User user, Show show, int seatCount) {
        this.user = user;
        this.show = show;
        this.seatCount = seatCount;
    }

    public void confirmBooking() {
        if (show.bookSeats(seatCount)) {
            confirmed = true;
            System.out.println("Booking confirmed for " + user.getName() + 
                               " | Movie: " + show.getMovie().getTitle() +
                               " | Seats: " + seatCount);
        } else {
            System.out.println(" Not enough seats available for " + user.getName());
        }
    }
}

public class MovieTicketSystem {
    public static void main(String[] args) {
        Movie m1 = new Movie("Avengers: Endgame", 180);
        Movie m2 = new Movie("Inception", 150);

        Theater t1 = new Theater("PVR Cinemas");
        t1.addShow(new Show(m1, new Date(), 50));
        t1.addShow(new Show(m2, new Date(), 40));

        User u1 = new User("Ari");

        Show selectedShow = t1.getShows().get(0);
        Booking b1 = new Booking(u1, selectedShow, 4);
        b1.confirmBooking();

        Booking b2 = new Booking(new User("Pragadeesh"), selectedShow, 60);
        b2.confirmBooking();
    }
}
