package com.iie.group8_prog7313_poe_pt_2.model.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.iie.group8_prog7313_poe_pt_2.model.entity.Contribution
import com.iie.group8_prog7313_poe_pt_2.model.entity.SavingsGoal
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SavingsGoalRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun savingsGoalsRef(userId: String) =
        db.collection("users").document(userId).collection("savingsGoals")

    private fun contributionsRef(userId: String, goalId: String) =
        savingsGoalsRef(userId).document(goalId).collection("contributions")

    suspend fun insert(userId: String, goal: SavingsGoal): String {
        val ref = savingsGoalsRef(userId).add(goal).await()
        return ref.id
    }

    suspend fun update(userId: String, goal: SavingsGoal) {
        savingsGoalsRef(userId).document(goal.id).set(goal).await()
    }

    suspend fun delete(userId: String, goalId: String) {
        val contributions = contributionsRef(userId, goalId).get().await()
        val batch = db.batch()
        contributions.documents.forEach { batch.delete(it.reference) }
        batch.delete(savingsGoalsRef(userId).document(goalId))
        batch.commit().await()
    }

    suspend fun getById(userId: String, goalId: String): SavingsGoal? {
        val snap = savingsGoalsRef(userId).document(goalId).get().await()
        return snap.toObject(SavingsGoal::class.java)
    }

    fun getAllByUser(userId: String): Flow<List<SavingsGoal>> = callbackFlow {
        val listener = savingsGoalsRef(userId)
            .orderBy("targetDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(SavingsGoal::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun addContribution(userId: String, goalId: String, amount: Double) {
        val goalRef = savingsGoalsRef(userId).document(goalId)
        val contributionRef = contributionsRef(userId, goalId).document()

        db.runTransaction { transaction ->
            val snapshot = transaction.get(goalRef)
            val currentSaved = snapshot.toObject(SavingsGoal::class.java)?.savedAmount ?: 0.0
            transaction.set(
                contributionRef,
                Contribution(amount = amount, date = System.currentTimeMillis())
            )
            transaction.update(goalRef, "savedAmount", currentSaved + amount)
        }.await()
    }
}
