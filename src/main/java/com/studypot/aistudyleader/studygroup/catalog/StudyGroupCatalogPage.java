package com.studypot.aistudyleader.studygroup.catalog;

import java.util.List;

public record StudyGroupCatalogPage(List<StudyGroupCatalogEntry> items, String nextCursor) {
}
