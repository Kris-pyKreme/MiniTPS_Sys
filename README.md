# KlinikAccess — Java Swing clinic system

A light desktop workspace for patient registration, appointments, consultations, and billing. Built with Java Swing, FlatLaf, Maven, and MySQL Connector/J. Opens directly as a Maven project in NetBeans. No web server is needed.

## Interface previews

The desktop interface uses bundled Lato typography, neutral cards, and teal navigation accents. These are screenshots of the running Swing application.

[Sign in](docs/screenshots/sign-in.png) · [Overview](docs/screenshots/overview.png) · [Patients](docs/screenshots/patients.png) · [Appointments](docs/screenshots/appointments.png) · [Consultations](docs/screenshots/consultations.png) · [Billing](docs/screenshots/billing.png) · [Staff](docs/screenshots/staff.png) · [Profile](docs/screenshots/profile.png)

## Run in NetBeans with XAMPP

1. Install **JDK 11 or newer** and Apache NetBeans with Java/Maven support. Java 21 is recommended. Set NetBeans' project Java platform to that JDK.
2. Start **MySQL** in the XAMPP Control Panel. Open phpMyAdmin and import `database/schema.sql`, then `database/staff_users.sql`. The default database is `clinic_tps`. XAMPP's MariaDB is supported.
3. In NetBeans choose **File → Open Project** and select this repository's root, the directory containing `pom.xml`.
4. The defaults are `localhost:3306`, user `root`, and an empty password. For other settings copy `db.properties.example` to **`db.properties` at the project root** and edit it. This local file is ignored by Git. No Java source edit is required.
5. Choose **Clean and Build**, then **Run Project** with main class `Main`. Maven downloads the dependencies on the first build. The command-line equivalent is `mvn package` then `mvn exec:java` from the repository root. A desktop display is required.
6. Select **Admin**, username **`admin`**, password **`admin123`**. Open **Staff** to create Doctor, Nurse, and Receptionist accounts, or reset passwords. A second seeded Receptionist account is **`staff1` / `staff123`**. Replace the development passwords through the admin dashboard before real use.

The first launch performs an additive upgrade of `staff_users` and creates `password_requests`. The database user needs CREATE and ALTER privileges for this first launch. Existing patient records, staff passwords, and clinic transactions are retained. Legacy SHA-256 passwords are upgraded to salted PBKDF2 after a successful login. Do not reimport `schema.sql` into an existing database to upgrade: its doctor seed statements append rows. `staff_users.sql` preserves existing accounts on reimport.

The sign-in screen uses a centered white card on a pale teal background, with bundled Lato regular/bold typography and drawn icons. Typing does not resize or maximize the login window. Inputs use their font metrics, actual padding, and display scale to keep typed text fully visible.

Alternative connection settings, in priority order: Java properties `clinic.db.url`, `clinic.db.user`, `clinic.db.password`; environment variables `CLINIC_DB_URL`, `CLINIC_DB_USER`, `CLINIC_DB_PASSWORD`; then `db.properties`. Use `-Dclinic.config=/path/to/db.properties` to select another configuration file. Passwords belong in local configuration, never in source control. The app uses Philippine time (`Asia/Manila`) for its calendar and timestamps.

## Role workspaces

| Capability | Admin | Doctor | Nurse | Receptionist |
| --- | --- | --- | --- | --- |
| Dashboard and appointment calendar | All visits | Assigned visits | All visits | All visits |
| Patient records | Register / update / delete | View assigned patients | Register / update | Register / update |
| Appointments | Book / update / delete | View assigned | View | Book / update |
| Consultation notes and prescriptions | Create / update / delete | Create / update assigned | No access | No access |
| Billing and payment status | Create / update / delete | No access | No access | Create / update |
| Staff accounts, access, password resets | Yes | No | No | No |
| Profile and password requests | Yes | Yes | Yes | Yes |

Selecting a different role at login does not grant access: it must match the stored account. Authorization is enforced in the data-access layer as well as the navigation. Doctor searches, pending appointments, and consultation writes are scoped to the linked `doctor_id`. When creating or editing a Doctor account, link it to the correct provider from the `doctors` table. The three existing sample providers remain available; additional provider records can be added in MySQL. Legacy Cashier accounts remain in the database and can be reassigned to one of the four supported roles through **Edit access**.

The calendar supports **Appointment date** (scheduled visit day) and **Date booked** (when the booking was created). Use the month arrows, Today, and individual day cells to browse the daily agenda. Visits and dashboard counts come from MySQL; there are no fabricated patient records. Click Refresh to reload data; the overview and account-request pages also refresh every 15 seconds. Registration, appointment, consultation, and billing forms refresh when opened so unsaved edits are not overwritten by polling.

## Password request and reset

- On the login screen, enter the username, select the correct role, and click **Forgot password?**. The response does not disclose whether the account exists.
- A signed-in staff member can open **My profile → Change password · Send request**. This sends a request; it does not let the staff member change their password directly.
- The administrator sees the pending count and request queue in **Staff**. Select a request, choose **Set password**, and enter and confirm the new password. Admins may also reset an account directly from the staff directory.
- The password and request status are committed in one transaction. Requests are deduplicated per account. The admin shares the new password directly with the staff member; there is no email/SMS integration.
- Existing sessions are rejected on their next authorized operation and signed out by the next 15-second session check. The previous password no longer works. Changes to staff access also invalidate prior sessions.
- Admins cannot remove their own administrator role. Keep another administrator account available for recovery if the primary administrator forgets their password.

Passwords use PBKDF2-HMAC-SHA256 with a random per-account salt and 210,000 iterations. New passwords require 8–128 characters. This is a desktop application connecting directly to a local database; role checks are application controls, not a replacement for database/network access controls on a shared production server.

## Development and tests

`mvn test` runs the database-independent password, role, and date/time checks. Database integration and desktop UI tests are opt-in to avoid touching a developer's clinic data.

Create an empty disposable database named `clinic_tps_test`, import copies of both SQL files with `clinic_tps` replaced by `clinic_tps_test`, then run:

```sh
mvn -Dclinic.integration=true -Dclinic.ui.tests=true \
  '-Dclinic.db.url=jdbc:mysql://localhost:3306/clinic_tps_test?serverTimezone=Asia/Manila' test
```

Provide the test database credentials using the same configuration mechanisms above. These tests refuse databases whose names do not end in `_test`. They create and clean `IT_` patient fixtures and `it_` staff fixtures; use a dedicated test database. UI tests require a real display or Xvfb. Omit `-Dclinic.ui.tests=true` for database-only checks. Tests cover role matching, denied operations, doctor isolation, password requests and resets, session invalidation, double booking, consultation transactions, billing totals, and the login and role-specific screens.

`mvn -Dclinic.ui.tests=true -Dtest=LoginInteractionTest,InputLayoutTest test` runs the desktop input checks without a database. They check that typing preserves the window size and inputs, password visibility works, and editors have room for their full text lines. Run the layout checks with `-DargLine=-Dflatlaf.uiScale=1.25x` (or `1.5x` / `2x`) to exercise larger display scales in a fresh JVM.

The cloud development setup stores tools in `/workspace/.clinic-tools`; source its `env.sh` and run `start-services.sh` before building. Use its Maven settings and local repository when running in this cloud environment. Xvfb validates the desktop interface but does not provide an interactive web preview.

The Lato fonts are bundled under `src/main/resources/fonts` with their SIL Open Font License, so typography stays consistent in NetBeans and on other computers. The application name is KlinikAccess; the existing `clinic_tps` database name is retained so existing records continue to work.
