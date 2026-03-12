package edu.ap.testbackend.exceptions;

public class NegativeIntegerException extends RuntimeException {
  public NegativeIntegerException(String message) {
    super(message);
  }
}
