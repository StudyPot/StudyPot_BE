package com.studypot.aistudyleader.auth.service;

import java.util.regex.Pattern;

public record AuthSessionMetadata(String deviceInfo, String ipAddress) {

	private static final int MAX_DEVICE_INFO_LENGTH = 255;
	private static final int MAX_IP_ADDRESS_LENGTH = 45;
	private static final Pattern IPV4_SEGMENTS = Pattern.compile("\\.");
	private static final Pattern HEXADECIMAL = Pattern.compile("[0-9a-fA-F]{1,4}");

	public AuthSessionMetadata {
		deviceInfo = normalize(deviceInfo, MAX_DEVICE_INFO_LENGTH);
		ipAddress = normalizeIpAddress(ipAddress);
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

	private static String normalizeIpAddress(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.strip();
		if (normalized.length() > MAX_IP_ADDRESS_LENGTH) {
			return null;
		}
		if (isIpv4Address(normalized) || isIpv6Address(normalized)) {
			return normalized;
		}
		return null;
	}

	private static boolean isIpv4Address(String value) {
		String[] parts = IPV4_SEGMENTS.split(value, -1);
		if (parts.length != 4) {
			return false;
		}
		for (String part : parts) {
			if (part.isEmpty() || part.length() > 3) {
				return false;
			}
			for (int index = 0; index < part.length(); index++) {
				if (!Character.isDigit(part.charAt(index))) {
					return false;
				}
			}
			int octet = Integer.parseInt(part);
			if (octet > 255) {
				return false;
			}
		}
		return true;
	}

	private static boolean isIpv6Address(String value) {
		if (!value.contains(":") || value.contains("[") || value.contains("]") || value.contains("%")) {
			return false;
		}
		int compression = value.indexOf("::");
		if (compression != value.lastIndexOf("::")) {
			return false;
		}
		if (compression < 0) {
			return countIpv6Hextets(value) == 8;
		}
		String left = value.substring(0, compression);
		String right = value.substring(compression + 2);
		int knownHextets = countIpv6Hextets(left) + countIpv6Hextets(right);
		return knownHextets >= 0 && knownHextets < 8;
	}

	private static int countIpv6Hextets(String value) {
		if (value.isEmpty()) {
			return 0;
		}
		String[] parts = value.split(":", -1);
		int count = 0;
		for (int index = 0; index < parts.length; index++) {
			String part = parts[index];
			if (part.isEmpty()) {
				return -1;
			}
			if (part.contains(".")) {
				if (index != parts.length - 1 || !isIpv4Address(part)) {
					return -1;
				}
				count += 2;
			} else if (HEXADECIMAL.matcher(part).matches()) {
				count++;
			} else {
				return -1;
			}
		}
		return count;
	}
}
