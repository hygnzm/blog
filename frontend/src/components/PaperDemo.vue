<script setup>
import { computed, ref } from "vue";

defineProps({ kind: { type: String, required: true } });

const tokens = ["我", "喜欢", "猫"];
const queries = [[1, 0], [0, 1], [1, 1]];
const keys = [[1, 0], [0, 1], [1, 1]];
const values = [[1, 0], [0, 2], [1, 1]];
const queryPosition = ref(1);
const causal = ref(true);
const attention = computed(() => {
  const query = queries[queryPosition.value];
  const rows = tokens.map((token, index) => {
    const score = query[0] * keys[index][0] + query[1] * keys[index][1];
    return { token, index, score, scaled: score / Math.sqrt(2),
      masked: causal.value && index > queryPosition.value, key: keys[index], value: values[index] };
  });
  const maximum = Math.max(...rows.filter(row => !row.masked).map(row => row.scaled));
  const exponentials = rows.map(row => row.masked ? 0 : Math.exp(row.scaled - maximum));
  const sum = exponentials.reduce((total, value) => total + value, 0);
  const weighted = rows.map((row, index) => ({ ...row, weight: exponentials[index] / sum }));
  const output = [0, 1].map(dimension => weighted.reduce((total, row) => total + row.weight * row.value[dimension], 0));
  return { query, rows: weighted, output };
});
const number = value => (Math.abs(value) < 0.0005 ? 0 : value).toFixed(3);
const vector = values => `[${values.map(value => number(value)).join(", ")}]`;
const inputVectorLabel = values => `[${values.join(", ")}]`;

const decoderInputs = ["开始标记", "我", "喜欢", "猫"];
const decoderTargets = ["我", "喜欢", "猫", "结束标记"];
const predictionPosition = ref(2);
const prefix = computed(() => decoderInputs.slice(0, predictionPosition.value + 1).join(" → "));

const reversed = ref(false);
const usePosition = ref(true);
const sentence = computed(() => reversed.value ? ["猫", "喜欢", "我"] : tokens);
const wordPosition = computed(() => reversed.value ? 2 : 0);
const embedding = [0.2, 0.1, -0.3, 0.4];
const positionEncoding = computed(() => [Math.sin(wordPosition.value), Math.cos(wordPosition.value),
  Math.sin(wordPosition.value / 100), Math.cos(wordPosition.value / 100)]);
const inputVector = computed(() => embedding.map((value, dimension) =>
  value + (usePosition.value ? positionEncoding.value[dimension] : 0)));
</script>

<template>
  <section class="paper-demo" :data-demo="kind" :aria-label="kind === 'attention' ? '注意力计算示例' : kind === 'shift' ? '目标对齐示例' : '位置编码示例'">
    <template v-if="kind === 'attention'">
      <h3>换一个查询位置，看看权重怎么变</h3>
      <div class="demo-controls">
        <label>查询位置
          <select v-model.number="queryPosition" aria-label="注意力查询位置">
            <option v-for="(token, index) in tokens" :key="token" :value="index">{{ index }} · {{ token }}</option>
          </select>
        </label>
        <label class="demo-check"><input v-model="causal" type="checkbox" />遮住后面的位置</label>
      </div>
      <p class="demo-current">Q = {{ inputVectorLabel(attention.query) }}，d<sub>k</sub> = 2</p>
      <p class="demo-scroll-hint">表格可左右滑动，查看各位置的完整数值。</p>
      <div class="demo-table-scroll" tabindex="0" role="region" aria-label="注意力数值表格，可左右滚动">
        <table>
          <thead><tr><th>被查询位置</th><th>K / V</th><th>点积 / softmax 前分数</th><th>注意力权重</th></tr></thead>
          <tbody>
            <tr v-for="row in attention.rows" :key="row.index" :class="{ 'demo-masked': row.masked }">
              <td>{{ row.index }} · {{ row.token }}</td>
              <td>K {{ inputVectorLabel(row.key) }}<br />V {{ inputVectorLabel(row.value) }}</td>
              <td>{{ number(row.score) }} / {{ row.masked ? '−∞（遮住）' : number(row.scaled) }}</td>
              <td><span class="weight-value">{{ number(row.weight) }}</span><span class="weight-track"><span :style="{ width: `${row.weight * 100}%` }"></span></span></td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="demo-result" aria-live="polite">加权汇总后的向量：<strong>{{ vector(attention.output) }}</strong></p>
      <p class="demo-note">数值用于演示单头计算。计算结果是一个向量，词表投影还在后面。</p>
    </template>

    <template v-else-if="kind === 'shift'">
      <h3>这一位置输入什么，又要预测什么</h3>
      <div class="demo-position-buttons" role="group" aria-label="选择训练位置">
        <button v-for="(token, index) in decoderInputs" :key="index" type="button"
          :aria-pressed="predictionPosition === index" @click="predictionPosition = index">
          <span>位置 {{ index }}</span><strong>{{ token }}</strong>
        </button>
      </div>
      <p class="demo-current">该位置的已知前文：{{ prefix }}</p>
      <div class="demo-table-scroll">
        <table>
          <thead><tr><th>输入位置</th><th>输入 token</th><th>能否读取</th></tr></thead>
          <tbody><tr v-for="(token, index) in decoderInputs" :key="index" :class="{ 'demo-masked': index > predictionPosition }">
            <td>{{ index }}</td><td>{{ token }}</td><td>{{ index <= predictionPosition ? '可读取' : '后面的位置，遮住' }}</td>
          </tr></tbody>
        </table>
      </div>
      <p class="demo-result" aria-live="polite">输入 <strong>{{ decoderInputs[predictionPosition] }}</strong>，这一位置的预测目标是 <strong>{{ decoderTargets[predictionPosition] }}</strong>。</p>
      <p class="demo-note">表格展示训练输入、预测目标和掩码怎样对应。</p>
    </template>

    <template v-else-if="kind === 'position'">
      <h3>同一个“我”，换到另一个位置</h3>
      <div class="demo-controls">
        <button type="button" @click="reversed = !reversed">交换“我”与“猫”</button>
        <label class="demo-check"><input v-model="usePosition" type="checkbox" />加入位置编码</label>
      </div>
      <div class="demo-sentence" aria-label="当前词序">
        <div v-for="(token, index) in sentence" :key="index" :class="{ observed: token === '我' }">
          <strong>{{ token }}</strong><span>位置 {{ index }}</span>
        </div>
      </div>
      <p class="demo-current">观察“我” · 位置 {{ wordPosition }} · {{ usePosition ? 'X = E + PE' : 'X = E' }}</p>
      <div class="demo-table-scroll">
        <table>
          <thead><tr><th>维度</th><th>词向量 E</th><th>位置向量 PE{{ usePosition ? '' : '（未加入）' }}</th><th>输入向量 X</th></tr></thead>
          <tbody><tr v-for="(value, dimension) in embedding" :key="dimension">
            <td>{{ dimension }}</td><td>{{ number(value) }}</td><td>{{ number(positionEncoding[dimension]) }}</td><td class="position-value">{{ number(inputVector[dimension]) }}</td>
          </tr></tbody>
        </table>
      </div>
      <p class="demo-result" aria-live="polite">送入 QKV 投影的 X：<strong>{{ vector(inputVector) }}</strong></p>
      <p class="demo-note">4 维示意；E 是教学数值，PE 按原论文正弦公式计算。基础模型实际使用 512 维。</p>
    </template>
  </section>
</template>
