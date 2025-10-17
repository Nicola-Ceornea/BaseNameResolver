package nicola.ceornea.basenameresolver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.basenameservice.BaseNameResolver
import kotlinx.coroutines.launch
import nicola.ceornea.basenameresolver.ui.theme.BaseNameresolverTheme


const val RPC_URL =  "YOUR-API"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BaseNameresolverTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BaseNameResolverScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun BaseNameResolverScreen(modifier: Modifier = Modifier) {
    var nameInput by remember { mutableStateOf("jesse.base.eth") }
    var resolvedAddress by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Base Name Resolver",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Text(
            text = "Resolve Base names (*.base.eth) to Ethereum addresses",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Base Name") },
            placeholder = { Text("e.g., jesse.base.eth") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )
        
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    resolvedAddress = null
                    errorMessage = null
                    
                    try {
                        // Replace with your Ethereum RPC URL
                        // You can get a free API key from Alchemy, Infura, or other providers
                        val rpcUrl = RPC_URL
                        
                        val resolver = BaseNameResolver(ethereumRpcUrl = rpcUrl)
                        val result = resolver.resolve(nameInput)
                        
                        if (result.address != null) {
                            resolvedAddress = result.address
                            errorMessage = null
                        } else {
                            resolvedAddress = null
                            errorMessage = result.error ?: "Name not found or has no address set"
                        }
                        
                        resolver.close()
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
                        resolvedAddress = null
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && nameInput.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLoading) "Resolving..." else "Resolve")
        }
        
        if (resolvedAddress != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Resolved Address:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = resolvedAddress!!,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
        
        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Error:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ℹ️ How it works:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "1. Enter a Base name (e.g., 'jesse.base.eth')\n" +
                            "2. The app queries the L1Resolver contract on Ethereum\n" +
                            "3. It fetches data from the CCIP gateway\n" +
                            "4. Verifies the signature\n" +
                            "5. Returns the resolved Ethereum address",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "⚠️ Note:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "You need to replace 'YOUR-API-KEY' in the code with a real Ethereum RPC API key from Alchemy, Infura, or another provider.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}