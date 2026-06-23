package com.iie.group8_prog7313_poe_pt_2.model.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.iie.group8_prog7313_poe_pt_2.model.entity.Subscription
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SubscriptionRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun subscriptionsRef(userId: String) =
        db.collection("users").document(userId).collection("subscriptions")

    suspend fun insert(userId: String, subscription: Subscription): String {
        val ref = subscriptionsRef(userId).add(subscription).await()
        return ref.id
    }

    suspend fun update(userId: String, subscription: Subscription) {
        subscriptionsRef(userId).document(subscription.id).set(subscription).await()
    }

    suspend fun delete(userId: String, subscriptionId: String) {
        subscriptionsRef(userId).document(subscriptionId).delete().await()
    }

    suspend fun getById(userId: String, subscriptionId: String): Subscription? {
        val snap = subscriptionsRef(userId).document(subscriptionId).get().await()
        return snap.toObject(Subscription::class.java)
    }

    fun getAllByUser(userId: String): Flow<List<Subscription>> = callbackFlow {
        val listener = subscriptionsRef(userId)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Subscription::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }
}