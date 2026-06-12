package com.stoganet.tv.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.stoganet.tv.proto.Tokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File

class TokenStore(private val dataStore: DataStore<Tokens>) {

    val tokens: Flow<Tokens> = dataStore.data

    suspend fun current(): Tokens = tokens.first()

    suspend fun save(access: String, refresh: String, userId: String, displayName: String) {
        dataStore.updateData {
            Tokens.newBuilder()
                .setAccess(access)
                .setRefresh(refresh)
                .setUserId(userId)
                .setDisplayName(displayName)
                .build()
        }
    }

    suspend fun clear() {
        dataStore.updateData { Tokens.getDefaultInstance() }
    }

    companion object {
        fun create(context: Context): TokenStore {
            val ds = DataStoreFactory.create(
                serializer = TokensSerializer(context),
                produceFile = { File(context.filesDir, "stoganet-tokens.pb") },
            )
            return TokenStore(ds)
        }
    }
}
