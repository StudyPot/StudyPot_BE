package com.studypot.aistudyleader.global.security;

import com.studypot.aistudyleader.global.error.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ProblemDetailFactory problemDetailFactory;
	private final ProblemDetailResponseWriter responseWriter;

	public ProblemDetailAuthenticationEntryPoint(
		ProblemDetailFactory problemDetailFactory,
		ProblemDetailResponseWriter responseWriter
	) {
		this.problemDetailFactory = problemDetailFactory;
		this.responseWriter = responseWriter;
	}

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException {
		var problemDetail = problemDetailFactory.unauthorized("Authentication is required to access this resource.");
		problemDetail.setInstance(URI.create(request.getRequestURI()));
		responseWriter.write(response, problemDetail);
	}
}
