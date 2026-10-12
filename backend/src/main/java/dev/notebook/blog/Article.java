package dev.notebook.blog;

import java.time.LocalDate;
import java.util.List;

public record Article(String slug, String title, String category, List<String> tags,
                      LocalDate publishedAt, int readingMinutes, boolean featured,
                      String summary, String content) {
    public ArticleSummary summaryView() {
        return new ArticleSummary(slug, title, category, tags, publishedAt, readingMinutes, featured, summary);
    }

    public record ArticleSummary(String slug, String title, String category, List<String> tags,
                                 LocalDate publishedAt, int readingMinutes, boolean featured, String summary) {}
}
