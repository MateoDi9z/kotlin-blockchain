package api.controllers

import api.dtos.PeerRegistrationRequest
import api.dtos.Transaction
import api.services.NodeService
import entities.block.Block
import entities.results.OperationResult
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class NodeController(
    private val nodeService: NodeService,
) {

    @PostMapping("/register")
    fun registerPeer(
        @RequestBody request: PeerRegistrationRequest,
    ): ResponseEntity<Any> {
        val result = nodeService.handleNewPeerRegistration(request)

        return if (result.isSuccess) {
            ResponseEntity.ok(result.knownPeers)
        } else {
            ResponseEntity.badRequest().body(result.errorMessage)
        }
    }

    @GetMapping("/blocks")
    fun getChain(): ResponseEntity<List<Block>> {
        val chain = nodeService.getChain()
        return ResponseEntity.ok(chain)
    }

    @GetMapping("/peers")
    fun getPeers(): ResponseEntity<Set<String>> = ResponseEntity.ok(nodeService.getPeers())

    @PostMapping("/mine")
    fun mineBlock(): ResponseEntity<Map<String, Any>> {
        val minedBlock = nodeService.mineBlock()

        val response =
            mapOf(
                "message" to "Block mined and broadcast to the network successfully",
                "block" to minedBlock,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/transactions")
    fun addTransaction(
        @RequestBody transaction: Transaction,
    ): ResponseEntity<Any> =
        when (val result = nodeService.addTransaction(transaction)) {
            is OperationResult.Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(mapOf("message" to "Transaction queued for the next block"))
            is OperationResult.Failure -> ResponseEntity.badRequest().body(result.errors)
        }

    @PostMapping("/peers")
    fun registerPeer(
        @RequestBody payload: Map<String, String>,
    ): ResponseEntity<String> {
        val url = payload["url"] ?: return ResponseEntity.badRequest().body("Missing 'url' field")
        nodeService.addPeer(url)
        return ResponseEntity.ok("Peer $url added to the network successfully")
    }

    @PostMapping("/blocks/receive")
    fun receiveExternalBlock(
        @RequestBody block: Block,
    ): ResponseEntity<Any> =
        when (val result = nodeService.receiveBlock(block)) {
            is OperationResult.Success ->
                ResponseEntity.ok(
                    "Block accepted and added to the local chain",
                )
            is OperationResult.Failure -> ResponseEntity.badRequest().body(result.errors)
        }
}
