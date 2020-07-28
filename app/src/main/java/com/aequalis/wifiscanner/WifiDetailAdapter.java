package com.aequalis.wifiscanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class WifiDetailAdapter extends RecyclerView.Adapter<WifiDetailAdapter.CustomViewHolder> {

    private static final String TAG = "WifiDetailAdapter";
    List<NetworkModel> mNetworkList = new ArrayList<>();
    ItemClickListener mItemClickListener;

    public WifiDetailAdapter(ItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }

    public void setData(List<NetworkModel> networkList) {
        DiffUtil.DiffResult updatedList = DiffUtil.calculateDiff(new MyDiffCallback(networkList, this.mNetworkList));
        this.mNetworkList.clear();
        this.mNetworkList.addAll(networkList);
        updatedList.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_network, parent, false);
        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        NetworkModel model = mNetworkList.get(position);
        holder.txtNetworkName.setText(model.getNetworkName());
        holder.txtNetworkMac.setText(model.getNetworkMac());
//        switch (model.getConnectionStatus()) {
//            case CONNECTED:
//                holder.btnAction.setText("Disconnect");
//                break;
//            case CONNECTING:
//                holder.btnAction.setText("Connecting");
//                break;
//            case DISCONNECTED:
//                holder.btnAction.setText("Connect");
//                break;
//        }
    }

    @Override
    public int getItemCount() {
        return mNetworkList.size();
    }

    public static class MyDiffCallback extends DiffUtil.Callback {

        List<NetworkModel> oldList;
        List<NetworkModel> newList;

        MyDiffCallback(List<NetworkModel> newList, List<NetworkModel> oldList) {
            this.newList = newList;
            this.oldList = oldList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getNetworkMac().equals(newList.get(newItemPosition).getNetworkMac());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            NetworkModel oldItem = oldList.get(oldItemPosition);
            NetworkModel newItem = newList.get(newItemPosition);
            return (oldItem == newItem);
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            //you can return particular field for changed item.
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }

    public class CustomViewHolder extends RecyclerView.ViewHolder {
        TextView txtNetworkName, txtNetworkMac;
        Button btnAction;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNetworkName = itemView.findViewById(R.id.txtNetworkName);
            txtNetworkMac = itemView.findViewById(R.id.txtMacAddress);
            btnAction = itemView.findViewById(R.id.btnAction);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    NetworkModel model = mNetworkList.get(getAdapterPosition());
                    mItemClickListener.OnItemClick(getAdapterPosition(),model.getConnectionStatus());
                }
            });
        }
    }
}
