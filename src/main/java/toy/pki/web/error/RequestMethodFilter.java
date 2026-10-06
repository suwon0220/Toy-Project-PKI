package toy.pki.web.error;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestMethodFilter extends OncePerRequestFilter {

    static final String ORIGINAL_METHOD = RequestMethodFilter.class.getName() + ".method";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        request.setAttribute(ORIGINAL_METHOD, request.getMethod());
        chain.doFilter(request, response);
    }
}
