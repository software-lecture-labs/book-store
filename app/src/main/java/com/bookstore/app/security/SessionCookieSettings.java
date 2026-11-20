package com.bookstore.app.security;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.annotation.WebListener;

/**
 * Sets up the session cookie when the application starts (ADR-0020): scripts cannot read it
 * ({@code HttpOnly}) and it has a name of its own.
 * Payara writes {@code SameSite=Lax} from {@code WEB-INF/glassfish-web.xml}, the only place it reads it.
 */
@WebListener
public class SessionCookieSettings implements ServletContextListener {

    /** The cookie's name; not the default {@code JSESSIONID}, which says which server runs the shop. */
    public static final String COOKIE_NAME = "BOOKSTORE_SESSION";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        SessionCookieConfig cookie = event.getServletContext().getSessionCookieConfig();
        cookie.setName(COOKIE_NAME);
        cookie.setHttpOnly(true);
    }
}
