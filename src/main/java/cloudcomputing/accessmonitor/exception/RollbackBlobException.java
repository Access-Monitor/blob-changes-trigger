package cloudcomputing.accessmonitor.exception;

public class RollbackBlobException extends RuntimeException {

  public RollbackBlobException() {
  }

  public RollbackBlobException(Exception e) {
    super(e);
  }
}
