package cn.cccs7.cynexus.common.web.filter;

import cn.cccs7.cynexus.common.core.constant.TraceId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            TraceId.set(request.getHeader(TraceId.HEADER_NAME));
            response.setHeader(TraceId.HEADER_NAME, TraceId.current());
            filterChain.doFilter(request, response);
        } finally {
            TraceId.clear();
        }
    }
}
