package io.basenameservice.models

/**
 * CCIP-Read (ERC-3668) data models
 */

/**
 * Represents the OffchainLookup error data from the L1Resolver
 * This is thrown when a wildcard resolution is requested (e.g., jesse.base.eth)
 * 
 * error OffchainLookup(
 *     address sender,
 *     string[] urls,
 *     bytes callData,
 *     bytes4 callbackFunction,
 *     bytes extraData
 * )
 */
data class OffchainLookup(
    val sender: String,
    val urls: List<String>,
    val callData: ByteArray,
    val callbackFunction: ByteArray,
    val extraData: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OffchainLookup

        if (sender != other.sender) return false
        if (urls != other.urls) return false
        if (!callData.contentEquals(other.callData)) return false
        if (!callbackFunction.contentEquals(other.callbackFunction)) return false
        if (!extraData.contentEquals(other.extraData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sender.hashCode()
        result = 31 * result + urls.hashCode()
        result = 31 * result + callData.contentHashCode()
        result = 31 * result + callbackFunction.contentHashCode()
        result = 31 * result + extraData.contentHashCode()
        return result
    }
}

/**
 * Response from the CCIP gateway
 * 
 * The response is encoded as: abi.encode(bytes memory result, uint64 expires, bytes memory sig)
 */
data class GatewayResponse(
    val result: ByteArray,
    val expires: Long,
    val signature: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GatewayResponse

        if (!result.contentEquals(other.result)) return false
        if (expires != other.expires) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result1 = result.contentHashCode()
        result1 = 31 * result1 + expires.hashCode()
        result1 = 31 * result1 + signature.contentHashCode()
        return result1
    }
}

/**
 * Result of a name resolution
 */
data class ResolutionResult(
    val name: String,
    val address: String?,
    val error: String? = null
)

