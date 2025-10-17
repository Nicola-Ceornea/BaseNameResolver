# BaseName Service for Android

A Kotlin library for resolving Base names (`*.base.eth`) to Ethereum addresses on Android. This library implements the CCIP-Read (ERC-3668) protocol to resolve names using the L1Resolver contract and offchain gateways.

## Features

- 🔍 Resolve Base names to Ethereum addresses
- 🔐 CCIP-Read protocol support with signature verification
- ⚡ Async/await support with Kotlin Coroutines
- 🎯 Simple, intuitive API
- 📱 Android-optimized

## Installation

### Step 1: Add JitPack repository

In your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // Add this line
    }
}
```

### Step 2: Add the dependency

In your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.Nicola-Ceornea:basenameservice:1.0.2")
}
```

## Usage

### Basic Example

```kotlin
import io.basenameservice.BaseNameResolver
import kotlinx.coroutines.launch

// In your Activity or ViewModel
lifecycleScope.launch {
    // Create resolver with your Ethereum RPC URL
    val resolver = BaseNameResolver(
        ethereumRpcUrl = "https://eth-mainnet.g.alchemy.com/v2/YOUR_API_KEY"
    )
    
    // Resolve a Base name
    val result = resolver.resolve("jesse.base.eth")
    
    if (result.address != null) {
        println("Resolved to: ${result.address}")
    } else {
        println("Error: ${result.error}")
    }
    
    // Clean up when done
    resolver.close()
}
```

### Resolving Multiple Names

```kotlin
lifecycleScope.launch {
    val resolver = BaseNameResolver(ethereumRpcUrl = "YOUR_RPC_URL")
    
    val names = listOf("jesse.base.eth", "brian.base.eth", "derek.base.eth")
    
    names.forEach { name ->
        val result = resolver.resolve(name)
        println("$name -> ${result.address ?: result.error}")
    }
    
    resolver.close()
}
```

### With Error Handling

```kotlin
lifecycleScope.launch {
    val resolver = BaseNameResolver(ethereumRpcUrl = "YOUR_RPC_URL")
    
    try {
        val result = resolver.resolve("myname.base.eth")
        
        when {
            result.address != null -> {
                // Success! Use the address
                sendTransaction(result.address)
            }
            result.error != null -> {
                // Handle the error
                showError(result.error)
            }
        }
    } catch (e: Exception) {
        showError("Network error: ${e.message}")
    } finally {
        resolver.close()
    }
}
```

## Requirements

- Android minSdk 21+
- compileSdk 34 / targetSdk 34
- Kotlin 1.9+
- An Ethereum RPC endpoint (e.g., from [Alchemy](https://www.alchemy.com/), [Infura](https://infura.io/), or [QuickNode](https://www.quicknode.com/))

## API Reference

### `BaseNameResolver`

**Constructor:**
```kotlin
BaseNameResolver(
    ethereumRpcUrl: String,
    l1ResolverAddress: String = DEFAULT_L1_RESOLVER_ADDRESS
)
```

**Methods:**

- `suspend fun resolve(name: String): ResolutionResult`
  - Resolves a Base name to an Ethereum address
  - Returns a `ResolutionResult` with either an `address` or an `error`

- `fun close()`
  - Closes the Web3j connection and releases resources

### `ResolutionResult`

```kotlin
data class ResolutionResult(
    val name: String,
    val address: String?,
    val error: String?
)
```

## How It Works

This library implements the [CCIP-Read (ERC-3668)](https://eips.ethereum.org/EIPS/eip-3668) protocol:

1. Encodes the name in DNS format
2. Calls the L1Resolver contract on Ethereum
3. If it reverts with `OffchainLookup`, queries the offchain gateway
4. Verifies the gateway response signature
5. Returns the resolved address

## License

GPL 3.0 License - see LICENSE file for details

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues, questions, or contributions, please visit the [GitHub repository](https://github.com/Nicola-Ceornea/BaseNameResolver).

