package com.example.mobil_uygulama.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mobil_uygulama.databinding.LogItemBinding

class LogAdapter(private val items: List<LogItem>) : RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    // Artık findViewById yok. Tamamen tip güvenli (type-safe) ViewBinding kullanıyoruz.
    class ViewHolder(val binding: LogItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Verileri XML'deki ilgili ID'lere doğrudan basıyoruz
        holder.binding.labelText.text = item.label
        holder.binding.confText.text = "Doğruluk: %%${String.format("%.0f", item.confidence * 100)}" // %95 formatında gösterir
        holder.binding.timeText.text = item.timestamp // Saati temiz bir şekilde atıyoruz
        holder.binding.imageView.setImageBitmap(item.image)
    }

    override fun getItemCount() = items.size
}