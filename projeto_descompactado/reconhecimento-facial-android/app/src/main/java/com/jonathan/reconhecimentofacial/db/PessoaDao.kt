package com.jonathan.reconhecimentofacial.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PessoaDao {

    @Insert
    suspend fun inserir(pessoa: PessoaEntity): Long

    @Query("SELECT * FROM pessoas")
    suspend fun listarTodas(): List<PessoaEntity>

    @Query("DELETE FROM pessoas WHERE id = :id")
    suspend fun remover(id: Long)
}
