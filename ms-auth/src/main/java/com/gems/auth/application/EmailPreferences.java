package com.gems.auth.application;

/**
 * @param courseNotices notices about the user's own courses (graded work, announcements, forum replies)
 * @param tips          occasional reminders and ideas to keep studying (at most one a week)
 */
public record EmailPreferences(boolean courseNotices, boolean tips) {
  public static final EmailPreferences DEFAULTS = new EmailPreferences(true, true);
}
