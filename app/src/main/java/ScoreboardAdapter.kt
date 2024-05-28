package com.example.med6grp5supercykelstig

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScoreboardAdapter(var userList: List<Pair<String, String>>) : RecyclerView.Adapter<ScoreboardAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.scoreboard_user_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = userList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = userList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: Pair<String, String>) {
            val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
            val contributionsTextView: TextView = itemView.findViewById(R.id.contributionTextView)
            usernameTextView.text = item.first
            contributionsTextView.text = item.second
        }
    }
}