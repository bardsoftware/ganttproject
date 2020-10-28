// Copyright (C) 2012 BarD Software
package biz.ganttproject.core;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

/**
 * Status object which contains status of the operation.
 * If operation was successful it holds result returned by operation.
 * Otherwise it contains message describing result of the operation.
 *
 * @param <Code> type of result code
 * @param <T>    type of result returned by operation
 * @author gkalabin@bardsoftware.com
 */
public class OperationStatus<T, Code extends Enum> {
  public enum DefaultCode {
    OK, FAIL
  }

  private final Code myResultCode;

  /**
   * True if operation was successful
   */
  private final boolean myOk;

  /**
   * Result of the operation. Null if operation was not successful
   */
  private final T myOperationResult;

  /**
   * Message describing result of operation
   */
  private final String myMessage;

  private OperationStatus(boolean ok, T result, String message, @Nonnull Code resultCode) {
    myOperationResult = result;
    myMessage = message;
    myOk = ok;
    myResultCode = Preconditions.checkNotNull(resultCode);
  }

  /**
   * Constructs new object with OK status
   *
   * @param result result returned by the operation
   */
  public static <T> OperationStatus<T, DefaultCode> success(T result) {
    return new OperationStatus<>(true, result, null, DefaultCode.OK);
  }

  /**
   * Constructs new object with <b>not</b> OK status.
   *
   * @param message describing result of the operation
   */
  public static <T> OperationStatus<T, DefaultCode> failure(@Nonnull String message) {
    return new OperationStatus<>(false, null, Preconditions.checkNotNull(message), DefaultCode.FAIL);
  }

  public static <T> OperationStatus<T, DefaultCode> failure(@Nonnull String message, Object... args) {
    return new OperationStatus<>(false, null, String.format(Preconditions.checkNotNull(message), args), DefaultCode.FAIL);
  }

  /**
   * Constructs new object with OK status.
   *
   * @param result     result returned by the operation
   * @param statusCode code describing result of the operation
   */
  public static <Code extends Enum, T> OperationStatus<T, Code> success(T result, @Nonnull Code statusCode) {
    return new OperationStatus<>(true, result, null, statusCode);
  }

  /**
   * Constructs new object with <b>not</b> OK status.
   *
   * @param message   describing result of the operation
   * @param errorCode code describing result of the operation
   */
  public static <Code extends Enum, T> OperationStatus<T, Code> failure(@Nonnull String message, @Nonnull Code errorCode) {
    return new OperationStatus<>(false, null, Preconditions.checkNotNull(message), errorCode);
  }


  /**
   * Constructs new object with the same fail details
   *
   * @param failStatus failed status which fail details should be copied
   */
  public static <Code extends Enum, T> OperationStatus<T, Code> failure(@Nonnull OperationStatus<?, Code> failStatus) {
    Preconditions.checkNotNull(failStatus);
    Preconditions.checkArgument(!failStatus.isOk());
    return new OperationStatus<>(false, null, failStatus.getMessage(), failStatus.getResultCode());
  }

  /**
   * @return true if operation was successful
   */
  public boolean isOk() {
    return myOk;
  }

  /**
   * @return result returned by the operation
   * @throws IllegalStateException if operation was not successful
   */
  public T get() {
    Preconditions.checkState(myOk);
    return myOperationResult;
  }

  /**
   * @return message describing result of the operation
   */
  @Nonnull
  public String getMessage() {
    Preconditions.checkState(!myOk, "Message available only for fail status");
    return myMessage;
  }

  /**
   * @return code describing result of the operation (used for detecting what exactly went wrong)
   */
  public Code getResultCode() {
    return myResultCode;
  }

  @Override
  public String toString() {
    return isOk() ? String.valueOf(getResultCode()) : String.format("%s: %s", getResultCode(), getMessage());
  }
}
