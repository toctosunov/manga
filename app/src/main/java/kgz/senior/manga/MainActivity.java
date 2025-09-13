package kgz.senior.manga;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 1;
    private static final int PICK_FONT = 2;
    private ImageView backgroundImage;
    private SharedPreferences sharedPreferences;
    private Button btnEditor;
    private Button senkuro, ranobelib, amedia;
    private List<Button> allButtons;
    private Map<String, ButtonConfig> buttonConfigs;
    private Gson gson;
    private String selectedOption = "background";
    private float currentFontSize = 20f;
    private Typeface currentFont;
    private AlertDialog currentDialog;
    private AlertDialog colorPickerDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hideSystemUI();

        backgroundImage = findViewById(R.id.backgroundImage);
        btnEditor = findViewById(R.id.btnEditor);
        senkuro = findViewById(R.id.senkuro);
        ranobelib = findViewById(R.id.ranobelib);
        amedia = findViewById(R.id.amedia);

        allButtons = new ArrayList<>();
        allButtons.add(senkuro);
        allButtons.add(ranobelib);
        allButtons.add(amedia);
        allButtons.add(btnEditor);

        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        gson = new Gson();

        loadButtonConfigs();
        loadSavedColors();
        loadBackground();
        loadSavedFontSize();
        loadSavedFont();

        setupButtonListeners();
    }

    private void loadButtonConfigs() {
        String configsJson = sharedPreferences.getString("buttonConfigs", null);
        if (configsJson == null) {
            // Создаем конфигурации по умолчанию
            buttonConfigs = new HashMap<>();
            buttonConfigs.put("senkuro", new ButtonConfig("senkuro", "https://senkuro.com/"));
            buttonConfigs.put("ranobelib", new ButtonConfig("ranobelib", "https://ranobelib.me/"));
            buttonConfigs.put("amedia", new ButtonConfig("amedia", "https://amedia.lol/"));
            buttonConfigs.put("editor", new ButtonConfig("Редактор", ""));
            saveButtonConfigs();
        } else {
            buttonConfigs = gson.fromJson(configsJson, new TypeToken<Map<String, ButtonConfig>>(){}.getType());
            if (!buttonConfigs.containsKey("editor")) {
                buttonConfigs.put("editor", new ButtonConfig("Редактор", ""));
                saveButtonConfigs();
            }
        }
        updateButtonTexts();
    }

    private void saveButtonConfigs() {
        String configsJson = gson.toJson(buttonConfigs);
        sharedPreferences.edit().putString("buttonConfigs", configsJson).apply();
    }

    private void updateButtonTexts() {
        senkuro.setText(buttonConfigs.get("senkuro").getText());
        ranobelib.setText(buttonConfigs.get("ranobelib").getText());
        amedia.setText(buttonConfigs.get("amedia").getText());
        btnEditor.setText(buttonConfigs.get("editor").getText());
    }

    private void setupButtonListeners() {
        btnEditor.setOnLongClickListener(v -> showEditDialog("editor"));
        
        btnEditor.setOnClickListener(v -> showEditorMenu());

        senkuro.setOnLongClickListener(v -> showEditDialog("senkuro"));
        ranobelib.setOnLongClickListener(v -> showEditDialog("ranobelib"));
        amedia.setOnLongClickListener(v -> showEditDialog("amedia"));

        senkuro.setOnClickListener(v -> openWebsite(buttonConfigs.get("senkuro").getUrl()));
        ranobelib.setOnClickListener(v -> openWebsite(buttonConfigs.get("ranobelib").getUrl()));
        amedia.setOnClickListener(v -> openWebsite(buttonConfigs.get("amedia").getUrl()));
    }

    private void showEditorMenu() {
        View dialogView = getLayoutInflater().inflate(R.layout.editor_menu, null);
        
        Button btnChangeBackground = dialogView.findViewById(R.id.btnChangeBackground);
        Button btnColorPicker = dialogView.findViewById(R.id.btnColorPicker);
        Button btnChangeFont = dialogView.findViewById(R.id.btnChangeFont);
        Button btnReset = dialogView.findViewById(R.id.btnReset);

        btnChangeBackground.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE);
        });

        btnColorPicker.setOnClickListener(v -> showColorPickerDialog());

        btnChangeFont.setOnClickListener(v -> showFontSizeDialog());

        btnReset.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                .setTitle("Сбросить настройки")
                .setMessage("Вы уверены, что хотите сбросить все настройки?")
                .setPositiveButton("Да", (dialog, which) -> {
                    resetAllSettings();
                })
                .setNegativeButton("Нет", null)
                .show();
        });

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void resetAllSettings() {
        // Сброс фона
        sharedPreferences.edit().remove("backgroundPath").apply();
        backgroundImage.setImageResource(android.R.color.transparent);

        // Сброс цветов
        sharedPreferences.edit().remove("buttonTextColor").remove("buttonBackgroundColor").apply();
        for (Button button : allButtons) {
            button.setTextColor(Color.BLACK);
            button.setBackgroundColor(Color.WHITE);
        }

        // Сброс шрифта
        sharedPreferences.edit().remove("fontPath").remove("fontSize").apply();
        currentFont = null;
        currentFontSize = 20f;
        for (Button button : allButtons) {
            button.setTypeface(null);
            button.setTextSize(currentFontSize);
        }

        Toast.makeText(this, "Все настройки сброшены", Toast.LENGTH_SHORT).show();
    }

    private void showColorPickerDialog() {
        // Сначала показываем диалог выбора типа цвета
        String[] options = {"Фон", "Текст"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Выберите тип цвета")
                .setItems(options, (dialog, which) -> {
                    selectedOption = which == 0 ? "background" : "text";
                    // После выбора типа показываем палитру
                    new ColorPickerDialog.Builder(this)
                            .setTitle("Выберите цвет")
                            .setPreferenceName("MyColorPicker")
                            .setPositiveButton("Применить", (ColorEnvelopeListener) (envelope, fromUser) -> {
                                int selectedColor = envelope.getColor();
                                if (selectedOption.equals("background")) {
                                    applyBackgroundColor(selectedColor);
                                } else {
                                    applyTextColor(selectedColor);
                                }
                                Toast.makeText(this, "Цвет сохранён!", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Отмена", null)
                            .show();
                })
                .show();
    }

    private boolean showEditDialog(String buttonId) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_button, null);
        EditText textInput = dialogView.findViewById(R.id.textInput);
        EditText urlInput = dialogView.findViewById(R.id.urlInput);
        EditText textColorInput = dialogView.findViewById(R.id.textColorInput);
        EditText backgroundColorInput = dialogView.findViewById(R.id.backgroundColorInput);
        CheckBox useGlobalColorsCheckbox = dialogView.findViewById(R.id.useGlobalColors);

        ButtonConfig config = buttonConfigs.get(buttonId);
        textInput.setText(config.getText());
        urlInput.setText(config.getUrl());
        textColorInput.setText(config.getTextColor());
        backgroundColorInput.setText(config.getBackgroundColor());
        useGlobalColorsCheckbox.setChecked(config.isUseGlobalColors());

        // Обработчик изменения состояния чекбокса
        useGlobalColorsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            textColorInput.setEnabled(!isChecked);
            backgroundColorInput.setEnabled(!isChecked);
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle("Редактировать кнопку")
                .setView(dialogView)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    config.setText(textInput.getText().toString());
                    config.setUrl(urlInput.getText().toString());
                    
                    // Проверяем корректность цветов перед сохранением
                    String textColor = textColorInput.getText().toString().trim();
                    String backgroundColor = backgroundColorInput.getText().toString().trim();
                    
                    if (!useGlobalColorsCheckbox.isChecked()) {
                        try {
                            if (!textColor.isEmpty()) {
                                parseColor(textColor);
                            }
                            if (!backgroundColor.isEmpty()) {
                                parseColor(backgroundColor);
                            }
                            config.setTextColor(textColor);
                            config.setBackgroundColor(backgroundColor);
                        } catch (IllegalArgumentException e) {
                            Toast.makeText(this, "Неверный формат цвета: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    
                    config.setUseGlobalColors(useGlobalColorsCheckbox.isChecked());
                    saveButtonConfigs();
                    updateButtonTexts();
                    updateButtonColors();
                    Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null);

        currentDialog = builder.show();
        return true;
    }

    private void updateButtonColors() {
        for (Map.Entry<String, ButtonConfig> entry : buttonConfigs.entrySet()) {
            Button button = null;
            switch (entry.getKey()) {
                case "senkuro":
                    button = senkuro;
                    break;
                case "ranobelib":
                    button = ranobelib;
                    break;
                case "amedia":
                    button = amedia;
                    break;
                case "editor":
                    button = btnEditor;
                    break;
            }
            if (button != null) {
                ButtonConfig config = entry.getValue();
                if (config.isUseGlobalColors()) {
                    button.setTextColor(getColor("buttonTextColor", Color.BLACK));
                    button.setBackgroundColor(getColor("buttonBackgroundColor", Color.WHITE));
                } else {
                    try {
                        if (config.getTextColor() != null && !config.getTextColor().isEmpty()) {
                            button.setTextColor(parseColor(config.getTextColor()));
                        }
                        if (config.getBackgroundColor() != null && !config.getBackgroundColor().isEmpty()) {
                            button.setBackgroundColor(parseColor(config.getBackgroundColor()));
                        }
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(this, "Неверный формат цвета: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private int parseColor(String colorString) {
        // Удаляем пробелы
        colorString = colorString.trim();
        
        // Проверяем формат rgb(r,g,b)
        if (colorString.startsWith("rgb(") && colorString.endsWith(")")) {
            String[] rgb = colorString.substring(4, colorString.length() - 1).split(",");
            if (rgb.length == 3) {
                int r = Integer.parseInt(rgb[0].trim());
                int g = Integer.parseInt(rgb[1].trim());
                int b = Integer.parseInt(rgb[2].trim());
                return Color.rgb(r, g, b);
            }
        }
        
        // Проверяем формат rgba(r,g,b,a)
        if (colorString.startsWith("rgba(") && colorString.endsWith(")")) {
            String[] rgba = colorString.substring(5, colorString.length() - 1).split(",");
            if (rgba.length == 4) {
                int r = Integer.parseInt(rgba[0].trim());
                int g = Integer.parseInt(rgba[1].trim());
                int b = Integer.parseInt(rgba[2].trim());
                float a = Float.parseFloat(rgba[3].trim());
                return Color.argb((int)(a * 255), r, g, b);
            }
        }
        
        // Проверяем именованные цвета
        try {
            Field field = Color.class.getField(colorString.toLowerCase());
            return field.getInt(null);
        } catch (Exception e) {
            // Если не нашли именованный цвет, пробуем стандартный парсер
            return Color.parseColor(colorString);
        }
    }

    private void openWebsite(String url) {
        Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
        intent.putExtra("URL", url);
        startActivity(intent);
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }


    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI(); // Скрываем систему при возврате в приложение
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    File file = new File(getFilesDir(), "backgroundImage.png");
                    FileOutputStream outputStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.close();

                    sharedPreferences.edit().putString("backgroundPath", file.getAbsolutePath()).apply();
                    backgroundImage.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Ошибка при сохранении изображения", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == PICK_FONT && resultCode == RESULT_OK && data != null) {
            Uri fontUri = data.getData();
            if (fontUri != null) {
                try {
                    // Копируем файл шрифта во внутреннее хранилище
                    InputStream inputStream = getContentResolver().openInputStream(fontUri);
                    File fontFile = new File(getFilesDir(), "custom_font.ttf");
                    FileOutputStream outputStream = new FileOutputStream(fontFile);
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                    }
                    outputStream.close();
                    inputStream.close();

                    // Загружаем шрифт
                    currentFont = Typeface.createFromFile(fontFile);
                    applyFont(currentFont);
                    saveFontPath(fontFile.getAbsolutePath());
                    Toast.makeText(this, "Шрифт успешно загружен", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Ошибка при загрузке шрифта", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void loadBackground() {
        String savedPath = sharedPreferences.getString("backgroundPath", null);
        if (savedPath != null) {
            try {
                File file = new File(savedPath);
                if (file.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    backgroundImage.setImageBitmap(bitmap);
                } else {
                    Toast.makeText(this, "Фон не найден", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка при загрузке фона", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void applyBackgroundColor(int color) {
        for (Button button : allButtons) {
            button.setBackgroundColor(color);
        }
        sharedPreferences.edit().putInt("buttonBackgroundColor", color).apply();
    }

    private void applyTextColor(int color) {
        for (Button button : allButtons) {
            button.setTextColor(color);
        }
        sharedPreferences.edit().putInt("buttonTextColor", color).apply();
    }

    private int getColor(String key, int defaultColor) {
        return sharedPreferences.getInt(key, defaultColor);
    }

    private void loadSavedColors() {
        int textColor = getColor("buttonTextColor", Color.BLACK);
        int backgroundColor = getColor("buttonBackgroundColor", Color.WHITE);

        for (Button button : allButtons) {
            button.setTextColor(textColor);
            button.setBackgroundColor(backgroundColor);
        }
    }

    private void showFontSizeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_font_chooser, null);
        Button btnSelectFont = dialogView.findViewById(R.id.btnSelectFont);
        SeekBar seekBar = dialogView.findViewById(R.id.seekBar);
        TextView previewText = dialogView.findViewById(R.id.previewText);

        // Устанавливаем текущий шрифт и размер
        if (currentFont != null) {
            previewText.setTypeface(currentFont);
        }
        seekBar.setProgress((int) (currentFontSize * 2));
        previewText.setTextSize(currentFontSize);

        // Обработчик выбора файла шрифта
        btnSelectFont.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("font/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_FONT);
        });

        // Обработчик изменения размера
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float newSize = progress / 2f;
                previewText.setTextSize(newSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle("Настройки шрифта")
                .setView(dialogView)
                .setPositiveButton("Применить", (dialog, which) -> {
                    currentFontSize = seekBar.getProgress() / 2f;
                    applyFontSize(currentFontSize);
                    saveFontSize(currentFontSize);
                    Toast.makeText(this, "Настройки шрифта сохранены", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void applyFontSize(float size) {
        for (Button button : allButtons) {
            button.setTextSize(size);
        }
    }

    private void saveFontSize(float size) {
        sharedPreferences.edit().putFloat("fontSize", size).apply();
    }

    private void loadSavedFontSize() {
        currentFontSize = sharedPreferences.getFloat("fontSize", 20f);
        applyFontSize(currentFontSize);
    }

    private void applyFont(Typeface font) {
        for (Button button : allButtons) {
            button.setTypeface(font);
        }
    }

    private void saveFontPath(String path) {
        sharedPreferences.edit().putString("fontPath", path).apply();
    }

    private void loadSavedFont() {
        String fontPath = sharedPreferences.getString("fontPath", null);
        if (fontPath != null) {
            try {
                File fontFile = new File(fontPath);
                if (fontFile.exists()) {
                    currentFont = Typeface.createFromFile(fontFile);
                    applyFont(currentFont);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
