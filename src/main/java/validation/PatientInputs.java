package validation;

/** Input rules shared by the patient form and its data-access layer. */
public final class PatientInputs {
    public static final String CONTACT_MESSAGE =
            "Contact number must contain 10 digits and start with 9 (for example, 9123456789).";

    private PatientInputs() { }

    /** Keep the optional field blank, or return a trimmed number in the requested local format. */
    public static String contact(String value) {
        String number = value == null ? "" : value.trim();
        if (!number.isEmpty() && !number.matches("9[0-9]{9}")) {
            throw new IllegalArgumentException(CONTACT_MESSAGE);
        }
        return number;
    }
}
