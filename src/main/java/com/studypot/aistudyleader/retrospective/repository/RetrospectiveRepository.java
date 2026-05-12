package com.studypot.aistudyleader.retrospective.repository;

import com.studypot.aistudyleader.retrospective.domain.Retrospective;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveMembershipContext;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveProgress;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveTaskSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RetrospectiveRepository {

	boolean existsCurriculumWeek(UUID weekId);

	Optional<RetrospectiveMembershipContext> findMembershipByWeekId(UUID weekId, UUID userId);

	Optional<RetrospectiveProgress> findProgress(UUID weekId, UUID memberId);

	Optional<Retrospective> findRetrospective(UUID progressId, UUID weekId, UUID memberId);

	List<RetrospectiveTaskSummary> findTaskSummaries(UUID progressId, UUID weekId, UUID memberId);

	boolean insertRetrospective(Retrospective retrospective);
}
