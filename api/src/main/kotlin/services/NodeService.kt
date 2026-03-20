package api.services

import api.configuration.NetworkProperties
import api.dtos.PeerRegistrationRequest
import api.dtos.Transaction
import api.entities.SignatureManager
import api.entities.SignatureManager.verifySignature
import api.entities.node.NodeIdentity
import entities.block.Block
import entities.block.BlockMiner
import entities.blockchain.Blockchain
import entities.results.OperationResult
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

data class PeerRegistrationResult(
    val isSuccess: Boolean,
    val knownPeers: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

@Service
class NodeService(
    private val restTemplate: RestTemplate,
    private val nodeIdentity: NodeIdentity,
    private val networkProps: NetworkProperties,
    val blockchain: Blockchain,
    private val miner: BlockMiner = BlockMiner,
) {
    private val log = LoggerFactory.getLogger(NodeService::class.java)

    private val activePeers = ConcurrentHashMap.newKeySet<String>()

    @EventListener(ApplicationReadyEvent::class)
    fun bootstrapNetwork() {
        CompletableFuture.runAsync {
            log.info("Bootstrapping node in background... Contacting seed peers.")

            val myUrl = nodeIdentity.publicUrl
            val request = createRegistrationRequest(myUrl)

            networkProps.knownPeers.forEach { seedUrl ->
                if (seedUrl != myUrl) {
                    registerWithSeedPeer(seedUrl, request)
                }
            }
            log.info("Bootstrap finished. Current active peers: ${activePeers.size}")
        }
    }

    fun handleNewPeerRegistration(request: PeerRegistrationRequest): PeerRegistrationResult {
        val publicKeyObject = SignatureManager.getPublicKeyFromString(request.publicKey)

        val isValidSignature =
            verifySignature(
                publicKey = publicKeyObject,
                message = request.peerUrl,
                signature = request.signature,
            )

        if (!isValidSignature) {
            log.warn(
                "Security Alert: Invalid ECDSA signature from ${request.peerUrl}. Registration rejected.",
            )
            return PeerRegistrationResult(
                isSuccess = false,
                errorMessage = "Invalid cryptographic signature.",
            )
        }

        if (request.peerUrl == nodeIdentity.publicUrl) {
            return PeerRegistrationResult(isSuccess = false, errorMessage = "Cannot register self.")
        }

        val isNewPeer = activePeers.add(request.peerUrl)

        if (isNewPeer) {
            log.info("New verified peer joined: ${request.peerUrl}. Gossiping to network...")
            gossipNewPeerAsync(request)
        }

        val peersToShare = activePeers.filter { it != request.peerUrl }.toSet()
        return PeerRegistrationResult(isSuccess = true, knownPeers = peersToShare)
    }

    private fun gossipNewPeerAsync(newPeerRequest: PeerRegistrationRequest) {
        CompletableFuture.runAsync {
            activePeers.forEach { existingPeerUrl ->
                if (existingPeerUrl != newPeerRequest.peerUrl &&
                    existingPeerUrl != nodeIdentity.publicUrl
                ) {
                    try {
                        restTemplate.postForEntity(
                            "$existingPeerUrl/api/peers/register",
                            newPeerRequest,
                            Any::class.java,
                        )
                    } catch (e: Exception) {
                        log.debug(
                            "Failed to gossip to $existingPeerUrl. Node might be offline. Removing from active peers.",
                        )
                        activePeers.remove(existingPeerUrl) // Auto-cleanup dead nodes
                    }
                }
            }
        }
    }

    private fun registerWithSeedPeer(
        seedUrl: String,
        request: PeerRegistrationRequest,
    ) {
        try {
            val response =
                restTemplate.postForEntity(
                    "$seedUrl/api/peers/register",
                    request,
                    Array<String>::class.java,
                )

            activePeers.add(seedUrl)

            // Add friends of the seed node safely
            response.body?.forEach { peerUrl ->
                if (peerUrl != nodeIdentity.publicUrl) {
                    activePeers.add(peerUrl)
                }
            }
        } catch (e: Exception) {
            log.warn(
                "Seed node unreachable at $seedUrl. Proceeding with remaining network operations.",
            )
        }
    }

    private fun createRegistrationRequest(myUrl: String): PeerRegistrationRequest =
        PeerRegistrationRequest(
            peerUrl = myUrl,
            publicKey = nodeIdentity.getPublicKeyBase64(),
            signature = nodeIdentity.signMessage(myUrl),
        )

    fun addTransaction(transaction: Transaction): OperationResult<Unit> =
        blockchain.addTransaction(transaction)

    fun getChain(): List<Block> = blockchain.chain

    fun addPeer(url: String) {
        activePeers.add(url)
    }

    fun getPeers(): Set<String> = activePeers.toSet()

    fun mineBlock(): Block {
        val unminedBlock = makeBlock()

        val minedBlock = miner.mine(unminedBlock, blockchain.difficulty)

        blockchain.addBlock(minedBlock)

        broadcastBlock(minedBlock)

        return minedBlock
    }

    private fun makeBlock(): Block {
        val transactionsToMine = blockchain.getPendingTransactions()

        val latestBlock = blockchain.getLatestBlock()

        val unminedBlock =
            Block(
                index = latestBlock.index + 1,
                timestamp = System.currentTimeMillis(),
                transactions = transactionsToMine,
                previousHash = latestBlock.hash,
            )
        return unminedBlock
    }

    private fun broadcastBlock(block: Block) {
        activePeers.forEach { peerUrl ->
            try {
                val endpoint = "$peerUrl/blocks/receive"
                restTemplate.postForEntity<Any>(endpoint, block)
            } catch (e: Exception) {
                activePeers.remove(peerUrl)
            }
        }
    }

    fun receiveBlock(block: Block): OperationResult<Unit> =
        when (val result = blockchain.addBlock(block)) {
            is OperationResult.Success -> result
            is OperationResult.Failure -> handleReceiveBlockFailure(block)
        }

    fun handleReceiveBlockFailure(block: Block): OperationResult<Unit> =
        blockchain.replaceChain(listOf(block))
}
