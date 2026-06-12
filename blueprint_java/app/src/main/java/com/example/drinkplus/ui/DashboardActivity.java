package com.example.drinkplus.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.drinkplus.R;
import com.example.drinkplus.broadcast.NotificationReceiver;
import com.example.drinkplus.data.StreakManager;

/**
 * DashboardActivity: Painel central interativo de controle de consumo e monitoramento.
 * Gerencia os atalhos de consumo rápido, atualiza o progresso visual de hidratação,
 * atualiza e visualiza o contador de ofensiva ativa (Streak) e agenda notificações automáticas.
 */
public class DashboardActivity extends AppCompatActivity {

    private TextView tvStreakCount;
    private TextView tvProgressStats;
    private ProgressBar pbHydrationMeter;
    private TextView tvInstructions;
    private Button btnLogout;

    private StreakManager streakManager;
    private int dailyGoalMl = 2500; // Valor de contingência padrão

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Instanciar o StreakManager local
        streakManager = new StreakManager(this);

        // Bind das views
        tvStreakCount = findViewById(R.id.tv_streak_count);
        tvProgressStats = findViewById(R.id.tv_progress_stats);
        pbHydrationMeter = findViewById(R.id.pb_hydration_meter);
        tvInstructions = findViewById(R.id.tv_instructions);
        btnLogout = findViewById(R.id.btn_logout);

        // Buscar a meta diária calculada
        SharedPreferences prefs = getSharedPreferences("drinkplus_prefs", MODE_PRIVATE);
        dailyGoalMl = prefs.getInt("daily_goal_ml", 2500);

        // Registrar atalhos de consumo rápido (grid de botões)
        setupPresetButton(R.id.btn_preset_100, 100);
        setupPresetButton(R.id.btn_preset_150, 150);
        setupPresetButton(R.id.btn_preset_200, 200);
        setupPresetButton(R.id.btn_preset_300, 300);
        setupPresetButton(R.id.btn_preset_500, 500);
        setupPresetButton(R.id.btn_preset_750, 750);

        // Listener de Logout
        btnLogout.setOnClickListener(v -> executeUserLogout());

        // Atualizar estado e agendar lembretes periódicos
        refreshDashboardState();
        scheduleBackgroundHydrationAlarms();
    }

    private void setupPresetButton(int buttonResId, final int amountMl) {
        Button btn = findViewById(buttonResId);
        if (btn != null) {
            btn.setOnClickListener(v -> logWaterHydration(amountMl));
        }
    }

    private void logWaterHydration(int amountMl) {
        // Tenta adicionar consumo em mL e verifica se alcançou a meta para incrementar a ofensiva
        boolean holdsNewStreak = streakManager.addWaterIntake(amountMl, dailyGoalMl);

        if (holdsNewStreak) {
            Toast.makeText(this, "Fabuloso! Meta diária concluída e ofensiva mantida com sucesso! 🔥", Toast.LENGTH_LONG).show();
        } else {
            String msg = String.format(getString(R.string.msg_logged_water), amountMl);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }

        refreshDashboardState();
    }

    private void refreshDashboardState() {
        int todayTotal = streakManager.getTodayIntake();
        int activeStreak = streakManager.getStreakDays();

        // Atualizar Ofensiva (Gamificação)
        if (activeStreak > 0) {
            tvStreakCount.setText(String.format(getString(R.string.streak_active_text), activeStreak));
        } else {
            tvStreakCount.setText(getString(R.string.streak_empty_text));
        }

        // Atualizar totalizadores de volume de água
        tvProgressStats.setText(todayTotal + " / " + dailyGoalMl + " mL");

        pbHydrationMeter.setMax(dailyGoalMl);
        pbHydrationMeter.setProgress(todayTotal);

        // Atualizar instruções de rodapé
        if (todayTotal >= dailyGoalMl) {
            tvInstructions.setText(getString(R.string.goal_completed));
        } else {
            int remaining = dailyGoalMl - todayTotal;
            tvInstructions.setText(String.format(getString(R.string.goal_remaining), remaining));
        }
    }

    /**
     * Agenda lembretes locais automáticos via AlarmManager em intervalos padrão sugeridos.
     */
    private void scheduleBackgroundHydrationAlarms() {
        Intent alarmIntent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            // Lembrete a cada 1 hora (60 minutos) para manter as boas métricas de saúde
            long intervalMillis = AlarmManager.INTERVAL_HOUR;
            long triggerAtMillis = SystemClock.elapsedRealtime() + intervalMillis;

            alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    intervalMillis,
                    pendingIntent
            );
        }
    }

    private void executeUserLogout() {
        // Redefinir preferências e dados locais de ofensiva
        SharedPreferences prefs = getSharedPreferences("drinkplus_prefs", MODE_PRIVATE);
        prefs.edit()
             .putBoolean("is_logged_in", false)
             .putString("user_email", "")
             .putString("user_name", "")
             .putInt("user_weight", 0)
             .putInt("daily_goal_ml", 0)
             .apply();

        streakManager.resetStreakAndTracker();

        // Parar alarmes de lembrete
        Intent alarmIntent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        Toast.makeText(this, getString(R.string.msg_logged_out), Toast.LENGTH_SHORT).show();

        // Voltar para a tela de login
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
