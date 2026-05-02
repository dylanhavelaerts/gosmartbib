package edu.ap.gosmartlib.dto.loan;

public record UpsertLoanPolicyRequest(
    int defaultLoanPeriodDays,
    int defaultExtensionPeriodDays
) {}
