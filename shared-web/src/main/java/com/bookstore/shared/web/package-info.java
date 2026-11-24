/**
 * The HTTP boundary shared by every module's {@code rest} package (ADR-0019, ADR-0020): RFC 9457
 * problems, JSON-B settings, request parameters and the signed-in caller, on JAX-RS. Signing in
 * itself is the server's Jakarta Security; it sees no business module.
 */
package com.bookstore.shared.web;
