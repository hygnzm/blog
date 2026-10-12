package dev.notebook.blog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Service
public class ArticleService {
    private final List<Article> articles;
    private final List<Category> categories = List.of(
        new Category("backend", "后端学习", "从基础到架构，构建可靠的服务"),
        new Category("agent", "Agent 学习", "连接模型与工具，让想法付诸行动"),
        new Category("papers", "论文学习", "拆解核心思路，读懂研究的下一步"),
        new Category("career", "求职经历", "记录投递、面试与复盘的每一步")
    );

    public ArticleService(ObjectMapper mapper) throws IOException {
        try (var input = new ClassPathResource("articles.json").getInputStream()) {
            articles = List.copyOf(mapper.readValue(input, new TypeReference<List<Article>>() {}));
        }
        if (articles.stream().map(Article::slug).distinct().count() != articles.size()) {
            throw new IllegalStateException("文章 slug 不能重复");
        }
    }

    public ArticlePage list(String q, String category, List<String> tags, LocalDate from,
                            LocalDate to, String sort, int page, int size) {
        validateCategory(category);
        if (page < 1 || size < 1 || size > 50) badRequest("page 必须大于 0，size 必须在 1 到 50 之间");
        if (from != null && to != null && from.isAfter(to)) badRequest("开始日期不能晚于结束日期");
        if (!Set.of("newest", "oldest").contains(sort)) badRequest("sort 仅支持 newest 或 oldest");
        String keyword = q == null ? "" : q.strip().toLowerCase(Locale.ROOT);
        Comparator<Article> order = Comparator.comparing(Article::publishedAt).thenComparing(Article::slug);
        if (sort.equals("newest")) order = order.reversed();
        List<Article> filtered = articles.stream()
            .filter(a -> category == null || category.isBlank() || a.category().equals(category))
            .filter(a -> tags == null || tags.isEmpty() || tags.stream().anyMatch(a.tags()::contains))
            .filter(a -> from == null || !a.publishedAt().isBefore(from))
            .filter(a -> to == null || !a.publishedAt().isAfter(to))
            .filter(a -> keyword.isEmpty() || (a.title() + " " + a.summary() + " " + String.join(" ", a.tags()))
                .toLowerCase(Locale.ROOT).contains(keyword))
            .sorted(order).toList();
        long offset = ((long) page - 1) * size;
        List<Article.ArticleSummary> items = offset >= filtered.size() ? List.of()
            : filtered.subList((int) offset, (int) Math.min(offset + size, filtered.size()))
                .stream().map(Article::summaryView).toList();
        return new ArticlePage(items, filtered.size(), page, size, (filtered.size() + size - 1) / size);
    }

    public Article detail(String slug) {
        return articles.stream().filter(a -> a.slug().equals(slug)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "这篇文章不存在"));
    }

    public Metadata metadata(String category) {
        validateCategory(category);
        List<CategoryCount> counts = categories.stream().map(c -> new CategoryCount(c.id(), c.name(),
            c.description(), articles.stream().filter(a -> a.category().equals(c.id())).count())).toList();
        Map<String, Long> tagCounts = new LinkedHashMap<>();
        articles.stream().filter(a -> category == null || category.isBlank() || a.category().equals(category))
            .flatMap(a -> a.tags().stream()).forEach(tag -> tagCounts.merge(tag, 1L, Long::sum));
        List<TagCount> tags = tagCounts.entrySet().stream().map(e -> new TagCount(e.getKey(), e.getValue())).toList();
        return new Metadata(counts, tags, articles.size());
    }

    private void validateCategory(String category) {
        if (category != null && !category.isBlank() && categories.stream().noneMatch(c -> c.id().equals(category)))
            badRequest("未知的文章分类");
    }
    private void badRequest(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    public record Category(String id, String name, String description) {}
    public record CategoryCount(String id, String name, String description, long count) {}
    public record TagCount(String name, long count) {}
    public record Metadata(List<CategoryCount> categories, List<TagCount> tags, int total) {}
    public record ArticlePage(List<Article.ArticleSummary> items, int total, int page, int size, int totalPages) {}
}
