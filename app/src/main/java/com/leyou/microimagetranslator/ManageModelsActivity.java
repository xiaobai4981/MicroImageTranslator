package com.leyou.microimagetranslator;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.leyou.microimagetranslator.databinding.ActivityManageModelsBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ManageModelsActivity extends AppCompatActivity implements LanguageAdapter.OnItemClickListener {

    private ActivityManageModelsBinding binding;
    private LanguageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageModelsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.manageModelsToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(binding.recyclerView.getContext(),
                layoutManager.getOrientation());
        binding.recyclerView.addItemDecoration(dividerItemDecoration);

        loadLanguages();

        binding.downloadAllButton.setOnClickListener(v -> showDownloadAllConfirmation());
    }

    private void showDownloadAllConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Download All Models")
                .setMessage("This will download all 59 language models. This can be a very large download (over 2.5 GB) and will use a lot of data and storage. Do you want to proceed?")
                .setPositiveButton("Yes", (dialog, which) -> downloadAllModels())
                .setNegativeButton("No", null)
                .show();
    }

    private void downloadAllModels() {
        showToast("Starting download for all models...");
        binding.downloadAllButton.setEnabled(false); // Disable button during process

        List<String> allLanguageCodes = TranslateLanguage.getAllLanguages();
        final int[] downloadsCompleted = {0};
        final int totalDownloads = allLanguageCodes.size();

        DownloadConditions conditions = new DownloadConditions.Builder().requireWifi().build();

        for (String code : allLanguageCodes) {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(code)
                    .build();
            Translator translator = Translation.getClient(options);

            translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener(v -> {
                        downloadsCompleted[0]++;
                        if (downloadsCompleted[0] == totalDownloads) {
                            showToast("All model downloads attempted.");
                            binding.downloadAllButton.setEnabled(true);
                            loadLanguages(); // Refresh list after all attempts
                        }
                    })
                    .addOnFailureListener(e -> {
                        downloadsCompleted[0]++;
                        showToast("Failed to download " + new Locale(code).getDisplayLanguage() + ": " + e.getMessage());
                        if (downloadsCompleted[0] == totalDownloads) {
                            showToast("All model downloads attempted (some may have failed).");
                            binding.downloadAllButton.setEnabled(true);
                            loadLanguages(); // Refresh list after all attempts
                        }
                    });
        }
    }

    private void loadLanguages() {
        RemoteModelManager.getInstance().getDownloadedModels(TranslateRemoteModel.class)
                .addOnSuccessListener(downloadedModels -> {
                    Set<String> downloadedLangCodes = new HashSet<>();
                    for (TranslateRemoteModel model : downloadedModels) {
                        downloadedLangCodes.add(model.getLanguage());
                    }

                    List<Language> languages = new ArrayList<>();
                    List<String> allLanguageCodes = TranslateLanguage.getAllLanguages();

                    for (String code : allLanguageCodes) {
                        languages.add(new Language(code, new Locale(code).getDisplayLanguage()));
                    }
                    Collections.sort(languages, (l1, l2) -> l1.displayName.compareTo(l2.displayName));

                    adapter = new LanguageAdapter(languages, downloadedLangCodes, this);
                    binding.recyclerView.setAdapter(adapter);
                })
                .addOnFailureListener(e -> showToast("Failed to get downloaded models: " + e.getMessage()));
    }

    @Override
    public void onDownloadClick(Language language) {
        showToast("Downloading " + language.displayName + " model...");
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH) // Source is arbitrary for model download
                .setTargetLanguage(language.code)
                .build();
        Translator translator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder().requireWifi().build();
        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(v -> {
                    showToast(language.displayName + " model downloaded.");
                    loadLanguages(); // Refresh the list
                })
                .addOnFailureListener(e -> showToast("Model download failed: " + e.getMessage()));
    }

    @Override
    public void onDeleteClick(Language language) {
        showToast("Deleting " + language.displayName + " model...");

        TranslateRemoteModel modelToDelete = new TranslateRemoteModel.Builder(language.code).build();

        RemoteModelManager.getInstance().deleteDownloadedModel(modelToDelete)
                .addOnSuccessListener(v -> {
                    showToast(language.displayName + " model deleted.");
                    loadLanguages(); // Refresh the list
                })
                .addOnFailureListener(e -> showToast("Model deletion failed: " + e.getMessage()));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
