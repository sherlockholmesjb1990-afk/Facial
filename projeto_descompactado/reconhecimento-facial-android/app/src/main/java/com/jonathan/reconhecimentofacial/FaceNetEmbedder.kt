package com.jonathan.reconhecimentofacial

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Carrega o modelo facenet.tflite (128 dimensões) e converte uma foto de rosto
 * já recortada em um "embedding": um vetor numérico que representa aquele rosto.
 * Dois embeddings de fotos da mesma pessoa ficam matematicamente "próximos";
 * de pessoas diferentes, ficam "distantes". É essa distância que usamos pra reconhecer.
 *
 * Modelo: baixar de
 * https://raw.githubusercontent.com/shubham0204/FaceRecognition_With_FaceNet_Android/master/app/src/main/assets/facenet_512_int_quantized.tflite
 * e colocar em app/src/main/assets/facenet_512_int_quantized.tflite
 */
class FaceNetEmbedder(context: Context) {

    companion object {
        const val TAMANHO_ENTRADA = 160   // o modelo espera imagens 160x160
        const val DIMENSAO_EMBEDDING = 512
    }

    private val interpreter: Interpreter

    init {
        val opcoes = Interpreter.Options().apply { setNumThreads(4) }
        val modelo = FileUtil.loadMappedFile(context, "facenet_512_int_quantized.tflite")
        interpreter = Interpreter(modelo, opcoes)
    }

    fun gerarEmbedding(rosto: Bitmap): FloatArray {
        val entrada = prepararEntrada(rosto)
        val saida = Array(1) { FloatArray(DIMENSAO_EMBEDDING) }
        interpreter.run(entrada, saida)
        return normalizarL2(saida[0])
    }

    private fun prepararEntrada(rosto: Bitmap): ByteBuffer {
        val redimensionado = Bitmap.createScaledBitmap(rosto, TAMANHO_ENTRADA, TAMANHO_ENTRADA, true)
        val buffer = ByteBuffer.allocateDirect(4 * TAMANHO_ENTRADA * TAMANHO_ENTRADA * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(TAMANHO_ENTRADA * TAMANHO_ENTRADA)
        redimensionado.getPixels(pixels, 0, TAMANHO_ENTRADA, 0, 0, TAMANHO_ENTRADA, TAMANHO_ENTRADA)

        for (pixel in pixels) {
            // Normaliza cada canal de [0,255] pra [-1,1], que é o que o FaceNet espera
            buffer.putFloat(((pixel shr 16 and 0xFF) - 127.5f) / 127.5f) // R
            buffer.putFloat(((pixel shr 8 and 0xFF) - 127.5f) / 127.5f)  // G
            buffer.putFloat(((pixel and 0xFF) - 127.5f) / 127.5f)        // B
        }
        return buffer
    }

    private fun normalizarL2(vetor: FloatArray): FloatArray {
        var soma = 0f
        for (v in vetor) soma += v * v
        val norma = kotlin.math.sqrt(soma)
        return FloatArray(vetor.size) { i -> vetor[i] / norma }
    }

    fun fechar() {
        interpreter.close()
    }
}
