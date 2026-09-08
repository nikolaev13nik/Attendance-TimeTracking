package att.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InternalApiException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
