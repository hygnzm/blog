package dev.notebook.blog;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BlogController {
    private final ArticleService service;
    public BlogController(ArticleService service) { this.service = service; }

    @GetMapping("/articles")
    public ArticleService.ArticlePage articles(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) List<String> tags,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "newest") String sort,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "6") int size) {
        return service.list(q, category, tags, from, to, sort, page, size);
    }
    @GetMapping("/articles/{slug}")
    public Article detail(@PathVariable String slug) { return service.detail(slug); }
    @GetMapping("/metadata")
    public ArticleService.Metadata metadata(@RequestParam(required = false) String category) { return service.metadata(category); }
    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "ok"); }
}
