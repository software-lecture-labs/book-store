package com.bookstore.app;

import com.bookstore.catalog.domain.FileStorage;
import com.bookstore.catalog.persistence.FileSystemFileStorage;

import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Where uploaded files are kept on the server (ADR-0021): the directory comes from MicroProfile
 * Config ({@code bookstore.files.directory}, or the environment variable
 * {@code BOOKSTORE_FILES_DIRECTORY}). Like {@link BookstoreDataSource} it is a server-only part, so
 * the CDI wiring test replaces it.
 */
@ApplicationScoped
public class BookstoreFiles {

    private final String directory;

    /**
     * Creates the producer.
     *
     * @param directory the directory for uploaded files
     */
    @Inject
    public BookstoreFiles(@ConfigProperty(name = "bookstore.files.directory") String directory) {
        this.directory = directory;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected BookstoreFiles() {
        this.directory = null;
    }

    /** @return the file storage in the configured directory */
    @Produces
    @Singleton
    public FileStorage fileStorage() {
        return new FileSystemFileStorage(Path.of(directory));
    }
}
