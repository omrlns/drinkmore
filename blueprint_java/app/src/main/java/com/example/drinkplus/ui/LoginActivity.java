package com.example.drinkplus.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.drinkplus.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

/**
 * LoginActivity: Gerencia os fluxos de login tradicional (Email/Senha) e autenticação segura com o Google.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9005;

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLoginTraditional;
    private SignInButton btnGoogleSignIn;

    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Verifica se o usuário já possui um peso cadastrado para pular diretamente para o Dashboard
        SharedPreferences prefs = getSharedPreferences("drinkplus_prefs", MODE_PRIVATE);
        int savedWeight = prefs.getInt("user_weight", 0);
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);

        if (isLoggedIn && savedWeight > 0) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // Bind das views com tratamento de alto contraste
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLoginTraditional = findViewById(R.id.btn_login_traditional);
        btnGoogleSignIn = findViewById(R.id.btn_google_signin);

        // Configuração segura do Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Listeners
        btnLoginTraditional.setOnClickListener(v -> handleTraditionalLogin());
        btnGoogleSignIn.setOnClickListener(v -> initiateGoogleSignIn());
    }

    private void initiateGoogleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String email = account.getEmail();
                String displayName = account.getDisplayName();

                SharedPreferences prefs = getSharedPreferences("drinkplus_prefs", MODE_PRIVATE);
                prefs.edit()
                     .putBoolean("is_logged_in", true)
                     .putString("user_email", email)
                     .putString("user_name", displayName)
                     .apply();

                Toast.makeText(this, "Autenticação via Google realizada!", Toast.LENGTH_SHORT).show();
                
                // Conduzir à configuração de metas
                navigateToGoalSetup();
            }
        } catch (ApiException e) {
            Log.e(TAG, "Falha na autenticação via Google. Código: " + e.getStatusCode());
            Toast.makeText(this, "Erro ao conectar com a conta Google. Tente novamente.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleTraditionalLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        // Simulação de validação padrão local
        if (email.contains("@") && password.length() >= 6) {
            SharedPreferences prefs = getSharedPreferences("drinkplus_prefs", MODE_PRIVATE);
            prefs.edit()
                 .putBoolean("is_logged_in", true)
                 .putString("user_email", email)
                 .putString("user_name", "Usuário DRINK+")
                 .apply();

            Toast.makeText(this, "Login efetuado com sucesso!", Toast.LENGTH_SHORT).show();
            navigateToGoalSetup();
        } else {
            Toast.makeText(this, getString(R.string.error_invalid_credentials), Toast.LENGTH_LONG).show();
        }
    }

    private void navigateToGoalSetup() {
        Intent intent = new Intent(this, GoalActivity.class);
        startActivity(intent);
        finish();
    }
}
