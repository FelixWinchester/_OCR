package com.example.ocrmsg.data

object MetricsCalculator {

    /**
     CER — Character Error Rate
     Считается через расстояние Левенштейна на уровне символов.
     CER = (substitutions + insertions + deletions) / len(reference)
     Результат: 0.0 = идеально, 1.0 = полностью неверно, >1.0 возможно при длинных вставках
     */
    fun cer(reference: String, hypothesis: String): Float {
        val ref = reference.trim()
        val hyp = hypothesis.trim()

        if (ref.isEmpty()) return if (hyp.isEmpty()) 0f else 1f

        val distance = levenshtein(ref, hyp)
        return distance.toFloat() / ref.length.toFloat()
    }

    /**
      WER — Word Error Rate
      То же самое, но на уровне слов.
      WER = (substitutions + insertions + deletions) / len(reference_words)
     */
    fun wer(reference: String, hypothesis: String): Float {
        val refWords = reference.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val hypWords = hypothesis.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }

        if (refWords.isEmpty()) return if (hypWords.isEmpty()) 0f else 1f

        val distance = levenshtein(refWords, hypWords)
        return distance.toFloat() / refWords.size.toFloat()
    }

    /**
     Exact Match — точное совпадение после нормализации
     Убираем лишние пробелы и приводим к нижнему регистру
     */
    fun exactMatch(reference: String, hypothesis: String): Boolean {
        return normalize(reference) == normalize(hypothesis)
    }

    /**
     Precision — доля слов из hypothesis которые есть в reference
     */
    fun wordPrecision(reference: String, hypothesis: String): Float {
        val refWords = reference.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
        val hypWords = hypothesis.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }

        if (hypWords.isEmpty()) return 0f
        val correct = hypWords.count { it in refWords }
        return correct.toFloat() / hypWords.size.toFloat()
    }

    /**
     Recall — доля слов из reference которые нашлись в hypothesis
     */
    fun wordRecall(reference: String, hypothesis: String): Float {
        val refWords = reference.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val hypWords = hypothesis.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()

        if (refWords.isEmpty()) return 0f
        val found = refWords.count { it in hypWords }
        return found.toFloat() / refWords.size.toFloat()
    }

    // ── Нормализация ──────────────────────────────────────────────────

    private fun normalize(text: String): String =
        text.trim().lowercase().replace("\\s+".toRegex(), " ")

    // ── Расстояние Левенштейна (строки) ──────────────────────────────

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[m][n]
    }

    // ── Расстояние Левенштейна (списки слов) ─────────────────────────

    private fun levenshtein(a: List<String>, b: List<String>): Int {
        val m = a.size
        val n = b.size
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[m][n]
    }
}