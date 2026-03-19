package api.services

import api.dtos.Transaction
import entities.block.Block
import entities.block.BlockMiner
import entities.blockchain.Blockchain
import entities.results.OperationResult
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

@Service
class NodeService(
    private val restTemplate: RestTemplate,
    val blockchain: Blockchain = Blockchain(difficulty = 3),
    private val miner: BlockMiner = BlockMiner,
) {

    private val peers = mutableSetOf<String>()

    fun addTransaction(transaction: Transaction): OperationResult<Unit> =
        blockchain.addTransaction(transaction)

    fun getChain(): List<Block> = blockchain.chain

    fun addPeer(url: String) {
        peers.add(url)
    }

    fun getPeers(): Set<String> = peers

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
        peers.forEach { peerUrl ->
            try {
                val endpoint = "$peerUrl/blocks/receive"
                restTemplate.postForEntity<String>(endpoint, block)
            } catch (e: Exception) {
                // TODO()
            }
        }
    }

    fun receiveBlock(block: Block): OperationResult<Unit> {
        return blockchain.addBlock(block) // TODO()
    }
}
