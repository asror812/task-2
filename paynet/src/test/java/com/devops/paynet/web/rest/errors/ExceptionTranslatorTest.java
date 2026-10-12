package com.devops.paynet.web.rest.errors;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause;

class ExceptionTranslatorTest {

    private final ExceptionTranslator translator = new ExceptionTranslator(new MockEnvironment());

    @Test
    void preservesBusinessErrorTitleAndDetail() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/recipients");
        BadRequestAlertException exception = new BadRequestAlertException(
            "Category not found with id 999",
            "recipient",
            "categorynotfound"
        );

        ProblemDetailWithCause problem = translator.wrapAndCustomizeProblem(exception, new ServletWebRequest(request));

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Category not found with id 999");
        assertThat(problem.getDetail()).isEqualTo("Category not found with id 999");
        assertThat(problem.getProperties())
            .containsEntry("message", "error.categorynotfound")
            .containsEntry("params", "recipient")
            .containsEntry("path", URI.create("/api/recipients"));
    }
}
