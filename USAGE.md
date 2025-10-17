# Base Name Resolver - Usage Guide

This Android library provides a simple way to resolve Base names (*.base.eth) to Ethereum addresses using the CCIP-Read (ERC-3668) protocol.

## Overview

The Base Name Resolver library implements the complete CCIP-Read flow:

1. **DNS Encoding**: Converts ENS names to DNS wire format
2. **L1 Resolution**: Queries the L1Resolver contract on Ethereum mainnet
3. **Gateway Request**: Fetches resolution data from the offchain CCIP gateway
4. **Signature Verification**: Verifies the gateway response using the L1Resolver
5. **Address Decoding**: Extracts and returns the Ethereum address

## Setup

### 1. Add Dependencies (via JitPack)

Add JitPack to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then add the dependency in your app/library `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.Nicola-Ceornea:basenameservice:1.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

### 2. Get an Ethereum RPC API Key

You'll need an Ethereum RPC endpoint to query the L1Resolver contract. You can get a free API key from:

- [Alchemy](https://www.alchemy.com/) - Recommended
- [Infura](https://www.infura.io/)
- [QuickNode](https://www.quicknode.com/)

### 3. Add Internet Permission

Make sure your `AndroidManifest.xml` has the INTERNET permission (already added):

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Basic Usage

### Simple Resolution

```kotlin
import io.basenameservice.BaseNameResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// In your Activity or ViewModel
CoroutineScope(Dispatchers.IO).launch {
    val rpcUrl = "https://eth-mainnet.g.alchemy.com/v2/YOUR-API-KEY"
    val resolver = BaseNameResolver(ethereumRpcUrl = rpcUrl)
    
    // Resolve a Base name
    val result = resolver.resolve("jesse.base.eth")
    
    if (result.address != null) {
        println("Resolved address: ${result.address}")
    } else {
        println("Error: ${result.error}")
    }
    
    // Clean up
    resolver.close()
}
```

### In a Compose UI

```kotlin
@Composable
fun NameResolverScreen() {
    var address by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    Button(onClick = {
        scope.launch {
            val rpcUrl = "https://eth-mainnet.g.alchemy.com/v2/YOUR-API-KEY"
            val resolver = BaseNameResolver(ethereumRpcUrl = rpcUrl)
            val result = resolver.resolve("jesse.base.eth")
            
            address = result.address
            error = result.error
            
            resolver.close()
        }
    }) {
        Text("Resolve Name")
    }
    
    address?.let { Text("Address: $it") }
    error?.let { Text("Error: $it", color = Color.Red) }
}
```

## Advanced Usage

### Custom L1Resolver Address

If you want to use a different L1Resolver (e.g., for testing on Sepolia):

```kotlin
val resolver = BaseNameResolver(
    ethereumRpcUrl = "YOUR-RPC-API",
    l1ResolverAddress = "0x084D10C07EfEecD9fFc73DEb38ecb72f9eEb65aB" // Sepolia address
)
```

### Batch Resolution

```kotlin
val names = listOf("jesse.base.eth", "vitalik.base.eth", "coinbase.base.eth")
val rpcUrl = "https://eth-mainnet.g.alchemy.com/v2/YOUR-API-KEY"
val resolver = BaseNameResolver(ethereumRpcUrl = rpcUrl)

val results = names.map { name ->
    async(Dispatchers.IO) {
        resolver.resolve(name)
    }
}.awaitAll()

results.forEach { result ->
    println("${result.name}: ${result.address ?: result.error}")
}

resolver.close()
```

### Error Handling

```kotlin
val result = resolver.resolve("jesse.base.eth")

