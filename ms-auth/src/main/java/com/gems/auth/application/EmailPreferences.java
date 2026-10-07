package com.gems.auth.application;

/**
 * @param courseNotices notices about the user's own courses (graded work, announcements, forum replies)
 * @param tips          occasional reminders and ideas to keep studying (at most one a week)
 */
public record EmailPreferences(boolean courseNotices, boolean tips) {
  /**
   * Course notices are part of the service. Tips are promotional, so they need prior and express
   * authorization (Ley 1581 de 2012): off until the user turns them on.
   */
  public static final EmailPreferences DEFAULTS = new EmailPreferences(true, false);
}
