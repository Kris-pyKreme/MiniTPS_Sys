package security;

public final class User {
    public final int id, credentialVersion;
    public final Integer doctorId;
    public final String username, fullName;
    public final Role role;
    public User(int id, String username, String fullName, Role role, Integer doctorId, int version) {
        this.id = id; this.username = username; this.fullName = fullName;
        this.role = role; this.doctorId = doctorId; this.credentialVersion = version;
    }
}
