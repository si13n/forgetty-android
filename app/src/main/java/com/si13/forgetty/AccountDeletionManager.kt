package com.si13.forgetty

import android.content.Context
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AccountDeletionManager(context: Context) {
    private val appContext = context.applicationContext
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun delete(user: FirebaseUser) {
        val remoteTasks = RemoteTaskDataSource(firestore, user.uid)
        remoteTasks.deleteAll()
        firestore.collection("users").document(user.uid).delete().await()
        user.delete().await()
        clearLocalData()
    }

    private suspend fun clearLocalData() {
        TaskDatabase.getInstance(appContext).taskDao().deleteAll()
        AuthRepository(appContext).clear()
        ForgettyPreferences.create(appContext).clear()
        appContext.getSharedPreferences("forgetty_task_lists", Context.MODE_PRIVATE)
            .edit().clear().commit()
        appContext.getSharedPreferences("forgetty_task_tags", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }
}
