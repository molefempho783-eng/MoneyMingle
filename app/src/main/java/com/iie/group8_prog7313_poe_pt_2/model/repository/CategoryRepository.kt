package com.iie.group8_prog7313_poe_pt_2.model.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.iie.group8_prog7313_poe_pt_2.model.entity.BudgetGoal
import com.iie.group8_prog7313_poe_pt_2.model.entity.Category
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CategoryRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun categoriesRef(userId: String) =
        db.collection("users").document(userId).collection("categories")

    private fun budgetGoalsRef(userId: String) =
        db.collection("users").document(userId).collection("budgetGoals")

    // --- Category operations ---

    suspend fun insertCategory(userId: String, category: Category): String {
        val ref = categoriesRef(userId).add(category).await()
        return ref.id
    }

    suspend fun updateCategory(userId: String, category: Category) {
        categoriesRef(userId).document(category.id).set(category).await()
    }

    suspend fun deleteCategory(userId: String, categoryId: String) {
        categoriesRef(userId).document(categoryId).delete().await()
    }

    suspend fun getCategoryById(userId: String, categoryId: String): Category? {
        val snap = categoriesRef(userId).document(categoryId).get().await()
        return snap.toObject(Category::class.java)
    }

    fun getAllCategoriesByUser(userId: String): Flow<List<Category>> = callbackFlow {
        val listener = categoriesRef(userId)
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.toObjects(Category::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // --- Budget Goal operations ---

    suspend fun insertBudgetGoal(userId: String, budgetGoal: BudgetGoal) {
        budgetGoalsRef(userId).add(budgetGoal).await()
    }

    suspend fun updateBudgetGoal(userId: String, budgetGoal: BudgetGoal) {
        budgetGoalsRef(userId).document(budgetGoal.id).set(budgetGoal).await()
    }

    suspend fun deleteBudgetGoal(userId: String, budgetGoalId: String) {
        budgetGoalsRef(userId).document(budgetGoalId).delete().await()
    }

    fun getBudgetGoalsByMonth(userId: String, month: Int, year: Int): Flow<List<BudgetGoal>> = callbackFlow {
        val listener = budgetGoalsRef(userId)
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.toObjects(BudgetGoal::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getBudgetGoalForCategory(
        userId: String,
        categoryId: String,
        month: Int,
        year: Int
    ): BudgetGoal? {
        val snap = budgetGoalsRef(userId)
            .whereEqualTo("categoryId", categoryId)
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .get()
            .await()
        return snap.toObjects(BudgetGoal::class.java).firstOrNull()
    }
}
