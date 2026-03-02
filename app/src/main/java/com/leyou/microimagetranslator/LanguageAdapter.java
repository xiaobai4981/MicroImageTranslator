package com.leyou.microimagetranslator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Set;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder> {

    public interface OnItemClickListener {
        void onDownloadClick(Language language);
        void onDeleteClick(Language language);
    }

    private final List<Language> languages;
    private final Set<String> downloadedLanguages;
    private final OnItemClickListener listener;

    public LanguageAdapter(List<Language> languages, Set<String> downloadedLanguages, OnItemClickListener listener) {
        this.languages = languages;
        this.downloadedLanguages = downloadedLanguages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.language_item, parent, false);
        return new LanguageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
        Language language = languages.get(position);
        holder.bind(language, downloadedLanguages.contains(language.code), listener);
    }

    @Override
    public int getItemCount() {
        return languages.size();
    }

    static class LanguageViewHolder extends RecyclerView.ViewHolder {
        private final TextView languageNameTextView;
        private final ImageButton actionButton;

        public LanguageViewHolder(@NonNull View itemView) {
            super(itemView);
            languageNameTextView = itemView.findViewById(R.id.languageNameTextView);
            actionButton = itemView.findViewById(R.id.actionButton);
        }

        public void bind(final Language language, boolean isDownloaded, final OnItemClickListener listener) {
            languageNameTextView.setText(language.displayName);
            if (isDownloaded) {
                actionButton.setImageResource(android.R.drawable.ic_menu_delete);
                actionButton.setOnClickListener(v -> listener.onDeleteClick(language));
            } else {
                actionButton.setImageResource(R.drawable.ic_download);
                actionButton.setOnClickListener(v -> listener.onDownloadClick(language));
            }
        }
    }
}
