package PlateRecognitionApp.plakass.utils

import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

// Versión SIN extensiones de Companion (compatible con Retrofit 2.11.0)
object RequestBodyUtils {

    fun createTextRequestBody(text: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), text)
    }

    fun createJsonRequestBody(json: String): RequestBody {
        return RequestBody.create(MediaType.parse("application/json"), json)
    }

    fun createImageRequestBody(file: File): RequestBody {
        return RequestBody.create(MediaType.parse("image/*"), file)
    }

    fun createJpegRequestBody(file: File): RequestBody {
        return RequestBody.create(MediaType.parse("image/jpeg"), file)
    }

    fun createPngRequestBody(file: File): RequestBody {
        return RequestBody.create(MediaType.parse("image/png"), file)
    }

    fun createMultipartPart(partName: String, file: File): MultipartBody.Part {
        return MultipartBody.Part.createFormData(
            partName,
            file.name,
            createImageRequestBody(file)
        )
    }

    fun createMultipartPart(partName: String, file: File, contentType: String): MultipartBody.Part {
        val requestFile = RequestBody.create(MediaType.parse(contentType), file)
        return MultipartBody.Part.createFormData(partName, file.name, requestFile)
    }
}