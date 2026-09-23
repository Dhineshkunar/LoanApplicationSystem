package com.loanapp.enums;

/**
 * Category of loan product being applied for.
 *
 * Kept simpler than LoanStatus deliberately — there's no state machine
 * here, just a closed set of valid categories, plus metadata that's
 * genuinely useful in business logic (e.g. a maximum tenure cap per type,
 * often used for validation or interest-rate calculators).
 */
public enum LoanType {

    HOME("HOME", "Home Loan", 360),
    PERSONAL("PERSONAL", "Personal Loan", 60),
    VEHICLE("VEHICLE", "Vehicle Loan", 84),
    EDUCATION("EDUCATION", "Education Loan", 180),
    BUSINESS("BUSINESS", "Business Loan", 120);

    private final String dbValue;
    private final String displayName;
    private final int maxTenureMonths;

    LoanType(String dbValue, String displayName, int maxTenureMonths) {
        this.dbValue = dbValue;
        this.displayName = displayName;
        this.maxTenureMonths = maxTenureMonths;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxTenureMonths() {
        return maxTenureMonths;
    }
}
