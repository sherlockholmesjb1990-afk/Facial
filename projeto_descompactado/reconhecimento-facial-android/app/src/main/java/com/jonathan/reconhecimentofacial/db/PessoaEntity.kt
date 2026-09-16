package com.jonathan.reconhecimentofacial.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pessoas")
data class PessoaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val embedding: FloatArray,   // vetor de 128 dimensões gerado pelo FaceNet
    val criadoEm: Long = System.currentTimeMillis()
) {
    // equals/hashCode padrão do data class não lidam bem com FloatArray, mas
    // não comparamos PessoaEntity diretamente em nenhum lugar, então não é problema aqui.
}
