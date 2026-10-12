package dev.notebook.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BlogApiTest {
    @Autowired private MockMvc mvc;

    @Test void paginatesWithoutLeakingFullContent() throws Exception {
        mvc.perform(get("/api/articles")).andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(19)).andExpect(jsonPath("$.items", hasSize(6)))
            .andExpect(jsonPath("$.totalPages").value(4))
            .andExpect(jsonPath("$.items[0].content").doesNotExist());
        mvc.perform(get("/api/articles").param("page", "4")).andExpect(jsonPath("$.items", hasSize(1)));
    }
    @Test void combinesCategoryTagKeywordAndInclusiveDates() throws Exception {
        mvc.perform(get("/api/articles").param("category", "backend").param("tags", "Java")
            .param("q", "spring").param("from", "2026-10-07").param("to", "2026-10-07"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].slug").value("spring-boot-first-api"));
    }
    @Test void multipleTagsMatchAnyAndMetadataFollowsCategory() throws Exception {
        mvc.perform(get("/api/articles").param("category", "backend").param("tags", "MySQL", "Redis"))
            .andExpect(jsonPath("$.total").value(2));
        mvc.perform(get("/api/metadata").param("category", "papers")).andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(19))
            .andExpect(jsonPath("$.tags[*].name", not(hasItem("Java"))));
    }
    @Test void returnsRealDetailAnd404() throws Exception {
        mvc.perform(get("/api/articles/transformer-attention-notes")).andExpect(status().isOk())
            .andExpect(jsonPath("$.content", containsString("<!-- demo:attention -->")))
            .andExpect(jsonPath("$.content", containsString("transformer-overview.svg")));
        mvc.perform(get("/api/articles/spring-boot-first-api")).andExpect(status().isOk())
            .andExpect(jsonPath("$.content", containsString("@RestController")));
        mvc.perform(get("/api/articles/does-not-exist")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("这篇文章不存在"));
    }
    @Test void rejectsInvalidParameters() throws Exception {
        mvc.perform(get("/api/articles").param("from", "invalid")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/articles").param("from", "2026-10-07").param("to", "2026-09-01")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/articles").param("size", "100")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/articles").param("page", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/articles").param("sort", "unknown")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/metadata").param("category", "unknown")).andExpect(status().isBadRequest());
    }
    @Test void publishesBothJavaNotesWithCompleteCodeAndSharedTags() throws Exception {
        mvc.perform(get("/api/articles").param("category", "backend").param("tags", "Java基础"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.items[*].tags[*]", hasItem("Java")));
        mvc.perform(get("/api/articles/java-reflection-annotations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", containsString("class ReflectionDemo")))
            .andExpect(jsonPath("$.content", containsString("class ValidationDemo")))
            .andExpect(jsonPath("$.content", containsString("几句话看懂")));
        mvc.perform(get("/api/articles/java-keywords-generics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", containsString("class GenericBoxDemo")))
            .andExpect(jsonPath("$.tags", hasItems("Java", "Java基础")));
    }
    @Test void handlesEmptyResultsAndOldestSorting() throws Exception {
        mvc.perform(get("/api/articles").param("q", "missing-keyword")).andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.items", hasSize(0)));
        mvc.perform(get("/api/articles").param("sort", "oldest")).andExpect(jsonPath("$.items[0].publishedAt").value("2026-09-04"));
    }
}
