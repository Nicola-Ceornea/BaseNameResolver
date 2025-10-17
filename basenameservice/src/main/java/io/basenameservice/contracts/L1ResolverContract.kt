package io.basenameservice.contracts

import io.basenameservice.models.OffchainLookup
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.*
import org.web3j.abi.datatypes.generated.Bytes4
import org.web3j.abi.datatypes.generated.Uint64
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthCall
import org.web3j.utils.Numeric

/**
 * Exception thrown when a contract call reverts
 */
class ContractRevertException(val revertData: String) : Exception("Contract reverted: $revertData")

/**
 * L1Resolver contract interface for Base names
 * 
 * This class handles interactions with the L1Resolver contract deployed on Ethereum mainnet
 * at address 0xde9049636F4a1dfE0a64d1bFe3155C0A14C54F31
 */
class L1ResolverContract(
    private val web3j: Web3j,
    private val contractAddress: String = DEFAULT_L1_RESOLVER_ADDRESS
) {
    
    companion object {
        // L1Resolver contract address on Ethereum mainnet
        const val DEFAULT_L1_RESOLVER_ADDRESS = "0xde9049636F4a1dfE0a64d1bFe3155C0A14C54F31"
        
        // Function selectors
        const val RESOLVE_FUNCTION = "resolve"
        const val RESOLVE_WITH_PROOF_FUNCTION = "resolveWithProof"
        
        // Error selectors
        const val OFFCHAIN_LOOKUP_ERROR_SIGNATURE = "0x556f1830" // OffchainLookup error selector
        
        // Standard ENS resolver function selectors
        const val ADDR_FUNCTION_SELECTOR = "0x3b3b57de" // addr(bytes32)
    }
    
    /**
     * Calls the resolve function on the L1Resolver contract
     * 
     * This will typically revert with an OffchainLookup error for wildcard names
     * 
     * @param dnsEncodedName The DNS-encoded name
     * @param data The ABI encoded data for the resolution function (e.g., addr(bytes32))
     * @return The result bytes or null if an OffchainLookup is needed
     */
    suspend fun resolve(dnsEncodedName: ByteArray, data: ByteArray): Result<ByteArray> {
        try {
            val function = Function(
                RESOLVE_FUNCTION,
                listOf(
                    DynamicBytes(dnsEncodedName),
                    DynamicBytes(data)
                ),
                listOf(object : TypeReference<DynamicBytes>() {})
            )
            
            val encodedFunction = FunctionEncoder.encode(function)
            val transaction = Transaction.createEthCallTransaction(
                null,
                contractAddress,
                encodedFunction
            )
            
            val ethCall: EthCall = web3j.ethCall(
                transaction,
                DefaultBlockParameterName.LATEST
            ).sendAsync().get()
            
            // Debug: Print all available fields
            println("DEBUG: EthCall response details:")
            println("  hasError: ${ethCall.hasError()}")
            println("  isReverted: ${ethCall.isReverted}")
            println("  value: ${ethCall.value?.take(100)}")
            println("  error: ${ethCall.error}")
            println("  error.code: ${ethCall.error?.code}")
            println("  error.message: ${ethCall.error?.message}")
            println("  error.data: ${ethCall.error?.data?.take(100)}")
            println("  revertReason: ${ethCall.revertReason?.take(100)}")
            println("  result: ${ethCall.result?.take(100)}")
            
            // Check if the call reverted or had an error
            if (ethCall.hasError() || ethCall.isReverted) {
                // For contract reverts, the actual revert data is often in the result/value field
                // Try multiple sources to get the revert data
                var revertData: String? = null
                
                // Try getting from error data first
                if (ethCall.error?.data != null) {
                    val errorData = ethCall.error.data.toString()
                        .trim()
                        .removePrefix("\"")
                        .removeSuffix("\"")
                    
                    println("DEBUG: Raw error.data: '${ethCall.error.data}'")
                    println("DEBUG: Cleaned error.data: '${errorData.take(66)}'")
                    
                    if (errorData.startsWith("0x")) {
                        revertData = errorData
                        println("DEBUG: Got revert data from error.data!")
                    }
                }
                
                // If not found, try the revert reason
                if (revertData == null && ethCall.revertReason != null) {
                    val cleanReason = ethCall.revertReason.toString()
                        .trim()
                        .removePrefix("\"")
                        .removeSuffix("\"")
                    
                    if (cleanReason.startsWith("0x")) {
                        revertData = cleanReason
                        println("DEBUG: Got revert data from revertReason: ${revertData.take(66)}")
                    }
                }
                
                // If still not found, try the value field (Web3j sometimes puts it here)
                if (revertData == null && ethCall.value != null) {
                    val cleanValue = ethCall.value.toString()
                        .trim()
                        .removePrefix("\"")
                        .removeSuffix("\"")
                    
                    if (cleanValue.startsWith("0x") && cleanValue != "0x") {
                        revertData = cleanValue
                        println("DEBUG: Got revert data from value: ${revertData.take(66)}")
                    }
                }
                
                // If we still don't have hex data, use the error message
                if (revertData == null || !revertData.startsWith("0x")) {
                    revertData = ethCall.error?.message ?: "execution reverted"
                    println("DEBUG: Using error message as fallback: $revertData")
                } else {
                    println("DEBUG: Successfully extracted revert data: ${revertData.take(66)}...")
                }
                
                return Result.failure(ContractRevertException(revertData))
            }
            
            // Success case - decode the result
            val result = FunctionReturnDecoder.decode(
                ethCall.value,
                function.outputParameters
            )
            return if (result.isNotEmpty()) {
                val bytes = result[0] as DynamicBytes
                Result.success(bytes.value)
            } else {
                Result.failure(Exception("No result returned"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * Parses an OffchainLookup error from the revert data
     * 
     * The OffchainLookup error has the following structure:
     * error OffchainLookup(address sender, string[] urls, bytes callData, bytes4 callbackFunction, bytes extraData)
     * 
     * @param revertData The revert data from the failed transaction
     * @return The parsed OffchainLookup object
     */
    fun parseOffchainLookup(revertData: String): OffchainLookup? {
        try {
            println("DEBUG: Parsing OffchainLookup from revert data: ${revertData.take(100)}...")
            
            // Remove "0x" prefix and error selector (first 4 bytes)
            val cleanData = Numeric.cleanHexPrefix(revertData)
            if (cleanData.length < 8) {
                println("DEBUG: Revert data too short: ${cleanData.length}")
                return null
            }
            
            val errorSelector = "0x" + cleanData.substring(0, 8)
            println("DEBUG: Error selector: $errorSelector (expected: $OFFCHAIN_LOOKUP_ERROR_SIGNATURE)")
            
            if (errorSelector != OFFCHAIN_LOOKUP_ERROR_SIGNATURE) {
                println("DEBUG: Error selector mismatch!")
                return null
            }
            
            // Decode the parameters
            val encodedParams = "0x" + cleanData.substring(8)
            
            @Suppress("UNCHECKED_CAST")
            val types = mutableListOf(
                object : TypeReference<Address>() {},
                object : TypeReference<DynamicArray<Utf8String>>() {},
                object : TypeReference<DynamicBytes>() {},
                object : TypeReference<Bytes4>() {},
                object : TypeReference<DynamicBytes>() {}
            ) as MutableList<TypeReference<Type<*>>>
            
            val decoded = FunctionReturnDecoder.decode(encodedParams, types)
            
            if (decoded.size != 5) {
                return null
            }
            
            val sender = (decoded[0] as Address).value
            val urls = (decoded[1] as DynamicArray<*>).value.map { (it as Utf8String).value }
            val callData = (decoded[2] as DynamicBytes).value
            val callbackFunction = (decoded[3] as Bytes4).value
            val extraData = (decoded[4] as DynamicBytes).value
            
            return OffchainLookup(
                sender = sender,
                urls = urls,
                callData = callData,
                callbackFunction = callbackFunction,
                extraData = extraData
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Calls the resolveWithProof function to verify and get the final result
     * 
     * @param response The response from the gateway
     * @param extraData The extraData from the OffchainLookup
     * @return The resolved data
     */
    suspend fun resolveWithProof(response: ByteArray, extraData: ByteArray): Result<ByteArray> {
        try {
            val function = Function(
                RESOLVE_WITH_PROOF_FUNCTION,
                listOf(
                    DynamicBytes(response),
                    DynamicBytes(extraData)
                ),
                listOf(object : TypeReference<DynamicBytes>() {})
            )
            
            val encodedFunction = FunctionEncoder.encode(function)
            val transaction = Transaction.createEthCallTransaction(
                null,
                contractAddress,
                encodedFunction
            )
            
            val ethCall: EthCall = web3j.ethCall(
                transaction,
                DefaultBlockParameterName.LATEST
            ).sendAsync().get()
            
            return if (ethCall.hasError()) {
                Result.failure(Exception(ethCall.error.message))
            } else {
                val result = FunctionReturnDecoder.decode(
                    ethCall.value,
                    function.outputParameters
                )
                if (result.isNotEmpty()) {
                    val bytes = result[0] as DynamicBytes
                    Result.success(bytes.value)
                } else {
                    Result.failure(Exception("No result returned"))
                }
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * Creates the data parameter for an addr(bytes32) resolution
     * 
     * @param node The namehash of the name to resolve
     * @return The encoded function call data
     */
    fun createAddrFunctionData(node: ByteArray): ByteArray {
        val function = Function(
            "addr",
            listOf(org.web3j.abi.datatypes.generated.Bytes32(node)),
            emptyList()
        )
        return Numeric.hexStringToByteArray(FunctionEncoder.encode(function))
    }
}

