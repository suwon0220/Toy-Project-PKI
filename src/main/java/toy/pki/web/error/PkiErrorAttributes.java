package toy.pki.web.error;

import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PkiErrorAttributes extends DefaultErrorAttributes {

    private static final String ERROR_QUERY_STRING = "jakarta.servlet.error.query_string";
    private static final int STACK_TRACE_LINES = 10;

    private final Environment environment;
    private final MessageSource messageSource;

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> attributes = super.getErrorAttributes(webRequest, options);
        int status = (int) attributes.getOrDefault("status", 500);
        Throwable error = getError(webRequest);
        Locale locale = webRequest.getLocale();
        String traceId = HexFormat.of().toHexDigits(ThreadLocalRandom.current().nextLong());

        HttpServletRequest request = webRequest instanceof NativeWebRequest nativeRequest
            ? nativeRequest.getNativeRequest(HttpServletRequest.class)
            : null;
        String path = (String) attributes.get("path");
        Object query = webRequest.getAttribute(ERROR_QUERY_STRING, RequestAttributes.SCOPE_REQUEST);
        if (path != null && query != null) {
            path = path + "?" + query;
        }

        attributes.put("errorTitle", statusMessage(status, "title", locale));
        attributes.put("message", statusMessage(status, "message", locale));
        attributes.put("details", status == 400 ? details(error, locale) : List.of());
        attributes.put("method", method(webRequest, request));
        attributes.put("path", path);
        attributes.put("traceId", traceId);
        attributes.put("backUrl", backUrl(request));

        if (status >= 500) {
            if (error instanceof RestClientException) {
                // KMS exceptions may contain response bodies; keep their contents out of logs and the error page.
                log.error("[{}] {} {} 처리 중 KMS 오류: {}",
                    traceId, attributes.get("method"), path, error.getClass().getSimpleName());
                return attributes;
            }
            log.error("[{}] {} {} 처리 중 오류", traceId, attributes.get("method"), path, error);
            if (error != null && environment.acceptsProfiles(Profiles.of("dev"))) {
                attributes.put("exception", stackTrace(error, locale));
            }
        }
        return attributes;
    }

    /** 오류 디스패치에서는 getMethod() 가 GET 으로 바뀌므로 필터가 남긴 원래 메서드를 쓴다. */
    private String method(WebRequest webRequest, HttpServletRequest request) {
        Object method = webRequest.getAttribute(RequestMethodFilter.ORIGINAL_METHOD, RequestAttributes.SCOPE_REQUEST);
        if (method != null) {
            return (String) method;
        }
        return request != null ? request.getMethod() : "";
    }

    private String statusMessage(int status, String field, Locale locale) {
        String fallback = "error." + (status >= 500 ? "5xx" : "default") + "." + field;
        return messageSource.getMessage("error." + status + "." + field, null,
            messageSource.getMessage(fallback, null, locale), locale);
    }

    private List<String> details(Throwable error, Locale locale) {
        if (error instanceof MethodArgumentTypeMismatchException e) {
            return List.of(messageSource.getMessage("error.detail.field",
                new Object[] { e.getName(), e.getValue(), expected(e.getRequiredType(), locale) }, locale));
        }
        if (error instanceof MissingServletRequestParameterException e) {
            return List.of(messageSource.getMessage("error.detail.required",
                new Object[] { e.getParameterName() }, locale));
        }
        if (error instanceof BindingResult result) {
            List<String> details = new ArrayList<>();
            for (FieldError fieldError : result.getFieldErrors()) {
                details.add(messageSource.getMessage("error.detail.field",
                    new Object[] { fieldError.getField(), fieldError.getRejectedValue(),
                        messageSource.getMessage(fieldError, locale) },
                    locale));
            }
            return details;
        }
        return List.of();
    }

    private String expected(Class<?> type, Locale locale) {
        if (type == null) {
            return messageSource.getMessage("error.expected.invalid", null, locale);
        }
        if (type.isEnum()) {
            String constants = Arrays.stream(type.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
            return messageSource.getMessage("error.expected.enum", new Object[] { constants }, locale);
        }
        if (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class)) {
            return messageSource.getMessage("error.expected.number", null, locale);
        }
        return messageSource.getMessage("error.expected.type", new Object[] { type.getSimpleName() }, locale);
    }

    /** 같은 호스트에서 온 Referer 만 되돌아갈 주소로 쓴다. */
    private String backUrl(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null) {
            return null;
        }
        try {
            URI uri = URI.create(referer);
            return request.getServerName().equalsIgnoreCase(uri.getHost()) ? referer : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String stackTrace(Throwable error, Locale locale) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        String[] lines = writer.toString().split("\\R");
        if (lines.length <= STACK_TRACE_LINES) {
            return String.join("\n", lines);
        }
        return String.join("\n", Arrays.copyOf(lines, STACK_TRACE_LINES)) + "\n    "
            + messageSource.getMessage("error.stackTrace.truncated", new Object[] { STACK_TRACE_LINES }, locale);
    }
}
