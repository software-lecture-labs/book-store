package com.bookstore.shared.kernel.testing;

import com.bookstore.shared.kernel.address.Address;

/** Valid addresses for every module's tests. */
public final class TestAddresses {

    private TestAddresses() {
    }

    /** @return a valid home address in Kadıköy */
    public static Address home() {
        return new Address("Ayşe Yılmaz", "+90 532 000 00 00", "Bağdat Cad. No: 1 D: 2", "Kadıköy", "İstanbul",
                "34710");
    }

    /** @return a valid work address in Şişli */
    public static Address work() {
        return new Address("Ayşe Yılmaz", "+90 212 000 00 00", "Büyükdere Cad. 100", "Şişli", "İstanbul", "34394");
    }
}
