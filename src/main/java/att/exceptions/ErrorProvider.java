package att.exceptions;

public class ErrorProvider {

    public static void workDateMismatch(String msg) {
        throw new BadRequestException(msg);
    }

    public static void leaveDayAmountExceedsFullDay(String msg) {
        throw new BadRequestException(msg);
    }
}
