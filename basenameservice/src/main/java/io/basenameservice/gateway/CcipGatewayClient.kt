package io.basenameservice.gateway

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.basenameservice.models.GatewayResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint64
import org.web3j.utils.Numeric
import java.io.IOException

/**
 * Client for making CCIP-Read requests to the offchain gateway
 */
class CcipGatewayClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    /**
     * Performs a CCIP-Read gateway request
     * 
     * The gateway expects a POST request with JSON body:
     * {
     *   "data": "0x..." (hex-encoded callData),
     *   "sender": "0x..." (contract address)
     * }
     * 
     * And returns:
     * {
     *   "data": "0x..." (hex-encoded response)
     * }
     * 
     * @param gatewayUrl The URL of the CCIP gateway (from OffchainLookup)
     * @param sender The sender address (from OffchainLookup)
     * @param callData The callData to send (from OffchainLookup)
     * @return The raw response bytes from the gateway
     */
    suspend fun query(
        gatewayUrl: String,
        sender: String,
        callData: ByteArray
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            // Format the URL - replace {sender} and {data} placeholders if present
            val callDataHex = Numeric.toHexString(callData)
            var url = gatewayUrl
                .replace("{sender}", sender.lowercase())
                .replace("{data}", callDataHex.lowercase())
            
            // Determine request method based on URL format
            val request = if (url.contains("{sender}") || url.contains("{data}")) {
                // URL still has placeholders, use POST with JSON body
                val jsonBody = """
                    {
                        "data": "$callDataHex",
                        "sender": "$sender"
                    }
                """.trimIndent()
                
                Request.Builder()
                    .url(url)
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()
            } else {
                // URL has no placeholders (they were replaced), use GET
                Request.Builder()
                    .url(url)
                    .get()
                    .build()
            }
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Gateway request failed with code ${response.code}: ${response.message}")
                )
            }
            
            val responseBody = response.body?.string() 
                ?: return@withContext Result.failure(IOException("Empty response body"))
            
            // Parse the JSON response
            val gatewayResponse = parseGatewayResponse(responseBody)
                ?: return@withContext Result.failure(IOException("Failed to parse gateway response"))
            
            Result.success(Numeric.hexStringToByteArray(gatewayResponse))
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Parses the gateway JSON response to extract the data field
     * Expected format: {"data": "0x..."}
     */
    private fun parseGatewayResponse(json: String): String? {
        return try {
            val adapter: JsonAdapter<Map<String, Any>> = moshi.adapter(Map::class.java) as JsonAdapter<Map<String, Any>>
            val parsed = adapter.fromJson(json)
            parsed?.get("data") as? String
        } catch (e: Exception) {
            // If JSON parsing fails, check if the response is already hex data
            if (json.trim().startsWith("0x")) {
                json.trim()
            } else {
                null
            }
        }
    }
    
    /**
     * Decodes the gateway response into its components
     * 
     * The response is ABI-encoded as: abi.encode(bytes result, uint64 expires, bytes sig)
     * 
     * @param encodedResponse The raw response bytes from the gateway
     * @return The decoded GatewayResponse
     */
    fun decodeGatewayResponse(encodedResponse: ByteArray): GatewayResponse? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val types = mutableListOf(
                object : TypeReference<DynamicBytes>() {},  // result
                object : TypeReference<Uint64>() {},         // expires
                object : TypeReference<DynamicBytes>() {}    // signature
            ) as MutableList<TypeReference<Type<*>>>
            
            val decoded = FunctionReturnDecoder.decode(
                Numeric.toHexString(encodedResponse),
                types
            )
            
            if (decoded.size != 3) {
                return null
            }
            
            val result = (decoded[0] as DynamicBytes).value
            val expires = (decoded[1] as Uint64).value.toLong()
            val signature = (decoded[2] as DynamicBytes).value
            
            GatewayResponse(
                result = result,
                expires = expires,
                signature = signature
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

