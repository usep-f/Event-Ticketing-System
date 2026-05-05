public class Ticket {
    private int ticketId;
    private int eventId;
    private String eventName;
    private String venue;
    private String date;
    private String seatNumber;
    private double amountPaid;
    private String buyerName;

    public Ticket(int ticketId, int eventId, String eventName, String venue, String date, String seatNumber, double amountPaid, String buyerName) {
        this.ticketId = ticketId;
        this.eventId = eventId;
        this.eventName = eventName;
        this.venue = venue;
        this.date = date;
        this.seatNumber = seatNumber;
        this.amountPaid = amountPaid;
        this.buyerName = buyerName;
    }

    // Getters exactly matching PropertyValueFactory names
    public int getTicketId() { return ticketId; }
    public int getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public String getVenue() { return venue; }
    public String getDate() { return date; }
    public String getSeatNumber() { return seatNumber; }
    public double getAmountPaid() { return amountPaid; }
    public String getBuyerName() { return buyerName; }
}