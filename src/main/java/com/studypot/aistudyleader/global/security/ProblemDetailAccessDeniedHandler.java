package com.studypot.aistudyleader.global.security;

import com.studypot.aistudyleader.global.error.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

	private final ProblemDetailFactory problemDetailFactory;
	private final ProblemDetailResponseWriter responseWriter;

	public ProblemDetailAccessDeniedHandler(
		ProblemDetailFactory problemDetailFactory,
		ProblemDetailResponseWriter responseWriter
	) {
		this.problemDetailFactory = problemDetailFactory;
		this.responseWriter = responseWriter;
	}

	@Override
	public void handle(
		HttpServletRequest request,
		HttpServletResponse response,
		AccessDeniedException accessDeniedException
	) throws IOException {
		var problemDetail = problemDetailFactory.forbidden("The authenticated user is not allowed to access this resource.");
		problemDetail.setInstance(URI.create(request.getRequestURI()));
		responseWriter.write(response, problemDetail);
	}
}
