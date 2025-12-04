package com.example.listadecompras
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.listadecompras.databinding.ActivityMainBinding
import androidx.activity.viewModels
import com.example.listadecompras.viewmodel.UserViewModel
import com.example.listadecompras.viewmodel.ListaViewModel


class MainActivity : AppCompatActivity() {

    private val userViewModel: UserViewModel by viewModels()
    private val listaViewModel: ListaViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding
    private lateinit var listaAdapter: ListaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!userViewModel.isUserLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        setupListeners()
        setupObservers()
        listaViewModel.startListasListener()
    }

    

    private fun setupRecyclerView() {
        listaAdapter = ListaAdapter(mutableListOf(), { listaCompra -> abrirItensDaLista(listaCompra) }, listaViewModel)
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = listaAdapter
        }
    }

    private fun setupListeners() {
        binding.fabAddLista.setOnClickListener {
            showCreateListDialog()
        }

        binding.btnLogout.setOnClickListener {
            userViewModel.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Toast.makeText(this, "Logout realizado com sucesso!", Toast.LENGTH_SHORT).show()
        }

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    listaViewModel.startListasListener()
                } else {
                    listaViewModel.stopListasListener()
                    listaViewModel.searchListas(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupObservers() {
        listaViewModel.listas.observe(this) { listas ->
            listaAdapter.updateList(listas.toMutableList())
        }

        listaViewModel.createResult.observe(this) { result ->
            result.onSuccess { listaId ->
                Toast.makeText(this, "Lista criada com sucesso!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "Erro ao criar lista: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        listaViewModel.updateResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Lista atualizada com sucesso!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "Erro ao atualizar lista: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        listaViewModel.deleteResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Lista excluída com sucesso!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "Erro ao excluir lista: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showCreateListDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Criar Nova Lista de Compras")
        val input = EditText(this)
        input.hint = "Título da Lista"
        builder.setView(input)
        builder.setPositiveButton("Criar") { dialog, _ ->
            val titulo = input.text.toString().trim()
            if (titulo.isNotEmpty()) {
                listaViewModel.createLista(titulo)
            } else {
                Toast.makeText(this, "O título não pode ser vazio!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun abrirItensDaLista(listaCompra: ListaCompra) {
        val intent = Intent(this, ItemActivity::class.java)
        intent.putExtra("LISTA_ID", listaCompra.id)
        intent.putExtra("TITULO_LISTA", listaCompra.titulo)
        startActivity(intent)
    }
}
