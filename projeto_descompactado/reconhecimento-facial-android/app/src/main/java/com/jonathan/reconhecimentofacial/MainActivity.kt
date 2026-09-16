package com.jonathan.reconhecimentofacial

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.jonathan.reconhecimentofacial.db.AppDatabase
import com.jonathan.reconhecimentofacial.db.EventoEntity
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var textoStatus: TextView
    private lateinit var embedder: FaceNetEmbedder
    private lateinit var db: AppDatabase

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var ultimoProcessamento = 0L
    private val intervaloMinimoMs = 1000L  // não processa mais que 1x por segundo (poupa bateria/CPU)

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Layout simples, sem XML: preview da câmera + texto de status + botão de cadastro
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }
        previewView = PreviewView(this)
        textoStatus = TextView(this).apply { text = "Iniciando…"; textSize = 16f; setPadding(24, 24, 24, 24) }
        val botaoCadastro = android.widget.Button(this).apply { text = "Cadastrar nova pessoa" }
        botaoCadastro.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }

        layout.addView(previewView, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        layout.addView(textoStatus)
        layout.addView(botaoCadastro)
        setContentView(layout)

        embedder = FaceNetEmbedder(this)
        db = AppDatabase.obter(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            iniciarCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            iniciarCamera()
        } else {
            Toast.makeText(this, "Preciso da permissão de câmera pra funcionar", Toast.LENGTH_LONG).show()
        }
    }

    private fun iniciarCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(cameraExecutor) { imageProxy -> processarFrame(imageProxy) }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis
            )
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processarFrame(imageProxy: ImageProxy) {
        val agora = System.currentTimeMillis()
        if (agora - ultimoProcessamento < intervaloMinimoMs) {
            imageProxy.close()
            return
        }
        ultimoProcessamento = agora

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { rostos ->
                if (rostos.isNotEmpty()) {
                    val bitmap = imageProxyParaBitmap(imageProxy)
                    val rosto = rostos[0]
                    val caixa = rosto.boundingBox
                    val rostoRecortado = recortarComSeguranca(bitmap, caixa)

                    if (rostoRecortado != null) {
                        lifecycleScope.launch {
                            val embedding = embedder.gerarEmbedding(rostoRecortado)
                            val cadastro = db.pessoaDao().listarTodas()
                            val resultado = FaceMatcher.comparar(embedding, cadastro)

                            db.eventoDao().inserir(
                                EventoEntity(
                                    pessoaId = resultado.pessoaId,
                                    nomePessoa = resultado.nome,
                                    distancia = resultado.distancia,
                                    reconhecido = resultado.reconhecido
                                )
                            )

                            textoStatus.text = if (resultado.reconhecido) {
                                "Reconhecido: ${resultado.nome} (dist. ${"%.2f".format(resultado.distancia)})"
                            } else {
                                "Desconhecido (dist. ${"%.2f".format(resultado.distancia)})"
                            }
                        }
                    }
                } else {
                    textoStatus.text = "Nenhum rosto detectado"
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun recortarComSeguranca(bitmap: Bitmap, caixa: android.graphics.Rect): Bitmap? {
        val x = caixa.left.coerceAtLeast(0)
        val y = caixa.top.coerceAtLeast(0)
        val largura = (caixa.width()).coerceAtMost(bitmap.width - x)
        val altura = (caixa.height()).coerceAtMost(bitmap.height - y)
        if (largura <= 0 || altura <= 0) return null
        return Bitmap.createBitmap(bitmap, x, y, largura, altura)
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun imageProxyParaBitmap(imageProxy: ImageProxy): Bitmap {
        val bitmap = ImageUtils.imageProxyParaBitmap(imageProxy)
        val matrix = Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        embedder.fechar()
        cameraExecutor.shutdown()
    }
}
