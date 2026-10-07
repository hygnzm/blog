<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import {
  ArrowUpRight,
  ArrowLeft,
  ArrowRight,
  Search,
  Code2,
  Server,
  Bot,
  BookOpen,
  BriefcaseBusiness,
  CalendarDays,
  Clock3,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  X,
  SlidersHorizontal,
  Check,
  RotateCcw,
  Sparkles,
  Braces,
  FileText,
} from "lucide-vue-next";
import { marked } from "marked";
import DOMPurify from "dompurify";
import { categoryDefinitions, getContent } from "./content";

const categories = ref(categoryDefinitions.map((item) => ({ ...item, count: 0 })));
const categoryIcons = { backend: Server, agent: Bot, papers: BookOpen, career: BriefcaseBusiness };
const category = ref(""),
  tags = ref([]),
  selectedTags = ref([]);
const searchInput = ref(""),
  keyword = ref(""),
  from = ref(""),
  to = ref(""),
  period = ref("all");
const sort = ref("newest"),
  page = ref(1),
  total = ref(0),
  totalPages = ref(0),
  libraryTotal = ref(0);
const articles = ref([]),
  loading = ref(true),
  error = ref(""),
  metadataError = ref("");
const mobileFilters = ref(false),
  slug = ref(""),
  article = ref(null),
  detailError = ref(""),
  detailLoading = ref(false);
let listController, detailController, metadataController, searchTimer;
const categoryName = (id) =>
  categories.value.find((c) => c.id === id)?.name || id;
const listHeading = computed(() =>
  category.value ? categoryName(category.value) : "全部文章",
);
const hasFilters = computed(
  () =>
    keyword.value ||
    category.value ||
    selectedTags.value.length ||
    from.value ||
    to.value,
);
const dateError = computed(() =>
  from.value && to.value && from.value > to.value
    ? "开始日期不能晚于结束日期"
    : "",
);
const body = computed(() =>
  DOMPurify.sanitize(marked.parse(article.value?.content || "", { gfm: true })),
);
const dateLabel = (date) => date?.replaceAll("-", ".");

async function loadMetadata() {
  metadataController?.abort();
  const controller = (metadataController = new AbortController());
  metadataError.value = "";
  try {
    const data = await getContent(
      "/metadata",
      { category: category.value },
      controller.signal,
    );
    categories.value = data.categories;
    tags.value = data.tags;
    libraryTotal.value = data.total;
  } catch (e) {
    if (e.name !== "AbortError") metadataError.value = e.message;
  }
}
async function loadArticles() {
  listController?.abort();
  error.value = "";
  if (dateError.value) {
    loading.value = false;
    return;
  }
  const controller = (listController = new AbortController());
  loading.value = true;
  try {
    const data = await getContent(
      "/articles",
      {
        q: keyword.value,
        category: category.value,
        tags: selectedTags.value,
        from: from.value,
        to: to.value,
        sort: sort.value,
        page: page.value,
        size: 6,
      },
      controller.signal,
    );
    articles.value = data.items;
    total.value = data.total;
    totalPages.value = data.totalPages;
  } catch (e) {
    if (e.name !== "AbortError") error.value = e.message;
  } finally {
    if (!controller.signal.aborted) loading.value = false;
  }
}
function chooseCategory(id) {
  if (category.value === id) return;
  category.value = id;
  selectedTags.value = [];
  loadMetadata();
}
function toggleTag(tag) {
  selectedTags.value = selectedTags.value.includes(tag)
    ? selectedTags.value.filter((t) => t !== tag)
    : [...selectedTags.value, tag];
}
function localDate(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}
function choosePeriod(value) {
  period.value = value;
  const now = new Date(),
    start = new Date(now);
  if (value === "all") {
    from.value = "";
    to.value = "";
    return;
  }
  if (value === "week") start.setDate(now.getDate() - 6);
  if (value === "month") start.setDate(1);
  if (value === "year") {
    start.setMonth(0);
    start.setDate(1);
  }
  from.value = localDate(start);
  to.value = localDate(now);
}
function reset() {
  clearTimeout(searchTimer);
  searchInput.value = "";
  keyword.value = "";
  selectedTags.value = [];
  from.value = "";
  to.value = "";
  period.value = "all";
  sort.value = "newest";
  if (category.value) {
    category.value = "";
    loadMetadata();
  }
}
function submitSearch() {
  clearTimeout(searchTimer);
  keyword.value = searchInput.value.trim();
}
function changePage(value) {
  page.value = value;
  loadArticles();
  document
    .getElementById("article-list")
    ?.scrollIntoView({ behavior: "smooth", block: "start" });
}
async function readRoute() {
  const match = window.location.hash.match(/^#\/articles\/([a-z0-9-]+)$/);
  slug.value = match?.[1] || "";
  detailController?.abort();
  article.value = null;
  detailError.value = "";
  if (!slug.value) {
    detailLoading.value = false;
    document.title = "码间 · 技术博客";
    return;
  }
  window.scrollTo({ top: 0, behavior: "instant" });
  const controller = (detailController = new AbortController());
  detailLoading.value = true;
  try {
    article.value = await getContent(
      `/articles/${slug.value}`,
      {},
      controller.signal,
    );
    document.title = `${article.value.title} · 码间`;
  } catch (e) {
    if (e.name !== "AbortError") detailError.value = e.message;
  } finally {
    if (!controller.signal.aborted) detailLoading.value = false;
  }
}
watch(searchInput, () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(submitSearch, 300);
});
watch([keyword, category, selectedTags, from, to, sort], () => {
  page.value = 1;
  loadArticles();
});
onMounted(() => {
  loadMetadata();
  loadArticles();
  readRoute();
  window.addEventListener("hashchange", readRoute);
});
onUnmounted(() => {
  window.removeEventListener("hashchange", readRoute);
  listController?.abort();
  detailController?.abort();
  metadataController?.abort();
  clearTimeout(searchTimer);
});
</script>

