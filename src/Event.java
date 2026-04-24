public class Event {
    private int id;
    private String name;
    private String date;
    private String venue;
    private double price;
    private int seats;

    public Event(int id, String name, String date, String venue, double price, int seats) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.venue = venue;
        this.price = price;
        this.seats = seats;
    }

    // Getters for the TableView
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDate() { return date; }
    public String getVenue() { return venue; }
    public double getPrice() { return price; }

    // This is what will show in the "Seats Remaining" column
    public String getSeats() {
        return (seats <= 0) ? "Sold Out" : seats + " Left";
    }

    // For logic calculations
    public int getSeatsNum() { return seats; }
}