when {
    result.address != null -> {
        // Success - use the address
        println("Address: ${result.address}")
    }
    result.error != null -> {
        // Handle specific errors
        when {
            result.error.contains("Invalid ENS name") -> {
                println("The name format is invalid")
            }
            result.error.contains("Gateway request failed") -> {
                println("Could not reach the CCIP gateway")
            }
            result.error.contains("Proof verification failed") -> {
                println("Signature verification failed")
            }
            else -> {
                println("Unknown error: ${result.error}")
            }
        }
    }
}
```

## API Reference

### BaseNameResolver

Main class for resolving Base names.

#### Constructor

```kotlin
BaseNameResolver(
    ethereumRpcUrl: String,
    l1ResolverAddress: String = "0xde9049636F4a1dfE0a64d1bFe3155C0A14C54F31"
)
```

**Parameters:**
- `ethereumRpcUrl`: Ethereum RPC endpoint URL
- `l1ResolverAddress`: L1Resolver contract address (defaults to mainnet)

#### Methods

##### resolve(name: String): ResolutionResult

Resolves a Base name to its Ethereum address.

**Parameters:**
- `name`: The Base name to resolve (e.g., "jesse.base.eth")

**Returns:**
- `ResolutionResult` object containing:
  - `name`: The resolved name
  - `address`: The Ethereum address (or null if failed)
  - `error`: Error message (or null if successful)

**Example:**
```kotlin
val result = resolver.resolve("jesse.base.eth")
```

##### close()

Closes the Web3j connection and releases resources.

**Example:**
```kotlin
resolver.close()
```

### ResolutionResult

Data class representing the result of a name resolution.

**Properties:**
- `name: String` - The name that was resolved
- `address: String?` - The resolved Ethereum address (null if failed)
- `error: String?` - Error message (null if successful)

## Utility Classes

### DnsEncoder

Utility for encoding/decoding ENS names to DNS wire format.

```kotlin
import io.basenameservice.utils.DnsEncoder

// Encode a name
val encoded = DnsEncoder.dnsEncode("jesse.base.eth")

// Decode a name
val decoded = DnsEncoder.dnsDecode(encoded)

// Validate a name
val isValid = DnsEncoder.isValidEnsName("jesse.base.eth")
```

### NameHash

Utility for calculating ENS namehashes.

```kotlin
import io.basenameservice.utils.NameHash

// Calculate namehash
val hash = NameHash.nameHash("jesse.base.eth")

// Convert to hex string
val hexHash = NameHash.toHexString(hash)
```

## Contract Addresses

### Ethereum Mainnet
- L1Resolver: `0xde9049636F4a1dfE0a64d1bFe3155C0A14C54F31`

### Sepolia Testnet
- L1Resolver: `0x084D10C07EfEecD9fFc73DEb38ecb72f9eEb65aB`

## How It Works

The resolution process follows the CCIP-Read (ERC-3668) protocol:

```
1. Client encodes "jesse.base.eth" to DNS format
2. Client calls L1Resolver.resolve(dnsName, addrFunctionData)
3. L1Resolver reverts with OffchainLookup error containing:
   - Gateway URL
   - Call data
   - Callback function
4. Client queries the gateway with the call data
5. Gateway returns signed response with:
   - Result data
   - Expiry timestamp
   - Signature
6. Client calls L1Resolver.resolveWithProof(response, extraData)
7. L1Resolver verifies signature and returns the address
8. Client decodes the address and returns it
```

## Troubleshooting

### "Gateway request failed"
- Check your internet connection
- Verify the gateway URL is accessible
- Check for any firewall or network restrictions

### "Proof verification failed"
- The gateway response signature is invalid
- The response may have been tampered with
- Try again or contact support

### "Invalid ENS name format"
- Ensure the name follows the format: `[label].base.eth`
- Names are case-insensitive
- Labels cannot be empty or exceed 63 characters

### "Name not found or has no address set"
- The name may not be registered
- The name may not have an address record set
- Verify the name exists on [Base Names](https://www.base.org/names)

## Performance Considerations

- **Caching**: Consider caching resolution results to reduce RPC calls
- **Connection Pooling**: Reuse the `BaseNameResolver` instance when possible
- **Async Operations**: Always resolve names on a background thread (using coroutines)
- **Resource Management**: Always call `close()` when done to release resources

## License

This library follows the same license as the Base Usernames project (MIT).

## Support

For issues or questions:
- Check the [Base Names documentation](https://docs.base.org/building-with-base/base-names/)
- Review the [ENS documentation](https://docs.ens.domains/)
- Submit issues to the project repository

