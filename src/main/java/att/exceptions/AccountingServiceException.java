package att.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_GATEWAY)
public class AccountingServiceException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public AccountingServiceException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
