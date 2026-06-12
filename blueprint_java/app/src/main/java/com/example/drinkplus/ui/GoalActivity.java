package com.example.drinkplus.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.drinkplus.R;

/**
 * GoalActivity: Configuração de Metas Personalizadas.
 * Executa a equação: Peso (kg) x 35 mL para obter a ingestão de água diária sugerida.
 */
public class GoalActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etWeight;
    private Button btnSaveGoal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal);

        etName = findViewById(R.id.et_name);
        etWeight = findViewById(R.id.et_weight);
        btnSaveGoal = findViewById(R.id.btn_save_goal);

        // Pré-carregar dados salvos caso existam
        SharedPreferences prefs = getSharedPreferences("drinkplus_prefs", MODE_PRIVATE);
        String savedName = prefs.getString("user_name", "");
        int savedWeight = prefs.getInt("user_weight", 0);

        if (!TextUtils.isEmpty(savedName)) {
            etName.setText(savedName);
        }
        if (savedWeight > 0) {
            etWeight.setText(String.valueOf(savedWeight));
        }

        btnSaveGoal.setOnClickListener(v -> saveUserGoalConfiguration());
    }

    private void saveUserGoalConfiguration() {
        String name = etName.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(weightStr)) {
            Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int weightKg = Integer.parseInt(weightStr);
            if (weightKg <= 0 || weightKg > 500) {
                Toast.makeText(this, getString(R.string.error_invalid_weight), Toast.LENGTH_SHORT).show();
                return;
            }

            // Exemplo da Fórmula: Peso Kg * 35 = Meta Diária mL
            int calculatedGoalMl = weightKg * 35;

            // Salvar configurações localmente
            SharedPreferences prefs = getSharedPreferences("drinkplus_prefs", MODE_PRIVATE);
            prefs.edit()
                 .putString("user_name", name)
                 .putInt("user_weight", weightKg)
                 .putInt("daily_goal_ml", calculatedGoalMl)
                 .apply();

            Toast.makeText(this, String.format(getString(R.string.goal_recalculated), calculatedGoalMl), Toast.LENGTH_LONG).show();

            // Avançar para o Dashboard de consumo
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
            finish();

        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.error_invalid_weight), Toast.LENGTH_SHORT).show();
        }
    }
}
