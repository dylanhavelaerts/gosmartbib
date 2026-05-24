package edu.ap.gosmartlib.exceptions;

public class SchoolNotApprovedException extends RuntimeException {
    public SchoolNotApprovedException(String domain) {
        super("School nog niet goedgekeurd: " + domain);
    }
}
