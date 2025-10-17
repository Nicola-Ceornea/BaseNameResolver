package io.basenameservice.examples

import io.basenameservice.BaseNameResolver
import kotlinx.coroutines.runBlocking

/**
 * Simple example demonstrating how to use the BaseNameResolver library
 * 
 * This is a standalone example that can be run from any Kotlin environment
 * (not just Android). For Android-specific examples, see MainActivity.kt
 * *
 * *
 */

const val RPC_URL =  "YOUR-API"

object SimpleExample {
    
    /**
     * Example 1: Basic name resolution
     */
    fun basicResolution() = runBlocking {
        // Replace with your actual Ethereum RPC URL
        val rpcUrl = RPC_URL
        
        // Create resolver instance
        val resolver = BaseNameResolver(ethereumRpcUrl = rpcUrl)
        
        // Resolve a name
        val result = resolver.resolve("jesse.base.eth")
        
        // Check the result
        if (result.address != null) {
            println("✓ Resolved ${result.name} to ${result.address}")
        } else {
            println("✗ Failed to resolve ${result.name}: ${result.error}")
        }
        
        // Clean up
        resolver.close()
    }
    
    /**
     * Example 2: Resolving multiple names
     */
    fun batchResolution() = runBlocking {
        val rpcUrl = RPC_URL
        val resolver = BaseNameResolver(ethereumRpcUrl = rpcUrl)
        
        val names = listOf(
            "jesse.base.eth",
            "vitalik.base.eth",
            "coinbase.base.eth"
        )
        
        println("Resolving ${names.size} names...")
        
        for (name in names) {
            val result = resolver.resolve(name)
            when {
                result.address != null -> 
                    println("✓ $name -> ${result.address}")
                else -> 
                    println("✗ $name -> ${result.error}")
            }
        }
        
        resolver.close()
    }
    
    /**
     * Example 3: Error handling
     */
    fun errorHandling() = runBlocking {
        val rpcUrl = RPC_URL
        val resolver = BaseNameResolver(ethereumRpcUrl = rpcUrl)
        
        val testCases = listOf(
            "valid.base.eth",           // Valid name
            "invalid",                  // Missing .base.eth
            "toolongname" + "x".repeat(100) + ".base.eth",  // Too long
            ""                          // Empty name
        )
        
        for (name in testCases) {
            val result = resolver.resolve(name)
            
            println("\nTesting: '$name'")
            when {
                result.address != null -> {
                    println("  Success: ${result.address}")
                }
                result.error != null -> {
                    when {
                        result.error.contains("Invalid ENS name") -> 
                            println("  Error: Invalid name format")
                        result.error.contains("Gateway request failed") -> 
                            println("  Error: Network issue")
                        result.error.contains("not found") -> 
                            println("  Error: Name not registered")
                        else -> 
                            println("  Error: ${result.error}")
                    }
                }
            }
        }
        
        resolver.close()
    }
    
    /**
     * Example 4: Auto-appending .base.eth
     */
    fun autoAppendDomain() = runBlocking {
        val rpcUrl = RPC_URL
        val resolver = BaseNameResolver(ethereumRpcUrl = rpcUrl)
        
        // Both of these work - .base.eth is auto-appended if missing
        val result1 = resolver.resolve("jesse")
        val result2 = resolver.resolve("jesse.base.eth")
        
        println("Resolving 'jesse':")
        println("  Normalized to: ${result1.name}")
        println("  Address: ${result1.address ?: "Not found"}")
        
        println("\nResolving 'jesse.base.eth':")
        println("  Normalized to: ${result2.name}")
        println("  Address: ${result2.address ?: "Not found"}")
        
        resolver.close()
    }
    
    /**
     * Example 5: Using with different networks (Sepolia testnet)
     */
    fun testnetResolution() = runBlocking {
        val sepoliaRpcUrl = RPC_URL
        val sepoliaL1Resolver = "0x084D10C07EfEecD9fFc73DEb38ecb72f9eEb65aB"
        
        val resolver = BaseNameResolver(
            ethereumRpcUrl = sepoliaRpcUrl,
            l1ResolverAddress = sepoliaL1Resolver
        )
        
        val result = resolver.resolve("test.base.eth")
        
        println("Sepolia testnet resolution:")
        println("  Address: ${result.address ?: result.error}")
        
        resolver.close()
    }
}

/**
 * Usage in Android Activity/Fragment:
 * 
 * class MyActivity : AppCompatActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         
 *         lifecycleScope.launch {
 *             val rpcUrl = "https://eth-mainnet.g.alchemy.com/v2/YOUR-API-KEY"
 *             val resolver = BaseNameResolver(ethereumRpcUrl = rpcUrl)
 *             
 *             val result = resolver.resolve("jesse.base.eth")
 *             
 *             withContext(Dispatchers.Main) {
 *                 if (result.address != null) {
 *                     textView.text = result.address
 *                 } else {
 *                     Toast.makeText(this@MyActivity, result.error, Toast.LENGTH_SHORT).show()
 *                 }
 *             }
 *             
 *             resolver.close()
 *         }
 *     }
 * }
 */

/**
 * Usage in Jetpack Compose:
 * 
 * @Composable
 * fun NameResolverScreen() {
 *     var address by remember { mutableStateOf<String?>(null) }
 *     val scope = rememberCoroutineScope()
 *     
 *     Button(onClick = {
 *         scope.launch {
 *             val rpcUrl = "https://eth-mainnet.g.alchemy.com/v2/YOUR-API-KEY"
 *             val resolver = BaseNameResolver(ethereumRpcUrl = rpcUrl)
 *             val result = resolver.resolve("jesse.base.eth")
 *             address = result.address
 *             resolver.close()
 *         }
 *     }) {
 *         Text("Resolve")
 *     }
 *     
 *     address?.let { 
 *         Text("Address: $it") 
 *     }
 * }
 */

