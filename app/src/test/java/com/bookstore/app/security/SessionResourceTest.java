package com.bookstore.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.app.BookstoreModule;
import com.bookstore.app.DemoAdapters;
import com.bookstore.app.DemoData;
import com.bookstore.app.Pbkdf2PasswordHasher;
import com.bookstore.customer.api.CustomerView;
import com.bookstore.sales.api.CartRef;
import com.bookstore.sales.api.GuestCart;
import com.bookstore.shared.jdbc.ModuleMigrations;
import com.bookstore.shared.jdbc.TestDatabase;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.testing.TestClock;
import com.bookstore.shared.web.WebException;

import java.lang.reflect.Proxy;
import java.util.Set;

import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

/**
 * Signing in with {@code POST /api/v1/session} (ADR-0020): the real mechanism and identity store
 * over the H2 bookstore with the demo data, with the servlet objects faked. A visitor's cart joins
 * the stored cart and leaves the session (CRT-2).
 */
class SessionResourceTest {

    private static final BookId BOOK = BookId.of(1);

    private final TestDatabase database = TestDatabase.withModules(ModuleMigrations.ALL.toArray(String[]::new));
    private final BookstoreModule bookstore;
    private final GuestCart visitorCart = new GuestCart();
    private final FakeHttp http = new FakeHttp();
    private final SessionResource resource;

    SessionResourceTest() {
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(1_000);
        bookstore = new BookstoreModule(database.dataSource(), new BookstoreModule.Adapters(
                TestClock.CLOCK, DemoAdapters.approvingPayments(), hasher, event -> { }));
        DemoData.load(database.dataSource(), bookstore.accounts(), hasher, TestClock.CLOCK);
        AccountIdentityStore store = new AccountIdentityStore(bookstore.accounts());
        BookstoreAuthenticationMechanism mechanism = new BookstoreAuthenticationMechanism(
                credential -> credential instanceof UsernamePasswordCredential password
                        ? store.validate(password)
                        : CredentialValidationResult.NOT_VALIDATED_RESULT, bookstore.accounts());
        resource = new SessionResource(securityContextOver(mechanism), bookstore.accounts(), bookstore.cart(),
                () -> visitorCart);
    }

    @Test
    void signingInKeepsTheCustomerInTheSessionAndAnswersWithTheProfile() {
        CustomerView profile = signIn(DemoData.CUSTOMER_EMAIL, DemoData.PASSWORD);

        assertThat(profile.email()).isEqualTo(DemoData.CUSTOMER_EMAIL);
        assertThat(http.sessionAttributes.get(SessionCaller.ATTRIBUTE))
                .isEqualTo(new SessionCaller(Long.toString(profile.id().value()), Set.of("CUSTOMER")));
    }

    @Test
    void theVisitorsCartJoinsTheStoredCartAndLeavesTheSession() {
        bookstore.cart().add(CartRef.guest(visitorCart), BOOK, 2);

        CustomerView profile = signIn(DemoData.CUSTOMER_EMAIL, DemoData.PASSWORD);

        assertThat(bookstore.cart().view(CartRef.customer(profile.id())).lines())
                .anySatisfy(line -> {
                    assertThat(line.bookId()).isEqualTo(BOOK);
                    assertThat(line.quantity()).isEqualTo(2);
                });
        assertThat(visitorCart.isEmpty()).isTrue();
    }

    @Test
    void aWrongPasswordIsA401AndTheVisitorKeepsTheCart() {
        bookstore.cart().add(CartRef.guest(visitorCart), BOOK, 1);

        assertThatThrownBy(() -> signIn(DemoData.CUSTOMER_EMAIL, "wrong-password"))
                .isInstanceOfSatisfying(WebException.class, e -> {
                    assertThat(e.status()).isEqualTo(401);
                    assertThat(e.code()).isEqualTo("authentication.failed");
                });
        assertThat(visitorCart.quantityOf(BOOK)).isEqualTo(1);
        assertThat(http.sessionAttributes).isEmpty();
    }

    private CustomerView signIn(String email, String password) {
        return resource.signIn(new SessionResource.SignInRequest(email, password), http.request(), http.response());
    }

    /** A Jakarta Security context whose {@code authenticate} runs the mechanism, as the container does. */
    private SecurityContext securityContextOver(BookstoreAuthenticationMechanism mechanism) {
        return (SecurityContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{SecurityContext.class}, (self, method, args) -> {
                    if (!method.getName().equals("authenticate")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    http.authParameters = (AuthenticationParameters) args[2];
                    return mechanism.validateRequest((HttpServletRequest) args[0], (HttpServletResponse) args[1],
                            http.context());
                });
    }
}
