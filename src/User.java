public class User {
    private String username;
    private String fullName;
    private String role;
    private String tier;
    private double balance;

    public User(String username, String fullName, String role, String tier, double balance) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.tier = tier;
        this.balance = balance;
    }

    // --- GETTERS ---
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getTier() { return tier; }
    public double getBalance() { return balance; }

    // --- SETTERS (Crucial for live UI updates) ---

    // This is the one you asked for:
    public void setTier(String tier) {
        this.tier = tier;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    // --- BUSINESS LOGIC ---

    /**
     * Calculates the price after applying membership discounts.
     * Elite: 20% off (price * 0.8)
     * Gold: 10% off (price * 0.9)
     * Standard: 0% off
     */
    public double calculateDiscountedPrice(double basePrice) {
        if (tier == null) return basePrice;

        if (tier.equalsIgnoreCase("Elite")) {
            return basePrice * 0.80;
        } else if (tier.equalsIgnoreCase("Gold")) {
            return basePrice * 0.90;
        }
        return basePrice;
    }
}