package com.example.listadecompras
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.listadecompras.databinding.ActivityItemBinding
import com.example.listadecompras.databinding.DialogEditItemBinding
import androidx.activity.viewModels
import com.example.listadecompras.viewmodel.ItemViewModel
import android.util.Log

class ItemActivity : AppCompatActivity() {

    private val itemViewModel: ItemViewModel by viewModels()

    private lateinit var binding: ActivityItemBinding
    private lateinit var itemAdapter: ItemAdapter
    private var listaId: String? = null
    private var listaTitulo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        loadListData()
        setupUI()
        setupListeners()
        setupObservers()
        listaId?.let { itemViewModel.startItensListener(it) }
    }
    private fun loadListData() {
        listaId = intent.getStringExtra("LISTA_ID")
        listaTitulo = intent.getStringExtra("TITULO_LISTA")
        title = listaTitulo ?: "Detalhes da Lista"

        if (listaId.isNullOrEmpty()) {
           Toast.makeText(this, "ID da Lista não encontrado!", Toast.LENGTH_SHORT).show()
          finish()
          return
        }
    }



    private fun setupUI() {
        itemAdapter = ItemAdapter(mutableListOf(),
            onEditClick = { item -> showEditItemDialog(item) },
            onDeleteClick = { item -> deleteItem(item) },
            onCheckedChange = { item, isChecked -> toggleItemPurchased(item, isChecked) }
        )
        binding.recyclerViewItens.apply {
            layoutManager = LinearLayoutManager(this@ItemActivity)
            adapter = itemAdapter
        }
    }

    private fun setupListeners() {
        binding.fabAddItem.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            intent.putExtra("LISTA_ID", listaId)
            intent.putExtra("TITULO_LISTA", listaTitulo)
            startActivity(intent)
        }
        

        binding.edtSearchItem.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                listaId?.let { id ->

                    if (query.isEmpty()) {
                        itemViewModel.startItensListener(id)
                    } else {
                        itemViewModel.stopItensListener()
                        itemViewModel.searchItens(id, query)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupObservers() {
        itemViewModel.itens.observe(this) { itens ->
            itemAdapter.updateList(itens.toMutableList())
        }


        itemViewModel.createResult.observe(this) { result ->
            result.onSuccess { itemId ->
                Toast.makeText(this, "Item criado com sucesso! ID: $itemId", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Log.e("ItemActivity", "Erro ao criar item: ${it.message}", it)
                Toast.makeText(this, "Erro ao criar item: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }



        itemViewModel.updateResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Item atualizado com sucesso!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Log.e("ItemActivity", "Erro ao atualizar item: ${it.message}", it)
                Toast.makeText(this, "Erro ao atualizar item: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }



        itemViewModel.deleteResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Item excluído com sucesso!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Log.e("ItemActivity", "Erro ao excluir item: ${it.message}", it)
                Toast.makeText(this, "Erro ao excluir item: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }



    private fun showEditItemDialog(item: Item) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar Item")

        val dialogBinding = DialogEditItemBinding.inflate(LayoutInflater.from(this))

        dialogBinding.edtNomeEdit.setText(item.nome)
        dialogBinding.edtQuantidadeEdit.setText(item.quantidade.toString())
        dialogBinding.edtUnidadeEdit.setText(item.unidade)
        ArrayAdapter.createFromResource(
            this,
            R.array.categorias_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dialogBinding.spinnerCategoriaEdit.adapter = adapter
            val spinnerPosition = adapter.getPosition(item.categoria)
            dialogBinding.spinnerCategoriaEdit.setSelection(spinnerPosition)
        }

        builder.setView(dialogBinding.root)

        builder.setPositiveButton("Salvar") { dialog, _ ->
            val novoNome = dialogBinding.edtNomeEdit.text.toString().trim()
            val novaQuantidade = dialogBinding.edtQuantidadeEdit.text.toString().toIntOrNull() ?: 0
            val novaUnidade = dialogBinding.edtUnidadeEdit.text.toString().trim()
            val novaCategoria = dialogBinding.spinnerCategoriaEdit.selectedItem.toString()

            if (novoNome.isNotEmpty() && novaQuantidade > 0 && novaUnidade.isNotEmpty()) {
                item.nome = novoNome
                item.quantidade = novaQuantidade
                item.unidade = novaUnidade
                item.categoria = novaCategoria
                listaId?.let { itemViewModel.updateItem(it, item) }
            } else {
                Toast.makeText(this, "Preencha todos os campos corretamente", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun deleteItem(item: Item) {
        listaId?.let { itemViewModel.deleteItem(it, item.id) }
    }

    private fun toggleItemPurchased(item: Item, isChecked: Boolean) {
        item.comprado = isChecked
        listaId?.let { itemViewModel.updateItem(it, item) }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
