package com.example.listadecompras.data
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import android.util.Log
import com.example.listadecompras.ListaCompra
import com.example.listadecompras.Item


class FirebaseRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val usersCollection = firestore.collection("users")
    private val listsCollection = firestore.collection("shopping_lists")


    fun getCurrentUser() = auth.currentUser
    fun registerUser(email: String, password: String, name: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
              val userId = auth.currentUser?.uid ?: ""
                 saveUserName(userId, name, onSuccess = { onSuccess(userId) }, onFailure = onFailure)
                } else {
                    onFailure(task.exception ?: Exception("Erro desconhecido ao cadastrar"))
                }
            }
    }

    private fun saveUserName(userId: String, name: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userMap = hashMapOf("name" to name)
        usersCollection.document(userId).set(userMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun loginUser(email: String, password: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
                } else {
                    onFailure(task.exception ?: Exception("Erro desconhecido ao logar"))
                }
            }
    }
    fun logoutUser() {
        auth.signOut()
    }
    fun resetPassword(email: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception ?: Exception("Erro desconhecido ao enviar e-mail de recuperação"))
                }
            }
    }



    fun createLista(lista: ListaCompra, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onFailure(Exception("Usuário não logado"))
        lista.userId = userId
        listsCollection.add(lista)
            .addOnSuccessListener { documentReference ->
                documentReference.update("id", documentReference.id)
                    .addOnSuccessListener { onSuccess(documentReference.id) }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }


    fun getListas(onSuccess: (List<ListaCompra>) -> Unit, onFailure: (Exception) -> Unit): ListenerRegistration {
        val userId = auth.currentUser?.uid ?: run {
            onFailure(Exception("Usuário não logado"))
            return object : ListenerRegistration {
                override fun remove() {}
            }
        }
        return listsCollection
            .whereEqualTo("userId", userId)
            .orderBy("titulo")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                  Log.e("FirebaseRepository", "Erro ao ouvir listas: ${firebaseFirestoreException.message}", firebaseFirestoreException)
                    onFailure(firebaseFirestoreException)
                    return@addSnapshotListener
                }

                val listas = querySnapshot?.documents?.mapNotNull { document ->
                    document.toObject(ListaCompra::class.java)?.apply { id = document.id }
                } ?: emptyList()
                Log.d("FirebaseRepository", "Listas recebidas: ${listas.size}")
                onSuccess(listas)
            }
    }

    fun updateLista(lista: ListaCompra, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        listsCollection.document(lista.id).set(lista)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteLista(listaId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        listsCollection.document(listaId).delete()
            .addOnSuccessListener {
             deleteItensFromLista(listaId, onSuccess, onFailure)
            }
            .addOnFailureListener { onFailure(it) }
    }

    private fun getItemsCollection(listaId: String) = listsCollection.document(listaId).collection("itens")

    fun createItem(listaId: String, item: Item, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        item.listaId = listaId
        getItemsCollection(listaId).add(item)
            .addOnSuccessListener { documentReference ->
                documentReference.update("id", documentReference.id)
                    .addOnSuccessListener { onSuccess(documentReference.id) }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }
    fun getItemsFromLista(listaId: String, onSuccess: (List<Item>) -> Unit, onFailure: (Exception) -> Unit): ListenerRegistration {
        return getItemsCollection(listaId)
            .orderBy("categoria")
            .orderBy("nome")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                 Log.e("FirebaseRepository", "Erro ao ouvir itens: ${firebaseFirestoreException.message}", firebaseFirestoreException)
                  onFailure(firebaseFirestoreException)
                    return@addSnapshotListener
                }

                val itens = querySnapshot?.documents?.mapNotNull { document ->
                    document.toObject(Item::class.java)?.apply { id = document.id }
                } ?: emptyList()
                Log.d("FirebaseRepository", "Itens recebidos para lista $listaId: ${itens.size}")
                onSuccess(itens)
            }
    }

    fun updateItem(listaId: String, item: Item, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        getItemsCollection(listaId).document(item.id).set(item)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteItem(listaId: String, itemId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        getItemsCollection(listaId).document(itemId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    private fun deleteItensFromLista(listaId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        getItemsCollection(listaId).get()
            .addOnSuccessListener { querySnapshot ->
                val batch = firestore.batch()
                querySnapshot.documents.forEach { document ->
                    batch.delete(document.reference)
                }
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun searchListas(query: String, onSuccess: (List<ListaCompra>) -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onFailure(Exception("Usuário não logado"))
        listsCollection
            .whereEqualTo("userId", userId)
            .orderBy("titulo")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .get()
            .addOnSuccessListener { querySnapshot ->
           val listas = querySnapshot.documents.mapNotNull { document ->
            document.toObject(ListaCompra::class.java)?.apply { id = document.id }
             }
              onSuccess(listas)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun searchItens(listaId: String, query: String, onSuccess: (List<Item>) -> Unit, onFailure: (Exception) -> Unit) {
        getItemsCollection(listaId)
            .orderBy("nome")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val itens = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(Item::class.java)?.apply { id = document.id }
                }
                onSuccess(itens)
            }
            .addOnFailureListener { onFailure(it) }
    }
}
