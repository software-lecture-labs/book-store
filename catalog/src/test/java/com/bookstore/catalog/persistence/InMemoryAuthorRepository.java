package com.bookstore.catalog.persistence;

import com.bookstore.catalog.domain.Author;
import com.bookstore.catalog.domain.AuthorId;
import com.bookstore.catalog.domain.AuthorRepository;
import com.bookstore.shared.kernel.testing.InMemoryStore;
import com.bookstore.shared.kernel.text.TextNormalizer;

import java.util.Comparator;
import java.util.List;

/** Authors kept in memory, sorted like the JDBC repository: by search key, then id. */
public class InMemoryAuthorRepository extends InMemoryStore<AuthorId, Author> implements AuthorRepository {

    /** Creates an empty repository. */
    public InMemoryAuthorRepository() {
        super(AuthorId::new);
    }

    @Override
    protected Author copyOf(Author author) {
        return Author.reconstitute(author.id(), author.version(), author.name());
    }

    @Override
    public List<Author> findAll() {
        return all().stream()
                .sorted(Comparator.comparing((Author author) -> TextNormalizer.searchKey(author.name()))
                        .thenComparing(author -> author.id().value()))
                .toList();
    }
}
