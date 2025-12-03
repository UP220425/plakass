package PlateRecognitionApp.plakass.data.api

import PlateRecognitionApp.plakass.data.model.BasicResponse
import PlateRecognitionApp.plakass.data.model.LoginResponse
import PlateRecognitionApp.plakass.data.model.RegisterResponse
import PlateRecognitionApp.plakass.data.model.UpdateUserResponse
import PlateRecognitionApp.plakass.data.model.UserDataResponse
import PlateRecognitionApp.plakass.data.model.VehiclesResponse
import PlateRecognitionApp.plakass.data.model.ParkingHistoryResponse
import PlateRecognitionApp.plakass.data.model.PendingVehicleResponse
import PlateRecognitionApp.plakass.data.model.EntryQRResponse
import PlateRecognitionApp.plakass.data.model.ParkingStatusResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    // ------------------------
    // AUTH
    // ------------------------

    @POST("register")
    suspend fun register(@Body body: HashMap<String, Any>): Response<RegisterResponse>

    @POST("login")
    suspend fun login(@Body body: HashMap<String, Any>): Response<LoginResponse>

    // Método original para actualizar sin imagen
    @PUT("user/update")
    suspend fun updateUser(@Body body: HashMap<String, Any>): Response<UpdateUserResponse>

    // Método para actualizar CON imagen (multipart/form-data)
    @Multipart
    @PUT("user/update")
    suspend fun updateUserWithImage(
        @Part("name") name: RequestBody,
        @Part("email") email: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part profile_picture: MultipartBody.Part  // NO usar = null aquí
    ): Response<UpdateUserResponse>

    // OPCIÓN B: Si quieres que la imagen sea opcional, usa RequestBody en lugar de MultipartBody.Part
    @Multipart
    @PUT("user/update")
    suspend fun updateUserWithOptionalImage(
        @Part("name") name: RequestBody,
        @Part("email") email: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part("profile_picture") profilePicture: RequestBody? = null  // Esta es otra opción
    ): Response<UpdateUserResponse>

    @POST("change-password")
    suspend fun changePassword(@Body body: HashMap<String, Any>): Response<BasicResponse>

    @GET("user/me")
    suspend fun getUser(): Response<UserDataResponse>

    // ------------------------
    // VEHICLES
    // ------------------------

    @POST("vehicles/add")
    suspend fun addVehicle(@Body body: HashMap<String, Any>): Response<BasicResponse>

    @GET("vehicles/my")
    suspend fun getMyVehicles(): Response<VehiclesResponse>

    @DELETE("vehicles/delete/{id}")
    suspend fun deleteVehicle(@Path("id") id: String): Response<BasicResponse>

    @GET("parking/history/all")
    suspend fun getHistoryAll(): Response<ParkingHistoryResponse>

    @GET("parking/history/week")
    suspend fun getHistoryWeek(): Response<ParkingHistoryResponse>

    @GET("parking/history/month")
    suspend fun getHistoryMonth(): Response<ParkingHistoryResponse>

    @GET("parking/entry-qr")
    suspend fun getEntryQR(): Response<EntryQRResponse>

    @GET("parking/pending")
    suspend fun getPendingNoApp(): Response<PendingVehicleResponse>

    @POST("parking/pay/noapp")
    suspend fun payNoApp(@Body body: HashMap<String, Any>): Response<BasicResponse>

    @POST("parking/scan")
    suspend fun registerScan(@Body body: HashMap<String, Any>): Response<BasicResponse>

    @POST("parking/pay")
    suspend fun payParking(@Body body: HashMap<String, Any>): Response<BasicResponse>

    @GET("parking/status")
    suspend fun parkingStatus(): Response<ParkingStatusResponse>
}