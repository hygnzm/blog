<script setup>
import { computed, nextTick, ref } from "vue";
import { marked } from "marked";
import DOMPurify from "dompurify";
import PaperDemo from "./PaperDemo.vue";

const props = defineProps({ content: { type: String, default: "" } });
const sections = computed(() => props.content
  .split(/<!--\s*demo:(attention|shift|position)\s*-->/g)
  .map((value, index) => index % 2
    ? { kind: "demo", value }
    : { kind: "html", value: DOMPurify.sanitize(marked.parse(value, { gfm: true })) }));
const diagramDialog = ref(null);
const diagramSource = ref("");
const diagramDescription = ref("");

async function openDiagram(event) {
  const trigger = event.target.closest?.("button.diagram-open");
  if (!trigger) return;
  const image = trigger.querySelector("img");
  if (!image) return;
  diagramSource.value = image.getAttribute("src");
  diagramDescription.value = image.alt;
  await nextTick();
  if (!diagramDialog.value.open) diagramDialog.value.showModal();
}
</script>

<template>
  <div class="markdown-body" @click="openDiagram">
    <template v-for="(section, index) in sections" :key="index">
      <div v-if="section.kind === 'html'" v-html="section.value"></div>
      <PaperDemo v-else :kind="section.value" />
    </template>
    <dialog ref="diagramDialog" class="diagram-dialog" aria-label="文章配图大图"
      @click="event => { if (event.target === diagramDialog) diagramDialog.close(); }">
      <div class="diagram-toolbar">
        <span>配图大图 · 可滚动查看</span>
        <button type="button" autofocus @click="diagramDialog.close()">关闭大图</button>
      </div>
      <div class="diagram-scroll">
        <img v-if="diagramSource" :src="diagramSource" :alt="diagramDescription" />
      </div>
    </dialog>
  </div>
</template>
