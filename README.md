# Bus Ticket Management System

A Java Swing desktop application with an Oracle Database backend through JDBC. It manages cities, routes, buses, seat layouts, trip schedules, bookings, and cancellations.

## Features

- Search trips by origin, destination, and travel date.
- View departure and arrival times, bus, fare, and available seats.
- Select seats from a visual bus layout; up to eight seats per booking.
- Retrieve a ticket by booking reference and passenger phone number.
- Cancel eligible bookings and release their seats.
- Password-protected administration panel for managing cities, buses, seats, routes, schedules, bookings, payment status, and seat reports.
- Oracle constraints prevent the same seat from being actively booked more than once on the same trip.

## Requirements

- JDK 17 or newer.
- Maven 3.6.3 or newer.
- Oracle Database 12c or newer.

## Oracle Setup

1. Open `schema.sql` in SQL Developer or SQL*Plus.
2. As a database administrator, uncomment and run the user creation statements at the top of the file after changing the password.
3. Connect as `BUS_TICKET` and run the rest of the script. It creates the tables, constraints, indexes, sample cities, a sample bus and seats, and an initial trip.
4. Change the sample database password. Do not store real passwords in project files.

## Connection Settings

The default URL is `jdbc:oracle:thin:@//localhost:1521/FREEPDB1`, and the default user is `BUS_TICKET`. Configure the connection with environment variables:

- `ORACLE_URL` — Oracle JDBC URL.
- `ORACLE_USER` — database user.
- `ORACLE_PASSWORD` — database password.
- `APP_ADMIN_PASSWORD` — administrator panel password. The default is `change-this-admin-password`; change it before use.

In Windows PowerShell, set these in the current terminal before launching:

```powershell
$env:ORACLE_URL = "jdbc:oracle:thin:@//localhost:1521/FREEPDB1"
$env:ORACLE_USER = "BUS_TICKET"
$env:ORACLE_PASSWORD = "your-database-password"
$env:APP_ADMIN_PASSWORD = "choose-a-strong-admin-password"
```

## Build and Run

From the project directory:

```text
mvn clean package
java -jar target/bus-ticket-desktop-1.0.0.jar
```

Maven includes the Oracle JDBC driver in the packaged JAR. Use the **Admin Login** button and the password configured in `APP_ADMIN_PASSWORD` to open the administration panel.

## Data Model

`cities` stores cities; `routes` stores origin/destination pairs; `buses` and `bus_seats` store buses and their physical seat layouts; `schedules` stores dated trips and fares; `bookings` stores passenger and booking details; and `booking_seats` links bookings to seats. Cancellation history is retained while seats are released.

## Scope and Limitations

Payment and refund statuses are recorded manually; no payment gateway is connected. SMS and email notifications are not included. Oracle must be installed and configured on the local computer or network before the application can connect to the database.
