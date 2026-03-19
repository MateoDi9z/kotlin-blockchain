package api.controllers

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
                "message" to "Bloque minado y transmitido a la red con éxito",
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
                    .body(mapOf("message" to "Transacción encolada para el próximo bloque"))
            is OperationResult.Failure -> ResponseEntity.badRequest().body(result.errors)
        }

    @PostMapping("/peers")
    fun registerPeer(
        @RequestBody payload: Map<String, String>,
    ): ResponseEntity<String> {
        val url = payload["url"] ?: return ResponseEntity.badRequest().body("Falta el campo 'url'")
        nodeService.addPeer(url)
        return ResponseEntity.ok("Peer $url agregado correctamente a la red")
    }

    @PostMapping("/blocks/receive")
    fun receiveExternalBlock(
        @RequestBody block: Block,
    ): ResponseEntity<Any> =
        when (val result = nodeService.receiveBlock(block)) {
            is OperationResult.Success ->
                ResponseEntity.ok(
                    "Bloque aceptado y añadido a la cadena local",
                )
            is OperationResult.Failure -> ResponseEntity.badRequest().body(result.errors)
        }
}
