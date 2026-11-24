/**
 * The WAR that runs on Payara Micro (ADR-0010): the JAX-RS application, the CDI producers for what
 * the container cannot make ({@link com.bookstore.app.BookstoreWiring},
 * {@link com.bookstore.app.BookstoreDataSource}), the server-side implementations of the
 * transaction and event ports, the startup step and the adapters used until real ones exist. The
 * hand-written {@link com.bookstore.app.BookstoreModule} wires the same application without a
 * container, for the tests.
 */
package com.bookstore.app;
