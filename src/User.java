public class User {
    private String username;
    private String fullName;
    private String role;
    private String tier;   // Added
    private double balance; // Added

    public User(String username, String fullName, String role, String tier, double balance) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.tier = tier;
        this.balance = balance;
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getTier() { return tier; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    // This method is required by TicketingController
    public double calculateDiscountedPrice(double basePrice) {
        if (tier == null) return basePrice;
        if (tier.equalsIgnoreCase("Gold")) return basePrice * 0.90;
        if (tier.equalsIgnoreCase("Elite")) return basePrice * 0.80;
        return basePrice;
    }
}