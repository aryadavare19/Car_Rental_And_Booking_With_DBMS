import java.sql.*;
import java.util.Scanner;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


class CarRentalApp {

    // Connection setup
    private static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/wanderwheel", "root", "root");
    }


    // Signup method
    private static void signup(Connection conn) throws SQLException {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter username: ");
        String username = sc.nextLine();
        System.out.print("Enter password: ");
        String password = sc.nextLine();
        System.out.print("Enter full name: ");
        String fullName = sc.nextLine();
        System.out.print("Enter email: ");
        String email = sc.nextLine();
        System.out.print("Enter phone number: ");
        String phoneNumber = sc.nextLine();

        String sql = "INSERT INTO Users(username, password, full_name, email, phone_number) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, fullName);
            pstmt.setString(4, email);
            pstmt.setString(5, phoneNumber);
            pstmt.executeUpdate();
            System.out.println("\nSignup successful!");
        }
    }

    // Login method
    private static int login(Connection conn, String username, String password) throws SQLException {
        String sql = "SELECT user_id FROM Users WHERE username = ? AND password = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("user_id");  // Return the user_id if login is successful
            }
        }
        return -1;  // Return -1 if login fails
    }

    // Display available cars
    private static void displayAvailableCars(Connection conn) throws SQLException {
        String sql = "SELECT * FROM Cars WHERE status = 'available'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\nAvailable Cars:");
            while (rs.next()) {
                System.out.printf("car ID: %d, Model: %s, Seats: %d, Rate(per day): %.2f, Car No: %s%n",
                        rs.getInt("car_id"), rs.getString("car_model"),
                        rs.getInt("seating_capacity"), rs.getDouble("rental_rate"), rs.getString("car_no"));
            }
            System.out.println();
        }
    }

    // Make a booking

    private static void makeBooking(Connection conn, int userId) throws SQLException {
        Scanner sc = new Scanner(System.in);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);

        displayAvailableCars(conn);

        System.out.print("Enter Car ID to book: ");
        int carId = sc.nextInt();
        sc.nextLine(); // consume newline

        String startDate = "";
        Date startDateParsed = null;
        boolean validStartDate = false;

        // Validate start date
        while (!validStartDate) {
            System.out.print("Enter journey start date (YYYY-MM-DD): ");
            startDate = sc.nextLine();
            try {
                startDateParsed = dateFormat.parse(startDate);
                Date today = new Date();

                if (!startDateParsed.before(today)) {
                    validStartDate = true;
                } else {
                    System.out.println("Invalid start date. Start date must be today or a future date.");
                }
            } catch (ParseException e) {
                System.out.println("Invalid date format. Please enter in YYYY-MM-DD format.");
            }
        }

        System.out.print("Enter journey end date (YYYY-MM-DD): ");
        String endDate = sc.nextLine();

        // Calculate total cost using the CalculateTotalCost function
        String sqlCost = "SELECT CalculateTotalCost(?, ?, ?) AS total_cost";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlCost)) {
            pstmt.setInt(1, carId);
            pstmt.setString(2, startDate);
            pstmt.setString(3, endDate);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double totalCost = rs.getDouble("total_cost");

                // Insert booking into Bookings table
                String insertBooking = "INSERT INTO Bookings (user_id, car_id, driver_id, start_date, end_date, total_cost, payment_status) VALUES (?, ?, ?, ?, ?, ?, 'pending')";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertBooking, Statement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setInt(2, carId);
                    insertStmt.setInt(3, carId); // assuming car_id for driver, change if different
                    insertStmt.setString(4, startDate);
                    insertStmt.setString(5, endDate);
                    insertStmt.setDouble(6, totalCost);
                    insertStmt.executeUpdate();

                    // Update car status to 'booked'
                    String updateCarStatus = "UPDATE Cars SET status = 'booked' WHERE car_id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateCarStatus)) {
                        updateStmt.setInt(1, carId);
                        updateStmt.executeUpdate();
                    }

                    System.out.println("\nBooking successful! Total cost: " + totalCost);

                    // Retrieve driver details for the specific car
                    String sql = "SELECT d.driver_id, d.full_name, d.license_number, d.phone_number, c.car_no " +
                            "FROM drivers d " +
                            "JOIN cars c ON d.driver_id = c.current_driver_id " +
                            "WHERE c.car_id = ?";
                    try (PreparedStatement pstmt1 = conn.prepareStatement(sql)) {
                        pstmt1.setInt(1, carId);  // use pstmt1 here
                        try (ResultSet rst = pstmt1.executeQuery()) {
                            System.out.println("\nDriver Details:");
                            while (rst.next()) {
                                System.out.printf("ID: %d, Full Name: %s, License Number: %s, Phone Number: %s, Car No: %s%n",
                                        rst.getInt("driver_id"), rst.getString("full_name"),
                                        rst.getString("license_number"), rst.getString("phone_number"), rst.getString("car_no"));
                            }
                            System.out.println();
                        }
                    }
                }
            } else {
                System.out.println("Error calculating cost: no cost returned.");
            }
        }
    }


    // H

    // Display "My Account" options
    private static void displayMyAccount(Connection conn, int userId) throws SQLException {
        Scanner sc = new Scanner(System.in);

        System.out.println("1. View Ongoing Bookings\n2. View Completed Bookings\n3. Total Spending\n4. Bookings Above Average Cost");
        int choice = sc.nextInt();

        switch (choice) {
            case 1:
                displayActiveBookings(conn, userId);
                break;
            case 2:
                displayCompletedBookings(conn, userId);
                break;
            case 3:
                displayTotalSpending(conn, userId);
                break;
            case 4:
                displayHighCostBookings(conn, userId);
                break;
            default:
                System.out.println("Invalid choice.");
        }
    }

    // Display ongoing bookings from Bookings table
    public static void displayActiveBookings(Connection conn, int userId) throws SQLException {
        String sql = "{CALL GetActiveBookings(?)}"; // Calling the stored procedure
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            int activeBooking = 0;
            System.out.println("\nOngoing Bookings:");
            while (rs.next()) {
                System.out.printf("Booking ID: %d, Car ID: %d, Start Date: %s, End Date: %s%n",
                        rs.getInt("booking_id"), rs.getInt("car_id"),
                        rs.getString("start_date"), rs.getString("end_date"));
                activeBooking = 1;
            }
            if (activeBooking == 0) {
                System.out.println("Ohh !!! No ongoing bookings\n");
            }
        }
    }

    public static void updateExpiredBookings(Connection conn) throws SQLException {
        // Insert expired bookings into BookingHistory with 'completed' payment status
        String moveExpiredQuery =
                "INSERT INTO BookingHistory (booking_id, user_id, car_id, driver_id, start_date, end_date, total_cost, payment_status) " +
                        "SELECT booking_id, user_id, car_id, driver_id, start_date, end_date, total_cost, 'completed' " +
                        "FROM Bookings WHERE end_date < CURDATE()";

        String deleteExpiredQuery = "DELETE FROM Bookings WHERE end_date < CURDATE()";



        try (PreparedStatement moveStmt = conn.prepareStatement(moveExpiredQuery);
             PreparedStatement deleteStmt = conn.prepareStatement(deleteExpiredQuery);
        ) {

            conn.setAutoCommit(false);  // Use transaction to ensure consistency
            moveStmt.executeUpdate();
            deleteStmt.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // Display completed bookings from BookingHistory table
    public static void displayCompletedBookings(Connection conn, int userId) throws SQLException {
        String sqlHistory = "SELECT * FROM BookingHistory WHERE user_id = ?";
        try (PreparedStatement pstmtHistory = conn.prepareStatement(sqlHistory)) {
            pstmtHistory.setInt(1, userId);
            ResultSet rsHistory = pstmtHistory.executeQuery();
            int Complete=0;
            System.out.println("Completed Bookings:");
            while (rsHistory.next()) {
                System.out.printf("Booking ID: %d, Car ID: %d, Start Date: %s, End Date: %s%n",
                        rsHistory.getInt("booking_id"), rsHistory.getInt("car_id"),
                        rsHistory.getString("start_date"), rsHistory.getString("end_date"));
                Complete=1;
            }
            if(Complete==0){
                System.out.println("Ohh !!! No completed bookings\n");

            }
        }
    }


    // Display total spending
    private static void displayTotalSpending(Connection conn, int userId) throws SQLException {
        String sql = "SELECT * FROM TotalSpendingView WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("Total Spending: " + rs.getDouble("total_spending"));
            } else {
                System.out.println("No spending history found.");
            }
        }
    }

    // Display bookings with total cost higher than average
    private static void displayHighCostBookings(Connection conn, int userId) throws SQLException {
        String sql = "SELECT * FROM Bookings WHERE user_id = ? AND total_cost > (SELECT AVG(total_cost) FROM Bookings)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            int done=0;
            System.out.println("High-Cost Bookings:");
            while (rs.next()) {
                System.out.printf("Booking ID: %d, Start Date: %s, End Date: %s, Total Cost: %.2f%n",
                        rs.getInt("booking_id"), rs.getDate("start_date"), rs.getDate("end_date"), rs.getDouble("total_cost"));
                done=1;
            }
            if(done==0){
                System.out.println("No bookings yet");
            }
        }
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            Scanner sc = new Scanner(System.in);
            updateExpiredBookings(conn);
            while (true) {
                System.out.println("1. Signup\n2. Login\n3. Exit");
                int mainChoice = sc.nextInt();
                sc.nextLine(); // Consume newline

                switch (mainChoice) {
                    case 1:
                        signup(conn);
                        break;
                    case 2:
                        System.out.print("Enter username: ");
                        String username = sc.nextLine();
                        System.out.print("Enter password: ");
                        String password = sc.nextLine();

                        int userId = login(conn, username, password);
                        if (userId != -1) {
                            System.out.println("\nLogin successful!\n");

                            boolean userLoggedIn = true;
                            while (userLoggedIn) {
                                System.out.println("1. Book a Cab\n2. My Account\n3. Back to Main Menu");
                                int userChoice = sc.nextInt();
                                sc.nextLine(); // Consume newline

                                switch (userChoice) {
                                    case 1:
                                        makeBooking(conn, userId);
                                        break;
                                    case 2:
                                        displayMyAccount(conn,userId);
                                        break;
                                    case 3:
                                        userLoggedIn = false;
                                        break;
                                    default:
                                        System.out.println("please enter valid choice");
                                }
                            }
                        } else {
                            System.out.println("Invalid login credentials.");
                        }
                        break;
                    case 3:
                        System.out.println("Thank you for using WanderWheel!");
                        break;
                    default:
                        System.out.println("Please enter valid choice");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}






