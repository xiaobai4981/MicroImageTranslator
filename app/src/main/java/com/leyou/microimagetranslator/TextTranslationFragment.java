package com.leyou.microimagetranslator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.leyou.microimagetranslator.databinding.FragmentTextTranslationBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TextTranslationFragment extends Fragment {

    private FragmentTextTranslationBinding binding;

    private Language currentTargetLanguage;
    private LanguageIdentifier languageIdentifier;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTextTranslationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        languageIdentifier = LanguageIdentification.getClient();

        setupTargetLanguageSpinner();

        binding.translateTextButton.setOnClickListener(v -> processText());
    }

    private void setupTargetLanguageSpinner() {
        List<Language> languages = new ArrayList<>();
        List<String> languageCodes = TranslateLanguage.getAllLanguages();
        for (String code : languageCodes) {
            languages.add(new Language(code, new Locale(code).getDisplayLanguage()));
        }
        Collections.sort(languages, (l1, l2) -> l1.displayName.compareTo(l2.displayName));

        ArrayAdapter<Language> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, languages);
        AutoCompleteTextView textView = (AutoCompleteTextView) binding.targetLangMenu.getEditText();

        if (textView != null) {
            textView.setAdapter(adapter);
            for (Language lang : languages) {
                if (lang.code.equals("es")) {
                    currentTargetLanguage = lang;
                    textView.setText(lang.displayName, false);
                    break;
                }
            }

            textView.setOnItemClickListener((parent, view, position, id) -> {
                currentTargetLanguage = (Language) parent.getItemAtPosition(position);
                // No retranslateText() needed here, as translation is triggered by button click
            });
        }
    }

    private void processText() {
        String textToTranslate = binding.textInputEditText.getText().toString().trim();
        if (textToTranslate.isEmpty()) {
            showToast("Please enter text to translate.");
            return;
        }

        setProcessing(true);
        identifyLanguageAndTranslate(textToTranslate);
    }

    private void identifyLanguageAndTranslate(String text) {
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener(languageCode -> {
                    if (languageCode.equals("und")) {
                        showToast("Could not identify language from text.");
                        setProcessing(false);
                    } else {
                        if (currentTargetLanguage != null) {
                            translateText(text, languageCode, currentTargetLanguage.code);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showToast("Language identification failed: " + e.getMessage());
                    setProcessing(false);
                });
    }

    private void translateText(String text, String sourceLang, String targetLang) {
        setProcessing(true);
        String targetLangName = new Locale(targetLang).getDisplayLanguage();
        binding.translatedTextView.setText("Downloading " + targetLangName + " model...");

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build();
        Translator translator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder().requireWifi().build();
        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(v -> translator.translate(text)
                        .addOnSuccessListener(translatedText -> {
                            binding.translatedTextView.setText(translatedText);
                            setProcessing(false);
                            translator.close();
                        })
                        .addOnFailureListener(e -> {
                            showToast("Translation failed: " + e.getMessage());
                            setProcessing(false);
                            translator.close();
                        }))
                .addOnFailureListener(e -> {
                    showToast("Model download failed: " + e.getMessage());
                    setProcessing(false);
                    translator.close();
                });
    }

    private void setProcessing(boolean isProcessing) {
        // For now, just disable button
        binding.translateTextButton.setEnabled(!isProcessing);
        binding.textInputLayout.setEnabled(!isProcessing);
        binding.targetLangMenu.setEnabled(!isProcessing);
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}