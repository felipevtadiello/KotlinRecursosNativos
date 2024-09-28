package com.example.recursosnativos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var dbHelper: DatabaseHelper
    private var photoFilePath: String = ""
    private var capturedImageUri by mutableStateOf<Uri?>(null)

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private val openCameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Imagem capturada com sucesso!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Captura de imagem cancelada ou falhou.", Toast.LENGTH_SHORT).show()
            capturedImageUri = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = DatabaseHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            HomeScreen()
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(this, "Erro ao criar arquivo de imagem", Toast.LENGTH_SHORT).show()
            null
        }
        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${BuildConfig.APPLICATION_ID}.provider",
                it
            )
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            capturedImageUri = photoURI
            openCameraLauncher.launch(cameraIntent)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", ".jpg", storageDir
        ).apply {
            photoFilePath = absolutePath
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationAndShow(
        context: Context,
        onLocationReceived: (String, String) -> Unit
    ) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val lat = it.latitude.toString()
                val lon = it.longitude.toString()
                onLocationReceived(lat, lon)
                Toast.makeText(context, "Localização obtida: $lat, $lon", Toast.LENGTH_LONG).show()
            } ?: run {
                Toast.makeText(
                    context,
                    "Não deu para obter a localização",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Erro ao tentar obter a localização", Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    fun HomeScreen() {
        MaterialTheme {
            val context = LocalContext.current
            var name by remember { mutableStateOf("") }
            var email by remember { mutableStateOf("") }
            var comment by remember { mutableStateOf("") }
            var latitude by remember { mutableStateOf<String?>(null) }
            var longitude by remember { mutableStateOf<String?>(null) }
            var formDataList by remember { mutableStateOf(listOf<FormData>()) }

            LaunchedEffect(Unit) {
                formDataList = dbHelper.getAllFormData()
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                if (permissions[Manifest.permission.CAMERA] == true) {
                    openCamera()
                } else {
                    Toast.makeText(context, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
                }
            }

            val locationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    getLocationAndShow(context) { lat, lon ->
                        latitude = lat
                        longitude = lon
                    }
                } else {
                    Toast.makeText(context, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Formulário") }
                    )
                },
                content = { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nome") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            label = { Text("Comentário") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    openCamera()
                                } else {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Capturar imagem")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (capturedImageUri != null) {
                                    val formData = FormData(
                                        name = name,
                                        email = email,
                                        comment = comment,
                                        imagePath = capturedImageUri.toString()
                                    )
                                    val rowId = dbHelper.insertFormData(formData)
                                    if (rowId != -1L) {
                                        Toast.makeText(
                                            context,
                                            "Dados salvos no banco de dados",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        formDataList = dbHelper.getAllFormData()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Erro ao salvar os dados",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Tire uma foto primeiro",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Salvar no Banco de dados")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    getLocationAndShow(context) { lat, lon ->
                                        latitude = lat
                                        longitude = lon
                                    }
                                } else {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Obter Localização")
                        }

                        latitude?.let { Text("Latitude: $it", fontWeight = FontWeight.Bold) }
                        longitude?.let { Text("Longitude: $it", fontWeight = FontWeight.Bold) }

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn {
                            items(formDataList) { formData ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    elevation = 4.dp
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "Nome: ${formData.name}",
                                            style = MaterialTheme.typography.h6
                                        )
                                        Text(
                                            "Email: ${formData.email}",
                                            style = MaterialTheme.typography.body1
                                        )
                                        Text(
                                            "Comentário: ${formData.comment}",
                                            style = MaterialTheme.typography.body1
                                        )
                                        formData.imagePath?.let { path ->
                                            val imageUri = Uri.parse(path)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Image(
                                                painter = rememberAsyncImagePainter(model = imageUri),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}
