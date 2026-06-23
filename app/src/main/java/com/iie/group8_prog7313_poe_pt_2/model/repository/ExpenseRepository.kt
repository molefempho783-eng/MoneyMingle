package com.iie.group8_prog7313_poe_pt_2.model.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.iie.group8_prog7313_poe_pt_2.model.entity.Expense
import com.iie.group8_prog7313_poe_pt_2.util.ReceiptImageManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class CategoryTotal(
    val categoryId: String?,
    val total: Double
)

class ExpenseRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun expensesRef(userId: String) =
        db.collection("users").document(userId).collection("expenses")

    suspend fun insert(userId: String, expense: Expense): String {
        val ref = expensesRef(userId).add(expense).await()
        return ref.id
    }

    suspend fun update(userId: String, expense: Expense) {
        expensesRef(userId).document(expense.id).set(expense).await()
    }

    suspend fun delete(userId: String, expense: Expense) {
        ReceiptImageManager.deleteReceiptImage(expense.receiptImagePath)
        expensesRef(userId).document(expense.id).delete().await()
    }

    suspend fun getById(userId: String, expenseId: String): Expense? {
        val snap = expensesRef(userId).document(expenseId).get().await()
        return snap.toObject(Expense::class.java)
    }

    fun getAllByUser(userId: String): Flow<List<Expense>> = callbackFlow {
        val listener = expensesRef(userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Expense::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<Expense>> = callbackFlow {
        val listener = expensesRef(userId)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Expense::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun getByUserAndDateRange(userId: String, startDate: Long, endDate: Long): List<Expense> {
        val snap = expensesRef(userId)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .get()
            .await()
        return snap.toObjects(Expense::class.java)
    }

    // Firestore has no GROUP BY, so we fetch and aggregate in memory
    suspend fun getCategoryTotals(userId: String, startDate: Long, endDate: Long): List<CategoryTotal> {
        val snap = expensesRef(userId)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .get()
            .await()
        return snap.toObjects(Expense::class.java)
            .groupBy { it.categoryId }
            .map { (catId, expenses) -> CategoryTotal(catId, expenses.sumOf { it.amount }) }
    }
}
