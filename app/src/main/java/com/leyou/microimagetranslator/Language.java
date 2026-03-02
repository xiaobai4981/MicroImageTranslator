package com.leyou.microimagetranslator;

import androidx.annotation.NonNull;

import java.util.Objects;

public class Language {
    String code;
    String displayName;

    public Language(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    @NonNull
    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Language language = (Language) o;
        return code.equals(language.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
