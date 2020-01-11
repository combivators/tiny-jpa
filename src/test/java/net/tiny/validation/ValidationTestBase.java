package net.tiny.validation;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Future;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class ValidationTestBase {

    @AssertTrue
    boolean mustTrue;

    @AssertFalse
    boolean mustFalse;

    @Max(10)
    int intMaxValue;

    @Min(-10)
    int intMinValue;

    @Min(3)
    @Max(4)
    BigDecimal bigMinMaxValue;

    @DecimalMax("10")
    String stringDecMaxValue;
    @DecimalMin("-10.0")
    int intDecMinValue;

    @DecimalMin("3.00")
    @DecimalMax("4.00")
    BigDecimal bigDeciMinMaxValue;

    @Digits(integer = 3, fraction = 1)
    String stringValue;

    @Digits(integer = 4, fraction = 0)
    int intValue;

    @Digits(integer = 4, fraction = 3)
    BigDecimal bigDecimalValue;

    @Past
    Date date;

    @Future
    Calendar calendar;

    @Pattern(regexp = "hoge")
    String hoge;

    @Pattern(regexp = "bar")
    String bar;

    @Pattern.List({
        @Pattern(regexp = "^aaa.*"),
        @Pattern(regexp = ".*bbb$")
    })
    String strPattern;

    @Pattern(regexp = ".+@.+\\..+")
    String email;

    @Size(min = 10)
    String str;

    @Size(min = 3, max = 6)
    List<Integer> list;

    @Size
    Map<String, Integer> map;

    @Size(max = 100)
    int[] array;

    //Message
    @Size(min = 10)
    String defaultMessage = "hoge";

    @Size(min = 10, message = "サイズが{min}と{max}の間ではないよ")
    String directMessage = "hoge";

    @Size(min = 10, message = "{mykey}")
    String fromPropMessage = "hoge";

    // ValidationMessages.properties
    /*****************
javax.validation.constraints.AssertFalse.message=must be false
javax.validation.constraints.AssertTrue.message=must be true
javax.validation.constraints.DecimalMax.message=must be less than or equal to {value}
javax.validation.constraints.DecimalMin.message=must be greater than or equal to {value}
javax.validation.constraints.Digits.message=numeric value out of bounds (<{integer} digits>.<{fraction} digits> expected)
javax.validation.constraints.Future.message=must be in the future
javax.validation.constraints.Max.message=must be less than or equal to {value}
javax.validation.constraints.Min.message=must be greater than or equal to {value}
javax.validation.constraints.NotNull.message=may not be null
javax.validation.constraints.Null.message=must be null
javax.validation.constraints.Past.message=must be in the past
javax.validation.constraints.Pattern.message=must match "{regexp}"
javax.validation.constraints.Size.message=size must be between {min} and {max}
org.hibernate.validator.constraints.Email.message=not a well-formed email address
org.hibernate.validator.constraints.Length.message=length must be between {min} and {max}
org.hibernate.validator.constraints.NotBlank.message=may not be empty
org.hibernate.validator.constraints.NotEmpty.message=may not be empty
org.hibernate.validator.constraints.Range.message=must be between {min} and {max}
org.hibernate.validator.constraints.URL.message=must be a valid URL
org.hibernate.validator.constraints.CreditCardNumber.message=invalid credit card number
org.hibernate.validator.constraints.ScriptAssert.message=script expression "{script}" didn't evaluate to true
mykey={min}から{max}ではないな
     */

    String pass1 = "pass";
    String pass2 = "passsssss";
    int a = 2;
    int b = 4;
    @Max(value = 5, message = "合計は{value}以下じゃないとダメ")
    public int getSum() {
        return a + b;
    }
    @AssertTrue(message = "パスワードが一致しません")
    public boolean isComparePass() {
        return pass1.equals(pass2);
    }
}
