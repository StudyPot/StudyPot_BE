package com.studypot.aistudyleader.auth.service;

import java.util.List;

public record UpdateCurrentUserProfileCommand(
	String nickname,
	String profileImage,
	String bio,
	List<String> preferredTopics,
	String skillLevel
) {

	public UpdateCurrentUserProfileCommand {
		preferredTopics = preferredTopics == null ? List.of() : List.copyOf(preferredTopics);
	}
}
