package att.exceptions;

public class ErrorConstants {

    public static final String OPEN_CLOSE_DATE_MISSING_MSG = "Open/close session date:%s should be related to today " +
            "and not be null.";
    public static final String PREVIOUS_DAY_REQUIRES_ADMIN_MSG =
            "Input work date:%s is before today:%s, only an administrator can record a previous day.";
    public static final String FUTURE_WORK_DATE_MSG = "Input work date:%s is after today:%s.";
    public static final String ATTENDANCE_NOT_FOUND_MSG = "Attendance with id:%s is not found";
    public static final String INCOMPLETE_SESSIONS_MSG = "There are incomplete sessions to be handled";
    public static final String LEAVE_DAY_AMOUNT_EXCEEDS_FULL_DAY_MSG =
            "Leave day %s exceeds a full day: vacation:%s, sick:%s";
    public static final String REPORT_REQUESTER_UNRESOLVED_MSG =
            "Reporting user cannot be resolved: the request carries no user id";

}
