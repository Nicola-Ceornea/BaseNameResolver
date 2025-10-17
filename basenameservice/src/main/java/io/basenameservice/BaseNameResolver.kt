package io.basenameservice

import io.basenameservice.contracts.L1ResolverContract
import io.basenameservice.gateway.CcipGatewayClient
import io.basenameservice.models.OffchainLookup
import io.basenameservice.models.ResolutionResult
import io.basenameservice.utils.DnsEncoder
import io.basenameservice.utils.NameHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Type
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric

/**
 * Main API class for resolving Base names (*.base.eth) to Ethereum addresses
 * 
 * This class implements the CCIP-Read (ERC-3668) protocol to resolve names using:
 * 1. L1Resolver contract on Ethereum mainnet
 * 2. Offchain gateway for wildcard resolution
 * 3. Signature verification
 *
 * @param ethereumRpcUrl The Ethereum RPC endpoint URL
 * @param l1ResolverAddress The L1Resolver contract address (defaults to mainnet deployment)
 */
class BaseNameResolver(
    ethereumRpcUrl: String,
    l1ResolverAddress: String = L1ResolverContract.DEFAULT_L1_RESOLVER_ADDRESS
) {
    private val web3j: Web3j = Web3j.build(HttpService(ethereumRpcUrl))
    private val l1Resolver = L1ResolverContract(web3j, l1ResolverAddress)
    private val gatewayClient = CcipGatewayClient()
    
    /**
     * Resolves a Base name to its associated Ethereum address
     * 
     * This method follows the CCIP-Read flow:
     * 1. Encode the name in DNS format
     * 2. Calculate the namehash
     * 3. Call L1Resolver.resolve() with the encoded name and addr function data
     * 4. If it reverts with OffchainLookup, query the gateway
     * 5. Call L1Resolver.resolveWithProof() to verify and get the final result
     * 6. Decode the result to extract the address
     * 
     * @param name The ENS name to resolve (e.g., "jesse.base.eth")
     * @return ResolutionResult containing the resolved address or error
     */
    suspend fun resolve(name: String): ResolutionResult = withContext(Dispatchers.IO) {
        try {
            // Validate the name
            if (!DnsEncoder.isValidEnsName(name)) {
                return@withContext ResolutionResult(
                    name = name,
                    address = null,
                    error = "Invalid ENS name format"
                )
            }
            
            // Ensure the name ends with .base.eth
            val normalizedName = if (!name.lowercase().endsWith(".base.eth")) {
                if (name.contains(".")) {
                    return@withContext ResolutionResult(
                        name = name,
                        address = null,
                        error = "Name must be a .base.eth domain"
                    )
                }
                "$name.base.eth"
            } else {
                name.lowercase()
            }
            
            // Step 1: DNS encode the name
            val dnsEncodedName = DnsEncoder.dnsEncode(normalizedName)
            
            // Step 2: Calculate namehash and create addr function data
            val nameHash = NameHash.nameHash(normalizedName)
            val addrFunctionData = l1Resolver.createAddrFunctionData(nameHash)
            
            // Step 3: Call L1Resolver.resolve()
            val resolveResult = l1Resolver.resolve(dnsEncodedName, addrFunctionData)
            
            // Check if we got a direct result (for base.eth itself)
            if (resolveResult.isSuccess) {
                val resultBytes = resolveResult.getOrNull()
                if (resultBytes != null) {
                    val address = decodeAddress(resultBytes)
                    return@withContext ResolutionResult(
                        name = normalizedName,
                        address = address
                    )
                }
            }
            
            // Step 4: If resolve failed, it should be an OffchainLookup error
            // We need to extract the OffchainLookup from the error message
            val exception = resolveResult.exceptionOrNull()
            println("DEBUG: Contract call failed: ${exception?.message}")
            exception?.printStackTrace()
            
            val offchainLookup = extractOffchainLookupFromError(exception)
            if (offchainLookup == null) {
                println("DEBUG: Failed to parse OffchainLookup error")
                return@withContext ResolutionResult(
                    name = normalizedName,
                    address = null,
                    error = "Failed to initiate CCIP-Read. Error: ${exception?.message}"
                )
            }
            
            println("DEBUG: OffchainLookup parsed successfully")
            println("DEBUG: Gateway URL: ${offchainLookup.urls.firstOrNull()}")
            
            // Step 5: Query the gateway
            if (offchainLookup.urls.isEmpty()) {
                return@withContext ResolutionResult(
                    name = normalizedName,
                    address = null,
                    error = "No gateway URLs provided"
                )
            }
            
            val gatewayUrl = offchainLookup.urls[0]
            val gatewayResponse = gatewayClient.query(
                gatewayUrl = gatewayUrl,
                sender = offchainLookup.sender,
                callData = offchainLookup.callData
            )
            
            if (gatewayResponse.isFailure) {
                return@withContext ResolutionResult(
                    name = normalizedName,
                    address = null,
                    error = "Gateway request failed: ${gatewayResponse.exceptionOrNull()?.message}"
                )
            }
            
            val gatewayData = gatewayResponse.getOrNull()
                ?: return@withContext ResolutionResult(
                    name = normalizedName,
                    address = null,
                    error = "Empty gateway response"
                )
            
            // Step 6: Call resolveWithProof
            val proofResult = l1Resolver.resolveWithProof(
                response = gatewayData,
                extraData = offchainLookup.extraData
            )
            
            if (proofResult.isFailure) {
                return@withContext ResolutionResult(
                    name = normalizedName,
                    address = null,
                    error = "Proof verification failed: ${proofResult.exceptionOrNull()?.message}"
                )
            }
            
            val finalResultBytes = proofResult.getOrNull()
                ?: return@withContext ResolutionResult(
                    name = normalizedName,
                    address = null,
                    error = "Empty proof result"
                )
            
            // Step 7: Decode the final result
            val address = decodeAddress(finalResultBytes)
            
            ResolutionResult(
                name = normalizedName,
                address = address
            )
            
        } catch (e: Exception) {
            e.printStackTrace()
            ResolutionResult(
                name = name,
                address = null,
                error = "Resolution failed: ${e.message}"
            )
        }
    }
    
    /**
     * Extracts OffchainLookup error from an exception
     * 
     * The L1Resolver contract reverts with OffchainLookup when wildcard resolution is needed.
     * We need to parse this error data.
     */
    private fun extractOffchainLookupFromError(exception: Throwable?): OffchainLookup? {
        if (exception == null) return null
        
        // Check if it's our custom ContractRevertException
        val revertData = if (exception is io.basenameservice.contracts.ContractRevertException) {
            exception.revertData
        } else {
            // Try to extract revert data from the error message
            val errorMessage = exception.message ?: return null
            val revertDataPattern = Regex("0x[0-9a-fA-F]{8,}")
            revertDataPattern.find(errorMessage)?.value ?: return null
        }
        
        // Parse the OffchainLookup from the revert data
        return l1Resolver.parseOffchainLookup(revertData)
    }
    
    /**
     * Decodes an address from ABI-encoded bytes
     * 
     * The addr(bytes32) function returns an address encoded as bytes
     */
    private fun decodeAddress(encodedData: ByteArray): String? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val types = mutableListOf(
                object : TypeReference<Address>() {}
            ) as MutableList<TypeReference<Type<*>>>
            val decoded = FunctionReturnDecoder.decode(
                Numeric.toHexString(encodedData),
                types
            )
            
            if (decoded.isNotEmpty()) {
                val address = decoded[0] as Address
                // Check for zero address
                if (address.value == "0x0000000000000000000000000000000000000000") {
                    null
                } else {
                    address.value
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Closes the Web3j instance and releases resources
     */
    fun close() {
        web3j.shutdown()
    }
}

