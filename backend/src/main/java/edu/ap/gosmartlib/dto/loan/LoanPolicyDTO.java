package edu.ap.gosmartlib.dto.loan;

public record LoanPolicyDTO(
    Long schoolId,
    int defaultLoanPeriodDays,
    int defaultExtensionPeriodDays,
    int dueDateReminderDays

) {}
