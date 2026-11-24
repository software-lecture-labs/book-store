package com.bookstore.shared.web;

/** The HTTP status codes the API uses, with their reason phrases. */
public final class HttpStatus {

    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int NO_CONTENT = 204;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    public static final int CONFLICT = 409;
    /** A condition such as {@code If-Match} did not hold. */
    public static final int PRECONDITION_FAILED = 412;
    public static final int UNSUPPORTED_MEDIA_TYPE = 415;
    public static final int UNPROCESSABLE_CONTENT = 422;
    public static final int INTERNAL_SERVER_ERROR = 500;

    private HttpStatus() {
    }

    /**
     * Returns the reason phrase of a status code.
     *
     * @param status the code
     * @return for example {@code "Not Found"}
     */
    public static String reasonOf(int status) {
        return switch (status) {
            case OK -> "OK";
            case CREATED -> "Created";
            case NO_CONTENT -> "No Content";
            case BAD_REQUEST -> "Bad Request";
            case UNAUTHORIZED -> "Unauthorized";
            case FORBIDDEN -> "Forbidden";
            case NOT_FOUND -> "Not Found";
            case METHOD_NOT_ALLOWED -> "Method Not Allowed";
            case CONFLICT -> "Conflict";
            case PRECONDITION_FAILED -> "Precondition Failed";
            case UNSUPPORTED_MEDIA_TYPE -> "Unsupported Media Type";
            case UNPROCESSABLE_CONTENT -> "Unprocessable Content";
            case INTERNAL_SERVER_ERROR -> "Internal Server Error";
            default -> "HTTP " + status;
        };
    }
}
