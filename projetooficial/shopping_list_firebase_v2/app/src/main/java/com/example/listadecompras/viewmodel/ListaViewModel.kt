package com.example.listadecompras.viewmodel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Log
import com.google.firebase.firestore.ListenerRegistration
import com.example.listadecompras.ListaCompra
import com.example.listadecompras.data.FirebaseRepository


class ListaViewModel : ViewModel() {

    private val repository = FirebaseRepository()
    private var listasListenerRegistration: ListenerRegistration? = null

    private val _listas = MutableLiveData<List<ListaCompra>>()
    val listas: LiveData<List<ListaCompra>> = _listas
    private val _createResult = MutableLiveData<Result<String>>()
    val createResult: LiveData<Result<String>> = _createResult
    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult
    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    fun startListasListener() {
        if (listasListenerRegistration != null) return

        listasListenerRegistration = repository.getListas(
            onSuccess = {
                Log.d("ListaViewModel", "Atualização de listas recebida. Tamanho: ${it.size}")
                _listas.value = it
            },
            onFailure = { Log.e("ListaViewModel", "Erro ao carregar listas: ${it.message}") }
        )
    }
    fun stopListasListener() {
        listasListenerRegistration?.remove()
        listasListenerRegistration = null
    }
    override fun onCleared() {
        super.onCleared()
        stopListasListener()
    }

    fun updateLista(lista: ListaCompra) {
        repository.updateLista(
            lista = lista,
            onSuccess = { _updateResult.value = Result.success(Unit) },
            onFailure = { _updateResult.value = Result.failure(it) }
        )
    }

    fun createLista(titulo: String) {
        val novaLista = ListaCompra(titulo = titulo)
        repository.createLista(
            lista = novaLista,
            onSuccess = { _createResult.value = Result.success(it) },
            onFailure = { _createResult.value = Result.failure(it) }
        )
    }
    fun deleteLista(listaId: String) {
        repository.deleteLista(
            listaId = listaId,
            onSuccess = { _deleteResult.value = Result.success(Unit) },
            onFailure = { _deleteResult.value = Result.failure(it) }
        )
    }

    fun searchListas(query: String) {
        repository.searchListas(
            query = query,
            onSuccess = { _listas.value = it },
            onFailure = { /* Tratar erro de busca */ }
        )
    }
}
