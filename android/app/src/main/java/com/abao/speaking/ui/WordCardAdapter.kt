package com.abao.speaking.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.abao.speaking.databinding.ItemWordCardBinding

class WordCardAdapter(
    private val items: List<Pair<String, String>>,
    private val onSpeak: (String) -> Unit
) : RecyclerView.Adapter<WordCardAdapter.Holder>() {

    class Holder(val binding: ItemWordCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemWordCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val (word, zh) = items[position]
        holder.binding.wordTitle.text = word
        holder.binding.wordZh.text = zh
        // script.js: data-speak 按钮点击 -> speak(word)
        holder.binding.speakButton.setOnClickListener { onSpeak(word) }
    }

    override fun getItemCount() = items.size
}
