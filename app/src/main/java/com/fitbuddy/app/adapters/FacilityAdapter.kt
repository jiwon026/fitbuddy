package com.fitbuddy.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fitbuddy.app.databinding.ItemFacilityBinding
import com.fitbuddy.app.network.FacilityDto

class FacilityAdapter(
    private val facilities: List<FacilityDto>,
    private val onItemClicked: (FacilityDto) -> Unit
) : RecyclerView.Adapter<FacilityAdapter.FacilityViewHolder>() {

    inner class FacilityViewHolder(private val binding: ItemFacilityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(facility: FacilityDto) {
            binding.tvFacilityName.text = facility.name
            binding.tvFacilityAddress.text = facility.address
            binding.tvFacilityDistance.text = String.format("%.1f km", facility.distance_km)
            
            // 아이템 뷰 클릭 시 람다 함수 호출
            itemView.setOnClickListener { onItemClicked(facility) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FacilityViewHolder {
        val binding = ItemFacilityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FacilityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FacilityViewHolder, position: Int) {
        holder.bind(facilities[position])
    }

    override fun getItemCount() = facilities.size
}
