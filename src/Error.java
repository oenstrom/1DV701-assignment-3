public enum Error {
  PREMATURE_TERMINATION((byte) 0, "The connection was terminated prematurly.".getBytes()),
  NOT_DEFINED((byte) 0, "Not defined.".getBytes()),
  FILE_NOT_FOUND((byte) 1, "File not found.".getBytes()),
  ACCESS_VIOLATION((byte) 2, "Access violation.".getBytes()),
  DISK_FULL((byte) 3, "Disk full or allocation exceeded.".getBytes()),
  ILLEGAL_OPERATION((byte) 4, "Illegal TFTP operation.".getBytes()),
  UNKNOWN_TRANSFER_ID((byte) 5, "Unknown tranfer ID.".getBytes()),
  FILE_ALREADY_EXISTS((byte) 6, "File already exists.".getBytes()),
  NO_SUCH_USER((byte) 7, "No such user.0".getBytes());

  byte code;
  byte[] message;
  
  Error(byte code, byte[] message) {
    this.code = code;
    this.message = message;
  }
}
