package com.autoguard.vpn.di

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

private val MEDIA_TYPE = "text/plain".toMediaTypeOrNull()

/**
 * String Converter Factory
 * Used for handling non-JSON responses (like CSV text)
 */
class StringConverterFactory : Converter.Factory() {

    /**
     * Handles response body conversion to String
     */
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return if (type == String::class.java) {
            StringResponseConverter()
        } else {
            super.responseBodyConverter(type, annotations, retrofit)
        }
    }

    /**
     * Handles RequestBody conversion
     */
    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        return if (type == String::class.java) {
            StringRequestConverter()
        } else {
            super.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
        }
    }
}

/**
 * String Response Converter
 */
private class StringResponseConverter : Converter<ResponseBody, String> {
    override fun convert(value: ResponseBody): String {
        return value.string()
    }
}

/**
 * String Request Converter
 */
private class StringRequestConverter : Converter<String, RequestBody> {
    override fun convert(value: String): RequestBody {
        return value.toRequestBody(MEDIA_TYPE)
    }
}
