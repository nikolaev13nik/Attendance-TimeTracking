package att.exceptions;

public class ErrorProvider {

    public static void workDateMismatch(String msg) {
        throw new BadRequestException(msg);
    }

    public static void leaveDayAmountExceedsFullDay(String msg) {
        throw new BadRequestException(msg);
    }

    public static void leaveDayDateAlreadyRecorded(String msg) {
        throw new BadRequestException(msg);
    }

    public static void leaveDayDateDuplicated(String msg) {
        throw new BadRequestException(msg);
    }

    public static void leaveDayNothingReported(String msg) {
        throw new BadRequestException(msg);
    }

    public static void leaveDayEmpty(String msg) {
        throw new BadRequestException(msg);
    }
}
