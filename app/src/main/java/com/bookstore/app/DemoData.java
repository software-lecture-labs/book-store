package com.bookstore.app;

import com.bookstore.customer.api.AccountFacade;
import com.bookstore.catalog.domain.Author;
import com.bookstore.catalog.domain.Book;
import com.bookstore.catalog.domain.Category;
import com.bookstore.catalog.domain.Isbn;
import com.bookstore.catalog.domain.Language;
import com.bookstore.catalog.domain.Stock;
import com.bookstore.catalog.persistence.JdbcAuthorRepository;
import com.bookstore.catalog.persistence.JdbcBookRepository;
import com.bookstore.catalog.persistence.JdbcCategoryRepository;
import com.bookstore.customer.api.CustomerView;
import com.bookstore.customer.api.RegistrationCommand;
import com.bookstore.customer.domain.Customer;
import com.bookstore.customer.domain.Email;
import com.bookstore.customer.domain.PasswordHasher;
import com.bookstore.customer.domain.PersonName;
import com.bookstore.customer.persistence.JdbcCustomerRepository;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.jdbc.JdbcTransactionRunner;
import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.money.Money;

import java.time.Clock;
import java.time.Year;

import javax.sql.DataSource;

/**
 * A small catalog and two accounts, so the demo server has something to show. The catalog has no
 * administration endpoints until Phase 8, so the books are stored straight through the catalog's
 * repositories; the customer registers through the account façade like anyone else.
 */
public final class DemoData {

    /** The demo administrator's e-mail address. */
    public static final String ADMIN_EMAIL = "admin@bookstore.local";

    /** The demo customer's e-mail address. */
    public static final String CUSTOMER_EMAIL = "ayse@bookstore.local";

    /** The password of both demo accounts. */
    public static final String PASSWORD = "kitap2026";

    private DemoData() {
    }

    /**
     * Stores the demo data, unless the catalog already has books (for example after a restart on
     * PostgreSQL).
     *
     * @param dataSource the database
     * @param accounts   the account use cases, to register the demo customer
     * @param hasher     the password hasher the application uses
     * @param clock      the clock the application uses
     */
    public static void load(DataSource dataSource, AccountFacade accounts, PasswordHasher hasher, Clock clock) {
        JdbcExecutor jdbc = new JdbcExecutor(new JdbcTransactionRunner(dataSource));
        if (jdbc.queryForLong("SELECT COUNT(*) FROM books") > 0) {
            return;
        }
        JdbcCategoryRepository categories = new JdbcCategoryRepository(jdbc);
        JdbcAuthorRepository authors = new JdbcAuthorRepository(jdbc);
        JdbcBookRepository books = new JdbcBookRepository(jdbc, categories);

        Category literature = categories.save(Category.root("Edebiyat"));
        Category novels = categories.save(literature.addSubcategory("Roman"));
        Category poetry = categories.save(literature.addSubcategory("Şiir"));
        Category science = categories.save(Category.root("Bilim"));
        Category scienceFiction = categories.save(literature.addSubcategory("Bilimkurgu"));

        Author ali = authors.save(Author.create("Sabahattin Ali"));
        Author pamuk = authors.save(Author.create("Orhan Pamuk"));
        Author hikmet = authors.save(Author.create("Nâzım Hikmet"));
        Author herbert = authors.save(Author.create("Frank Herbert"));
        Author leGuin = authors.save(Author.create("Ursula K. Le Guin"));
        Author sagan = authors.save(Author.create("Carl Sagan"));

        books.save(book("975363802", "Kürk Mantolu Madonna", ali, novels, Language.TURKISH, 1943, "95.00", 12));
        books.save(book("975363803", "İçimizdeki Şeytan", ali, novels, Language.TURKISH, 1940, "88.50", 4));
        books.save(book("975080119", "Masumiyet Müzesi", pamuk, novels, Language.TURKISH, 2008, "210.00", 7));
        books.save(book("975080120", "Benim Adım Kırmızı", pamuk, novels, Language.TURKISH, 1998, "185.00", 0));
        books.save(book("975050201", "Memleketimden İnsan Manzaraları", hikmet, poetry, Language.TURKISH, 1966,
                "140.00", 3));
        books.save(book("044117271", "Dune", herbert, scienceFiction, Language.ENGLISH, 1965, "350.00", 9));
        books.save(book("044147812", "The Left Hand of Darkness", leGuin, scienceFiction, Language.ENGLISH, 1969,
                "290.00", 2));
        books.save(book("034553943", "Cosmos", sagan, science, Language.ENGLISH, 1980, "320.00", 5));

        new JdbcCustomerRepository(jdbc).save(Customer.createAdmin(Email.of(ADMIN_EMAIL),
                new PersonName("Site", "Yöneticisi"), hasher.hash(PASSWORD), clock));
        CustomerView ayse = accounts.register(
                new RegistrationCommand(CUSTOMER_EMAIL, "Ayşe", "Yılmaz", PASSWORD));
        accounts.addAddress(ayse.id(), "Ev", new Address("Ayşe Yılmaz", "05551234567",
                "Kitap Sokak No: 5 Daire: 3", "Kadıköy", "İstanbul", "34710"), true);
    }

    private static Book book(String isbnBody, String title, Author author, Category category, Language language,
                             int year, String price, int stock) {
        return Book.builder()
                .isbn(isbn("978" + isbnBody))
                .title(title)
                .author(author)
                .category(category)
                .language(language)
                .publicationYear(Year.of(year))
                .price(Money.of(price))
                .stock(Stock.of(stock))
                .build();
    }

    /** Completes twelve digits with the ISBN-13 check digit. */
    private static Isbn isbn(String first12) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (first12.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return new Isbn(first12 + (10 - sum % 10) % 10);
    }
}
