package com.jonathan.reconhecimentofacial

import com.jonathan.reconhecimentofacial.db.PessoaEntity
import kotlin.math.sqrt

data class ResultadoComparacao(
    val pessoaId: Long?,
    val nome: String?,
    val distancia: Float,
    val reconhecido: Boolean
)

object FaceMatcher {

    // Embeddings normalizados (L2) do FaceNet: pessoas iguais costumam ficar abaixo de ~0.9-1.0;
    // pessoas diferentes, acima disso. Vale calibrar esse número testando com fotos reais.
    const val LIMIAR_RECONHECIMENTO = 0.9f

    fun comparar(embedding: FloatArray, cadastro: List<PessoaEntity>): ResultadoComparacao {
        if (cadastro.isEmpty()) {
            return ResultadoComparacao(null, null, Float.MAX_VALUE, false)
        }

        var melhorDistancia = Float.MAX_VALUE
        var melhorPessoa: PessoaEntity? = null

        for (pessoa in cadastro) {
            val distancia = distanciaEuclidiana(embedding, pessoa.embedding)
            if (distancia < melhorDistancia) {
                melhorDistancia = distancia
                melhorPessoa = pessoa
            }
        }

        val reconhecido = melhorDistancia <= LIMIAR_RECONHECIMENTO
        return ResultadoComparacao(
            pessoaId = if (reconhecido) melhorPessoa?.id else null,
            nome = if (reconhecido) melhorPessoa?.nome else null,
            distancia = melhorDistancia,
            reconhecido = reconhecido
        )
    }

    private fun distanciaEuclidiana(a: FloatArray, b: FloatArray): Float {
        var soma = 0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            soma += diff * diff
        }
        return sqrt(soma)
    }
}
