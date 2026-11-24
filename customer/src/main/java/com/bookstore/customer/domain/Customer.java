package com.bookstore.customer.domain;

import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.domain.Entity;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.validation.Guard;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A registered user of the store. Despite the name, administrators are customers too, with the
 * {@link Role#ADMIN} role.
 *
 * <p>The customer only ever sees a password <em>hash</em>. Turning the typed password into a hash
 * is the job of the application layer's password hasher, after {@link PasswordPolicy} has accepted
 * it.
 */
public class Customer extends Entity<CustomerId> {

    /** How many addresses a customer may save. */
    public static final int MAX_ADDRESSES = 10;

    private Email email;
    private PersonName name;
    private String passwordHash;
    private final Role role;
    private boolean active;
    private final Instant registeredAt;
    private final List<SavedAddress> addresses = new ArrayList<>();
    private SavedAddress defaultAddress;

    private Customer(Email email, PersonName name, String passwordHash, Role role, boolean active,
                     Instant registeredAt) {
        this.email = Guard.requireNonNull(email, "email");
        this.name = Guard.requireNonNull(name, "name");
        this.passwordHash = validHash(passwordHash);
        this.role = Guard.requireNonNull(role, "role");
        this.active = active;
        this.registeredAt = Guard.requireNonNull(registeredAt, "registeredAt");
    }

    /**
     * Registers a new customer with the {@link Role#CUSTOMER} role.
     *
     * @param email        the e-mail address, which must not be taken (checked by the caller)
     * @param name         the customer's name
     * @param passwordHash the hash of a password accepted by {@link PasswordPolicy}
     * @param clock        the clock giving the registration time
     * @return the customer, not yet stored
     */
    public static Customer register(Email email, PersonName name, String passwordHash, Clock clock) {
        return new Customer(email, name, passwordHash, Role.CUSTOMER, true, Instant.now(clock));
    }

    /**
     * Creates an administrator account.
     *
     * @param email        the e-mail address
     * @param name         the administrator's name
     * @param passwordHash the password hash
     * @param clock        the clock giving the creation time
     * @return the administrator, not yet stored
     */
    public static Customer createAdmin(Email email, PersonName name, String passwordHash, Clock clock) {
        return new Customer(email, name, passwordHash, Role.ADMIN, true, Instant.now(clock));
    }

    /**
     * Reconstitutes a customer loaded from storage.
     *
     * @param id               the stored id
     * @param version          the stored version
     * @param email            the stored e-mail address
     * @param name             the stored name
     * @param passwordHash     the stored password hash
     * @param role             the stored role
     * @param active           whether the account is active
     * @param registeredAt     when the customer registered
     * @param addresses        the saved addresses
     * @param defaultAddressId id of the default address among {@code addresses}, or {@code null}
     * @return the customer
     */
    public static Customer reconstitute(CustomerId id, int version, Email email, PersonName name, String passwordHash,
                                   Role role, boolean active, Instant registeredAt, List<SavedAddress> addresses,
                                   AddressId defaultAddressId) {
        Customer customer = new Customer(email, name, passwordHash, role, active, registeredAt);
        customer.markPersisted(id, version);
        customer.addresses.addAll(addresses);
        if (defaultAddressId != null) {
            customer.defaultAddress = customer.address(defaultAddressId);
        }
        return customer;
    }

    // ---------------------------------------------------------------- queries

    /** @return the e-mail address, used to sign in */
    public Email email() {
        return email;
    }

    /** @return the name */
    public PersonName name() {
        return name;
    }

    /** @return the password hash */
    public String passwordHash() {
        return passwordHash;
    }

    /** @return the role */
    public Role role() {
        return role;
    }

    /** @return {@code true} if the customer may sign in */
    public boolean isActive() {
        return active;
    }

    /** @return {@code true} for administrators */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /** @return when the customer registered */
    public Instant registeredAt() {
        return registeredAt;
    }

    /** @return the saved addresses, in the order they were added */
    public List<SavedAddress> addresses() {
        return Collections.unmodifiableList(addresses);
    }

    /** @return the address pre-selected at checkout, if any */
    public Optional<SavedAddress> defaultAddress() {
        return Optional.ofNullable(defaultAddress);
    }

    /**
     * Finds a saved address by id.
     *
     * @param addressId the address id
     * @return the address
     * @throws NotFoundException if the customer has no such address
     */
    public SavedAddress address(AddressId addressId) {
        return addresses.stream()
                .filter(a -> !a.isNew() && a.id().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Address", addressId));
    }

    // ---------------------------------------------------------------- commands

    /**
     * Changes the name.
     *
     * @param newName the new name
     */
    public void rename(PersonName newName) {
        this.name = Guard.requireNonNull(newName, "name");
    }

    /**
     * Changes the e-mail address. The caller checks that the new address is not taken.
     *
     * @param newEmail the new address
     */
    public void changeEmail(Email newEmail) {
        this.email = Guard.requireNonNull(newEmail, "email");
    }

    /**
     * Replaces the password hash after the customer chose a new password.
     *
     * @param newPasswordHash the hash of the new password
     */
    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = validHash(newPasswordHash);
    }

    /** Prevents the customer from signing in. */
    public void deactivate() {
        this.active = false;
    }

    /** Allows the customer to sign in again. */
    public void activate() {
        this.active = true;
    }

    /**
     * Saves a new address. The first address becomes the default automatically.
     *
     * @param label       the customer's name for it, for example {@code "Ev"}
     * @param address     the address
     * @param makeDefault whether to make it the default
     * @return the saved address, without an id until the customer is stored
     */
    public SavedAddress addAddress(String label, Address address, boolean makeDefault) {
        Guard.require(addresses.size() < MAX_ADDRESSES, "addresses",
                "at most " + MAX_ADDRESSES + " addresses can be saved");
        SavedAddress saved = new SavedAddress(label, address);
        addresses.add(saved);
        if (makeDefault || defaultAddress == null) {
            defaultAddress = saved;
        }
        return saved;
    }

    /**
     * Changes a saved address.
     *
     * @param addressId  the address id
     * @param newLabel   the new label
     * @param newAddress the new address
     */
    public void changeAddress(AddressId addressId, String newLabel, Address newAddress) {
        address(addressId).change(newLabel, newAddress);
    }

    /**
     * Makes a saved address the default.
     *
     * @param addressId the address id
     */
    public void makeDefault(AddressId addressId) {
        defaultAddress = address(addressId);
    }

    /**
     * Deletes a saved address. If it was the default, the first remaining address becomes the
     * default.
     *
     * @param addressId the address id
     */
    public void removeAddress(AddressId addressId) {
        SavedAddress removed = address(addressId);
        addresses.remove(removed);
        if (removed.equals(defaultAddress)) {
            defaultAddress = addresses.isEmpty() ? null : addresses.getFirst();
        }
    }

    @Override
    public String toString() {
        return "Customer[" + (isNew() ? "new" : id()) + ", " + email + ", " + role + "]";
    }

    private static String validHash(String hash) {
        return Guard.requireText(hash, "passwordHash", 255);
    }
}
