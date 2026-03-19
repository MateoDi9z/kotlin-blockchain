package api.services

import api.dtos.Transaction
import entities.block.Block
import entities.block.BlockMiner
import entities.blockchain.Blockchain
import entities.results.OperationResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class NodeServiceTest {

    @Test
    fun `addTransaction delegates to blockchain`() {
        val rest = mockk<org.springframework.web.client.RestTemplate>(relaxed = true)
        val bc = mockk<Blockchain>(relaxed = true)

        val tx = Transaction("a", "b", 1L, "sig")
        every { bc.addTransaction(tx) } returns OperationResult.Success(Unit)

        val svc = NodeService(restTemplate = rest, blockchain = bc, miner = BlockMiner)

        val result = svc.addTransaction(tx)

        assertEquals(OperationResult.Success(Unit), result)
        verify { bc.addTransaction(tx) }
    }

    @Test
    fun `addPeer and getPeers`() {
        val rest = mockk<org.springframework.web.client.RestTemplate>(relaxed = true)
        val svc = NodeService(restTemplate = rest, blockchain = Blockchain(difficulty = 1))

        svc.addPeer("http://peer1")
        svc.addPeer("http://peer2")

        val peers = svc.getPeers()

        assertEquals(setOf("http://peer1", "http://peer2"), peers)
    }

    @Test
    fun `receiveBlock delegates to blockchain`() {
        val rest = mockk<org.springframework.web.client.RestTemplate>(relaxed = true)
        val bc = mockk<Blockchain>(relaxed = true)
        val block = Block(index = 1, timestamp = 1L, transactions = emptyList(), previousHash = "0")

        every { bc.addBlock(block) } returns OperationResult.Success(Unit)

        val svc = NodeService(restTemplate = rest, blockchain = bc)

        val result = svc.receiveBlock(block)

        assertEquals(OperationResult.Success(Unit), result)
        verify { bc.addBlock(block) }
    }
}
