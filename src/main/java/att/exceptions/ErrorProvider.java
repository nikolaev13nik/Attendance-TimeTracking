package att.exceptions;

public class ErrorProvider {

    public static void workDateMismatch(String msg) {
        throw new BadRequestException(msg);
    }
}
