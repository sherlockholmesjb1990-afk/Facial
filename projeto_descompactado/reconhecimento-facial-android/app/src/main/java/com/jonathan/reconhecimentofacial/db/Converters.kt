package com.jonathan.reconhecimentofacial.db

import androidx.room.TypeConverter
import java.nio.ByteBuffer

/**
 * Room não sabe salvar FloatArray direto — convertemos pra ByteArray (binário compacto)
 * e voltamos quando lemos. Isso mantém o embedding de 128 floats ocupando só 512 bytes.
 */
class Converters {

    @TypeConverter
    fun fromFloatArray(valor: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(valor.size * 4)
        valor.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    @TypeConverter
    fun toFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes)
        val resultado = FloatArray(bytes.size / 4)
        for (i in resultado.indices) {
            resultado[i] = buffer.float
        }
        return resultado
    }
}
