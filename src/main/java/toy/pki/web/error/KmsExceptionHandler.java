package toy.pki.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Order(0)
@RequiredArgsConstructor
@ControllerAdvice(basePackages = { "toy.pki.kms.web", "toy.pki.ca.web.certificate" })
public class KmsExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(RestClientException.class)
    public void handle(RestClientException failure, HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        HttpStatus status = switch (failure) {
            case ResourceAccessException ignored -> HttpStatus.SERVICE_UNAVAILABLE;
            case RestClientResponseException http -> switch (http.getStatusCode().value()) {
                case 400 -> HttpStatus.BAD_REQUEST;
                case 404 -> HttpStatus.NOT_FOUND;
                default -> HttpStatus.BAD_GATEWAY;
            };
            default -> HttpStatus.BAD_GATEWAY;
        };
        String message = messageSource.getMessage("error.kms." + status.value(), null, request.getLocale());

        Integer kmsStatus = failure instanceof RestClientResponseException http ? http.getStatusCode().value() : null;
        log.warn("KMS request failed: method={}, path={}, status={}, kmsStatus={}, cause={}",
            request.getMethod(), request.getRequestURI(), status.value(), kmsStatus,
            failure.getMostSpecificCause().getClass().getSimpleName());
        response.sendError(status.value(), message);
    }
}
