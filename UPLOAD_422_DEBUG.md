# Image Upload 422 Error Analysis

## Potential Issues Causing 422 Status Code

### 1. **Missing Authentication Headers**
The upload endpoint likely requires authentication, but the current implementation doesn't add auth headers to multipart requests.

### 2. **Incorrect Media Type Handling**
```kotlin
"image/*".toMediaType()  // This might be too generic
```
Should use specific media types like "image/jpeg", "image/png", etc.

### 3. **File Name Issues**
```kotlin
file.name  // Temporary files have generated names like "temp_image_123456789.jpg"
```
The server might expect original file names or specific naming conventions.

### 4. **Missing Request Headers**
Multipart requests often need specific headers like Content-Type boundaries.

### 5. **File Size or Format Validation**
The server might have restrictions on file size, format, or dimensions.

## Recommended Fixes

### Fix 1: Add Authentication to Upload
```kotlin
@Multipart
@POST("upload/image")
suspend fun uploadImage(
    @Header("Authorization") authorization: String,
    @Part image: MultipartBody.Part
): Response<ImageUploadResponse>
```

### Fix 2: Improve Media Type Detection
```kotlin
private fun createMultipartBody(file: File): okhttp3.MultipartBody.Part {
    val mimeType = getMimeType(file) ?: "image/jpeg"
    val requestFile = okhttp3.RequestBody.create(
        mimeType.toMediaType(),
        file
    )
    return okhttp3.MultipartBody.Part.createFormData("image", file.name, requestFile)
}

private fun getMimeType(file: File): String? {
    return when (file.extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        else -> null
    }
}
```

### Fix 3: Better File Name Handling
```kotlin
private fun createMultipartBody(file: File, originalFileName: String? = null): okhttp3.MultipartBody.Part {
    val fileName = originalFileName ?: "upload_${System.currentTimeMillis()}.jpg"
    val mimeType = getMimeType(file) ?: "image/jpeg"
    val requestFile = okhttp3.RequestBody.create(
        mimeType.toMediaType(),
        file
    )
    return okhttp3.MultipartBody.Part.createFormData("image", fileName, requestFile)
}
```

### Fix 4: Add Logging for Debugging
```kotlin
suspend fun uploadImage(imageFile: File): Result<String> {
    refreshAuthToken()
    return try {
        val request = createMultipartBody(imageFile)
        Log.d("Upload", "Uploading file: ${imageFile.name}, size: ${imageFile.length()}")
        Log.d("Upload", "Auth token: ${cachedToken?.take(20)}...")
        
        val response = chatApi.uploadImage(request)
        
        Log.d("Upload", "Response code: ${response.code()}")
        Log.d("Upload", "Response body: ${response.body()}")
        if (!response.isSuccessful) {
            Log.e("Upload", "Error body: ${response.errorBody()?.string()}")
        }
        
        if (response.isSuccessful) {
            response.body()?.let { Result.success(it.imageUrl) }
                ?: Result.failure(Exception("Empty response body"))
        } else {
            Result.failure(Exception("Failed to upload image: ${response.code()} - ${response.errorBody()?.string()}"))
        }
    } catch (e: Exception) {
        Log.e("Upload", "Upload exception", e)
        Result.failure(e)
    }
}
```

## Immediate Actions to Debug

1. **Check server logs** for the specific validation error
2. **Add logging** to see the exact request being sent
3. **Test with a known good image** (small JPEG)
4. **Verify authentication** is working for other endpoints
5. **Check file size** and format requirements

## Most Likely Cause

The **missing authentication header** on the upload endpoint is the most likely cause of the 422 error, as multipart requests often require explicit auth header handling unlike regular JSON requests.
