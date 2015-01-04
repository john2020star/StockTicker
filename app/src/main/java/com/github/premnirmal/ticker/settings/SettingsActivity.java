package com.github.premnirmal.ticker.settings;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.devpaul.filepickerlibrary.FilePickerActivity;
import com.github.premnirmal.ticker.BaseActivity;
import com.github.premnirmal.ticker.StocksApp;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.widget.StockWidget;
import com.github.premnirmal.tickerwidget.BuildConfig;
import com.github.premnirmal.tickerwidget.R;

import javax.inject.Inject;

/**
 * Created by premnirmal on 12/22/14.
 */
public class SettingsActivity extends BaseActivity {

    @Inject
    IStocksProvider stocksProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((StocksApp) getApplicationContext()).inject(this);

        final SharedPreferences preferences = getSharedPreferences(Tools.PREFS_NAME, Context.MODE_PRIVATE);

        setContentView(R.layout.activity_settings);
        findViewById(R.id.action_export).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FileExportTask() {
                    @Override
                    protected void onPostExecute(String result) {
                        if (result == null) {
                            Toast.makeText(SettingsActivity.this, R.string.error_exporting, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SettingsActivity.this, "Exported to " + result, Toast.LENGTH_SHORT).show();
                        }
                    }
                }.execute(stocksProvider.getTickers().toArray());
            }
        });

        findViewById(R.id.action_import).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent filePickerIntent = new Intent(SettingsActivity.this, FilePickerActivity.class);
                filePickerIntent.putExtra(FilePickerActivity.REQUEST_CODE, FilePickerActivity.REQUEST_FILE);
                filePickerIntent.putExtra(FilePickerActivity.INTENT_EXTRA_COLOR_ID, R.color.maroon);
                startActivityForResult(filePickerIntent, FilePickerActivity.REQUEST_FILE);
            }
        });

        final CheckBox autoSortCheckbox = (CheckBox) findViewById(R.id.autosort_checkbox);
        autoSortCheckbox.setChecked(preferences.getBoolean(Tools.SETTING_AUTOSORT, false));
        autoSortCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(Tools.SETTING_AUTOSORT, isChecked).commit();
            }
        });

        findViewById(R.id.change_text_size).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final SharedPreferences preferences = getSharedPreferences(Tools.PREFS_NAME, MODE_PRIVATE);
                final ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(SettingsActivity.this, R.array.font_sizes, R.layout.textview);
                final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        final int fontSizeDimen;
                        switch (position) {
                            case 1:
                                fontSizeDimen = R.integer.text_size_medium;
                                break;
                            case 2:
                                fontSizeDimen = R.integer.text_size_large;
                                break;
                            case 0:
                            default:
                                fontSizeDimen = R.integer.text_size_small;
                                break;
                        }
                        final int fontSize = getResources().getInteger(fontSizeDimen);
                        preferences.edit().remove(Tools.FONT_SIZE).putInt(Tools.FONT_SIZE, fontSize).commit();
                        broadcastUpdateWidget();
                        Toast.makeText(SettingsActivity.this, R.string.text_size_updated_message, Toast.LENGTH_SHORT).show();
                    }
                };
                createListDialog(arrayAdapter, onItemClickListener).show();

            }
        });

        findViewById(R.id.change_widget_background).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final SharedPreferences preferences = getSharedPreferences(Tools.PREFS_NAME, MODE_PRIVATE);
                final ArrayAdapter<String> arrayAdapter =
                        new ArrayAdapter<>(SettingsActivity.this, R.layout.textview, new String[]{"Transparent", "Translucent"});
                final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (position == 0) { // transparent
                            preferences.edit().putInt(Tools.WIDGET_BG, Tools.TRANSPARENT).commit();
                        } else { // translucent
                            preferences.edit().putInt(Tools.WIDGET_BG, Tools.TRANSLUCENT).commit();
                        }
                        broadcastUpdateWidget();
                        Toast.makeText(SettingsActivity.this, R.string.bg_updated_message, Toast.LENGTH_SHORT).show();
                    }
                };
                createListDialog(arrayAdapter, onItemClickListener).show();
            }
        });

        ((TextView) findViewById(R.id.version)).setText("Version " + BuildConfig.VERSION_NAME);
    }

    private void broadcastUpdateWidget() {
        final Intent intent = new Intent(getApplicationContext(), StockWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
        // since it seems the onUpdate() is only fired on that:
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(getApplicationContext());
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(getApplicationContext(), StockWidget.class));
        widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private AlertDialog createListDialog(ArrayAdapter arrayAdapter, final AdapterView.OnItemClickListener onItemClickListener) {
        final ListView view = new ListView(SettingsActivity.this);
        final int padding = (int) getResources().getDimension(R.dimen.text_padding);
        view.setPadding(padding, padding, padding, padding);
        view.setAdapter(arrayAdapter);
        final AlertDialog dialog = new AlertDialog.Builder(SettingsActivity.this)
                .setView(view)
                .create();
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onItemClickListener.onItemClick(parent, view, position, id);
                dialog.dismiss();
            }
        });
        return dialog;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FilePickerActivity.REQUEST_FILE
                && resultCode == RESULT_OK) {
            final String filePath = data.
                    getStringExtra(FilePickerActivity.FILE_EXTRA_DATA_PATH);
            if (filePath != null) {
                new FileImportTask(stocksProvider) {
                    @Override
                    protected void onPostExecute(Boolean result) {
                        if (result) {
                            showDialog(getString(R.string.ticker_import_success));
                            finish();
                        } else {
                            showDialog(getString(R.string.ticker_import_fail));
                        }
                    }
                }.execute(filePath);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
