package com.example.drinkplus.broadcast;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.example.drinkplus.R;
import com.example.drinkplus.data.StreakManager;
import com.example.drinkplus.ui.LoginActivity;

/**
 * NotificationReceiver: Responsável pela manipulação e recebimento de intents em segundo plano enviadas por alarmes,
 * extraindo estatísticas de sequência/ofensiva para disparar lembretes personalizados altamente informativos.
 */
public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "channel_drinkplus_daily_reminders";
    private static final String CHANNEL_NAME = "Lembretes DRINK+";
    private static final int NOTIFICATION_ID = 551;

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        // Criar o canal de notificação para Android Oreo (8.0) ou superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription("Central ativa de avisos motivacionais para manter sua taxa de hidratação em dia.");
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        // Criar um intent de clique que leva o usuário direto para a LoginActivity
        Intent clickIntent = new Intent(context, LoginActivity.class);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Obter ofensiva atual para personalizar a mensagem motivacional
        StreakManager streakManager = new StreakManager(context);
        int currentStreak = streakManager.getStreakDays();

        String notificationTitle = "Hora de se hidratar! 💧";
        String notificationBody;

        if (currentStreak > 0) {
            notificationBody = String.format("A sua ofensiva atual é de %d dias! Beba um copo de água agora para continuar mantendo este ritmo excelente!", currentStreak);
        } else {
            notificationBody = "Mantenha o seu organismo funcionando a todo vapor! Beba água agora com o DRINK+.";
        }

        // Configuração de notificação limpa e com alto contraste
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_today) // Ícone nativo de alta legibilidade
                .setContentTitle(notificationTitle)
                .setContentText(notificationBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationBody))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
