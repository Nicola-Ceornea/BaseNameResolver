# Base Name Resolver - Android Library

An Android library for resolving Base names (*.base.eth) to Ethereum addresses using the CCIP-Read (ERC-3668) protocol.

## 🎯 What This Library Does

This library allows you to resolve Base names like `jesse.base.eth` to their associated Ethereum addresses directly from your Android app. It implements the complete CCIP-Read flow as specified in ERC-3668 and ENSIP-10.

### Example

```kotlin
val resolver = BaseNameResolver(ethereumRpcUrl = "YOUR-RPC-KEY")
val result = resolver.resolve("jesse.base.eth")
println("Address: ${result.address}") // 0x...
```

## 🏗️ Architecture

The library consists of several components:

### 1. **BaseNameResolver** (Main API)
- Entry point for resolving names
- Orchestrates the entire resolution flow
- Location: `basenameservice/src/main/java/io/basenameservice/BaseNameResolver.kt`

### 2. **L1ResolverContract**
- Interfaces with the L1Resolver smart contract on Ethereum mainnet
- Handles `resolve()` and `resolveWithProof()` calls
- Parses OffchainLookup errors
- Location: `basenameservice/src/main/java/io/basenameservice/contracts/L1ResolverContract.kt`

### 3. **CcipGatewayClient**
- Makes HTTP requests to the CCIP gateway
- Handles JSON parsing and response decoding
- Location: `basenameservice/src/main/java/io/basenameservice/gateway/CcipGatewayClient.kt`

### 4. **Utilities**
- **DnsEncoder**: Converts ENS names to DNS wire format
- **NameHash**: Calculates ENS namehashes using the keccak256 algorithm
- Location: `basenameservice/src/main/java/io/basenameservice/utils/`

### 5. **Data Models**
- `OffchainLookup`: Represents the CCIP-Read error data
- `GatewayResponse`: Represents the gateway's signed response
- `ResolutionResult`: Contains the final resolution result
- Location: `basenameservice/src/main/java/io/basenameservice/models/CcipReadModels.kt`

## 📋 Resolution Flow

```
┌─────────────────┐
│  Android App    │
└────────┬────────┘
         │ 1. resolve("jesse.base.eth")
         ↓
┌─────────────────────────────────────┐
│  BaseNameResolver                   │
│  - DNS encode name                  │
│  - Calculate namehash               │
│  - Create addr() function call      │
└────────┬────────────────────────────┘
         │ 2. Call resolve()
         ↓
┌─────────────────────────────────────┐
│  L1Resolver Contract (Ethereum)     │
│  0xde9049636F4a1dfE0a64d1b...       │
└────────┬────────────────────────────┘
         │ 3. Reverts with OffchainLookup
         │    (contains gateway URL)
         ↓
┌─────────────────────────────────────┐
│  CCIP Gateway (Offchain)            │
│  - Fetches name data from Base L2   │
│  - Signs the response                │
└────────┬────────────────────────────┘
         │ 4. Returns signed data
         ↓
┌─────────────────────────────────────┐
│  L1Resolver.resolveWithProof()      │
│  - Verifies signature               │
│  - Returns address data             │
└────────┬────────────────────────────┘
         │ 5. Decode address
         ↓
┌─────────────────┐
│  Android App    │
│  Gets: 0x123... │
└─────────────────┘
```

## 📦 Project Structure

```
BaseNameresolver/
├── app/                                    # Demo Android app
│   ├── src/main/java/nicola/ceornea/basenameresolver/
│   │   └── MainActivity.kt                 # Example UI with resolver
│   └── build.gradle.kts
│
├── basenameservice/                        # Library module
│   ├── src/main/java/io/basenameservice/
│   │   ├── BaseNameResolver.kt             # Main API class
│   │   ├── contracts/
│   │   │   └── L1ResolverContract.kt       # Smart contract interface
│   │   ├── gateway/
│   │   │   └── CcipGatewayClient.kt        # Gateway HTTP client
│   │   ├── models/
│   │   │   └── CcipReadModels.kt           # Data models
│   │   └── utils/
│   │       ├── DnsEncoder.kt               # DNS encoding utilities
│   │       └── NameHash.kt                 # ENS namehash calculator
│   └── build.gradle.kts
│
├── README.md                               # Base usernames info
├── USAGE.md                                # Detailed usage guide
├── PROJECT_README.md                       # This file
│
├── L1Resolver.sol                          # Reference contract
├── L2Resolver.sol                          # Reference contract
├── OffchainResolver.sol                    # Reference contract
└── PublicResolver.sol                      # Reference contract
```

## 🚀 Getting Started

### Prerequisites

