package com.gems.education.application.exceptions;

public class GroupNotFoundException extends RuntimeException {
  public static final String CODE = "GROUP_NOT_FOUND";

  public GroupNotFoundException(Long id) {
    super("Group not found: " + id);
  }
}
