package com.studypot.aistudyleader.studygroup.domain;

import com.studypot.aistudyleader.global.domain.AggregateRoot;
import com.studypot.aistudyleader.global.domain.AuditMetadata;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class GroupMember extends AggregateRoot<UUID> {

	private static final int MAX_DISPLAY_NAME_LENGTH = 80;

	private final UUID groupId;
	private final UUID userId;
	private final GroupMemberPermission permission;
	private final GroupMemberStatus status;
	private final String displayName;
	private final Instant joinedAt;
	private final Instant activatedAt;
	private final Instant leftAt;
	private final AuditMetadata auditMetadata;

	private GroupMember(
		UUID id,
		UUID groupId,
		UUID userId,
		GroupMemberPermission permission,
		GroupMemberStatus status,
		String displayName,
		Instant joinedAt,
		Instant activatedAt,
		Instant leftAt,
		AuditMetadata auditMetadata
	) {
		super(id);
		this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
		this.userId = Objects.requireNonNull(userId, "userId must not be null");
		this.permission = Objects.requireNonNull(permission, "permission must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.displayName = normalizeDisplayName(displayName);
		this.joinedAt = Objects.requireNonNull(joinedAt, "joinedAt must not be null");
		this.activatedAt = activatedAt;
		this.leftAt = leftAt;
		this.auditMetadata = Objects.requireNonNull(auditMetadata, "auditMetadata must not be null");
	}

	public static GroupMember owner(UUID id, UUID groupId, UUID userId, String displayName, Instant now) {
		Objects.requireNonNull(now, "now must not be null");
		return new GroupMember(
			id,
			groupId,
			userId,
			GroupMemberPermission.OWNER,
			GroupMemberStatus.PENDING_ONBOARDING,
			displayName,
			now,
			null,
			null,
			AuditMetadata.created(now)
		);
	}

	public static GroupMember member(UUID id, UUID groupId, UUID userId, String displayName, Instant now) {
		Objects.requireNonNull(now, "now must not be null");
		return new GroupMember(
			id,
			groupId,
			userId,
			GroupMemberPermission.MEMBER,
			GroupMemberStatus.PENDING_ONBOARDING,
			displayName,
			now,
			null,
			null,
			AuditMetadata.created(now)
		);
	}

	public UUID groupId() {
		return groupId;
	}

	public UUID userId() {
		return userId;
	}

	public GroupMemberPermission permission() {
		return permission;
	}

	public GroupMemberStatus status() {
		return status;
	}

	public Optional<String> displayName() {
		return Optional.ofNullable(displayName);
	}

	public Instant joinedAt() {
		return joinedAt;
	}

	public Optional<Instant> activatedAt() {
		return Optional.ofNullable(activatedAt);
	}

	public Optional<Instant> leftAt() {
		return Optional.ofNullable(leftAt);
	}

	public AuditMetadata auditMetadata() {
		return auditMetadata;
	}

	public GroupMember activate(Instant now) {
		Objects.requireNonNull(now, "now must not be null");
		if (status == GroupMemberStatus.ACTIVE) {
			return this;
		}
		if (status != GroupMemberStatus.PENDING_ONBOARDING) {
			throw new IllegalStateException("only PENDING_ONBOARDING members can be activated.");
		}
		return new GroupMember(
			id(),
			groupId,
			userId,
			permission,
			GroupMemberStatus.ACTIVE,
			displayName,
			joinedAt,
			now,
			leftAt,
			auditMetadata.touch(now)
		);
	}

	public static String normalizeDisplayName(String displayName) {
		if (displayName == null || displayName.isBlank()) {
			return null;
		}
		String normalized = displayName.strip();
		if (normalized.length() > MAX_DISPLAY_NAME_LENGTH) {
			throw new IllegalArgumentException("displayName length must be <= " + MAX_DISPLAY_NAME_LENGTH + ": " + normalized.length());
		}
		return normalized;
	}
}
