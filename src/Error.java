public enum Error {
  Not_Defined((byte) 0, "Not defined.".getBytes()),
  File_Not_Found((byte) 1, "File not found.".getBytes()),
  Access_Violation((byte) 2, "Access violation.".getBytes()),
  Disk_Full((byte) 3, "Disk full or allocation exceeded.".getBytes()),
  Illegal_Operation((byte) 4, "Illegal TFTP operation.".getBytes()),
  Unknown_Transfer_ID((byte) 5, "Unknown tranfer ID.".getBytes()),
  File_Already_Exists((byte) 6, "File already exists.".getBytes()),
  No_Such_User((byte) 7, "No such user.0".getBytes());

  byte code;
  byte[] message;
  
  Error(byte code, byte[] message) {
    this.code = code;
    this.message = message;
  }
}
