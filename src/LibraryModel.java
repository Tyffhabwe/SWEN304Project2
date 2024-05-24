/*
 * LibraryModel.java
 * Author:
 * Created on:
 */

import javax.swing.*;
import java.sql.*;
import java.util.*;

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
    //todo: FIX -1 SEARCH
    public String bookLookup(int isbn) {
        StringBuilder result = new StringBuilder("Lookup Book Stub");
        String sql = "SELECT * FROM book\n" +
                "LEFT OUTER JOIN book_author\n" +
                "ON book.isbn = book_author.isbn\n" +
                "LEFT OUTER JOIN author\n" +
                "ON book_author.authorid = author.authorid\n" +
                "WHERE book.isbn = + " + isbn + "\n" +
                "ORDER BY authorseqno";

        try(Statement statement = connection.createStatement()) {
            String booktitle = "bookTitle";
            int edition_no = -1;
            int numcopies = -1;
            int numleft = -1;
            List<String> authors = new ArrayList<>();

            ResultSet resultSet = statement.executeQuery(sql);
            result.setLength(0);
            result.append("Book Lookup:\n").append("\t").append(isbn).append(": ");

            while (resultSet.next()) {
                booktitle = resultSet.getString(Constants.BOOK_TITLE);
                edition_no = resultSet.getInt(Constants.BOOK_EDITION_NUM);
                numcopies = resultSet.getInt(Constants.BOOK_NUMBER_COPIES);
                numleft = resultSet.getInt(Constants.BOOK_NUM_LEFT);
                authors.add(resultSet.getString(Constants.AUTHOR_SURNAME).trim());
            }

            result.append(booktitle).append('\n')
                    .append("\tEdition: ").append(edition_no)
                    .append(" - Number of copies: ").append(numcopies)
                    .append(" - Copies left: ").append(numleft).append('\n');

            if (authors.isEmpty()) {
                result.append("(no authors)");
            } else {
                result.append("\tAuthors: ").append(String.join(", ", authors));
            }

            return  result.toString();
        } catch (SQLException e) {
            System.out.println("Could not look up that isbn");
            closeDBConnection();
            e.printStackTrace();
        }

        return result.toString();
    }

    public String showCatalogue() {
        String sql = "SELECT book.isbn, book.title, book.edition_no, book.numofcop, book.numleft, surname, authorseqno FROM book\n" +
                "LEFT OUTER JOIN book_author\n" +
                "ON book.isbn = book_author.isbn\n" +
                "LEFT OUTER JOIN author\n" +
                "ON book_author.authorid = author.authorid\n" +
                "ORDER BY book.isbn;\n";
        StringBuilder builder = new StringBuilder("Show Catalogue Stub");
        Map<Integer, CatalogueInformation> catalogueInfo = new HashMap<>();

        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            builder.setLength(0);

            while (resultSet.next()) {
                String title = resultSet.getString(Constants.BOOK_TITLE);
                int isbn = resultSet.getInt(Constants.BOOK_ISBN);
                int editionNo = resultSet.getInt(Constants.BOOK_EDITION_NUM);
                int numofcop = resultSet.getInt(Constants.BOOK_NUMBER_COPIES);
                int numleft = resultSet.getInt(Constants.BOOK_NUM_LEFT);

                CatalogueInformation catalogueInformation = catalogueInfo.getOrDefault(isbn,
                        new CatalogueInformation(title, editionNo, numofcop, numleft,
                                new ArrayList<>()));


                String authorSurname = resultSet.getString(Constants.AUTHOR_SURNAME);
                if (authorSurname == null) authorSurname = "(no authors)";

                int authorseqno = resultSet.getInt(Constants.AUTHOR_AUTHORSEQNO);
                catalogueInformation.authors().add(new Author(authorSurname, authorseqno));

                catalogueInfo.put(isbn, catalogueInformation);
            }

            catalogueInfo.entrySet().stream()
                    .sorted(Comparator.comparingInt(Map.Entry::getKey))
                    .forEach(
                    entry -> {
                        CatalogueInformation info = entry.getValue();
                        int isbn = entry.getKey();
                        builder.append(isbn).append(": ").append(info.title()).append('\n');
                        builder.append("\tEdition: ").append(info.editionNo())
                                .append(" - Number of copies: ").append(info.numofcop())
                                .append(" - Copies left: ").append(info.numleft()).append('\n');

                        List<String> authorNames = info.authors().stream()
                                .sorted(Comparator.comparingInt(Author::authorseqno))
                                .map(Author::surname)
                                .map(String::trim)
                                .toList();

                        builder.append("\tAuthors: ")
                                .append(String.join(", ", authorNames))
                                .append('\n');
                    }
            );

            resultSet.close();
        } catch (SQLException e) {
            System.out.println("Failed to show the full catalogue");
            closeDBConnection();
            e.printStackTrace();
        }
	    return builder.toString();
    }

    public String showLoanedBooks() {
	    return "Show Loaned Books Stub";
    }
    //todo: fix -1 search
    public String showAuthor(int authorID) {
        String sql = "SELECT author.authorid, name, surname, book_author.isbn, title FROM author\n" +
                "LEFT JOIN book_author\n" +
                "ON author.authorid = book_author.authorid\n" +
                "LEFT JOIN book\n" +
                "ON book_author.isbn = book.isbn\n" +
                "WHERE author.authorid = " +  authorID;
        StringBuilder builder = new StringBuilder("Show Author Stub");
        try(Statement statement = connection.createStatement()) {
            String authorname = "unknown";
            List<String> booksWritten = new ArrayList<>();

            ResultSet resultSet = statement.executeQuery(sql);
            builder.setLength(0);
            builder.append("Show Author:\n");

            while(resultSet.next()) {
                authorname = resultSet.getString(Constants.AUTHOR_NAME).trim()
                        + " " + resultSet.getString(Constants.AUTHOR_SURNAME).trim();
                booksWritten.add(
                        resultSet.getInt(Constants.BOOK_ISBN)
                        + " - "
                        + resultSet.getString(Constants.BOOK_TITLE)
                );
            }
        } catch (SQLException e) {
            System.out.println("Could not look up that authorID");
            closeDBConnection();
            e.printStackTrace();
        }

	    return builder.toString();
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

record CatalogueInformation(String title, int editionNo, int numofcop, int numleft, List<Author> authors) {}
record Author(String surname, int authorseqno) {}
class Constants {
    public static final String AUTHOR_NAME = "name";
    public static final String AUTHOR_SURNAME = "surname";
    public static final String AUTHOR_AUTHORSEQNO = "authorseqno";
    public static final String CUSTOMER_LAST_NAME = "l_name";
    public static final String CUSTOMER_FIRST_NAME = "f_name";
    public static final String BOOK_ISBN = "isbn";
    public static final String BOOK_TITLE = "title";
    public static final String BOOK_EDITION_NUM = "edition_no";
    public static final String BOOK_NUMBER_COPIES = "numofcop";
    public static final String BOOK_NUM_LEFT = "numleft";
}