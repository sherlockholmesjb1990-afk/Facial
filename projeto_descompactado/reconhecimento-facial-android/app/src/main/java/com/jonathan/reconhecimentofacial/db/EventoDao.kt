package com.jonathan.reconhecimentofacial.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EventoDao {

    @Insert
    suspend fun inserir(evento: EventoEntity): Long

    @Query("SELECT * FROM eventos ORDER BY criadoEm DESC LIMIT :limite")
    suspend fun listarRecentes(limite: Int = 100): List<EventoEntity>
}
