模型没有学过某份资料，或训练时使用的资料已经过时，可以在回答时检索外部文档。RAG 研究的就是怎样把检索结果用于生成。

Lewis 等人在 NeurIPS 2020 发表了 **Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks**。RAG 即检索增强生成，模型读取外部资料来回答问题，训练则根据最终答案调整检索器和生成器。

这篇接在 Transformer 后面，继续看检索到的文字怎样进入生成模型，以及生成结果怎样影响检索训练。

本组笔记：[BDI](#/articles/bdi-beliefs-desires-intentions) · [DQN](#/articles/dqn-environment-loop) · [Transformer](#/articles/transformer-attention-notes) · [RAG](#/articles/rag-retrieval-pipeline)

## 参数化记忆与外部文档

预训练让模型从文本中学习事实和语言规律，论文把参数中保存的知识称为参数化记忆。知识分布在模型参数里，通常通过训练更新。

非参数化记忆把知识放在外部文档与检索索引中。资料可以增删、替换，检索到的原文也可以直接检查。

| 知识存放方式 | 在原论文中的对应部分 | 可以怎样使用或更新 |
| --- | --- | --- |
| 参数化记忆 | BART 生成器的参数 | 通过模型计算使用，通常通过训练更新 |
| 非参数化记忆 | 维基百科文本及其向量索引 | 检索后作为输入阅读，可以替换外部资料与索引 |

检索器本身是带参数的神经网络，“非参数化记忆”描述外部文档怎样存储知识。

## 从公司文档中检索报销期限

假设有人问：“报销最晚要在多少天内提交？”知识库里有费用报销制度、差旅申请说明和付款流程。

这个公司文档例子用于说明流程。原论文使用 2018 年 12 月的维基百科，把文章切成约 100 个英文词的片段，共约 2100 万段。

<figure class="paper-figure">
  <button type="button" class="diagram-open" aria-label="放大 RAG 检索与生成流程图"><img src="./images/papers/rag-flow.svg" width="820" height="835" alt="文档提前切分编码建立索引；问题经过DPR查询编码器检索K篇资料；每篇资料分别与问题拼接给BART，再按检索概率合并生成概率，得到答案。" loading="lazy" /></button>
  <figcaption>原论文 Figure 1 的主要流程。每篇候选文档分别进入生成计算，随后合并预测概率。点击图片可看大图。</figcaption>
</figure>

查询前，系统将资料切成片段，用文档编码器计算向量并建立搜索索引，原文也要保留。索引用向量查找候选资料，生成器读取的是文档文字。

DPR 用两个基于 BERT 的编码器分别编码问题和文档：问题 x 编码为 q(x)，文档 z 编码为 d(z)。点积 q(x)·d(z) 给出匹配分数，检索器据此选取得分最高的 K 篇候选资料。

原论文用 FAISS 建立向量索引，训练时尝试 K = 5 或 10。这里的 K 是候选文档数量，Transformer 中的 Key 则是注意力计算使用的向量。

生成器采用 BART-large，一个编码器—解码器 Transformer。问题与一篇候选文档拼接后送入 BART 编码器，解码器结合问题、文档和已知答案前文预测下一 token。

在报销例子中，检索器可能找到“报销期限”那段文字，生成器读取期限并写出答案。检索分数衡量检索器判断的相关性。

## 检索向量与 BART 的 QKV

DPR 用整个问题的向量 q(x) 查询外部索引。BART 用某一层、某些位置的表示乘投影矩阵，得到注意力中的 Q。两者在不同模块中匹配信息。

检索完成后，系统取出文档文字，与问题一起交给 BART 编码器，得到上下文表示 H。解码器根据答案前文生成查询，通过交叉注意力访问 H。

索引中的文档向量负责找到资料，资料原文进入 BART，交叉注意力的 V 从 BART 的编码表示计算。

## RAG-Sequence 与 RAG-Token 的差别

检索出 K 篇资料后，原论文分别用“问题 + 每篇文档”计算生成概率，并按文档权重合并。每篇文档单独进入 BART。

<figure class="paper-figure">
  <button type="button" class="diagram-open" aria-label="放大 RAG Sequence 与 Token 对比图"><img src="./images/papers/rag-mixture.svg" width="820" height="665" alt="RAG-Sequence将各篇文档条件下的完整答案概率按文档权重相加；RAG-Token在每个生成步骤合并各篇文档给出的下一token概率。" loading="lazy" /></button>
  <figcaption>两种模型在不同阶段合并概率，文档集合均由问题检索得到。根据原文第 2.1 节重绘。点击图片可看大图。</figcaption>
</figure>

RAG-Sequence 在每条分支中固定文档，各分支计算完整答案概率，结果按检索权重相加。

RAG-Token 在每次预测下一 token 时，合并各篇文档给出的概率，因此不同文档可以支持答案的不同部分。原文生成与海明威两部小说有关的内容时，各篇文档在相应 token 上的作用不同。

用一组教学数值演示 RAG-Token。两篇文档的检索权重分别为 0.8 和 0.2，生成器在两篇文档条件下给下一 token“30”的概率分别为 0.9 和 0.1，合并结果如下：

```text
P(下一 token 是“30”) = 0.8 × 0.9 + 0.2 × 0.1 = 0.74
```

检索权重和生成概率共同决定合并结果。

原论文根据输入问题取出一组候选文档，生成期间用这组文档计算和合并各步的预测概率。

## 用答案训练检索器和生成器

联合微调用问题与正确答案训练检索器和生成器。

训练要求模型提高正确答案的概率。如果某篇资料有助于生成正确答案，训练就能调整它的检索权重。生成器也在这个过程中学习读取资料、使用前文和生成答案。

这个阶段更新问题编码器和 BART 生成器，文档编码器与已有索引固定下来，因而无需每次更新后重新计算 2100 万段资料的向量。

初始化使用的 DPR 已经接受过检索监督训练。联合微调则使用上述问题与正确答案。

正确答案同时为检索器和生成器提供训练依据，两部分通过同一个答案目标连接起来。

## 实验怎样比较检索和生成

原文用开放域问答、生成式问答、Jeopardy 问题生成和事实验证任务评估 RAG。比较对象包括仅使用模型参数的生成模型，以及检索后抽取答案的方法。

表 1 中，Natural Questions 测试集上的 DPR Exact Match 为 41.5，RAG-Token 为 44.1，RAG-Sequence 为 44.5。这些分数按评测规则检查输出是否匹配参考答案。

表 6 比较检索器是否参与训练。NQ 开发集上，RAG-Sequence 固定检索器时得分为 41.2，联合学习后为 44.0。这项消融用于检查训练检索部分的作用。

作者还用不同年份的维基百科建立索引，测试那些在这段时期职位发生变化的世界领导人问题。替换索引后，模型更倾向于给出对应年份的答案。作者用替换索引的方法更新了外部知识。

## 在后端服务中加入 RAG

后端知识问答服务要写入资料、切分文档、更新索引。用户提交问题后，服务检索资料，将原文加入生成输入，返回答案及其来源。

这类服务可以使用固定的检索模型和生成模型。按原论文实现时，需要联合微调，分别计算每篇文档下的生成概率，并用 Sequence 或 Token 的方法合并结果。

Agent 可以调用 RAG 查询资料。任务流程需要决定何时查询、是否继续查询、怎样使用结果，以及查询失败后怎样继续。可以用 BDI 检查任务状态，用 DQN 的交互步骤检查工具反馈，用 Transformer 的计算步骤检查资料怎样进入模型。

## 原文与阅读位置

- Lewis, P. et al. **Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks**. NeurIPS, 2020。[论文页面](https://papers.nips.cc/paper/2020/hash/6b493230205f780e1bc26945df7481e5-Abstract.html) · [原论文 PDF](https://papers.nips.cc/paper_files/paper/2020/file/6b493230205f780e1bc26945df7481e5-Paper.pdf)。
- Figure 1、第 2 节：检索、生成、两种概率模型与训练；Table 1：开放域问答测试结果；Table 6：检索消融；第 4.5 节的 Index hot-swapping：替换外部知识索引。

回看：[Transformer 的交叉注意力与目标对齐](#/articles/transformer-attention-notes)。
