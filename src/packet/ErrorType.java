package packet;

/**
 * Enum representing TFTP errors.
 */
public enum ErrorType {
  PREMATURE_TERMINATION((short) 0, "The connection was terminated prematurly.".getBytes()),
  NOT_DEFINED((short) 0, "Not defined.".getBytes()),
  FILE_NOT_FOUND((short) 1, "File not found.".getBytes()),
  ACCESS_VIOLATION((short) 2, "Access violation.".getBytes()),
  DISK_FULL((short) 3, "Disk full or allocation exceeded.".getBytes()),
  ILLEGAL_OPERATION((short) 4, "Illegal TFTP operation.".getBytes()),
  UNKNOWN_TRANSFER_ID((short) 5, "Unknown tranfer ID.".getBytes()),
  FILE_ALREADY_EXISTS((short) 6, "File already exists.".getBytes()),
  NO_SUCH_USER((short) 7, "No such user.0".getBytes());

  public short code;
  public byte[] message;
  
  ErrorType(short code, byte[] message) {
    this.code = code;
    this.message = message;
  }
}
