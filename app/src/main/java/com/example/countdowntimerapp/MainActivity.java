package com.example.countdowntimerapp;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import java.util.List;
import android.view.LayoutInflater;
import java.util.Arrays;
import android.widget.AdapterView;
import android.view.View;
import android.widget.ProgressBar;
import android.text.TextWatcher;
import android.text.Editable;
import java.util.ArrayList;
import android.database.Cursor;
import android.provider.OpenableColumns;
import java.util.Arrays;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import android.view.KeyEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.text.SpannableStringBuilder;
import android.graphics.MaskFilter;
import android.graphics.BlurMaskFilter;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 1;
    private static final int REQUEST_CODE_FONT_PICKER = 2;

    // UI Elements
private EditText inputSeconds;
private TextView statusText;
private Button downloadButton, textPropertiesButton;
private Button videoQualityButton;
private Dialog progressDialog;
private Dialog textPropertiesDialog;
private Spinner fontSpinner;
private ImageView previewImage; // Use ImageView instead of TextView

// Text Properties
private int textColor = Color.WHITE;
private int shadowColor = Color.BLACK;
private int glowColor = Color.YELLOW;
private int strokeColor = Color.WHITE;
private boolean isShadowEnabled = false;
private boolean isGlowEnabled = false;
private boolean isStrokeEnabled = false;
private float selectedFontSize = 100f;
private float strokeWidth = 5f;

// Font Properties
private String selectedFontFamily = "sans-serif";
private Typeface selectedTypeface; // Holds the current Typeface (custom or standard)
private Typeface customTypeface;   // Holds the custom Typeface if one is loaded
private String customFontName;
private List<String> fontOptions = new ArrayList<>(Arrays.asList(
    "sans-serif", "serif", "monospace", "cursive", "casual", "Add Custom Font"));

// Video Properties
private int selectedResolutionWidth = 1280;
private int selectedResolutionHeight = 720;
private int selectedFPS = 120;
private int selectedBitrate = 1000 * 1000;
private TextView estimatedSizeText;
private int backgroundColor = Color.parseColor("#00FF00"); 
    
    @Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    inputSeconds = findViewById(R.id.input_seconds);
    statusText = findViewById(R.id.status_text);
    downloadButton = findViewById(R.id.btn_download);
    textPropertiesButton = findViewById(R.id.btn_text_properties);
    videoQualityButton = findViewById(R.id.btn_video_quality);
    estimatedSizeText = findViewById(R.id.estimated_size_text); // Initialize the TextView

    videoQualityButton.setOnClickListener(v -> openVideoQualityDialog());
    checkPermissions();

    downloadButton.setOnClickListener(v -> {
        String input = inputSeconds.getText().toString();
        if (!input.isEmpty()) {
            try {
                int seconds = Integer.parseInt(input);
                if (seconds < 0) {
                    Toast.makeText(MainActivity.this, "Please enter a non-negative number", Toast.LENGTH_SHORT).show();
                } else {
                    generateCountdownVideo(seconds, isShadowEnabled, shadowColor, isGlowEnabled, glowColor, isStrokeEnabled, strokeColor, strokeWidth);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
        }
    });

    textPropertiesButton.setOnClickListener(v -> openTextPropertiesDialog());

    // Set up TextWatcher for inputSeconds to update estimated size
    inputSeconds.addTextChangedListener(new TextWatcher() {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        updateEstimatedSize();
    }

    @Override
    public void afterTextChanged(Editable s) {}
});
    // Initialize estimated size display
    updateEstimatedSize();
}

