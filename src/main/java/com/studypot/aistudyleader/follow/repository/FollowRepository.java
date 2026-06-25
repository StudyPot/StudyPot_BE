package com.studypot.aistudyleader.follow.repository;

import com.studypot.aistudyleader.follow.domain.FollowUserView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FollowRepository {

	boolean existsActiveUser(UUID userId);

	boolean existsFollow(UUID followerUserId, UUID followeeUserId);

	void insertFollow(UUID id, UUID followerUserId, UUID followeeUserId, Instant createdAt);

	void deleteFollow(UUID followerUserId, UUID followeeUserId);

	/** 내가 팔로우하는 사용자 목록(최신순). */
	List<FollowUserView> findFollowing(UUID userId);

	/** 나를 팔로우하는 사용자 목록(최신순). */
	List<FollowUserView> findFollowers(UUID userId);
}
