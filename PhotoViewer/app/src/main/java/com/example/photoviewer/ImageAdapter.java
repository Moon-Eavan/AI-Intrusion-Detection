package com.example.photoviewer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.photoviewer.model.PostItem;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private List<PostItem> postItemList;
    private final OnFavoriteToggleListener favoriteToggleListener;

    public interface OnFavoriteToggleListener {
        void onFavoriteToggle(PostItem postItem);
    }

    public ImageAdapter(List<PostItem> postItemList, OnFavoriteToggleListener favoriteToggleListener) {
        this.postItemList = postItemList;
        this.favoriteToggleListener = favoriteToggleListener;
    }

    /** 검색/정렬/필터 결과로 화면에 표시할 목록을 교체한다. */
    public void updateItems(List<PostItem> newItems) {
        this.postItemList = newItems;
        notifyDataSetChanged();
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 게시물 항목을 나타낼 뷰 생성
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        PostItem postItem = postItemList.get(position);

        holder.titleView.setText(postItem.getTitle());
        holder.textView.setText(postItem.getText());
        holder.dateView.setText(postItem.getDisplayDate());
        holder.favoriteIcon.setText(postItem.isFavorite() ? "♥" : "♡");

        if (postItem.getBitmap() != null) {
            holder.imageView.setImageBitmap(postItem.getBitmap());
            holder.imageView.setVisibility(View.VISIBLE);
            holder.noImageLabel.setVisibility(View.GONE);
        } else {
            // 이미지가 없거나 다운로드에 실패한 게시물도 안전하게 표시한다.
            holder.imageView.setImageBitmap(null);
            holder.imageView.setVisibility(View.GONE);
            holder.noImageLabel.setVisibility(View.VISIBLE);
        }

        // 이미지 유무와 상관없이 즐겨찾기 아이콘은 항상 동작해야 한다 (이미지 없는 게시물도 즐겨찾기 가능).
        holder.favoriteIcon.setOnClickListener(v -> {
            if (favoriteToggleListener != null) {
                favoriteToggleListener.onFavoriteToggle(postItem);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, DetailActivity.class);
            // Bitmap은 직접 전달하지 않고, 상세 화면에서 imageUrl로 다시 받아온다.
            intent.putExtra(DetailActivity.EXTRA_TITLE, postItem.getTitle());
            intent.putExtra(DetailActivity.EXTRA_TEXT, postItem.getText());
            intent.putExtra(DetailActivity.EXTRA_DATE, postItem.getDisplayDate());
            intent.putExtra(DetailActivity.EXTRA_IMAGE_URL, postItem.getImageUrl());
            intent.putExtra(DetailActivity.EXTRA_FAVORITE, postItem.isFavorite());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return postItemList.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView noImageLabel;
        TextView titleView;
        TextView textView;
        TextView dateView;
        TextView favoriteIcon;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
            noImageLabel = itemView.findViewById(R.id.noImageLabelItem);
            titleView = itemView.findViewById(R.id.itemTitle);
            textView = itemView.findViewById(R.id.itemText);
            dateView = itemView.findViewById(R.id.itemDate);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
        }
    }
}
