package com.loanapp.util;

/**
 * Centralized constants for defaults and magic values used across the
 * application (mainly pagination defaults for the GET /loans endpoint).
 *
 * WHY A CONSTANTS CLASS:
 * Hardcoding "0", "10", "id" as default parameter values directly in
 * @RequestParam annotations scatters them across controllers. If the
 * default page size needs to change from 10 to 20 later, this is the one
 * place to change it, and the change is impossible to miss in review.
 *
 * private constructor prevents instantiation — this class is a pure
 * namespace for constants, never meant to be "new"-ed.
 */
public final class AppConstants {

    private AppConstants() {
    }

    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final String DEFAULT_SORT_BY = "id";
    public static final String DEFAULT_SORT_DIRECTION = "asc";

    public static final String API_BASE_PATH = "/api/v1";
}
