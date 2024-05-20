/*
 * LibraryModel.java
 * Author:
 * Created on:
 */

import javax.swing.*;
import java.sql.*;

public class LibraryModel {
    //Credentials
    String url = "jdbc:postgresql://localhost:5433/LibraryDB";
    Connection connection = null;
    // For use in creating dialogs and making them modal
    private JFrame dialogParent;
    public LibraryModel(JFrame parent, String userid, String password) {
	    dialogParent = parent;
        try {
            Class.forName("org.postgresql.Driver");

            connection = DriverManager.getConnection(url, userid, password);

            if (connection != null) {
                System.out.println("Successful connection to DB!");
            } else {
                System.out.println("Failed to connect to DB.");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("PostgeSQL JDBC Driver was not found");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("DB Connection failed.");
            e.printStackTrace();
        }
    }

    public String bookLookup(int isbn) {
	    return "Lookup Book Stub";
    }

    public String showCatalogue() {
	    return "Show Catalogue Stub";
    }

    public String showLoanedBooks() {
	    return "Show Loaned Books Stub";
    }

    public String showAuthor(int authorID) {
	    return "Show Author Stub";
    }
    public String showAllAuthors() {
        String sql = "SELECT " + Constants.AUTHOR_NAME + ", " + Constants.AUTHOR_SURNAME + " FROM author";
        StringBuilder allAuthors = new StringBuilder("Show All Authors Stub");

        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            allAuthors.setLength(0);

            while (resultSet.next()) {
                String name = resultSet.getString(Constants.AUTHOR_NAME);
                String surname = resultSet.getString(Constants.AUTHOR_SURNAME);
                allAuthors.append(name).append("\t").append(surname).append('\n');
            }
        } catch (SQLException e) {
            System.out.println("Failed to show all authors");
            closeDBConnection();
            e.printStackTrace();
        }

	    return allAuthors.toString();
    }

    public String showCustomer(int customerID) {
	    return "Show Customer Stub";
    }

    public String showAllCustomers() {
        String sql = "SELECT " + Constants.CUSTOMER_LAST_NAME + ", " +
                Constants.CUSTOMER_FIRST_NAME + " FROM customer";
        StringBuilder allCustomers = new StringBuilder("Show All Customers Stub");

        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            allCustomers.setLength(0);

            while(resultSet.next()) {
                String lastName = resultSet.getString(Constants.CUSTOMER_LAST_NAME);
                String firstName = resultSet.getString(Constants.CUSTOMER_FIRST_NAME);
                allCustomers.append(firstName).append("\t").append(lastName).append('\n');
            }
        } catch (SQLException e) {
            System.out.println("Failed to show all customers");
            closeDBConnection();
            e.printStackTrace();
        }
        return allCustomers.toString();
    }

    public String borrowBook(int isbn, int customerID,
			     int day, int month, int year) {
	return "Borrow Book Stub";
    }

    public String returnBook(int isbn, int customerid) {
	return "Return Book Stub";
    }

    public void closeDBConnection() {
        if (connection!= null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public String deleteCus(int customerID) {
    	return "Delete Customer";
    }

    public String deleteAuthor(int authorID) {
    	return "Delete Author";
    }

    public String deleteBook(int isbn) {
    	return "Delete Book";
    }
}

class Constants {
    public static final String AUTHOR_NAME = "name";
    public static final String AUTHOR_SURNAME = "surname";
    public static final String CUSTOMER_LAST_NAME = "l_name";
    public static final String CUSTOMER_FIRST_NAME = "f_name";
}