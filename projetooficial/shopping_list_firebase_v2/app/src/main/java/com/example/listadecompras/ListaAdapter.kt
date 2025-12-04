package com.example.listadecompras
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.listadecompras.databinding.ItemListaBinding
import com.example.listadecompras.viewmodel.ListaViewModel


class ListaAdapter(
    private var listaDeCompras: MutableList<ListaCompra>,
    private val abrirItens: (ListaCompra) -> Unit,
    private val viewModel: ListaViewModel
) : RecyclerView.Adapter<ListaAdapter.ListaViewHolder>() {

    fun updateList(newList: List<ListaCompra>) {
        val diffCallback = ListaDiffCallback(this.listaDeCompras, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.listaDeCompras.clear()
        this.listaDeCompras.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListaViewHolder {
        val binding = ItemListaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListaViewHolder, position: Int) {
        val listaCompra = listaDeCompras[position]
        holder.bind(listaCompra)
    }

    override fun getItemCount(): Int = listaDeCompras.size

    inner class ListaViewHolder(private val binding: ItemListaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(listaCompra: ListaCompra) {
            binding.titulo.text = listaCompra.titulo
            binding.imgLista.setImageResource(R.drawable.placeholder)

            binding.root.setOnClickListener {
                abrirItens(listaCompra)
            }

            binding.root.setOnLongClickListener {
                showEditDeleteDialog(listaCompra)
                true
            }
        }

        private fun showEditDeleteDialog(listaCompra: ListaCompra) {
            val context = binding.root.context
            val options = arrayOf("Editar Título", "Excluir Lista")

            AlertDialog.Builder(context)
                .setTitle("Opções para ${listaCompra.titulo}")
                .setItems(options) { dialog, which ->
                    when (which) {
                        0 -> showEditTitleDialog(listaCompra)
                        1 -> showDeleteConfirmationDialog(listaCompra)
                    }
                }
                .show()
        }

        private fun showEditTitleDialog(listaCompra: ListaCompra) {
            val context = binding.root.context
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Editar Título da Lista")

            val input = EditText(context)
            input.setText(listaCompra.titulo)
            builder.setView(input)

            builder.setPositiveButton("Salvar") { dialog, _ ->
                val novoTitulo = input.text.toString().trim()
                if (novoTitulo.isNotEmpty()) {
                    listaCompra.titulo = novoTitulo
                    viewModel.updateLista(listaCompra)
                  notifyItemChanged(adapterPosition)
                } else {
                    Toast.makeText(context, "O título não pode ser vazio!", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }

            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
        }

        private fun showDeleteConfirmationDialog(listaCompra: ListaCompra) {
            val context = binding.root.context
            AlertDialog.Builder(context)
                .setTitle("Excluir Lista")
                .setMessage("Tem certeza que deseja excluir a lista '${listaCompra.titulo}' e todos os seus itens?")
                .setPositiveButton("Excluir") { _, _ ->
                    viewModel.deleteLista(listaCompra.id)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}


class ListaDiffCallback(
    private val oldList: List<ListaCompra>,
    private val newList: List<ListaCompra>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.titulo == newItem.titulo
    }
}