private void openTextPropertiesDialog() {
    textPropertiesDialog = new Dialog(this);
    textPropertiesDialog.setContentView(R.layout.dialog_text_properties);

    // Initialize UI components
    fontSpinner = textPropertiesDialog.findViewById(R.id.font_spinner);
    SeekBar fontSizeSeekBar = textPropertiesDialog.findViewById(R.id.font_size_seekbar);
    previewImage = textPropertiesDialog.findViewById(R.id.preview_image);
    Button colorPickerButton = textPropertiesDialog.findViewById(R.id.btn_color_picker);
    Button shadowColorPickerButton = textPropertiesDialog.findViewById(R.id.btn_shadow_color_picker);
    Button glowColorPickerButton = textPropertiesDialog.findViewById(R.id.btn_glow_color_picker);
    Button strokeColorPickerButton = textPropertiesDialog.findViewById(R.id.btn_stroke_color_picker);
    Button backgroundColorPickerButton = textPropertiesDialog.findViewById(R.id.btn_background_color_picker);
    SeekBar strokeWidthSeekBar = textPropertiesDialog.findViewById(R.id.stroke_width_seekbar);
    Button confirmButton = textPropertiesDialog.findViewById(R.id.confirm_button);
    CheckBox shadowCheckbox = textPropertiesDialog.findViewById(R.id.checkbox_shadow);
    CheckBox glowCheckbox = textPropertiesDialog.findViewById(R.id.checkbox_glow);
    CheckBox strokeCheckbox = textPropertiesDialog.findViewById(R.id.checkbox_stroke);

    // Initialize UI elements based on saved state
    shadowCheckbox.setChecked(isShadowEnabled);
    glowCheckbox.setChecked(isGlowEnabled);
    strokeCheckbox.setChecked(isStrokeEnabled);

    shadowColorPickerButton.setEnabled(isShadowEnabled);
    glowColorPickerButton.setEnabled(isGlowEnabled);
    strokeColorPickerButton.setEnabled(isStrokeEnabled);
    strokeWidthSeekBar.setEnabled(isStrokeEnabled);

    // Populate the font spinner with font options
    ArrayAdapter<String> fontAdapter = new ArrayAdapter<String>(this, R.layout.font_spinner_item, fontOptions) {
        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view = convertView;

            // Inflate the view if it is null
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                view = inflater.inflate(R.layout.font_spinner_item, parent, false);
            }

            TextView textView = view.findViewById(R.id.font_spinner_item_text);
            textView.setText(getItem(position));

            // Apply unique styling for "Add Custom Font" option
            if (position == fontOptions.size() - 1) {  // Last item, "Add Custom Font"
                textView.setTextColor(Color.BLUE);
                textView.setTypeface(null, Typeface.BOLD);
            } else {
                textView.setTextColor(Color.BLACK);
                textView.setTypeface(null, Typeface.NORMAL);
            }

            return view;
        }
    };
    fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    fontSpinner.setAdapter(fontAdapter);

    // Set default font selection
    String currentFont = customFontName != null ? customFontName : selectedFontFamily;
    int fontIndex = fontOptions.indexOf(currentFont);
    if (fontIndex >= 0) {
        fontSpinner.setSelection(fontIndex);
    } else {
        fontSpinner.setSelection(0);
    }

    // Set on item selected listener for font selection
    fontSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String selectedFont = fontOptions.get(position);
            if (selectedFont.equals("Add Custom Font")) {
                // Open file picker to select custom font
                openFilePicker();
            } else {
                // Apply selected font to preview
                selectedFontFamily = selectedFont;
                if (selectedFont.equals(customFontName) && customTypeface != null) {
                    selectedTypeface = customTypeface;
                } else {
                    selectedTypeface = Typeface.create(selectedFontFamily, Typeface.NORMAL);
                }
                updatePreviewText();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    });

    // Set initial font size and apply it to preview
    fontSizeSeekBar.setProgress((int) selectedFontSize);
    fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            selectedFontSize = progress;
            updatePreviewText();
        }

        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    });

    // Initialize the preview
    updatePreviewText();

    // Shadow effect checkbox and color picker
    shadowCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
        isShadowEnabled = isChecked;
        shadowColorPickerButton.setEnabled(isChecked);
        updatePreviewText();
    });

    shadowColorPickerButton.setOnClickListener(v -> new ColorPickerDialog.Builder(this)
        .setTitle("Pick Shadow Color")
        .setPositiveButton("Confirm", (ColorEnvelopeListener) (envelope, fromUser) -> {
            shadowColor = envelope.getColor();
            updatePreviewText();
        })
        .attachAlphaSlideBar(true)
        .attachBrightnessSlideBar(true)
        .create()
        .show()
    );

    // Glow effect checkbox and color picker
    glowCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
        isGlowEnabled = isChecked;
        glowColorPickerButton.setEnabled(isChecked);
        updatePreviewText();
    });

    glowColorPickerButton.setOnClickListener(v -> new ColorPickerDialog.Builder(this)
        .setTitle("Pick Glow Color")
        .setPositiveButton("Confirm", (ColorEnvelopeListener) (envelope, fromUser) -> {
            glowColor = envelope.getColor();
            updatePreviewText();
        })
        .attachAlphaSlideBar(true)
        .attachBrightnessSlideBar(true)
        .create()
        .show()
    );

    // Stroke effect checkbox, color picker, and width
    strokeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
        isStrokeEnabled = isChecked;
        strokeColorPickerButton.setEnabled(isChecked);
        strokeWidthSeekBar.setEnabled(isChecked);
        updatePreviewText();
    });

    strokeColorPickerButton.setOnClickListener(v -> new ColorPickerDialog.Builder(this)
        .setTitle("Pick Stroke Color")
        .setPositiveButton("Confirm", (ColorEnvelopeListener) (envelope, fromUser) -> {
            strokeColor = envelope.getColor();
            updatePreviewText();
        })
        .attachAlphaSlideBar(true)
        .attachBrightnessSlideBar(true)
        .create()
        .show()
    );

    strokeWidthSeekBar.setProgress((int) strokeWidth);
    strokeWidthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            strokeWidth = progress;
            updatePreviewText();
        }

        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    });

    // Text color picker
    colorPickerButton.setOnClickListener(v -> new ColorPickerDialog.Builder(this)
        .setTitle("Pick Text Color")
        .setPositiveButton("Confirm", (ColorEnvelopeListener) (envelope, fromUser) -> {
            textColor = envelope.getColor();
            updatePreviewText();
        })
        .attachAlphaSlideBar(true)
        .attachBrightnessSlideBar(true)
        .create()
        .show()
    );

    // Background color picker
    backgroundColorPickerButton.setOnClickListener(v -> new ColorPickerDialog.Builder(this)
        .setTitle("Pick Background Color")
        .setPositiveButton("Confirm", (ColorEnvelopeListener) (envelope, fromUser) -> {
            backgroundColor = envelope.getColor();
            updatePreviewText();
        })
        .attachAlphaSlideBar(true)
        .attachBrightnessSlideBar(true)
        .setPreferenceName("BackgroundColorPicker")
        .create()
        .show()
    );

    // Confirm button
    confirmButton.setOnClickListener(v -> {
        textPropertiesDialog.dismiss();
    });

    // Show the dialog
    textPropertiesDialog.show();
}

    
    private void updatePreviewText() {
    if (previewImage == null) return;

    int width = previewImage.getWidth();
    int height = previewImage.getHeight();

    if (width == 0 || height == 0) {
        // The view hasn't been laid out yet; retry after layout
        previewImage.post(this::updatePreviewText);
        return;
    }

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    canvas.drawColor(backgroundColor); // Use background color selected by user

    // Base paint for text
    Paint paint = new Paint();
    paint.setAntiAlias(true);
    paint.setTextSize(selectedFontSize);
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTypeface(selectedTypeface != null ? selectedTypeface : Typeface.DEFAULT);
    paint.setColor(textColor);
    paint.setStyle(Paint.Style.FILL);

    // Calculate text position
    float xPos = width / 2f;
    float yPos = (height / 2f) - ((paint.descent() + paint.ascent()) / 2);

    // Draw Glow effect
    if (isGlowEnabled) {
        Paint glowPaint = new Paint(paint);
        glowPaint.setColor(glowColor);
        glowPaint.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL));
        canvas.drawText("Preview Text", xPos, yPos, glowPaint);
    }

    // Draw Shadow effect
    if (isShadowEnabled) {
        Paint shadowPaint = new Paint(paint);
        shadowPaint.setColor(shadowColor);
        shadowPaint.setShadowLayer(10, 5, 5, shadowColor);
        canvas.drawText("Preview Text", xPos, yPos, shadowPaint);
    }

    // Draw Stroke effect
    if (isStrokeEnabled) {
        Paint strokePaint = new Paint(paint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setColor(strokeColor);
        canvas.drawText("Preview Text", xPos, yPos, strokePaint);
    }

    // Draw main text
    canvas.drawText("Preview Text", xPos, yPos, paint);

    // Set the bitmap to the ImageView
    previewImage.setImageBitmap(bitmap);
}

    
    // Method to load custom font and update spinner
    private void loadCustomFont(Uri uri) {
    try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r")) {
        if (pfd != null) {
            File tempFile = new File(getCacheDir(), "temp_font.otf");
            try (FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
                 FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                customTypeface = Typeface.createFromFile(tempFile);
                if (customTypeface != null) {
                    customFontName = getFileName(uri);
                    selectedTypeface = customTypeface;
                    updateFontSpinnerWithCustomFont(customFontName);
                    updatePreviewText();
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
        Toast.makeText(this, "Error loading custom font", Toast.LENGTH_SHORT).show();
    }
}

private String getFileName(Uri uri) {
    String result = null;
    if (uri.getScheme().equals("content")) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        } finally {
            cursor.close();
        }
    }
    if (result == null) {
        result = uri.getLastPathSegment();
    }
    return result;
}
    // Update Spinner to display custom font name and set preview text
    private void updateFontSpinnerWithCustomFont(String customFontName) {
    if (textPropertiesDialog != null && fontSpinner != null && customTypeface != null) {
        // Remove any existing custom fonts before adding new one
        fontOptions.removeIf(font -> font.equals(customFontName));
        // Insert custom font before "Add Custom Font"
        fontOptions.add(fontOptions.size() - 1, customFontName);
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) fontSpinner.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            fontSpinner.setSelection(adapter.getPosition(customFontName));

            selectedTypeface = customTypeface;
            updatePreviewText();
        }
    }
}
    private void openColorPicker(TextView previewText) {
        new ColorPickerDialog.Builder(this)
                .setTitle("Pick Text Color")
                .setPositiveButton("Confirm", (ColorEnvelopeListener) (envelope, fromUser) -> {
                    textColor = envelope.getColor();
                    previewText.setTextColor(textColor);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .create()
                .show();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Choose a font file"), REQUEST_CODE_FONT_PICKER);
    }

 @Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_CODE_FONT_PICKER && resultCode == RESULT_OK && data != null) {
        Uri uri = data.getData();
        if (uri != null) {
            loadCustomFont(uri);
        } else {
            Toast.makeText(this, "Failed to load font file", Toast.LENGTH_SHORT).show();
        }
    }
}
    
    private void openVideoQualityDialog() {
    // Create dialog
    Dialog dialog = new Dialog(this);
    dialog.setContentView(R.layout.dialog_video_quality);

    // Find UI components in dialog
    Spinner resolutionSpinner = dialog.findViewById(R.id.spinner_resolution);
    Spinner fpsSpinner = dialog.findViewById(R.id.spinner_fps);
    Spinner bitrateSpinner = dialog.findViewById(R.id.spinner_bitrate);
    EditText customBitrateInput = dialog.findViewById(R.id.custom_bitrate_input);
    TextView fpsWarning = dialog.findViewById(R.id.fps_warning);
    Button confirmButton = dialog.findViewById(R.id.confirm_button);

    // Resolution options
    String[] resolutions = {"640x480", "1280x720", "1920x1080", "3840x2160"};
    ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, resolutions);
    resolutionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    resolutionSpinner.setAdapter(resolutionAdapter);

    // FPS options
    Integer[] fpsOptions = {24, 30, 60, 120, 240};
    ArrayAdapter<Integer> fpsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fpsOptions);
    fpsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    fpsSpinner.setAdapter(fpsAdapter);

    // Bitrate options
    String[] bitrateOptions = {"1 Mbps", "2 Mbps", "5 Mbps", "10 Mbps", "20 Mbps", "Custom"};
    ArrayAdapter<String> bitrateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bitrateOptions);
    bitrateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    bitrateSpinner.setAdapter(bitrateAdapter);

    // Show/hide custom bitrate input
    bitrateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String selectedBitrateOption = bitrateOptions[position];
            if (selectedBitrateOption.equals("Custom")) {
                customBitrateInput.setVisibility(View.VISIBLE);
            } else {
                customBitrateInput.setVisibility(View.GONE);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    });

    // Set initial selections based on current settings
    // Resolution
    String currentResolution = selectedResolutionWidth + "x" + selectedResolutionHeight;
    int resolutionIndex = Arrays.asList(resolutions).indexOf(currentResolution);
    if (resolutionIndex != -1) {
        resolutionSpinner.setSelection(resolutionIndex);
    }

    // FPS
    int fpsIndex = Arrays.asList(fpsOptions).indexOf(selectedFPS);
    if (fpsIndex != -1) {
        fpsSpinner.setSelection(fpsIndex);
    }

    // Bitrate
    String currentBitrateOption = (selectedBitrate / 1000000) + " Mbps";
    int bitrateIndex = Arrays.asList(bitrateOptions).indexOf(currentBitrateOption);
    if (bitrateIndex != -1) {
        bitrateSpinner.setSelection(bitrateIndex);
    } else {
        bitrateSpinner.setSelection(bitrateOptions.length - 1); // Select "Custom"
        customBitrateInput.setVisibility(View.VISIBLE);
        customBitrateInput.setText(String.valueOf(selectedBitrate / 1000000));
    }

    // Set onItemSelectedListener for FPS Spinner to show warning for 24 FPS
    fpsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            int selectedFPSValue = fpsOptions[position];
            if (selectedFPSValue == 24) {
                fpsWarning.setVisibility(View.VISIBLE);
            } else {
                fpsWarning.setVisibility(View.GONE);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    });

    // Confirm button action to save selections
    confirmButton.setOnClickListener(v -> {
        // Get selected resolution
        String resolution = (String) resolutionSpinner.getSelectedItem();
        String[] resParts = resolution.split("x");
        selectedResolutionWidth = Integer.parseInt(resParts[0]);
        selectedResolutionHeight = Integer.parseInt(resParts[1]);

        // Get selected FPS
        selectedFPS = (Integer) fpsSpinner.getSelectedItem();

        // Get selected bitrate
        String bitrateSelection = (String) bitrateSpinner.getSelectedItem();
        if (bitrateSelection.equals("Custom")) {
            String customBitrateStr = customBitrateInput.getText().toString();
            if (!customBitrateStr.isEmpty()) {
                selectedBitrate = Integer.parseInt(customBitrateStr) * 1000 * 1000;
            } else {
                selectedBitrate = 1000 * 1000; // Default to 1 Mbps if input is empty
            }
        } else {
            selectedBitrate = Integer.parseInt(bitrateSelection.split(" ")[0]) * 1000 * 1000;
        }

        // Update estimated size
        updateEstimatedSize();

        dialog.dismiss();
    });

    // Show dialog
    dialog.show();
}
    

