package dbms1;

import javax.swing.*;

import java.awt.*;

import java.awt.event.ActionEvent;

import java.awt.event.ActionListener;

import java.sql.*;

import java.time.LocalDate;

import java.time.temporal.ChronoUnit;



public class CarRentalUI extends JFrame {

    private Connection conn;

    private JTextField userField;

    private JPasswordField passField;



    public CarRentalUI() {

        setTitle("WanderWheel Car Rental System");

        setSize(500, 400);

        setLayout(new BorderLayout());

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLocationRelativeTo(null);



        // Header

        JLabel headerLabel = new JLabel("WanderWheel Car Rentals", SwingConstants.CENTER);

        headerLabel.setFont(new Font("Arial", Font.BOLD, 28));

        headerLabel.setForeground(new Color(0, 102, 204));

        add(headerLabel, BorderLayout.NORTH);



        // Center panel for login form

        JPanel centerPanel = new JPanel(new GridBagLayout());

        centerPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(10, 10, 10, 10);



        // Username

        JLabel userLabel = new JLabel("Username:");

        userLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        gbc.gridx = 0;

        gbc.gridy = 0;

        centerPanel.add(userLabel, gbc);



        userField = new JTextField(15);

        gbc.gridx = 1;

        centerPanel.add(userField, gbc);



        // Password

        JLabel passLabel = new JLabel("Password:");

        passLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        gbc.gridx = 0;

        gbc.gridy = 1;

        centerPanel.add(passLabel, gbc);



        passField = new JPasswordField(15);

        gbc.gridx = 1;

        centerPanel.add(passField, gbc);



        // Buttons panel

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        buttonPanel.setBackground(Color.WHITE);



        JButton loginButton = new JButton("Login");

        loginButton.setFont(new Font("Arial", Font.BOLD, 14));

        loginButton.setBackground(new Color(0, 153, 76));

        loginButton.setForeground(Color.WHITE);

        buttonPanel.add(loginButton);



        JButton signupButton = new JButton("Sign Up");

        signupButton.setFont(new Font("Arial", Font.BOLD, 14));

        signupButton.setBackground(new Color(0, 102, 204));

        signupButton.setForeground(Color.WHITE);

        buttonPanel.add(signupButton);



        // Message Label

        JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);

        messageLabel.setForeground(Color.RED);

        add(messageLabel, BorderLayout.SOUTH);



        // Action Listeners

        loginButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                String username = userField.getText();

                String password = new String(passField.getPassword());

                int userId = login(username, password);

