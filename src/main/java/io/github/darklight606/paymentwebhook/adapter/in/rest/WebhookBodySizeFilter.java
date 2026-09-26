package io.github.darklight606.paymentwebhook.adapter.in.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects a request whose declared Content-Length exceeds the limit and, for the case of a
 * missing or understated Content-Length (e.g. chunked transfer), aborts the read once the actual
 * byte count crosses the limit instead of letting the body message converter materialize an
 * unbounded byte array.
 */
@Slf4j
public class WebhookBodySizeFilter extends OncePerRequestFilter {

    private final long maxBodyBytes;

    public WebhookBodySizeFilter(long maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > maxBodyBytes) {
            LOG.warn("AcmePay webhook request rejected [reason=body exceeds maximum size]");
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }
        filterChain.doFilter(new BoundedBodyRequest(request, maxBodyBytes), response);
    }

    private static final class BoundedBodyRequest extends HttpServletRequestWrapper {

        private final long maxBodyBytes;

        BoundedBodyRequest(HttpServletRequest request, long maxBodyBytes) {
            super(request);
            this.maxBodyBytes = maxBodyBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new BoundedServletInputStream(super.getInputStream(), maxBodyBytes);
        }
    }

    private static final class BoundedServletInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long maxBodyBytes;
        private long bytesRead;

        BoundedServletInputStream(ServletInputStream delegate, long maxBodyBytes) {
            this.delegate = delegate;
            this.maxBodyBytes = maxBodyBytes;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value >= 0) {
                countBytes(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int count = delegate.read(buffer, offset, length);
            if (count > 0) {
                countBytes(count);
            }
            return count;
        }

        private void countBytes(int count) throws IOException {
            bytesRead += count;
            if (bytesRead > maxBodyBytes) {
                throw new IOException("webhook body exceeds maximum size");
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }
    }
}