private void generateCountdownVideo(int seconds, boolean isShadowEnabled, int shadowColor, boolean isGlowEnabled, int glowColor, boolean isStrokeEnabled, int strokeColor, float strokeWidth) {
    runOnUiThread(() -> {
        // Create a custom dialog with a custom theme
        Dialog customDialog = new Dialog(MainActivity.this, R.style.CustomDialogTheme);
        customDialog.setContentView(R.layout.custom_progress_dialog);

        // Set the custom rounded background directly on the dialog window
        customDialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog_background);

        // Prevent the user from dismissing the dialog with the back button
        customDialog.setCancelable(false);
        customDialog.setOnKeyListener((dialog, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // Ignore back button presses
                return true;
            }
            return false;
        });

        // Find the UI components in the custom dialog layout
        TextView title = customDialog.findViewById(R.id.title);
        ProgressBar progressBar = customDialog.findViewById(R.id.progress_bar);
        TextView progressPercentage = customDialog.findViewById(R.id.progress_percentage);
        TextView tipText = customDialog.findViewById(R.id.tip_text);

        // Show the custom dialog
        customDialog.show();

        // Store reference to update later
        progressDialog = customDialog;
    });

    String[] tips = {
        "Tip: Did you know you can customize the font and effects?",
        "Tip: You can add a glow effect to make your text stand out!",
        "Tip: Try different fonts to make your countdown unique.",
        "Tip: You can use custom fonts to add a personal touch.",
        "Tip: Glow, shadow, and stroke effects can be combined for amazing results!"
    };

    new Thread(() -> {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String videoPath = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "countdown_video_" + timestamp + ".mp4").getAbsolutePath();

        File tempDir = new File(getCacheDir(), "temp_frames");
        if (!tempDir.exists()) tempDir.mkdirs();

        int frameCount = seconds * selectedFPS;

        for (int i = 0; i <= frameCount; i++) {
            // Calculate current time for counting up from 0
            double currentTime = (double) i / selectedFPS;

            if (currentTime > seconds) {
                currentTime = seconds; // Ensures the timer stops at the specified input value
            }

            // Format the current countdown time to display as "seconds.milliseconds"
            String countdownText = String.format(Locale.getDefault(), "%.2f", currentTime);

            // Create the bitmap image for the current frame
            String imagePath = createBitmapImage(countdownText, i, tempDir, isShadowEnabled, shadowColor, isGlowEnabled, glowColor, isStrokeEnabled, strokeColor, strokeWidth);
            if (imagePath == null) {
                Log.e("MainActivity", "Failed to create image at index " + i);
                break;
            }

            // Update the progress
            final int progress = (int) (((double) i / frameCount) * 100);
            runOnUiThread(() -> {
                if (progressDialog != null) {
                    ProgressBar progressBar = progressDialog.findViewById(R.id.progress_bar);
                    TextView progressPercentage = progressDialog.findViewById(R.id.progress_percentage);
                    TextView tipText = progressDialog.findViewById(R.id.tip_text);

                    // Update progress bar and text
                    progressBar.setProgress(progress);
                    progressPercentage.setText(progress + "%");

                    // Show a tip every 20% progress
                    if (progress % 20 == 0) {
                        String tip = tips[(progress / 20) % tips.length];
                        tipText.setText(tip);
                    }
                }
            });
        }

        // Update FFmpeg command to generate the video from the frames
        String ffmpegCommand = String.format(
            "-framerate %d -i \"%s/frame%%04d.png\" -s %dx%d -c:v libx264 -pix_fmt yuv420p -y \"%s\"",
            selectedFPS,
            tempDir.getAbsolutePath(),
            selectedResolutionWidth,
            selectedResolutionHeight,
            videoPath
        );

        FFmpegKit.executeAsync(ffmpegCommand, session -> {
            ReturnCode returnCode = session.getReturnCode();
            runOnUiThread(() -> {
                if (ReturnCode.isSuccess(returnCode)) {
                    File videoFile = new File(videoPath);
                    if (videoFile.exists()) {
                        saveVideoToMediaStore(videoFile);
                    } else {
                        Toast.makeText(MainActivity.this, "Video file not found after creation", Toast.LENGTH_SHORT).show();
                        Log.e("MainActivity", "Video file not found at path: " + videoPath);
                    }
                } else {
                    Log.e("MainActivity", "FFmpeg failed: " + session.getAllLogsAsString());
                    Toast.makeText(MainActivity.this, "Error creating video.", Toast.LENGTH_SHORT).show();
                }
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                deleteGeneratedFrames(tempDir);
            });
        });
    }).start();
}




