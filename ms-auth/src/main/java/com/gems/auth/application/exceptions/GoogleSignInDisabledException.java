package com.gems.auth.application.exceptions;

/** Google sign-in is not configured (no client id), so the endpoint behaves as if it did not exist. */
public class GoogleSignInDisabledException extends RuntimeException {
  public GoogleSignInDisabledException() {
    super("Not found");
  }
}