<template>
  <header class="site-header">
    <div class="header-inner">
      <a href="#" class="brand" aria-label="码间技术博客首页">
        <span class="brand-mark"><Code2 :size="27" :stroke-width="2.2" /></span>
        <span class="brand-name"
          >码间<span class="brand-dot">·</span
          ><span class="brand-description">技术博客</span></span
        >
      </a>
      <div class="header-motto">
        <span class="status-dot"></span> 学习不止，记录不息
        <span class="motto-code">&lt; / &gt;</span>
      </div>
    </div>
  </header>

  <main v-if="!slug">
    <section class="search-section">
      <div class="search-intro">
        <span class="eyebrow">A LITTLE BETTER, EVERY DAY</span>
        <h1>把学到的，<span>写下来。</span></h1>
        <p>关于代码、智能与研究的思考和实践</p>
      </div>
      <form class="search-box" role="search" @submit.prevent="submitSearch">
        <Search :size="21" class="search-leading" />
        <input
          v-model="searchInput"
          aria-label="搜索文章"
          placeholder="搜索文章、技术或关键词…"
        />
        <button
          v-if="searchInput"
          type="button"
          class="search-clear"
          aria-label="清空搜索"
          @click="
            searchInput = '';
            submitSearch();
          "
        >
          <X :size="17" />
        </button>
        <button type="submit" class="search-submit" aria-label="提交搜索">
          <ArrowRight :size="22" />
        </button>
      </form>
      <div class="search-suggestions">
        <span>试着搜搜</span
        ><button
          v-for="word in ['Java', 'Spring Boot', 'RAG', 'Transformer']"
          :key="word"
          @click="
            searchInput = word;
            submitSearch();
          "
        >
          {{ word }}
        </button>
      </div>
    </section>

    <div class="page-shell">
      <section class="category-grid" aria-label="文章分类">
        <button
          v-for="(item, index) in categories"
          :key="item.id"
          :class="['category-card', item.id, { active: category === item.id }]"
          :aria-pressed="category === item.id"
          @click="chooseCategory(category === item.id ? '' : item.id)"
        >
          <div class="category-icon">
            <component
              :is="categoryIcons[item.id]"
              :size="31"
              :stroke-width="1.8"
            /><span class="icon-orbit"></span>
          </div>
          <div class="category-text">
            <span class="category-number">0{{ index + 1 }} / {{ item.id === "career" ? "EXPERIENCE" : "LEARNING" }}</span>
            <h2>{{ item.name }}</h2>
            <p>{{ item.description }}</p>
          </div>
          <span class="category-corner"><ArrowUpRight :size="19" /></span>
          <span class="category-count">{{ item.count }} 篇文章</span>
        </button>
      </section>

      <div class="content-layout">
        <aside
          :class="['filters', { 'mobile-open': mobileFilters }]"
          aria-label="筛选文章"
        >
          <div class="filter-top">
            <span><SlidersHorizontal :size="15" /> 筛选文章</span
            ><button @click="reset">重置<RotateCcw :size="12" /></button>
          </div>
          <section class="filter-section">
            <h3>{{ category === "career" ? "求职标签" : "技术标签" }} <span>可多选</span><ChevronDown :size="15" /></h3>
            <p v-if="metadataError" class="small-error">
              标签加载失败<button @click="loadMetadata">重试</button>
            </p>
            <div class="tag-grid">
              <button
                v-for="tag in tags"
                :key="tag.name"
                :class="{ selected: selectedTags.includes(tag.name) }"
                :aria-pressed="selectedTags.includes(tag.name)"
                @click="toggleTag(tag.name)"
              >
                <span>{{ tag.name }}</span
                ><Check
                  v-if="selectedTags.includes(tag.name)"
                  :size="12"
                /><small v-else>{{ tag.count }}</small>
              </button>
            </div>
            <p class="filter-hint">{{ !tags.length && !metadataError ? "该分类暂无标签，发布文章后会自动显示" : "选择标签，找到感兴趣的内容" }}</p>
          </section>
          <section class="filter-section date-section">
            <h3>发布时间<ChevronDown :size="15" /></h3>
            <div class="period-grid">
              <button
                v-for="item in [
                  { id: 'all', name: '全部时间' },
                  { id: 'week', name: '最近 7 天' },
                  { id: 'month', name: '本月' },
                  { id: 'year', name: '本年' },
                ]"
                :key="item.id"
                :class="{ selected: period === item.id }"
                :aria-pressed="period === item.id"
                @click="choosePeriod(item.id)"
              >
                {{ item.name }}
              </button>
            </div>
            <label class="date-label" for="date-from">自定义日期</label>
            <div class="date-input">
              <input
                id="date-from"
                v-model="from"
                type="date"
                aria-label="开始日期"
                @change="period = 'custom'"
              />
            </div>
            <span class="date-divider">至</span>
            <div class="date-input">
              <input
                v-model="to"
                type="date"
                aria-label="结束日期"
                @change="period = 'custom'"
              />
            </div>
            <p v-if="dateError" class="small-error" role="alert">
              {{ dateError }}
            </p>
          </section>
          <div class="sidebar-note">
            <Braces :size="22" />
            <p>让知识相互连接，<br />让成长有迹可循。</p>
            <span>BUILD. LEARN. REPEAT.</span>
          </div>
        </aside>

        <section
          id="article-list"
          class="article-list"
          aria-label="文章列表"
          :aria-busy="loading"
        >
          <div class="list-header">
            <div>
              <div class="list-eyebrow">THE NOTEBOOK</div>
              <h2>
                {{ listHeading }}
                <span>{{ total.toString().padStart(2, "0") }}</span>
              </h2>
            </div>
            <div class="list-controls">
              <button
                class="mobile-filter-toggle"
                :aria-expanded="mobileFilters"
                @click="mobileFilters = !mobileFilters"
              >
                <SlidersHorizontal :size="16" />筛选</button
              ><label class="sort-label"
                ><span class="sr-only">文章排序</span
                ><select v-model="sort" aria-label="文章排序">
                  <option value="newest">最新发布</option>
                  <option value="oldest">最早发布</option></select
                ><ChevronDown :size="13"
              /></label>
            </div>
          </div>
          <div v-if="hasFilters" class="active-filters">
            <span v-if="keyword"
              >“{{ keyword }}”
              <button
                aria-label="清除关键词"
                @click="
                  searchInput = '';
                  submitSearch();
                "
              >
                <X :size="12" /></button></span
            ><span v-if="category"
              >{{ categoryName(category)
              }}<button aria-label="清除分类" @click="chooseCategory('')">
                <X :size="12" /></button></span
            ><span v-for="tag in selectedTags" :key="tag"
              >{{ tag
              }}<button
                :aria-label="`清除 ${tag} 标签`"
                @click="toggleTag(tag)"
              >
                <X :size="12" /></button></span
            ><span v-if="from || to"
              >{{ from || "不限" }} → {{ to || "不限"
              }}<button aria-label="清除日期" @click="choosePeriod('all')">
                <X :size="12" /></button></span
            ><button class="clear-all" @click="reset">清除全部</button>
          </div>

          <div v-if="dateError" class="empty-state">
            <CalendarDays :size="37" />
            <h3>请检查日期范围</h3>
            <p>{{ dateError }}</p>
            <button @click="choosePeriod('all')">清除日期筛选</button>
          </div>
          <div
            v-else-if="loading"
            class="skeleton-list"
            aria-label="正在加载文章"
          >
            <div v-for="n in 3" :key="n" class="skeleton-row">
              <i></i><i></i><i></i>
            </div>
          </div>
          <div v-else-if="error" class="empty-state" role="alert">
            <Server :size="37" />
            <h3>暂时无法加载文章</h3>
            <p>{{ error }}。请刷新页面或稍后重试。</p>
            <button
              @click="
                loadArticles();
                loadMetadata();
              "
            >
              重新加载
            </button>
          </div>
          <div v-else-if="!articles.length" class="empty-state">
            <Search :size="37" />
            <h3>还没有找到相关笔记</h3>
            <p>试试其他关键词，或放宽标签和日期范围。</p>
            <button @click="reset">查看全部文章</button>
          </div>
          <div v-else class="article-rows">
            <article
              v-for="item in articles"
              :key="item.slug"
              class="article-row"
            >
              <div class="article-topline">
                <span :class="['article-category', item.category]"
                  ><component :is="categoryIcons[item.category]" :size="13" />{{
                    categoryName(item.category)
                  }}</span
                ><span v-if="item.featured" class="featured"
                  ><Sparkles :size="11" />精选</span
                >
              </div>
              <a :href="`#/articles/${item.slug}`" class="article-title-link"
                ><h3>{{ item.title }}</h3>
                <ArrowUpRight :size="22"
              /></a>
              <div class="article-meta">
                <span
                  ><CalendarDays :size="13" />{{
                    dateLabel(item.publishedAt)
                  }}</span
                ><span class="meta-dot">·</span
                ><span
                  ><Clock3 :size="13" />{{ item.readingMinutes }} 分钟阅读</span
                ><span class="meta-dot tag-dot">·</span>
                <div class="inline-tags">
                  <button
                    v-for="tag in item.tags"
                    :key="tag"
                    @click="toggleTag(tag)"
                  >
                    {{ tag }}
                  </button>
                </div>
              </div>
              <p class="article-summary">{{ item.summary }}</p>
              <a :href="`#/articles/${item.slug}`" class="read-link"
                >阅读全文<ArrowRight :size="14"
              /></a>
            </article>
          </div>
          <div
            v-if="!loading && !error && !dateError && total"
            class="pagination"
          >
            <span
              >共 {{ total }} 篇笔记<span class="pagination-sub">
                · 每页 6 篇</span
              ></span
            >
            <div>
              <button
                :disabled="page === 1"
                aria-label="上一页"
                @click="changePage(page - 1)"
              >
                <ChevronLeft :size="16" /></button
              ><button
                v-for="n in totalPages"
                :key="n"
                :class="{ current: page === n }"
                :aria-label="`第 ${n} 页`"
                :aria-current="page === n ? 'page' : undefined"
                @click="changePage(n)"
              >
                {{ n }}</button
              ><button
                :disabled="page === totalPages"
                aria-label="下一页"
                @click="changePage(page + 1)"
              >
                <ChevronRight :size="16" />
              </button>
            </div>
          </div>
        </section>
      </div>
      <div class="closing-line">
        <span></span>
        <p>每一个问题，都是下一篇笔记的开始。</p>
        <span></span>
      </div>
    </div>
  </main>

  <main v-else class="detail-shell">
    <a class="back-link" href="#"><ArrowLeft :size="16" />返回文章列表</a>
    <div v-if="detailLoading" class="skeleton-list">
      <div class="skeleton-row"><i></i><i></i><i></i></div>
    </div>
    <div v-else-if="detailError" class="empty-state" role="alert">
      <FileText :size="38" />
      <h1>文章暂时无法打开</h1>
      <p>{{ detailError }}</p>
      <button @click="readRoute">重试</button>
    </div>
    <article v-else-if="article" class="detail-article">
      <span :class="['article-category', article.category]"
        ><component :is="categoryIcons[article.category]" :size="14" />{{
          categoryName(article.category)
        }}</span
      >
      <h1>{{ article.title }}</h1>
      <div class="detail-meta">
        <span
          ><CalendarDays :size="15" />{{ dateLabel(article.publishedAt) }}</span
        ><span><Clock3 :size="15" />{{ article.readingMinutes }} 分钟阅读</span>
      </div>
      <p class="detail-summary">{{ article.summary }}</p>
      <div class="markdown-body" v-html="body"></div>
      <div class="detail-bottom">
        <div class="detail-tags">
          <span v-for="tag in article.tags" :key="tag"># {{ tag }}</span>
        </div>
        <a href="#">继续阅读其他笔记<ArrowRight :size="16" /></a>
      </div>
    </article>
  </main>

  <footer v-if="!slug" class="site-footer">
    <a href="#" class="footer-brand"><Code2 :size="18" />码间</a>
    <p>记录技术，也记录成长。</p>
    <span
      >© {{ new Date().getFullYear() }} 码间 ·
      {{ libraryTotal }} 篇学习笔记</span
    >
  </footer>
</template>
