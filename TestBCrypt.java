import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestBCrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String hash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        
        // Test common passwords
        String[] passwords = {
            "password123",
            "admin123",
            "123456",
            "password",
            "admin",
            "12345678",
            "Password123",
            "Admin123"
        };
        
        System.out.println("Testing BCrypt hash: " + hash);
        System.out.println("==========================================");
        
        for (String pwd : passwords) {
            boolean matches = encoder.matches(pwd, hash);
            System.out.println(pwd + " -> " + (matches ? "✓ MATCH!" : "✗ No match"));
        }
    }
}
