package com.leyou.microimagetranslator;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayoutMediator;
import com.leyou.microimagetranslator.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final List<TargetLanguageListener> listeners = new ArrayList<>();

    public interface TargetLanguageListener {
        void onTargetLanguageChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(findViewById(R.id.toolbar));

        // Set up the ViewPager and TabLayout
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(this);
        binding.viewPager.setAdapter(viewPagerAdapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Image");
                            break;
                        case 1:
                            tab.setText("Text");
                            break;
                    }
                }
        ).attach();
    }

    public void setProcessing(boolean isProcessing) {
        binding.progressBar.setVisibility(isProcessing ? View.VISIBLE : View.GONE);
    }

    public void registerListener(TargetLanguageListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(TargetLanguageListener listener) {
        listeners.remove(listener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_manage_models) {
            startActivity(new Intent(this, ManageModelsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}