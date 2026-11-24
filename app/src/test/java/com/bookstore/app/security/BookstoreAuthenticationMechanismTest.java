package com.bookstore.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.customer.application.CustomerShop;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.web.Callers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import org.junit.jupiter.api.Test;

/**
 * The server's authentication mechanism (ADR-0020) over the real identity store and an in-memory
 * customer module: HTTP Basic, the session sign-in and the anonymous request.
 */
class BookstoreAuthenticationMechanismTest {

    private final CustomerShop shop = new CustomerShop();
    private final CustomerId ayse = shop.customer("ayse@example.com");
    private final AccountIdentityStore store = new AccountIdentityStore(shop.accounts);
    private final BookstoreAuthenticationMechanism mechanism = new BookstoreAuthenticationMechanism(
            credential -> credential instanceof UsernamePasswordCredential password
                    ? store.validate(password)
                    : CredentialValidationResult.NOT_VALIDATED_RESULT, shop.accounts);
    private final FakeHttp http = new FakeHttp();

    @Test
    void aRequestWithoutCredentialsStaysAnonymous() {
        assertThat(run()).isEqualTo(AuthenticationStatus.NOT_DONE);
        assertThat(http.signedInAs).isNull();
        assertThat(http.hasSession).isFalse();
    }

    @Test
    void basicCredentialsSignInWithTheCustomerIdAndRoleAndMakeNoSession() {
        http.headers.put("Authorization", basic("ayse@example.com", CustomerShop.PASSWORD));

        assertThat(run()).isEqualTo(AuthenticationStatus.SUCCESS);
        assertThat(http.signedInAs).isEqualTo(Long.toString(ayse.value()));
        assertThat(http.signedInGroups).containsExactly("CUSTOMER");
        assertThat(http.hasSession).isFalse();
    }

    @Test
    void wrongBasicCredentialsAreA401Problem() {
        http.headers.put("Authorization", basic("ayse@example.com", "wrong-password"));

        assertThat(run()).isEqualTo(AuthenticationStatus.SEND_FAILURE);
        assertThat(http.status).isEqualTo(401);
        assertThat(http.responseHeaders).containsEntry("Content-Type", "application/problem+json")
                .containsEntry("WWW-Authenticate", Callers.BASIC_CHALLENGE);
        assertThat(http.body.toString()).contains("\"code\":\"authentication.required\"");
        assertThat(http.signedInAs).isNull();
    }

    @Test
    void theBrowserGetsNoBasicChallengeThatWouldOpenItsPasswordDialog() {
        http.headers.put("Authorization", "Bearer something");
        http.headers.put(Callers.CLIENT_HEADER, "web");

        assertThat(run()).isEqualTo(AuthenticationStatus.SEND_FAILURE);
        assertThat(http.status).isEqualTo(401);
        assertThat(http.responseHeaders).doesNotContainKey("WWW-Authenticate");
    }

    @Test
    void aSignInKeepsTheCallerInTheSessionUnderANewSessionId() {
        http.hasSession = true;
        http.authParameters = signIn(CustomerShop.PASSWORD);

        assertThat(run()).isEqualTo(AuthenticationStatus.SUCCESS);
        assertThat(http.sessionIdChanges).isEqualTo(1);
        assertThat(http.sessionAttributes.get(SessionCaller.ATTRIBUTE))
                .isEqualTo(new SessionCaller(Long.toString(ayse.value()), Set.of("CUSTOMER")));
    }

    @Test
    void aFailedSignInKeepsNothing() {
        http.authParameters = signIn("wrong-password");

        assertThat(run()).isEqualTo(AuthenticationStatus.SEND_FAILURE);
        assertThat(http.hasSession).isFalse();
        assertThat(http.signedInAs).isNull();
    }

    @Test
    void theSessionIdentifiesTheCallerOfLaterRequests() {
        http.hasSession = true;
        http.sessionAttributes.put(SessionCaller.ATTRIBUTE, new SessionCaller(Long.toString(ayse.value()), Set.of("CUSTOMER")));

        assertThat(run()).isEqualTo(AuthenticationStatus.SUCCESS);
        assertThat(http.signedInAs).isEqualTo(Long.toString(ayse.value()));
        assertThat(http.signedInGroups).containsExactly("CUSTOMER");
    }

    @Test
    void theSessionOfAnAccountThatWasSwitchedOffEnds() {
        http.hasSession = true;
        http.sessionAttributes.put(SessionCaller.ATTRIBUTE, new SessionCaller(Long.toString(ayse.value()), Set.of("CUSTOMER")));
        shop.admin.setActive(ayse, false);

        assertThat(run()).isEqualTo(AuthenticationStatus.NOT_DONE);
        assertThat(http.invalidated).isTrue();
        assertThat(http.signedInAs).isNull();
    }

    @Test
    void aSessionWithoutACallerIsAnonymous() {
        http.hasSession = true;

        assertThat(run()).isEqualTo(AuthenticationStatus.NOT_DONE);
    }

    private AuthenticationStatus run() {
        return mechanism.validateRequest(http.request(), http.response(), http.context());
    }

    private static AuthenticationParameters signIn(String password) {
        return AuthenticationParameters.withParams()
                .credential(new UsernamePasswordCredential("ayse@example.com", password))
                .newAuthentication(true);
    }

    private static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }
}
