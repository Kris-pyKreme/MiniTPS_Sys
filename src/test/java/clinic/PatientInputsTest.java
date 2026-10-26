package clinic;

import org.junit.jupiter.api.Test;
import validation.PatientInputs;
import static org.junit.jupiter.api.Assertions.*;

class PatientInputsTest {
    @Test void acceptsTenDigitsStartingWithNine() {
        assertEquals("9123456789", PatientInputs.contact("9123456789"));
        assertEquals("9999999999", PatientInputs.contact("9999999999"));
        assertEquals("9123456789", PatientInputs.contact("  9123456789  "));
    }

    @Test void keepsTheOptionalFieldBlank() {
        assertEquals("", PatientInputs.contact(""));
        assertEquals("", PatientInputs.contact("   "));
        assertEquals("", PatientInputs.contact(null));
    }

    @Test void rejectsOtherStartingDigits() {
        assertInvalid("0123456789");
        assertInvalid("8123456789");
        assertInvalid("1123456789");
    }

    @Test void rejectsNumbersWithTheWrongLength() {
        assertInvalid("912345678");
        assertInvalid("91234567890");
        assertInvalid("09123456789");
    }

    @Test void rejectsLettersSeparatorsAndCountryPrefixes() {
        assertInvalid("912345678a");
        assertInvalid("912 3456789");
        assertInvalid("912-3456789");
        assertInvalid("+639123456789");
        assertInvalid("9１２３４５６７８９");
    }

    private void assertInvalid(String number) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> PatientInputs.contact(number), number);
        assertEquals(PatientInputs.CONTACT_MESSAGE, error.getMessage());
    }
}