private void saveVideoToMediaStore(File videoFile) {
    ContentValues values = new ContentValues();
    values.put(MediaStore.Video.Media.DISPLAY_NAME, "Countdown_" + System.currentTimeMillis() + ".mp4");
    values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
    values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES);

    Uri videoUri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    if (videoUri != null) {
        try (OutputStream out = getContentResolver().openOutputStream(videoUri);
             FileInputStream in = new FileInputStream(videoFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            Toast.makeText(this, "Video saved to gallery", Toast.LENGTH_SHORT).show();
            showVideoPreviewDialog(videoUri);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving video", Toast.LENGTH_SHORT).show();
        }
    } else {
        Toast.makeText(this, "Error accessing MediaStore", Toast.LENGTH_SHORT).show();
    }
}

private void showVideoPreviewDialog(Uri videoUri) {
    Dialog dialog = new Dialog(MainActivity.this);
    dialog.setContentView(R.layout.dialog_video_preview);
    dialog.setCancelable(true);

    VideoView videoView = dialog.findViewById(R.id.video_view);
    Button closeButton = dialog.findViewById(R.id.btn_close);

    // Ensure the dialog is displayed before setting up the video URI
    dialog.setOnShowListener(d -> {
        videoView.post(() -> {
            videoView.setVideoURI(videoUri);
            videoView.setOnPreparedListener(mediaPlayer -> {
                mediaPlayer.start();
                Log.d("VideoPreview", "Video started successfully.");
            });

            // Log any errors for debugging purposes
            videoView.setOnErrorListener((mp, what, extra) -> {
                Log.e("VideoPreview", "Error loading video: " + what + ", " + extra);
                return true;
            });
        });
    });

    // Toggle play/pause on video click and restart from the beginning when completed
    videoView.setOnClickListener(v -> {
        if (videoView.isPlaying()) {
            videoView.pause();
        } else {
            if (videoView.getCurrentPosition() >= videoView.getDuration()) {
                // If video has ended, restart from the beginning
                videoView.seekTo(0);
            }
            videoView.start();
        }
    });

    // Handle video completion gracefully
    videoView.setOnCompletionListener(mp -> {
        Log.d("VideoPreview", "Video completed successfully.");

        // Seek to the last frame and allow tapping to restart the video
        videoView.seekTo(videoView.getDuration());
    });

    closeButton.setOnClickListener(v -> {
        if (videoView.isPlaying()) {
            videoView.stopPlayback();
        }
        dialog.dismiss();
    });

    dialog.show();
}


