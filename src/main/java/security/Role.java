package security;

public enum Role {
    ADMIN("Admin"), DOCTOR("Doctor"), NURSE("Nurse"), RECEPTIONIST("Receptionist");
    private final String label;
    Role(String label) { this.label = label; }
    @Override public String toString() { return label; }
    public boolean registersPatients() { return this != DOCTOR; }
    public boolean booksAppointments() { return this == ADMIN || this == RECEPTIONIST; }
    public boolean consults() { return this == ADMIN || this == DOCTOR; }
    public boolean bills() { return this == ADMIN || this == RECEPTIONIST; }
    public static Role from(String text) {
        for (Role r : values()) if (r.label.equals(text)) return r;
        throw new IllegalArgumentException("Unsupported staff role: " + text);
    }
}
