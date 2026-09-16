package com.jonathan.reconhecimentofacial

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.jonathan.reconhecimentofacial.db.AppDatabase
import com.jonathan.reconhecimentofacial.db.PessoaEntity
import kotlinx.coroutines.launch

class CadastroActivity : ComponentActivity() {

    private lateinit var embedder: FaceNetEmbedder
    private lateinit var db: AppDatabase
    private lateinit var imageView: ImageView
    private lateinit var campoNome: EditText
    private var fotoAtual: Bitmap? = null

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
    )

    private val tirarFoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            fotoAtual = bitmap
            imageView.setImageBitmap(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        embedder = FaceNetEmbedder(this)
        db = AppDatabase.obter(this)

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        imageView = ImageView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 800
            )
        }
        campoNome = EditText(this).apply { hint = "Nome da pessoa" }
        val botaoTirarFoto = android.widget.Button(this).apply { text = "Tirar foto" }
        val botaoSalvar = android.widget.Button(this).apply { text = "Salvar cadastro" }

        botaoTirarFoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 200)
            } else {
                tirarFoto.launch(null)
            }
        }

        botaoSalvar.setOnClickListener { salvarCadastro() }

        layout.addView(imageView)
        layout.addView(campoNome)
        layout.addView(botaoTirarFoto)
        layout.addView(botaoSalvar)
        setContentView(layout)
    }

    private fun salvarCadastro() {
        val nome = campoNome.text.toString().trim()
        val foto = fotoAtual

        if (nome.isEmpty()) {
            Toast.makeText(this, "Digite o nome da pessoa", Toast.LENGTH_SHORT).show()
            return
        }
        if (foto == null) {
            Toast.makeText(this, "Tire uma foto primeiro", Toast.LENGTH_SHORT).show()
            return
        }

        val inputImage = InputImage.fromBitmap(foto, 0)
        detector.process(inputImage)
            .addOnSuccessListener { rostos ->
                if (rostos.isEmpty()) {
                    Toast.makeText(this, "Nenhum rosto encontrado na foto, tente de novo", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val caixa = rostos[0].boundingBox
                val x = caixa.left.coerceAtLeast(0)
                val y = caixa.top.coerceAtLeast(0)
                val largura = caixa.width().coerceAtMost(foto.width - x)
                val altura = caixa.height().coerceAtMost(foto.height - y)

                if (largura <= 0 || altura <= 0) {
                    Toast.makeText(this, "Não consegui recortar o rosto, tente outra foto", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val rostoRecortado = Bitmap.createBitmap(foto, x, y, largura, altura)

                lifecycleScope.launch {
                    val embedding = embedder.gerarEmbedding(rostoRecortado)
                    db.pessoaDao().inserir(PessoaEntity(nome = nome, embedding = embedding))
                    Toast.makeText(this@CadastroActivity, "$nome cadastrado(a)!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        embedder.fechar()
    }
}
