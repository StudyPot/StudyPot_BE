package com.studypot.aistudyleader.identity.service;

public record AuthSessionMetadata(String deviceInfo, String ipAddress) {

	private static final int MAX_DEVICE_INFO_LENGTH = 255;
	private static final int MAX_IP_ADDRESS_LENGTH = 45;

	public AuthSessionMetadata {
		deviceInfo = normalize(deviceInfo, MAX_DEVICE_INFO_LENGTH);
		ipAddress = normalize(ipAddress, MAX_IP_ADDRESS_LENGTH);
	}

	private static String normalize(String value, int maxLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.strip();
		if (normalized.length() > maxLength) {
			return normalized.substring(0, maxLength);
		}
		return normalized;
	}
}
