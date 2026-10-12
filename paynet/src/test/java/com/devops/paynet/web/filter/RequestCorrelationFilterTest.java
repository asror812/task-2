package com.devops.paynet.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void preservesSafeRequestIdAndClearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "client-request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER)).isEqualTo("client-request-123");
        assertThat(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)).isNull();
    }

    @Test
    void replacesUnsafeRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "unsafe\nrequest-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String requestId = response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);
        assertThat(requestId).isNotEqualTo("unsafe\nrequest-id");
        assertThatCodeIsUuid(requestId);
    }

    private void assertThatCodeIsUuid(String value) {
        assertThat(UUID.fromString(value)).isNotNull();
    }
}
