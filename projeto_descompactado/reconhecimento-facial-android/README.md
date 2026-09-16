# Reconhecimento Facial — 100% Android

Tudo roda no celular: câmera, IA (detecção + reconhecimento) e banco de dados.
Sem Docker, sem Python, sem servidor.

## Como funciona

1. **ML Kit** (do Google, embutido no Android) detecta onde está o rosto na imagem.
2. **FaceNet** (modelo `.tflite`, roda local) transforma o rosto recortado num vetor de
   128 números (o "embedding") — rostos parecidos geram vetores matematicamente próximos.
3. **Room** (SQLite local) guarda os embeddings cadastrados e o histórico de eventos —
   substitui o PostgreSQL da versão anterior.

## Antes de abrir no Android Studio

Baixe o modelo FaceNet e coloque em `app/src/main/assets/facenet_512_int_quantized.tflite`:

```
https://raw.githubusercontent.com/shubham0204/FaceRecognition_With_FaceNet_Android/master/app/src/main/assets/facenet_512_int_quantized.tflite
```

Esse é um modelo de 512 dimensões (mais preciso que a versão de 128, ao custo de um
arquivo um pouco maior). O nome "int_quantized" é só sobre como os pesos internos do
modelo foram compactados — a entrada e a saída continuam sendo números float normais,
então o código deste projeto funciona sem nenhum ajuste extra.

**Nota**: cheguei a recomendar um link de outro repositório do mesmo autor
(`OnDevice-Face-Recognition-Android`) antes, mas esse projeto migrou recentemente pra um
formato diferente (ExecuTorch, `.pte`) que não é compatível com o código daqui. Esse link
acima é do repositório mais simples e estável, que ainda usa o formato `.tflite` clássico.

## Estrutura

```
app/src/main/java/com/jonathan/reconhecimentofacial/
├── MainActivity.kt        # tela principal: câmera ao vivo + reconhecimento em tempo real
├── CadastroActivity.kt     # tela de cadastro: tira foto, salva nome + embedding
├── FaceNetEmbedder.kt      # carrega o modelo e gera os embeddings
├── FaceMatcher.kt          # compara um embedding novo com o cadastro
├── ImageUtils.kt           # conversões de imagem da câmera
└── db/
    ├── AppDatabase.kt      # Room (SQLite)
    ├── PessoaEntity.kt     # pessoa cadastrada (nome + embedding)
    ├── EventoEntity.kt     # cada reconhecimento/evento
    ├── PessoaDao.kt
    ├── EventoDao.kt
    └── Converters.kt       # salva o FloatArray do embedding como bytes
```

## Como testar

1. Abra a pasta `reconhecimento-facial-android/` no Android Studio (File → Open).
2. Deixe o Gradle sincronizar (baixa as dependências automaticamente).
3. Confirme que `facenet.tflite` está em `app/src/main/assets/`.
4. Rode no seu celular (via USB debugging) ou num emulador com câmera.
5. Toque em "Cadastrar nova pessoa", tire uma foto sua, dê seu nome, salve.
6. Volte pra tela principal — a câmera frontal já deve te reconhecer e mostrar seu nome.

## Calibrando o reconhecimento

Em `FaceMatcher.kt`, a constante `LIMIAR_RECONHECIMENTO` (atualmente `0.9`) decide o
quão parecido um rosto precisa ser pra contar como reconhecido. Se estiver reconhecendo
gente errada, diminua esse número; se estiver rejeitando a pessoa certa com frequência,
aumente um pouco. Teste com fotos reais em condições parecidas com o uso real
(iluminação, ângulo) antes de fechar esse valor.

## Limitações desta versão

- Cadastro com 1 foto só por pessoa — múltiplas fotos por pessoa (ângulos/iluminação
  diferentes) deixam o reconhecimento mais robusto, é uma melhoria natural pra depois.
- Sem sincronização entre celulares — os dados ficam só no aparelho que cadastrou.
- Sem autenticação/proteção de acesso ao cadastro.
