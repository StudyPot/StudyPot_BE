package com.studypot.aistudyleader.follow.service;

import com.studypot.aistudyleader.follow.domain.FollowUserView;
import com.studypot.aistudyleader.follow.repository.FollowRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

public class FollowService {

	private final FollowRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	public FollowService(FollowRepository repository, Clock clock, Supplier<UUID> idGenerator) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
	}

	/** 대상 사용자에 대한 팔로우 상태를 토글한다. (이미 팔로우 중이면 해제, 아니면 팔로우) */
	@Transactional
	public FollowToggleResult toggleFollow(UUID followerUserId, UUID followeeUserId) {
		Objects.requireNonNull(followerUserId, "followerUserId must not be null");
		Objects.requireNonNull(followeeUserId, "followeeUserId must not be null");
		if (followerUserId.equals(followeeUserId)) {
			throw new FollowSelfNotAllowedException("자기 자신은 팔로우할 수 없습니다.");
		}
		if (!repository.existsActiveUser(followeeUserId)) {
			throw new FollowTargetNotFoundException("follow target user was not found.");
		}
		boolean following;
		if (repository.existsFollow(followerUserId, followeeUserId)) {
			repository.deleteFollow(followerUserId, followeeUserId);
			following = false;
		} else {
			Instant now = clock.instant();
			repository.insertFollow(idGenerator.get(), followerUserId, followeeUserId, now);
			following = true;
		}
		return new FollowToggleResult(followeeUserId, following);
	}

	@Transactional(readOnly = true)
	public List<FollowUserView> listFollowing(UUID userId) {
		Objects.requireNonNull(userId, "userId must not be null");
		return repository.findFollowing(userId);
	}

	@Transactional(readOnly = true)
	public List<FollowUserView> listFollowers(UUID userId) {
		Objects.requireNonNull(userId, "userId must not be null");
		return repository.findFollowers(userId);
	}
}
