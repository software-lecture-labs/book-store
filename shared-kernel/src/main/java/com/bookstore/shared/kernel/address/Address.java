package com.bookstore.shared.kernel.address;

import com.bookstore.shared.kernel.validation.Guard;

import java.util.regex.Pattern;

/**
 * A postal delivery address. Orders copy the address they were placed with, so later changes to a
 * customer's saved addresses do not alter past orders.
 *
 * @param recipient  who receives the parcel
 * @param phone      a phone number for the courier
 * @param line       street, building and flat
 * @param district   district (ilçe)
 * @param city       city (il)
 * @param postalCode five-digit postal code
 */
public record Address(String recipient, String phone, String line, String district, String city, String postalCode) {

    private static final Pattern POSTAL_CODE = Pattern.compile("\\d{5}");
    private static final Pattern PHONE = Pattern.compile("\\+?[\\d ()-]{10,20}");

    /** Trims and checks every part. */
    public Address {
        recipient = Guard.requireText(recipient, "recipient", 200);
        phone = Guard.requireMatches(Guard.requireText(phone, "phone", 20), PHONE, "phone", "is not a valid phone number");
        line = Guard.requireText(line, "line", 300);
        district = Guard.requireText(district, "district", 100);
        city = Guard.requireText(city, "city", 100);
        postalCode = Guard.requireMatches(Guard.requireText(postalCode, "postalCode", 5), POSTAL_CODE,
                "postalCode", "must be five digits");
    }

    /**
     * Returns the address on one line, for lists.
     *
     * @return for example {@code "Ayşe Yılmaz, Bağdat Cad. 1, Kadıköy/İstanbul 34710"}
     */
    public String singleLine() {
        return recipient + ", " + line + ", " + district + "/" + city + " " + postalCode;
    }
}
