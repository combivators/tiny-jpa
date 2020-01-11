package net.tiny.validation.annotations;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import net.tiny.validation.InetAddressValidator;
import net.tiny.validation.constraints.IPv;

public class InetAddressValidatorTest {

    @IPv(type=IPv.Type.IPv4)
    private String ipv4;
    @IPv(type=IPv.Type.IPv6)
    String ipv6;
    @IPv
    String ipvAll;

    @Test
    public void testIPv4IPv6Pattern() throws Exception {
        IPv ipv4Annotation  = getClass().getDeclaredField("ipv4").getAnnotation(IPv.class);
        IPv ipv6Annotation  = getClass().getDeclaredField("ipv6").getAnnotation(IPv.class);
        IPv ipvAllAnnotation  = getClass().getDeclaredField("ipvAll").getAnnotation(IPv.class);

        InetAddressValidator validator = new InetAddressValidator();
        validator.initialize(ipv4Annotation);
        assertTrue(validator.isValid("0.0.0.0", null));
        assertTrue(validator.isValid("8.8.8.8", null));
        assertTrue(validator.isValid("127.0.0.1", null));
        assertTrue(validator.isValid("192.168.100.254", null));

        assertFalse(validator.isValid("256.127.0.1", null));
        assertFalse(validator.isValid("192.168.100.256", null));

        validator.initialize(ipv6Annotation);
        assertTrue(validator.isValid("2001:370::", null));
        assertTrue(validator.isValid("2001:370::ffff:ffff:ffff:ffff", null));
        assertTrue(validator.isValid("2001:15c0:65ff:8843::", null));
        assertTrue(validator.isValid("2001:15c0:65ff:8847:ffff:ffff:ffff:ffff", null));

        validator.initialize(ipvAllAnnotation);
        assertTrue(validator.isValid("0.0.0.0", null));
        assertTrue(validator.isValid("8.8.8.8", null));
        assertTrue(validator.isValid("127.0.0.1", null));
        assertTrue(validator.isValid("192.168.100.254", null));

        assertTrue(validator.isValid("2001:370::", null));
        assertTrue(validator.isValid("2001:370::ffff:ffff:ffff:ffff", null));
        assertTrue(validator.isValid("2001:15c0:65ff:8843::", null));
        assertTrue(validator.isValid("2001:15c0:65ff:8847:ffff:ffff:ffff:ffff", null));
    }
}
