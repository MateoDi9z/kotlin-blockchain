package org.example.crypto.hash

import api.entities.crypto.hash.Hash
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class HashTest {

    @Test
    fun sha256OfHello() {
        val hash = Hash.sha256("hello")

        assertEquals(64, hash.length)
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            hash,
        )
    }

    @Test
    fun sha256OfEmptyString() {
        val hash = Hash.sha256("")

        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hash,
        )
    }

    @Test
    fun deterministicHash() {
        val h1 = Hash.sha256("blockchain")
        val h2 = Hash.sha256("blockchain")

        assertEquals(h1, h2)
    }

    @Test
    fun differentInputsDifferentHashes() {
        val h1 = Hash.sha256("tx1")
        val h2 = Hash.sha256("tx2")

        assertNotEquals(h1, h2)
    }
}
