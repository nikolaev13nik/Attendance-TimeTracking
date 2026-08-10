package att.exceptions;

public class ErrorProvider {

    public static void workDateMismatch(String mesg) {
        throw new BadRequestException(mesg);
    }
}
