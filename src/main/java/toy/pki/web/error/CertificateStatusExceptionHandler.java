package toy.pki.web.error;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import toy.pki.ca.web.certificate.CertificateStatusController;

@ControllerAdvice(assignableTypes = CertificateStatusController.class)
public class CertificateStatusExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public void handle(Exception failure, HttpServletResponse response) throws Exception {
        // Wrapped signing/I/O failures retain their original error handling.
        if (!(failure instanceof IllegalArgumentException)) {
            throw failure;
        }
        response.sendError(HttpStatus.BAD_REQUEST.value(), failure.getMessage());
    }
}
