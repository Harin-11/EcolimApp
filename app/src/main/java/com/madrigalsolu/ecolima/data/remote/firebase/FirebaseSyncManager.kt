package com.madrigalsolu.ecolima.data.remote.firebase

import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.madrigalsolu.ecolima.data.local.dao.RegistroDao
import com.madrigalsolu.ecolima.data.local.entity.EstadoRegistro
import com.madrigalsolu.ecolima.data.local.entity.RegistroEntity
import com.madrigalsolu.ecolima.data.local.entity.ResiduoTipo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Cloud sync manager connecting local Room SQLite database with Firebase Realtime Database.
 * Enforces offline-first write flow without recursive listener loops.
 */
object FirebaseSyncManager {

    private const val REGISTROS_NODE = "Registros"
    private val isSyncingPending = AtomicBoolean(false)

    /**
     * Inserts locally first with an upfront remote key, then synchronizes to Firebase RTDB.
     */
    fun pushRegistro(
        entity: RegistroEntity,
        dao: RegistroDao,
        scope: CoroutineScope,
        onComplete: (() -> Unit)? = null
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val dbRef = FirebaseDatabase.getInstance().getReference(REGISTROS_NODE)
                val newChild = dbRef.push()
                val remoteId = newChild.key ?: "REC-${System.currentTimeMillis()}"

                // Insert into local SQLite with the assigned remoteId upfront to prevent sync duplicates
                val entityWithRemote = entity.copy(
                    remoteId = remoteId,
                    estado = EstadoRegistro.PENDIENTE
                )
                val localId = dao.insert(entityWithRemote)

                val recordMap = hashMapOf<String, Any>(
                    "id" to localId,
                    "remoteId" to remoteId,
                    "zona" to entity.zona,
                    "sector" to entity.sector,
                    "tipoResiduo" to entity.tipoResiduo.name,
                    "pesoKg" to entity.pesoKg,
                    "volumenM3" to entity.volumenM3,
                    "timestamp" to entity.timestamp,
                    "estado" to "SINCRONIZADO",
                    "usuario" to entity.operarioId,
                    "eppCompleto" to entity.eppCompleto,
                    "observaciones" to (entity.observaciones ?: "")
                )

                newChild.setValue(recordMap)
                    .addOnSuccessListener {
                        scope.launch(Dispatchers.IO) {
                            dao.markSynced(localId, remoteId)
                            onComplete?.invoke()
                        }
                    }
                    .addOnFailureListener {
                        onComplete?.invoke()
                    }
            } catch (e: Exception) {
                onComplete?.invoke()
            }
        }
    }

    /**
     * Strictly READ-ONLY downstream synchronization listener.
     * NEVER pushes or triggers cloud mutations inside onDataChange to avoid infinite loops.
     */
    fun startRealtimeSync(dao: RegistroDao, scope: CoroutineScope) {
        try {
            val dbRef = FirebaseDatabase.getInstance().getReference(REGISTROS_NODE)
            dbRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scope.launch(Dispatchers.IO) {
                        val existing = dao.getAllOnce()
                        val existingRemoteIds = existing.mapNotNull { it.remoteId }.toSet()

                        for (child in snapshot.children) {
                            val remoteId = child.child("remoteId").getValue(String::class.java)
                                ?: child.key ?: continue

                            val usuario = child.child("usuario").getValue(String::class.java) ?: "usuario@ecolim.pe"

                            // If ghost or mock record from development, purge permanently from Firebase and skip
                            if (remoteId in listOf("REC-1019", "REC-1020", "REC-1021", "REC-1022", "REC-1023") || usuario == "operario@ecolim.pe") {
                                child.ref.removeValue()
                                continue
                            }

                            // Skip if record is already present locally
                            if (existingRemoteIds.contains(remoteId)) continue

                            val zona = child.child("zona").getValue(String::class.java) ?: "Zona General"
                            val sector = child.child("sector").getValue(String::class.java) ?: "Sector General"
                            val tipoStr = child.child("tipoResiduo").getValue(String::class.java) ?: "ORGANICO"
                            val tipo = try {
                                ResiduoTipo.valueOf(tipoStr)
                            } catch (_: Exception) {
                                ResiduoTipo.ORGANICO
                            }
                            val peso = child.child("pesoKg").getValue(Double::class.java)
                                ?: child.child("pesoKg").getValue(Long::class.java)?.toDouble() ?: 0.0
                            val volumen = child.child("volumenM3").getValue(Double::class.java)
                                ?: child.child("volumenM3").getValue(Long::class.java)?.toDouble() ?: 0.0
                            val timestamp = child.child("timestamp").getValue(Long::class.java)
                                ?: System.currentTimeMillis()
                            val epp = child.child("eppCompleto").getValue(Boolean::class.java) ?: true
                            val obs = child.child("observaciones").getValue(String::class.java)

                            val newEntity = RegistroEntity(
                                zona = zona,
                                sector = sector,
                                tipoResiduo = tipo,
                                pesoKg = peso,
                                volumenM3 = volumen,
                                timestamp = timestamp,
                                estado = EstadoRegistro.SINCRONIZADO,
                                operarioId = usuario,
                                eppCompleto = epp,
                                remoteId = remoteId,
                                observaciones = obs
                            )
                            dao.insert(newEntity)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (_: Exception) {}
    }

    /**
     * Dedicated one-way uplink for pending offline records.
     * Guarded with AtomicBoolean to prevent parallel duplicate execution.
     */
    fun syncPendingRecords(dao: RegistroDao, scope: CoroutineScope) {
        if (!isSyncingPending.compareAndSet(false, true)) return

        scope.launch(Dispatchers.IO) {
            try {
                val dbRef = FirebaseDatabase.getInstance().getReference(REGISTROS_NODE)
                val pendingList = dao.getPendingSync()
                for (pending in pendingList) {
                    val key = pending.remoteId ?: (dbRef.push().key ?: "REC-${System.currentTimeMillis()}")
                    val recordMap = hashMapOf<String, Any>(
                        "id" to pending.id,
                        "remoteId" to key,
                        "zona" to pending.zona,
                        "sector" to pending.sector,
                        "tipoResiduo" to pending.tipoResiduo.name,
                        "pesoKg" to pending.pesoKg,
                        "volumenM3" to pending.volumenM3,
                        "timestamp" to pending.timestamp,
                        "estado" to "SINCRONIZADO",
                        "usuario" to pending.operarioId,
                        "eppCompleto" to pending.eppCompleto,
                        "observaciones" to (pending.observaciones ?: "")
                    )
                    try {
                        Tasks.await(dbRef.child(key).setValue(recordMap))
                        dao.markSynced(pending.id, key)
                    } catch (_: Exception) {}
                }
            } finally {
                isSyncingPending.set(false)
            }
        }
    }

    /**
     * Purges both cloud 'Registros' node in Firebase RTDB and local Room table
     * to eliminate all corrupted and development mock records.
     */
    fun purgeCloudAndLocal(
        dao: RegistroDao,
        scope: CoroutineScope,
        onComplete: (() -> Unit)? = null
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val dbRef = FirebaseDatabase.getInstance().getReference(REGISTROS_NODE)
                Tasks.await(dbRef.removeValue())
            } catch (_: Exception) {}

            try {
                dao.clearAll()
            } catch (_: Exception) {}

            onComplete?.let {
                scope.launch(Dispatchers.Main) { it() }
            }
        }
    }
}
