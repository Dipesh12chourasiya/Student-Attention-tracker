package com.example.detectfaceandexpression.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.detectfaceandexpression.models.SessionData
import com.example.irlstudentattentiontracker.databinding.ItemSessionCardBinding


class SessionAdapter(
    private val onItemClick: (SessionData) -> Unit,
    private val onItemLongClick: (SessionData) -> Unit
) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    private val diffUtil = object : DiffUtil.ItemCallback<SessionData>() {
        override fun areItemsTheSame(oldItem: SessionData, newItem: SessionData): Boolean {
            return oldItem.sessionId == newItem.sessionId
        }

        override fun areContentsTheSame(oldItem: SessionData, newItem: SessionData): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffUtil)

    inner class SessionViewHolder(private val binding: ItemSessionCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(session: SessionData) {
            binding.tvSessionName.text = session.title
            binding.tvSessionDate.text = session.dateTime
            binding.tvSessionDuration.text = session.duration

            binding.root.setOnLongClickListener {
                onItemLongClick(session)
                true
            }

            binding.root.setOnClickListener {
                onItemClick(session)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    override fun getItemCount(): Int = differ.currentList.size
}
