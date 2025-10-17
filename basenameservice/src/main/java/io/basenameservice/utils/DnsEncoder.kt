package io.basenameservice.utils

import java.io.ByteArrayOutputStream

/**
 * DNS encoding utilities for ENS names
 */
object DnsEncoder {
    
    /**
     * Encodes an ENS name to DNS wire format
     * 
     * For example: "jesse.base.eth" becomes:
     * [5]jesse[4]base[3]eth[0]
     * where the numbers are the length of each label
     * 
     * @param name The ENS name to encode (e.g., "jesse.base.eth")
     * @return The DNS-encoded byte array
     */
    fun dnsEncode(name: String): ByteArray {
        if (name.isEmpty() || name == ".") {
            return byteArrayOf(0)
        }
        
        val output = ByteArrayOutputStream()
        val labels = name.split(".")
        
        for (label in labels) {
            if (label.isEmpty()) {
                continue
            }
            
            val labelBytes = label.toByteArray(Charsets.UTF_8)
            if (labelBytes.size > 63) {
                throw IllegalArgumentException("Label too long: $label")
            }
            
            // Write length byte followed by label bytes
            output.write(labelBytes.size)
            output.write(labelBytes)
        }
        
        // Terminate with zero byte
        output.write(0)
        
        return output.toByteArray()
    }
    
    /**
     * Decodes a DNS wire format name to a readable string
     * 
     * @param dnsName The DNS-encoded byte array
     * @return The decoded ENS name
     */
    fun dnsDecode(dnsName: ByteArray): String {
        if (dnsName.isEmpty() || dnsName[0].toInt() == 0) {
            return ""
        }
        
        val labels = mutableListOf<String>()
        var i = 0
        
        while (i < dnsName.size) {
            val length = dnsName[i].toInt() and 0xFF
            if (length == 0) {
                break
            }
            
            i++
            val labelBytes = dnsName.sliceArray(i until (i + length))
            labels.add(String(labelBytes, Charsets.UTF_8))
            i += length
        }
        
        return labels.joinToString(".")
    }
    
    /**
     * Validates an ENS name
     * 
     * @param name The ENS name to validate
     * @return true if valid, false otherwise
     */
    fun isValidEnsName(name: String): Boolean {
        if (name.isEmpty()) {
            return false
        }
        
        val labels = name.split(".")
        if (labels.isEmpty()) {
            return false
        }
        
        for (label in labels) {
            if (label.isEmpty() || label.length > 63) {
                return false
            }
        }
        
        return true
    }
}