                if (userId != -1) {

                    JOptionPane.showMessageDialog(null, "Login successful!");

                    displayAvailableCars(userId);

                } else {

                    messageLabel.setText("Login failed. Try again.");

                }

            }

        });



        signupButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                signup();

            }

        });



        // Add Panels

        add(centerPanel, BorderLayout.CENTER);

        add(buttonPanel, BorderLayout.SOUTH);



        // Initialize Database Connection

        connectToDatabase();

    }



    private void connectToDatabase() {

        try {

            Class.forName("com.mysql.jdbc.Driver");  // Updated driver

            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/wanderwheel", "root", "root");

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(null, "Database connection failed.");

        }

    }



    private int login(String username, String password) {

        try {

            String query = "SELECT user_id FROM Users WHERE username = ? AND password = ?";

            PreparedStatement pst = conn.prepareStatement(query);

            pst.setString(1, username);

            pst.setString(2, password);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {

                return rs.getInt("user_id");

            }

        } catch (Exception e) {

            e.printStackTrace();

        }

        return -1;

    }



    private void signup() {

        String username = JOptionPane.showInputDialog("Enter Username:");

        String password = JOptionPane.showInputDialog("Enter Password:");

        String fullName = JOptionPane.showInputDialog("Enter Full Name:");

        String email = JOptionPane.showInputDialog("Enter Email:");

        String phone = JOptionPane.showInputDialog("Enter Phone Number:");



        try {

            String query = "INSERT INTO Users(username, password, full_name, email, phone_number) VALUES (?, ?, ?, ?, ?)";

            PreparedStatement pst = conn.prepareStatement(query);

            pst.setString(1, username);

            pst.setString(2, password);

            pst.setString(3, fullName);

            pst.setString(4, email);

            pst.setString(5, phone);

            pst.executeUpdate();

            JOptionPane.showMessageDialog(null, "Signup successful!");

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(null, "Signup failed. Try again.");

        }

    }



    private void displayAvailableCars(int userId) {

        try {

            String query = "SELECT * FROM Cars WHERE status = 'available'";

            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery(query);



            System.out.println("Available Cars:");

            while (rs.next()) {

                System.out.printf("Car ID: %d, Model: %s, Seats: %d, Rate(per day): %.2f, Car No: %s\n",

                        rs.getInt("car_id"), rs.getString("car_model"),

                        rs.getInt("seating_capacity"), rs.getDouble("rental_rate"), rs.getString("car_no"));

            }



            // Prompt the user for a car ID and validate input

            String carIdInput = JOptionPane.showInputDialog("Enter Car ID to book:");

            if (carIdInput == null || carIdInput.trim().isEmpty()) {

                JOptionPane.showMessageDialog(null, "Invalid input! Car ID cannot be empty.");

                return; // Exit if input is invalid

            }



            int carId = Integer.parseInt(carIdInput.trim());



            String startDate = JOptionPane.showInputDialog("Enter Start Date (YYYY-MM-DD):");

            String endDate = JOptionPane.showInputDialog("Enter End Date (YYYY-MM-DD):");



            makeBooking(userId, carId, startDate, endDate);



        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(null, "Invalid input! Car ID must be a number.");

        } catch (Exception e) {

            e.printStackTrace();

        }

    }



    private void makeBooking(int userId, int carId, String startDate, String endDate) {

        try {

            // Step 1: Retrieve the rental rate from the database

            String rateQuery = "SELECT rental_rate FROM Cars WHERE car_id = ?";

            PreparedStatement rateStmt = conn.prepareStatement(rateQuery);

            rateStmt.setInt(1, carId);



            ResultSet rateRs = rateStmt.executeQuery();

            double rentalRate = 0.0;

            if (rateRs.next()) {

                rentalRate = rateRs.getDouble("rental_rate");

            }



            // Step 2: Calculate the number of days between startDate and endDate

            LocalDate start = LocalDate.parse(startDate);

            LocalDate end = LocalDate.parse(endDate);

            long days = ChronoUnit.DAYS.between(start, end);



            // Ensure that the days are non-negative

            double totalCost = days > 0 ? rentalRate * days : 0.0;



            // Step 3: Insert the booking record without payment_status

            String bookingQuery = "INSERT INTO Bookings (user_id, car_id, driver_id, start_date, end_date, total_cost) "

                    + "VALUES (?, ?, ?, ?, ?, ?)";

            PreparedStatement bookingStmt = conn.prepareStatement(bookingQuery);

            bookingStmt.setInt(1, userId);

            bookingStmt.setInt(2, carId);

            bookingStmt.setInt(3, carId); // Placeholder for driver_id

            bookingStmt.setString(4, startDate);

            bookingStmt.setString(5, endDate);

            bookingStmt.setDouble(6, totalCost);

            bookingStmt.executeUpdate();



            // Update the car status to 'booked'

            String updateCarStatus = "UPDATE Cars SET status = 'booked' WHERE car_id = ?";

            PreparedStatement updateStmt = conn.prepareStatement(updateCarStatus);

            updateStmt.setInt(1, carId);

            updateStmt.executeUpdate();



            JOptionPane.showMessageDialog(null, "Booking successful! Total cost: " + totalCost);

            displayBookings();



        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(null, "Booking failed. Try again.");

        }

    }



    private void displayBookings() {

        try {

            String query = "SELECT * FROM Bookings";

            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery(query);



            System.out.println("\nCurrent Bookings:");

            while (rs.next()) {

                System.out.printf("Booking ID: %d, User ID: %d, Car ID: %d, Start Date: %s, End Date: %s, Total Cost: %.2f\n",

                        rs.getInt("booking_id"), rs.getInt("user_id"), rs.getInt("car_id"),

                        rs.getString("start_date"), rs.getString("end_date"),

                        rs.getDouble("total_cost"));

            }

        } catch (Exception e) {

            e.printStackTrace();

        }

    }



    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            new CarRentalUI().setVisible(true);

        });

    }

}