private String createBitmapImage(String text, int index, File tempDir, boolean isShadowEnabled, int shadowColor, boolean isGlowEnabled, int glowColor, boolean isStrokeEnabled, int strokeColor, float strokeWidth) {
    int width = selectedResolutionWidth;
    int height = selectedResolutionHeight;
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    canvas.drawColor(backgroundColor); // Use the selected background color for the video

    // Ensure custom Typeface is applied if available
    Typeface fontToUse = (selectedTypeface != null) ? selectedTypeface : Typeface.DEFAULT;

    // Base paint for text
    Paint paint = new Paint();
    paint.setAntiAlias(true);
    paint.setTextSize(selectedFontSize);
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTypeface(fontToUse); // Apply the correct typeface
    paint.setColor(textColor);
    paint.setStyle(Paint.Style.FILL);

    // Calculate text position
    float xPos = width / 2f;
    float yPos = (height / 2f) - ((paint.descent() + paint.ascent()) / 2);

    // Draw Glow effect
    if (isGlowEnabled) {
        Paint glowPaint = new Paint(paint);
        glowPaint.setColor(glowColor);
        glowPaint.setMaskFilter(new BlurMaskFilter(30, BlurMaskFilter.Blur.NORMAL));
        canvas.drawText(text, xPos, yPos, glowPaint);
    }

    // Draw Shadow effect
    if (isShadowEnabled) {
        Paint shadowPaint = new Paint(paint);
        shadowPaint.setColor(shadowColor);
        shadowPaint.setShadowLayer(10, 5, 5, shadowColor);
        canvas.drawText(text, xPos, yPos, shadowPaint);
    }

    // Draw Stroke effect
    if (isStrokeEnabled) {
        Paint strokePaint = new Paint(paint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setColor(strokeColor);
        canvas.drawText(text, xPos, yPos, strokePaint);
    }

    // Draw main text
    canvas.drawText(text, xPos, yPos, paint);

    String imagePath = new File(tempDir, String.format("frame%04d.png", index)).getAbsolutePath();
    try (FileOutputStream out = new FileOutputStream(imagePath)) {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        return imagePath;
    } catch (IOException e) {
        e.printStackTrace();
        return null;
    }
}


private void deleteGeneratedFrames(File tempDir) {
    File[] files = tempDir.listFiles();
    if (files != null) {
        for (File file : files) {
            if (file.isFile()) {
                file.delete();
            }
        }
    }
    tempDir.delete();
}
    
    private void updateEstimatedSize() {
    String input = inputSeconds.getText().toString();
    if (!input.isEmpty()) {
        try {
            int seconds = Integer.parseInt(input);
            if (seconds > 0) {
                long estimatedSizeBytes = (long) selectedBitrate * seconds / 8;
                String estimatedSizeMB = String.format("%.2f", estimatedSizeBytes / (1024.0 * 1024.0));
                estimatedSizeText.setText("Estimated Size: " + estimatedSizeMB + " MB");
            } else {
                estimatedSizeText.setText("Estimated Size: 0 MB");
            }
        } catch (NumberFormatException e) {
            estimatedSizeText.setText("Estimated Size: -- MB");
        }
    } else {
        estimatedSizeText.setText("Estimated Size: -- MB");
    }
}

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, REQUEST_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission required to save videos.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
