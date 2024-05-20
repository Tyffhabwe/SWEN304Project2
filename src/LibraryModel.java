/*
 * LibraryModel.java
 * Author:
 * Created on:
 */

import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class LibraryModel {
    //Credentials
    String url = "jdbc:postgresql://localhost:5433/LibraryDB";
    String user = "postgres";
    String password = "";
    Connection connection = null;
    // For use in creating dialogs and making them modal
    private JFrame dialogParent;

    public LibraryModel(JFrame parent, String userid, String password) {
	    dialogParent = parent;
        try {
            Class.forName("org.postgresql.Driver");

            connection = DriverManager.getConnection(url, user, password);

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
        } finally {
            if (connection!= null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
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
	return "Show All Authors Stub";
    }

    public String showCustomer(int customerID) {
	return "Show Customer Stub";
    }

    public String showAllCustomers() {
	return "Show All Customers Stub";
    }

    public String borrowBook(int isbn, int customerID,
			     int day, int month, int year) {
	return "Borrow Book Stub";
    }

    public String returnBook(int isbn, int customerid) {
	return "Return Book Stub";
    }

    public void closeDBConnection() {}

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