1. **Ethereum RPC API Key**: Get a free key from:
   - [Alchemy](https://www.alchemy.com/) (Recommended)
   - [Infura](https://www.infura.io/)
   - [QuickNode](https://www.quicknode.com/)

2. **Android Studio**: Latest version recommended

### Installation

The library is already set up as a module in this project. To use it in your app:

1. Add the dependency in your app's `build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":basenameservice"))
}
```

2. Add internet permissions to your `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### Quick Start

```kotlin
import io.basenameservice.BaseNameResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class YourActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        CoroutineScope(Dispatchers.IO).launch {
            val rpcUrl = "https://eth-mainnet.g.alchemy.com/v2/YOUR-API-KEY"
            val resolver = BaseNameResolver(ethereumRpcUrl = rpcUrl)
            
            val result = resolver.resolve("jesse.base.eth")
            
            withContext(Dispatchers.Main) {
                if (result.address != null) {
                    Toast.makeText(
                        this@YourActivity,
                        "Address: ${result.address}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            
            resolver.close()
        }
    }
}
```

## 🧪 Running the Demo App

1. Open the project in Android Studio
2. Update the RPC URL in `MainActivity.kt` (line 88):
   ```kotlin
   val rpcUrl = "https://eth-mainnet.g.alchemy.com/v2/YOUR-ACTUAL-API-KEY"
   ```
3. Build and run the app
4. Enter a Base name (e.g., "jesse.base.eth")
5. Tap "Resolve" to see the associated Ethereum address

## 🔑 Key Features

- ✅ Full CCIP-Read (ERC-3668) implementation
- ✅ Support for all Base names (*.base.eth)
- ✅ Automatic DNS encoding and namehash calculation
- ✅ Signature verification for security
- ✅ Comprehensive error handling
- ✅ Kotlin coroutines for async operations
- ✅ Clean, documented API
- ✅ No external dependencies beyond Web3j and OkHttp

## 📚 Dependencies

The library uses the following dependencies:

- **Web3j** (4.10.3): Ethereum interaction and ABI encoding/decoding
- **OkHttp** (4.12.0): HTTP client for gateway requests
- **Kotlinx Coroutines** (1.7.3): Asynchronous programming
- **Moshi** (1.15.0): JSON parsing

## 🔧 Configuration

### Mainnet (Default)
```kotlin
val resolver = BaseNameResolver(
    ethereumRpcUrl = "https://eth-mainnet.g.alchemy.com/v2/YOUR-API-KEY"
)
```

### Sepolia Testnet
```kotlin
val resolver = BaseNameResolver(
    ethereumRpcUrl = "https://eth-sepolia.g.alchemy.com/v2/YOUR-API-KEY",
    l1ResolverAddress = "0x084D10C07EfEecD9fFc73DEb38ecb72f9eEb65aB"
)
```

## 📖 Documentation

- **[USAGE.md](./USAGE.md)**: Comprehensive usage guide with examples
- **[README.md](./README.md)**: Base usernames architecture and contracts
- **Inline Documentation**: All classes and methods are documented with KDoc

## 🔐 Security Considerations

1. **Signature Verification**: All gateway responses are cryptographically verified
2. **HTTPS Only**: All network requests use secure connections
3. **No Private Keys**: The library only performs read operations
4. **Input Validation**: Names are validated before processing

## 🐛 Troubleshooting

### Common Issues

**Issue**: "Gateway request failed"
- **Solution**: Check your internet connection and verify the gateway is accessible

**Issue**: "Invalid ENS name format"
- **Solution**: Ensure the name follows the format `[label].base.eth`

**Issue**: "No result returned"
- **Solution**: The name may not be registered or has no address set

See [USAGE.md](./USAGE.md) for more troubleshooting tips.

## 🤝 Contributing

Contributions are welcome! Please ensure:
- Code follows Kotlin conventions
- All public APIs are documented
- Tests are added for new features

## 📄 License

This project follows the same license as the Base Usernames project (MIT).

## 🙏 Acknowledgments

- **Base Team**: For creating and maintaining Base Usernames
- **ENS Team**: For the ENS protocol and CCIP-Read specification
- **Coinbase**: For the L1Resolver implementation

## 📞 Support

- **Base Names**: https://www.base.org/names
- **ENS Documentation**: https://docs.ens.domains/
- **CCIP-Read Spec**: https://eips.ethereum.org/EIPS/eip-3668
- **ENSIP-10**: https://docs.ens.domains/ensip/10

## 🎓 How It Works

This library implements the complete CCIP-Read protocol:

1. **Name Encoding**: Converts "jesse.base.eth" to DNS wire format
2. **Namehash Calculation**: Computes the keccak256 hash of the name
3. **L1 Contract Call**: Calls `L1Resolver.resolve()` on Ethereum
4. **OffchainLookup**: Contract reverts with gateway URL and call data
5. **Gateway Query**: HTTP POST request to the CCIP gateway
6. **Signature Verification**: Calls `L1Resolver.resolveWithProof()`
7. **Address Decoding**: Extracts the Ethereum address from the result

For more details on the architecture, see the [Architecture](#-architecture) section above.

---

**Built with ❤️ for the Base ecosystem**

