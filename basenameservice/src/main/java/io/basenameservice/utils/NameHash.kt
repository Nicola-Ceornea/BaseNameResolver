package io.basenameservice.utils

import org.web3j.crypto.Hash
import java.nio.charset.StandardCharsets

/**
 * ENS Namehash implementation
 * 
 * Namehash is a recursive process that generates a unique hash for each name.
 * See: https://docs.ens.domains/ensip/1
 */
object NameHash {
    
    /**
     * Generates the namehash for an ENS name
     * 
     * Algorithm:
     * - namehash('') = 0x0000000000000000000000000000000000000000000000000000000000000000
     * - namehash(label + '.' + remainder) = keccak256(namehash(remainder) + keccak256(label))
     * 
     * @param name The ENS name (e.g., "jesse.base.eth")
     * @return The namehash as a 32-byte array
     */
    fun nameHash(name: String): ByteArray {
        if (name.isEmpty() || name == ".") {
            return ByteArray(32) { 0 }
        }
        
        val normalizedName = name.lowercase().trim()
        val labels = normalizedName.split(".")
        
        var node = ByteArray(32) { 0 }
        
        // Process labels in reverse order (from TLD to subdomain)
        for (i in labels.size - 1 downTo 0) {
            val label = labels[i]
            if (label.isEmpty()) {
                continue
            }
            
            val labelHash = Hash.sha3(label.toByteArray(StandardCharsets.UTF_8))
            node = Hash.sha3(node + labelHash)
        }
        
        return node
    }
    
    /**
     * Converts a namehash to a hex string
     * 
     * @param hash The namehash bytes
     * @return The hex string representation
     */
    fun toHexString(hash: ByteArray): String {
        return "0x" + hash.joinToString("") { "%02x".format(it) }
    }
}

