package api.controller

import api.controllers.NodeController
import api.dtos.Transaction
import api.services.NodeService
import entities.block.Block
import entities.results.OperationResult
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import org.springframework.http.HttpStatus

class NodeControllerTest {

    @Test
    fun `getChain returns chain from service`() {
        val svc = mockk<NodeService>(relaxed = true)
        val chain =
            listOf(Block(index = 1, timestamp = 1L, transactions = emptyList(), previousHash = "0"))
        every { svc.getChain() } returns chain

        val ctrl = NodeController(svc)

        val response = ctrl.getChain()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(chain, response.body)
    }

    @Test
    fun `addTransaction returns created on success and bad request on failure`() {
        val svc = mockk<NodeService>(relaxed = true)
        val tx = Transaction("a", "b", 1L, "sig")

        every { svc.addTransaction(tx) } returns OperationResult.Success(Unit)
        val ctrl1 = NodeController(svc)
        val r1 = ctrl1.addTransaction(tx)
        assertEquals(HttpStatus.CREATED, r1.statusCode)

        every { svc.addTransaction(tx) } returns OperationResult.Failure(listOf("err"))
        val ctrl2 = NodeController(svc)
        val r2 = ctrl2.addTransaction(tx)
        assertEquals(HttpStatus.BAD_REQUEST, r2.statusCode)
    }

    @Test
    fun `registerPeer validates payload`() {
        val svc = mockk<NodeService>(relaxed = true)
        val ctrl = NodeController(svc)

        val bad = ctrl.registerPeer(emptyMap())
        assertEquals(HttpStatus.BAD_REQUEST, bad.statusCode)

        val ok = ctrl.registerPeer(mapOf("url" to "http://peer"))
        assertEquals(HttpStatus.OK, ok.statusCode)
    }

    @Test
    fun `receiveExternalBlock maps success and failure`() {
        val svc = mockk<NodeService>(relaxed = true)
        val block = Block(index = 1, timestamp = 1L, transactions = emptyList(), previousHash = "0")

        every { svc.receiveBlock(block) } returns OperationResult.Success(Unit)
        val ctrl1 = NodeController(svc)
        val r1 = ctrl1.receiveExternalBlock(block)
        assertEquals(HttpStatus.OK, r1.statusCode)

        every { svc.receiveBlock(block) } returns OperationResult.Failure(listOf("err"))
        val ctrl2 = NodeController(svc)
        val r2 = ctrl2.receiveExternalBlock(block)
        assertEquals(HttpStatus.BAD_REQUEST, r2.statusCode)
    }
}
