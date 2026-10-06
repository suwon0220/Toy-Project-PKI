package toy.pki.web.error;

import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
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

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> attributes = super.getErrorAttributes(webRequest, options);
        int status = (int) attributes.getOrDefault("status", 500);
        Throwable error = getError(webRequest);
        String traceId = HexFormat.of().toHexDigits(ThreadLocalRandom.current().nextLong());

        HttpServletRequest request = webRequest instanceof NativeWebRequest nativeRequest
                                     ? nativeRequest.getNativeRequest(HttpServletRequest.class)
                                     : null;
        String path = (String) attributes.get("path");
        Object query = webRequest.getAttribute(ERROR_QUERY_STRING, RequestAttributes.SCOPE_REQUEST);
        if (path != null && query != null) {
            path = path + "?" + query;
        }

        attributes.put("errorTitle", title(status));
        attributes.put("message", message(status));
        attributes.put("details", status == 400 ? details(error) : List.of());
        attributes.put("method", method(webRequest, request));
        attributes.put("path", path);
        attributes.put("traceId", traceId);
        attributes.put("backUrl", backUrl(request));

        if (status >= 500) {
            log.error("[{}] {} {} 처리 중 오류", traceId, attributes.get("method"), path, error);
            if (error != null && environment.acceptsProfiles(Profiles.of("dev"))) {
                attributes.put("exception", stackTrace(error));
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

    private String title(int status) {
        return switch (status) {
            case 400 -> "요청을 처리할 수 없어요";
            case 403 -> "접근 권한이 없어요";
            case 404 -> "페이지를 찾을 수 없어요";
            case 405 -> "허용되지 않은 요청 방식이에요";
            default -> status >= 500 ? "서버에서 오류가 발생했어요" : "요청을 처리할 수 없어요";
        };
    }

    private String message(int status) {
        return switch (status) {
            case 400 -> "요청에 들어간 값이 올바르지 않아요. 주소나 입력값을 확인하고 다시 시도하세요.";
            case 403 -> "이 작업을 수행할 권한이 없어요. 필요하면 관리자에게 권한을 요청하세요.";
            case 404 -> "주소가 잘못됐거나, 이미 삭제된 인증서·프로파일·키일 수 있어요.";
            case 405 -> "이 주소는 해당 요청 방식을 지원하지 않아요. 화면의 버튼으로 다시 시도하세요.";
            default -> status >= 500
                       ? "요청을 처리하는 중 예상하지 못한 오류가 났어요. 잠시 후 다시 시도하고, 계속되면 아래 요청 ID를 관리자에게 알려 주세요."
                       : "요청을 처리하지 못했어요. 주소나 입력값을 확인하고 다시 시도하세요.";
        };
    }

    private List<String> details(Throwable error) {
        if (error instanceof MethodArgumentTypeMismatchException e) {
            return List.of(e.getName() + " = '" + e.getValue() + "' — " + expected(e.getRequiredType()));
        }
        if (error instanceof MissingServletRequestParameterException e) {
            return List.of(e.getParameterName() + " — 필수 값이에요");
        }
        if (error instanceof BindingResult result) {
            List<String> details = new ArrayList<>();
            for (FieldError fieldError : result.getFieldErrors()) {
                details.add(fieldError.getField() + " = '" + fieldError.getRejectedValue() + "' — "
                    + fieldError.getDefaultMessage());
            }
            return details;
        }
        return List.of();
    }

    private String expected(Class<?> type) {
        if (type == null) {
            return "형식이 올바르지 않아요";
        }
        if (type.isEnum()) {
            String constants = Arrays.stream(type.getEnumConstants())
                                     .map(Object::toString)
                                     .collect(Collectors.joining(", "));
            return constants + " 중 하나여야 해요";
        }
        if (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class)) {
            return "숫자여야 해요";
        }
        return type.getSimpleName() + " 형식이어야 해요";
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

    private String stackTrace(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        String[] lines = writer.toString().split("\\R");
        if (lines.length <= STACK_TRACE_LINES) {
            return String.join("\n", lines);
        }
        return String.join("\n", Arrays.copyOf(lines, STACK_TRACE_LINES)) + "\n    … (상위 " + STACK_TRACE_LINES + "줄만)";
    }
}
