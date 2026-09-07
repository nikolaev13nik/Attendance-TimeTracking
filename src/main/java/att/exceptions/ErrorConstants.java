package att.exceptions;

public class ErrorConstants {

    public static final String WORK_DATE_MISMATCH_MSG = "Input work date:%s expects to be as:%s.";
    public static final String OPEN_CLOSE_DATE_MISSING_MSG = "Open/close session date:%s should be related to today " +
            "and not be null.";
    public static final String ATTENDANCE_NOT_FOUND_MSG = "Attendance with id:%s is not found";
    public static final String INCOMPLETE_SESSIONS_MSG = "There are incomplete sessions to be handled";
    public static final String LEAVE_DAY_AMOUNT_EXCEEDS_FULL_DAY_MSG =
            "Leave day %s exceeds a full day: vacation:%s, sick:%s";

}
