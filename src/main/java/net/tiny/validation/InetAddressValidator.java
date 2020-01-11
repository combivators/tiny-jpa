package net.tiny.validation;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import net.tiny.validation.constraints.IPv;

public class InetAddressValidator implements ConstraintValidator<IPv, String> {

    public static final String IPV4_PATTERN  = "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    public static final String IPV6_PATTERN = "^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$";

    private IPv ip;

    @Override
    public void initialize(IPv ip) {
        this.ip = ip;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // validation logic goes here
        boolean ret = false;
        switch(this.ip.type()) {
        case IPv4:
            ret = isIPV4Format(value);
            break;
        case IPv6:
            ret = isIPV6Format(value);
            break;
        case ALL:
        default:
            ret = (isIPV4Format(value) || isIPV6Format(value));
            break;
        }
        return ret;
    }

    public static boolean isIPV4Format(String value) {
        return Pattern.matches(IPV4_PATTERN, value);
    }

    //IPv6 address validator matches these IPv6 formats
    //::ffff:21:7.8.9.221 | 2001:0db8:85a3:08d3:1319:8a2e:0370:7344
    //| ::8a2e:0:0370:7344 | 2001:0db8:85a3:08d3:1319:8a2e:100.22.44.55
    //| 2001:0db8::8a2e:100.22.44.55 | ::100.22.44.55 | ffff::
    //And such addresses are invalid
    //::8a2e:0:0370:7344.4 | 2001:idb8::111:7.8.9.111 | 2001::100.a2.44.55
    //| :2001::100.22.44.55
    public static boolean isIPV6Format(String value) {
        //in many cases such as URLs, IPv6 addresses are wrapped by []
        if(value.substring(0, 1).equals("[") && value.substring(value.length()-1).equals("]")) {
            value = value.substring(1, value.length()-1);
        }

        return (1 < Pattern.compile(":").split(value).length)
        //a valid IPv6 address should contains no less than 1,
        //and no more than 7 “:” as separators
            && (Pattern.compile(":").split(value).length <= 8)

        //the address can be compressed, but “::” can appear only once
            && (Pattern.compile("::").split(value).length <= 2)

        //if a compressed address
            && (Pattern.compile("::").split(value).length == 2)

            //if starts with “::” – leading zeros are compressed
            ? (((value.substring(0, 2).equals("::"))
            ? Pattern.matches("^::([\\da-f]{1,4}(:)){0,4}(([\\da-f]{1,4}(:)[\\da-f]{1,4})|([\\da-f]{1,4})|((\\d{1,3}.){3}\\d{1,3}))"
        , value)
                : Pattern.matches("^([\\da-f]{1,4}(:|::)){1,5}(([\\da-f]{1,4}(:|::)[\\da-f]{1,4})|([\\da-f]{1,4})|((\\d{1,3}.){3}\\d{1,3}))"
        , value)))

        //if ends with "::" - ending zeros are compressed
                : ((value.substring(value.length()-2).equals("::"))
                ? Pattern.matches("^([\\da-f]{1,4}(:|::)){1,7}", value)
                : Pattern.matches("^([\\da-f]{1,4}:){6}(([\\da-f]{1,4}:[\\da-f]{1,4})|((\\d{1,3}.){3}\\d{1,3}))"
        , value));
    }
}
