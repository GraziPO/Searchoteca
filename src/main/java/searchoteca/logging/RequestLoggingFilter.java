package searchoteca.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;


public class RequestLoggingFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID = "requestId";
    public static final String USER = "user";

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                 @NonNull HttpServletResponse response,
                                 @NonNull FilterChain filterChain) throws ServletException, IOException{
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long start = System.nanoTime();

        MDC.put(REQUEST_ID, requestId);
        MDC.put(USER, currentUser());

        response.setHeader("X-Request_Id", requestId);

        try{
            filterChain.doFilter(request,response);
        }finally{
            long ms = (System.nanoTime()-start)/1000000;
            int status = response.getStatus();
            String msg = "{} {} -> {} ({} ms)";

            if (status >= 500) {
                log.error(msg, request.getMethod(), request.getRequestURI(), status, ms);
            } else if (status >= 400) {
                log.warn(msg, request.getMethod(), request.getRequestURI(), status, ms);
            } else {
                log.info(msg, request.getMethod(), request.getRequestURI(), status, ms);
            }

            MDC.remove(REQUEST_ID);
            MDC.remove(USER);
        }
    }

    private String currentUser(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
    }
}
