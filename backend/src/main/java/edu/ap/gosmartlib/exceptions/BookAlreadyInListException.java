package edu.ap.gosmartlib.exceptions;

public class BookAlreadyInListException extends RuntimeException {
  public BookAlreadyInListException(String message) {
    super(message);
  }
}
