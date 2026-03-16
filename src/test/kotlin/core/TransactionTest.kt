package org.example.core

import org.example.core.transaction.Transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TransactionTest {
    private fun tx(a: String, b: String, amount: Double): Transaction {
        return Transaction(a, b, amount)
    }

    @Test
    fun serialize() {
        val tx = tx("X", "Y", 5.0)

        assertEquals(5000000, tx.value)
        assertEquals(5.0, tx.getRealValue(), 1e-9)
    }

    @Test
    fun serialize2() {
        val tx = tx("Xx", "Yy", 4125.2370)

        assertEquals(4125237000, tx.value)
        assertEquals(4125.2370, tx.getRealValue(), 1e-9)
    }
}