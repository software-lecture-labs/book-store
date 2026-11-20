package com.bookstore.customer.domain;

import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.domain.Entity;
import com.bookstore.shared.kernel.validation.Guard;

/**
 * An address in a customer's address book. It has its own id so that the customer can pick it at
 * checkout or delete it.
 */
public class SavedAddress extends Entity<AddressId> {

    /** Longest allowed label. */
    public static final int MAX_LABEL_LENGTH = 50;

    private String label;
    private Address address;

    SavedAddress(String label, Address address) {
        this.label = validLabel(label);
        this.address = Guard.requireNonNull(address, "address");
    }

    private SavedAddress(AddressId id, int version, String label, Address address) {
        super(id, version);
        this.label = validLabel(label);
        this.address = Guard.requireNonNull(address, "address");
    }

    /**
     * Reconstitutes a saved address loaded from storage, to be passed to {@link Customer#reconstitute}.
     *
     * @param id      the stored id
     * @param version the stored version
     * @param label   the stored label
     * @param address the stored address
     * @return the saved address
     */
    public static SavedAddress reconstitute(AddressId id, int version, String label, Address address) {
        return new SavedAddress(id, version, label, address);
    }

    /**
     * Returns the customer's name for the address.
     *
     * @return for example {@code "Ev"} or {@code "İş"}
     */
    public String label() {
        return label;
    }

    /**
     * Returns the address itself.
     *
     * @return the address
     */
    public Address address() {
        return address;
    }

    void change(String newLabel, Address newAddress) {
        this.label = validLabel(newLabel);
        this.address = Guard.requireNonNull(newAddress, "address");
    }

    private static String validLabel(String label) {
        return Guard.requireText(label, "label", MAX_LABEL_LENGTH);
    }
}
