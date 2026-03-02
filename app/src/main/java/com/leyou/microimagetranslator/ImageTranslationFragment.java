package com.leyou.microimagetranslator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.leyou.microimagetranslator.databinding.FragmentImageTranslationBinding;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ImageTranslationFragment extends Fragment {

    private FragmentImageTranslationBinding binding;

    private ActivityResultLauncher<String> getContentLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private TextRecognizer textRecognizer;
    private LanguageIdentifier languageIdentifier;

    private Uri tempImageUri;
    private String lastRecognizedText = "";
    private String identifiedLanguageCode = "";
    private Language currentTargetLanguage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentImageTranslationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        languageIdentifier = LanguageIdentification.getClient();

        setupLaunchers();
        setupTargetLanguageSpinner();

        binding.selectImageButton.setOnClickListener(v -> getContentLauncher.launch("image/*"));
        binding.takePictureButton.setOnClickListener(v -> checkCameraPermissionAndTakePicture());
    }

    private void setupLaunchers() {
        getContentLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                showImage(uri);
                processImage(uri);
            }
        });

        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success) {
                showImage(tempImageUri);
                processImage(tempImageUri);
            }
        });

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                takePicture();
            } else {
                showToast("Camera permission is required to take pictures.");
            }
        });
    }

    private void showImage(Uri uri) {
        binding.emptyStateLayout.setVisibility(View.GONE);
        binding.imageView.setVisibility(View.VISIBLE);
        binding.imageView.setImageURI(uri);
        clearResults();
    }

    private void checkCameraPermissionAndTakePicture() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePicture();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void takePicture() {
        tempImageUri = createImageUri();
        if (tempImageUri != null) {
            takePictureLauncher.launch(tempImageUri);
        }
    }

    private Uri createImageUri() {
        File imageFile = new File(requireActivity().getCacheDir(), "temp_image.jpg");
        return FileProvider.getUriForFile(requireContext(), requireActivity().getApplicationContext().getPackageName() + ".provider", imageFile);
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
                retranslateText();
            });
        }
    }

    private void retranslateText() {
        if (!lastRecognizedText.isEmpty() && !identifiedLanguageCode.isEmpty() && currentTargetLanguage != null) {
            translateText(lastRecognizedText, identifiedLanguageCode, currentTargetLanguage.code);
        }
    }

    private void clearResults() {
        lastRecognizedText = "";
        identifiedLanguageCode = "";
        binding.translatedTextView.setText("");
    }

    private void processImage(Uri imageUri) {
        setProcessing(true);
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), imageUri);
            textRecognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        lastRecognizedText = visionText.getText();
                        if (!lastRecognizedText.isEmpty()) {
                            identifyLanguageAndTranslate(lastRecognizedText);
                        } else {
                            binding.translatedTextView.setText("No text found in image.");
                            setProcessing(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        showToast("Error recognizing text: " + e.getMessage());
                        setProcessing(false);
                    });
        } catch (IOException e) {
            showToast("Error preparing image: " + e.getMessage());
            setProcessing(false);
        }
    }

    private void identifyLanguageAndTranslate(String text) {
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener(languageCode -> {
                    if (languageCode.equals("und")) {
                        showToast("Could not identify language from text.");
                        setProcessing(false);
                    } else {
                        identifiedLanguageCode = languageCode;
                        if (currentTargetLanguage != null) {
                            translateText(text, identifiedLanguageCode, currentTargetLanguage.code);
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
        // This should be handled by the parent activity now
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            // activity.setProcessing(isProcessing); // Need to implement this
        }
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Important for fragment memory management
    }
}
