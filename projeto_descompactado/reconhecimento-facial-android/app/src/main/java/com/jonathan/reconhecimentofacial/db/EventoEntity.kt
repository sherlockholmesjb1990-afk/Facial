package com.jonathan.reconhecimentofacial.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "eventos")
data class EventoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pessoaId: Long?,       // null = rosto desconhecido, não bateu com ninguém do cadastro
    val nomePessoa: String?,   // guardado aqui também pra não depender de join na hora de listar
    val distancia: Float,
    val reconhecido: Boolean,
    val criadoEm: Long = System.currentTimeMillis()
)
