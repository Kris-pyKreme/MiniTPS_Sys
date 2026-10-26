package clinic;

import gui.GuiUtil;
import security.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccessAndValidationTest {
    @Test void rolePolicy() {
        assertTrue(Role.ADMIN.registersPatients());assertTrue(Role.ADMIN.consults());assertTrue(Role.ADMIN.bills());
        assertTrue(Role.DOCTOR.consults());assertFalse(Role.DOCTOR.booksAppointments());assertFalse(Role.DOCTOR.bills());
        assertTrue(Role.NURSE.registersPatients());assertFalse(Role.NURSE.consults());assertFalse(Role.NURSE.bills());
        assertTrue(Role.RECEPTIONIST.booksAppointments());assertTrue(Role.RECEPTIONIST.bills());assertFalse(Role.RECEPTIONIST.consults());
    }
    @Test void passwordHashAndLegacyCompatibility() {
        char[] p="correct horse".toCharArray();String salt=Passwords.salt(),hash=Passwords.hash(p,salt);
        assertTrue(Passwords.matches(p,salt,hash,Passwords.SCHEME));assertFalse(Passwords.matches("wrong".toCharArray(),salt,hash,Passwords.SCHEME));
        assertNotEquals(hash,Passwords.hash(p,Passwords.salt()));
        assertTrue(Passwords.matches("admin123".toCharArray(),"a1b2c3","8eb597608190e3ddfc74aa0df49d139ebea59ee426f9280542481d4e4102db20","SHA256"));
        assertThrows(IllegalArgumentException.class,()->Passwords.validate("short".toCharArray()));
    }
    @Test void strictDatesAndTimes() {
        assertEquals("2028-02-29",GuiUtil.date("2028-02-29").toString());
        assertThrows(IllegalArgumentException.class,()->GuiUtil.date("2026-02-30"));
        assertEquals("14:30:00",GuiUtil.time("14:30").toString());
        assertThrows(IllegalArgumentException.class,()->GuiUtil.time("24:01"));
    }
    @Test void visibleDateAndTimeGuidesUseCalendarYearsAndMinutes() {
        assertEquals("2021-01-01",GuiUtil.date("2021-01-01").toString());
        assertEquals("14:30:00",GuiUtil.time("14-30").toString());
        assertEquals("14:13:00",GuiUtil.time("14-13").toString());
        assertEquals("00:00:00",GuiUtil.time("00-00").toString());
        assertEquals("23:59:00",GuiUtil.time("23-59").toString());
        assertThrows(IllegalArgumentException.class,()->GuiUtil.time("24-00"));
        assertThrows(IllegalArgumentException.class,()->GuiUtil.time("14-60"));
    }
    @Test void loggedOutSessionDenied() {Session.logout();assertThrows(SecurityException.class,Session::current);}
}
