/*
 * LibraryModel.java
 * Author:
 * Created on:
 */

import javax.swing.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.time.LocalDate;

public class LibraryModel {
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

            connection.setAutoCommit(false);
        } catch (ClassNotFoundException e) {
            System.out.println("PostgeSQL JDBC Driver was not found");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("DB Connection failed.");
            e.printStackTrace();
        }
    }
    public String bookLookup(int isbn) {
        StringBuilder builder = new StringBuilder("Book Lookup:\n");
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

            while (resultSet.next()) {
                booktitle = resultSet.getString(Constants.BOOK_TITLE);
                edition_no = resultSet.getInt(Constants.BOOK_EDITION_NUM);
                numcopies = resultSet.getInt(Constants.BOOK_NUMBER_COPIES);
                numleft = resultSet.getInt(Constants.BOOK_NUM_LEFT);
                authors.add(resultSet.getString(Constants.AUTHOR_SURNAME).trim());
            }

            if (numcopies == -1) throw new BookNotFoundException(
                    "\tBook with isbn: " + isbn + " not found in DB."
            );

            builder.append("\t").append(isbn).append(": ");
            builder.append(booktitle).append('\n')
                    .append("\tEdition: ").append(edition_no)
                    .append(" - Number of copies: ").append(numcopies)
                    .append(" - Copies left: ").append(numleft).append('\n');

            if (authors.isEmpty()) {
                builder.append("(no authors)");
            } else {
                builder.append("\tAuthors: ").append(String.join(", ", authors));
            }
            connection.commit();
        } catch (SQLException e) {
            rollBack();
            e.printStackTrace();
        } catch (BookNotFoundException e) {
            builder.append(e.getMessage());
            e.printStackTrace();
        }
        return builder.toString();
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

            connection.commit();
        } catch (SQLException e) {
            rollBack();
            e.printStackTrace();
        }
	    return builder.toString();
    }

    public String showLoanedBooks() {
        StringBuilder builder = new StringBuilder();
        Map<Integer, CatalogueInformation> map = new HashMap<>();

        String sql = "SELECT book.isbn, title, edition_no, numofcop, numleft,\n" +
                "\tauthor.surname, book_author.authorseqno, \n" +
                "\tcustomer.customerid, l_name, f_name, city\n" +
                "FROM book\n" +
                "LEFT OUTER JOIN book_author\n" +
                "ON book.isbn = book_author.isbn\n" +
                "LEFT OUTER JOIN author\n" +
                "ON book_author.authorid = author.authorid\n" +
                "INNER JOIN cust_book\n" +
                "ON book.isbn = cust_book.isbn\n" +
                "NATURAL JOIN customer;";
        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            while(resultSet.next()) {
                int isbn = resultSet.getInt(Constants.BOOK_ISBN);
                String title = resultSet.getString(Constants.BOOK_TITLE);
                int editionNo = resultSet.getInt(Constants.BOOK_EDITION_NUM);
                int numofcop = resultSet.getInt(Constants.BOOK_NUMBER_COPIES);
                int numleft = resultSet.getInt(Constants.BOOK_NUM_LEFT);

                CatalogueInformation catalogueInformation = map.getOrDefault(isbn,
                        new CatalogueInformation(title, editionNo, numofcop, numleft,
                                new ArrayList<>()));

                String authorSurname = resultSet.getString(Constants.AUTHOR_SURNAME);
                if (authorSurname == null) authorSurname = "(no authors)";

                int authorseqno = resultSet.getInt(Constants.AUTHOR_AUTHORSEQNO);
                catalogueInformation.authors().add(new Author(authorSurname, authorseqno));

                int customerId = resultSet.getInt(Constants.CUSTOMER_CUSTOMER_ID);
                String lName = resultSet.getString(Constants.CUSTOMER_LAST_NAME);
                String fName = resultSet.getString(Constants.CUSTOMER_FIRST_NAME);
                String city = resultSet.getString(Constants.CUSTOMER_CITY);

                catalogueInformation.customers().add(
                        new Customer(customerId, lName, fName, city)
                );
                map.put(isbn, catalogueInformation);
            }

            map.entrySet().stream()
                    .sorted(Comparator.comparingDouble(Map.Entry::getKey))
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

                            builder.append("\tBorrowers: \n");
                            info.customers().stream()
                                    .sorted(Comparator.comparingInt(Customer::customerId))
                                    .map(Customer::toString)
                                    .forEach(
                                        toString -> {
                                            builder.append("\t\t").append(toString).append('\n');
                                        }
                                    );

                        }
                    );
            connection.commit();
        } catch (SQLException e) {
            rollBack();
            e.printStackTrace();
        }
        return builder.toString();
    }
    public String showAuthor(int authorID) {
        String sql = "SELECT author.authorid, name, surname, book_author.isbn, title FROM author\n" +
                "LEFT JOIN book_author\n" +
                "ON author.authorid = book_author.authorid\n" +
                "LEFT JOIN book\n" +
                "ON book_author.isbn = book.isbn\n" +
                "WHERE author.authorid = " +  authorID;
        StringBuilder builder = new StringBuilder("Show Author:\n");
        try(Statement statement = connection.createStatement()) {
            String authorname = "unknown";
            List<String> booksWritten = new ArrayList<>();

            ResultSet resultSet = statement.executeQuery(sql);

            while(resultSet.next()) {
                authorname = resultSet.getString(Constants.AUTHOR_NAME).trim()
                        + " " + resultSet.getString(Constants.AUTHOR_SURNAME).trim();
                String bookStuff = resultSet.getInt(Constants.BOOK_ISBN)
                            + " - "
                            + resultSet.getString(Constants.BOOK_TITLE);

                if (resultSet.getString(Constants.BOOK_TITLE) == null) bookStuff = "(no books written)";

                booksWritten.add(bookStuff);
            }

            if (authorname.equals("unknown")) throw new AuthorNotFoundException(
                    "\tAuthor with authorID: " + authorID + " not found"
            );

            builder.append('\t').append(authorID)
                    .append(" - ").append(authorname).append('\n')
                    .append("\tBooks written: \n");

            for (String bookStuff: booksWritten) builder.append("\t\t").append(bookStuff).append('\n');
            connection.commit();
        } catch (SQLException e) {
            rollBack();
            e.printStackTrace();
        } catch (AuthorNotFoundException e) {
            builder.append(e.getMessage());
            e.printStackTrace();
        }

        return builder.toString();
    }
    public String showAllAuthors() {
        String sql = "SELECT * FROM author\n" +
                "ORDER BY authorid";

        StringBuilder allAuthors = new StringBuilder("Show All Authors Stub");
        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            allAuthors.setLength(0);
            allAuthors.append("Show All Authors: \n");

            while (resultSet.next()) {
                String name = resultSet.getString(Constants.AUTHOR_NAME).trim();
                String surname = resultSet.getString(Constants.AUTHOR_SURNAME).trim();
                int authorid = resultSet.getInt(Constants.AUTHOR_AUTHORID);
                allAuthors.append('\t').append(authorid).append(": ")
                        .append(name).append(", ").append(surname).append('\n');
            }
            connection.commit();
        } catch (SQLException e) {
            rollBack();
            e.printStackTrace();
        }

	    return allAuthors.toString();
    }
    public String showCustomer(int customerID) {
        StringBuilder builder = new StringBuilder("Show customer: \n");

        String sql = "SELECT * FROM customer\n" +
                "LEFT OUTER JOIN cust_book\n" +
                "ON customer.customerid = cust_book.customerid\n" +
                "LEFT OUTER JOIN book\n" +
                "ON cust_book.isbn = book.isbn\n" +
                "WHERE customer.customerid = " + customerID;

        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            List<Book> booksBorrowed = new ArrayList<>();
            Customer customer = null;

            while (resultSet.next()) {
                String lName = resultSet.getString(Constants.CUSTOMER_LAST_NAME);
                String fName = resultSet.getString(Constants.CUSTOMER_FIRST_NAME);
                String city = resultSet.getString(Constants.CUSTOMER_CITY);

                customer = new Customer(customerID, lName, fName, city);
                int isbn = resultSet.getInt(Constants.BOOK_ISBN);
                String title = resultSet.getString(Constants.BOOK_TITLE);
                if (title == null) break;
                booksBorrowed.add(new Book(isbn, title, -1 , -1, -1));
            }

            if (customer == null) throw new CustomerNotFoundException(
                    "\tCould not find customer with id: " + customerID
            );

            builder.append("\t").append(customer).append('\n')
                    .append("\tBooks borrowed: \n");

            if (!booksBorrowed.isEmpty()) {
                booksBorrowed
                        .forEach(
                                book -> {
                                    builder.append("\t\t").append(book.isbn())
                                            .append(" - ").append(book.title().trim())
                                            .append('\n');
                                }
                        );
            } else {
                builder.append("\t\t(no books borrowed)");
            }
            connection.commit();
        } catch (SQLException e) {
            rollBack();
            e.printStackTrace();
        } catch (CustomerNotFoundException e) {
            builder.append(e.getMessage());
            e.printStackTrace();
        }
        return builder.toString();
    }
    public String showAllCustomers() {
        String sql = "SELECT * FROM CUSTOMER";
        StringBuilder allCustomers = new StringBuilder("Show All Customers: \n");

        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);

            while(resultSet.next()) {
                int customerId = resultSet.getInt(Constants.CUSTOMER_CUSTOMER_ID);
                String lastName = resultSet.getString(Constants.CUSTOMER_LAST_NAME);
                String firstName = resultSet.getString(Constants.CUSTOMER_FIRST_NAME);
                String city = resultSet.getString(Constants.CUSTOMER_CITY);

                if (city == null) city = "(no city)";

                allCustomers.append("\t").append(customerId).append(": ")
                        .append(lastName.trim()).append(", ").append(firstName.trim()).append(" - ")
                        .append(city.trim()).append('\n');
            }
            connection.commit();
        } catch (SQLException e) {
            rollBack();
            e.printStackTrace();
        }
        return allCustomers.toString();
    }
    //todo: dialogue box and output when success
    public String borrowBook(int isbn, int customerID,
			     int day, int month, int year) {
        StringBuilder builder = new StringBuilder("Borrow book: \n\t");
        try {
            Customer customer = getCustomer(customerID);
            Book book = getBook(isbn);

            if(book.numleft() < 1) throw new CannotBorrowBookException();

            lockQuery("LOCK TABLE cust_book IN EXCLUSIVE MODE");
            String insertCustBookString = "INSERT INTO cust_book (isbn, duedate, customerid) " +
                    "VALUES (?, ?, ?);";
            CustBook custBook = new CustBook(customer.customerId(),
                    Date.valueOf(LocalDate.of(year, month, day)), isbn);
            boolean updateCustBook = updateCustBook(insertCustBookString, custBook);
            if (!updateCustBook) throw new SQLException("Could not update borrow book info");

            int response = JOptionPane.showConfirmDialog(dialogParent,
                    "Locked the tuple(s), ready to update. Click OK to continue.",
                    "Confirm",
                    JOptionPane.OK_CANCEL_OPTION);
            if (response == JOptionPane.OK_CANCEL_OPTION) throw new SQLException(
                    "User did not want to continue"
            );

            String updateBookString = "UPDATE book SET isbn = ?," + " title = ?, edition_no = ?, " +
                    "numofcop = ?, numleft = ? WHERE isbn = ?";
            boolean updateBook = updateBook(updateBookString,
                    new Book(isbn,
                            book.title(),
                            book.editionNo(),
                            book.numofcop() - 1,
                            book.numleft() - 1));
            if (!updateBook) throw new SQLException("Could not update book information");

            builder.append("Book: ").append(isbn).append("(").append(book.title().trim()).
                    append(")\n").append("\tLoaned to: ").append(customer.customerId()).
                    append("(").append(customer.fName().trim()).append(" ")
                    .append(customer.lName().trim()).append(")\n").append("\tDue date: ")
                    .append(custBook.dueDate().toString().trim());
            connection.commit();
        } catch (BookNotFoundException | CustomerNotFoundException | CannotBorrowBookException e) {
            builder.append(e.getMessage());
            rollBack();
            e.printStackTrace();
        } catch (SQLException e) {
            builder.append("Could not insert into the DB");
            rollBack();
            e.printStackTrace();
        }
        return builder.toString();
    }
    public boolean updateBook(String sql, Book book) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, book.isbn());
        statement.setString(2, book.title());
        statement.setInt(3, book.editionNo());
        statement.setInt(4, book.numofcop());
        statement.setInt(5, book.numleft());
        statement.setInt(6, book.isbn());
        return statement.executeUpdate() > 0;
    }
    public boolean updateCustBook(String sql, CustBook custBook) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, custBook.isbn());
        statement.setDate(2, custBook.dueDate());
        statement.setInt(3, custBook.customerId());
        return statement.executeUpdate() > 0;
    }
    public Book getBook(int isbn) throws BookNotFoundException {
        String sql = "SELECT * FROM book\n" +
                "WHERE isbn = " + isbn + " FOR UPDATE;";
        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                return new Book(
                        resultSet.getInt(Constants.BOOK_ISBN),
                        resultSet.getString(Constants.BOOK_TITLE),
                        resultSet.getInt(Constants.BOOK_EDITION_NUM),
                        resultSet.getInt(Constants.BOOK_NUMBER_COPIES),
                        resultSet.getInt(Constants.BOOK_NUM_LEFT)
                );
            }
        } catch (SQLException e) {
            System.out.println("GET BOOK QUERY STATEMENT FAILED!");
            closeDBConnection();
            e.printStackTrace();
        }
        throw new BookNotFoundException("Could not find that book");
    }
    public Customer getCustomer(int customerId) throws CustomerNotFoundException {
        String sql = "SELECT * FROM customer\n" +
                "WHERE customerid = " + customerId + " FOR UPDATE;";
        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                return new Customer(
                        resultSet.getInt(Constants.CUSTOMER_CUSTOMER_ID),
                        resultSet.getString(Constants.CUSTOMER_LAST_NAME),
                        resultSet.getString(Constants.CUSTOMER_FIRST_NAME),
                        resultSet.getString(Constants.CUSTOMER_CITY)
                );
            }
        } catch (SQLException e) {
            System.out.println("GET CUSTOMER QUERY STATEMENT FAILED!");
            closeDBConnection();
            e.printStackTrace();
        }
        throw new NoSuchElementException("Could not find customer customerId: " + customerId);
    }
    public Author getAuthor(int authorId) throws AuthorNotFoundException {
        String sql = "SELECT * FROM author\n" +
                "WHERE authorid = " + authorId + " FOR UPDATE;";
        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                return new Author(
                        resultSet.getString(Constants.AUTHOR_SURNAME),
                        resultSet.getInt(Constants.AUTHOR_AUTHORID)
                );
            }
        } catch (SQLException e) {
            System.out.println("STATEMENT FAIL AUTHOR PRINT");
            e.printStackTrace();
        }
        throw new AuthorNotFoundException("Did not find Author " + authorId);
    }
    public CustBook getCustBook(int customerId, int isbn) throws CustBookNotFoundException {
        String sql = "SELECT * FROM cust_book\n" +
                "WHERE isbn = " + isbn + " AND customerid = " + customerId + " FOR UPDATE;";
        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                return  new CustBook(
                        customerId,
                        resultSet.getDate(Constants.CUST_BOOK_DUE_DATE),
                        isbn
                );
            }
        } catch (SQLException e) {
            System.out.println("CUST BOOK QUERY FAILED");
            e.printStackTrace();
        }
        throw new CustBookNotFoundException("Customer " + customerId + " did not borrow " + isbn);
    }
    //todo: dialogue box and output when success
    public String returnBook(int isbn, int customerid) {
        StringBuilder builder = new StringBuilder("Return book: \n");
        try {
            CustBook custBook = getCustBook(customerid, isbn);
            Book book = getBook(isbn);

            boolean deleted = deleteCustBook(custBook);
            if (!deleted) throw  new SQLException("Did not delete book correctly.");

            String sql = "UPDATE book SET isbn = ?," + " title = ?, edition_no = ?, " +
                    "numofcop = ?, numleft = ? WHERE isbn = ?";
            boolean updated = updateBook(sql, new Book(
                    book.isbn(),
                    book.title(),
                    book.editionNo(),
                    book.numofcop() + 1,
                    book.numleft() + 1
            ));

            if (!updated) throw new SQLException("Did not update book correctly.");
            builder.append("\t Book ").append(isbn)
                    .append(" returned for customer ").append(customerid);

        } catch (CustBookNotFoundException | BookNotFoundException e) {
            builder.append(e.getMessage());
            rollBack();
            e.printStackTrace();
        } catch (SQLException e) {
            rollBack();
            e.printStackTrace();
        }
        return builder.toString();
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
        StringBuilder builder = new StringBuilder();
        try {
            String sql = "DELETE FROM customer \n" +
                    "WHERE customerid = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, customerID);

            boolean worked = statement.executeUpdate() > 0;
            if (!worked) throw new CustomerNotFoundException(
                    "\tCustomer with customerid: " + customerID + " was not found."
            );
            builder.append("\tCustomer with customerid: ").append(customerID)
                    .append(" has been deleted.");
            connection.commit();
        } catch (CustomerNotFoundException e) {
            builder.append(e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            rollBack();
            e.printStackTrace();
        }
        return builder.toString();
    }
    public String deleteAuthor(int authorID) {
        StringBuilder builder = new StringBuilder("Delete Author: \n");
        try {
            String sql = "DELETE FROM author \n" +
                    "WHERE authorid = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, authorID);
            boolean worked = statement.executeUpdate() > 0;

            if (!worked) throw new AuthorNotFoundException(
                    "\tAuthor with authorid: " + authorID + " was not found."
            );
            builder.append("\tAuthor with authorid: ").append(authorID)
                    .append(" has been deleted.");
            connection.commit();
        } catch (AuthorNotFoundException e) {
            builder.append(e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            rollBack();
            e.printStackTrace();
        }
        return builder.toString();
    }
    public String deleteBook(int isbn) {
        StringBuilder builder = new StringBuilder("Delete Book: \n");
        try {
            String sql = "DELETE FROM book \n" +
                    "WHERE isbn = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, isbn);
            boolean worked = statement.executeUpdate() > 0;

            if (!worked) throw new BookNotFoundException("" +
                    "\tBook with ISBN: " + isbn + " does not seem to exist."
            );
            builder.append("\tBook with ISBN: ").append(isbn)
                    .append(" has been deleted.");
            connection.commit();
        } catch (BookNotFoundException e) {
            builder.append(e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            rollBack();
            e.printStackTrace();
        }
        return builder.toString();
    }
    public boolean deleteCustBook(CustBook custBook) throws SQLException {
        String sql = "DELETE FROM cust_book\n" +
                "WHERE isbn = ? AND customerid = ?;";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, custBook.isbn());
        statement.setInt(2, custBook.customerId());
        return statement.executeUpdate() > 0;
    }
    public void lockQuery(String sql) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.execute();
    }
    private void rollBack() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
record CatalogueInformation(String title, int editionNo, int numofcop,
                            int numleft, List<Author> authors, Set<Customer> customers) {
    public CatalogueInformation(String title, int editionNo, int numofcop,
                                int numleft, List<Author> authors) {
        this(title, editionNo, numofcop, numleft, authors, new HashSet<>());
    }
}
record Author(String surname, int authorseqno) {}
record Book(int isbn, String title, int editionNo, int numofcop, int numleft) { }
record Customer(int customerId, String lName, String fName, String city) {
    @Override
    public String toString() {
        return customerId + ": " + lName.trim() +
                ", " + fName.trim() + " - " + city.trim();
    }
}
record CustBook(int customerId, java.sql.Date dueDate, int isbn) {}
class BookNotFoundException extends Exception {
    public BookNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
class CannotBorrowBookException extends Exception {
    public CannotBorrowBookException() {
        super();
    }
}
class CustomerNotFoundException extends Exception {
    public CustomerNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
class AuthorNotFoundException extends Exception {
    public AuthorNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
class CustBookNotFoundException extends Exception {
    public CustBookNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
class Constants {
    public static final String AUTHOR_NAME = "name";
    public static final String AUTHOR_SURNAME = "surname";
    public static final String AUTHOR_AUTHORSEQNO = "authorseqno";
    public static final String AUTHOR_AUTHORID = "authorid";
    public static final String CUSTOMER_LAST_NAME = "l_name";
    public static final String CUSTOMER_FIRST_NAME = "f_name";
    public static final String CUSTOMER_CUSTOMER_ID = "customerid";
    public static final String CUSTOMER_CITY = "city";
    public static final String CUST_BOOK_DUE_DATE = "duedate";
    public static final String BOOK_ISBN = "isbn";
    public static final String BOOK_TITLE = "title";
    public static final String BOOK_EDITION_NUM = "edition_no";
    public static final String BOOK_NUMBER_COPIES = "numofcop";
    public static final String BOOK_NUM_LEFT = "numleft";
}