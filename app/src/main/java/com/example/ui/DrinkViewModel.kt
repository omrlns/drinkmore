package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DrinkRepository
import com.example.data.HydrationLogEntity
import com.example.data.UserEntity
import com.example.notifications.HydrationNotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DrinkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DrinkRepository
    val activeUser: StateFlow<UserEntity?>

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    init {
        val drinkDao = AppDatabase.getDatabase(application).drinkDao()
        repository = DrinkRepository(drinkDao)
        activeUser = repository.activeUserFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        viewModelScope.launch {
            activeUser.collect { user ->
                if (user != null) {
                    reconcileStreak(user)
                }
            }
        }
    }

    private fun getTodayDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getYesterdayDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        return sdf.format(yesterday)
    }

    private fun reconcileStreak(user: UserEntity) {
        viewModelScope.launch {
            val today = getTodayDateKey()
            val yesterday = getYesterdayDateKey()
            val lastAchieved = user.lastGoalAchievedDate

            if (lastAchieved.isNotEmpty() && lastAchieved != today && lastAchieved != yesterday && user.streakDays > 0) {
                val updated = user.copy(streakDays = 0)
                repository.updateUser(updated)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayLogs: StateFlow<List<HydrationLogEntity>> = activeUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getLogsForDate(user.email, getTodayDateKey())
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun register(
        email: String,
        name: String,
        passwordPlain: String,
        weightKg: Double,
        reminderIntervalMinutes: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (email.isBlank() || name.isBlank() || passwordPlain.isBlank()) {
                onError("Por favor, preencha todos os campos do formulário.")
                return@launch
            }
            if (weightKg <= 0.0) {
                onError("Por favor, informe um peso corporal válido.")
                return@launch
            }

            try {
                val existing = repository.getUserByEmail(email)
                if (existing != null) {
                    onError("Este endereço de e-mail já está cadastrado.")
                    return@launch
                }

                // Calculate daily goal in mL dynamically: weight (kg) * 35 = recommended mL goal
                val dynamicGoalMl = (weightKg * 35).toInt()

                val newUser = UserEntity(
                    email = email.trim(),
                    name = name.trim(),
                    passwordHash = passwordPlain,
                    weightKg = weightKg,
                    dailyGoalMl = dynamicGoalMl,
                    reminderIntervalMinutes = reminderIntervalMinutes,
                    isLoggedIn = true
                )

                repository.logoutActiveUser() // Log out any previous profiles
                repository.insertUser(newUser)

                // Set up local reminders for the new user
                HydrationNotificationHelper.scheduleReminder(
                    getApplication(),
                    reminderIntervalMinutes
                )

                onSuccess()
            } catch (e: Exception) {
                onError("Erro ao registrar conta: ${e.localizedMessage}")
            }
        }
    }

    fun login(
        email: String,
        passwordPlain: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (email.isBlank() || passwordPlain.isBlank()) {
                onError("Por favor, preencha todos os campos de login.")
                return@launch
            }

            try {
                val user = repository.getUserByEmail(email.trim())
                if (user == null || user.passwordHash != passwordPlain) {
                    onError("Usuário ou senha incorretos.")
                    return@launch
                }

                repository.logoutActiveUser() // Log out others
                val loggedInUser = user.copy(isLoggedIn = true)
                repository.updateUser(loggedInUser)

                // Setup local alarms
                HydrationNotificationHelper.scheduleReminder(
                    getApplication(),
                    loggedInUser.reminderIntervalMinutes
                )

                onSuccess()
            } catch (e: Exception) {
                onError("Erro ao autenticar: ${e.localizedMessage}")
            }
        }
    }

    fun loginWithGoogle(
        email: String,
        name: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (email.isBlank() || name.isBlank()) {
                    onError("Conta do Google inválida ou inacessível.")
                    return@launch
                }

                val existing = repository.getUserByEmail(email.trim())
                if (existing != null) {
                    repository.logoutActiveUser()
                    val loggedInUser = existing.copy(isLoggedIn = true)
                    repository.updateUser(loggedInUser)

                    HydrationNotificationHelper.scheduleReminder(
                        getApplication(),
                        loggedInUser.reminderIntervalMinutes
                    )
                } else {
                    // Automatically register with default baseline values for Google users
                    // Baseline: 75kg -> 2625 mL daily recommended target, 60 minutes range.
                    val defaultWeight = 75.0
                    val computedGoal = (defaultWeight * 35).toInt()
                    val newUser = UserEntity(
                        email = email.trim(),
                        name = name.trim(),
                        passwordHash = "google_auth_placeholder_token",
                        weightKg = defaultWeight,
                        dailyGoalMl = computedGoal,
                        reminderIntervalMinutes = 60,
                        isLoggedIn = true
                    )
                    repository.logoutActiveUser()
                    repository.insertUser(newUser)

                    HydrationNotificationHelper.scheduleReminder(
                        getApplication(),
                        60
                    )
                }
                onSuccess()
                _uiMessage.value = "Conectado via Google com sucesso!"
            } catch (e: Exception) {
                onError("Autenticação Google falhou: ${e.localizedMessage}")
            }
        }
    }

    fun logWater(amountMl: Int) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            val log = HydrationLogEntity(
                userEmail = currentUser.email,
                amountMl = amountMl,
                timestamp = System.currentTimeMillis(),
                dateKey = getTodayDateKey()
            )
            repository.insertLog(log)

            // Dynamic quality of life: when logging water, refresh/reschedule the next alarm to start from now!
            if (currentUser.reminderIntervalMinutes > 0) {
                HydrationNotificationHelper.scheduleReminder(
                    getApplication(),
                    currentUser.reminderIntervalMinutes
                )
            }

            val today = getTodayDateKey()
            val currentSum = repository.getSumForDate(currentUser.email, today)
            if (currentSum >= currentUser.dailyGoalMl) {
                if (currentUser.lastGoalAchievedDate != today) {
                    val yesterday = getYesterdayDateKey()
                    val newStreak = if (currentUser.lastGoalAchievedDate == yesterday) {
                        currentUser.streakDays + 1
                    } else {
                        1
                    }
                    val updated = currentUser.copy(
                        streakDays = newStreak,
                        lastGoalAchievedDate = today
                    )
                    repository.updateUser(updated)
                }
            }

            // Use localizable structure hints
            _uiMessage.value = "SUCCESS_LOGGED_WATER:$amountMl"
        }
    }

    fun deleteLog(logId: Int) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            repository.deleteLog(logId)

            val today = getTodayDateKey()
            val currentSum = repository.getSumForDate(currentUser.email, today)
            if (currentSum < currentUser.dailyGoalMl && currentUser.lastGoalAchievedDate == today) {
                val newStreak = (currentUser.streakDays - 1).coerceAtLeast(0)
                val updated = currentUser.copy(
                    streakDays = newStreak,
                    lastGoalAchievedDate = ""
                )
                repository.updateUser(updated)
            }

            _uiMessage.value = "SUCCESS_LOG_DELETED"
        }
    }

    fun updateProfile(name: String, weightKg: Double, reminderIntervalMinutes: Int) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            if (name.isBlank() || weightKg <= 0.0) {
                _uiMessage.value = "Por favor, digite informações válidas."
                return@launch
            }

            try {
                val recalculatedGoal = (weightKg * 35).toInt()
                val today = getTodayDateKey()
                val currentSum = repository.getSumForDate(currentUser.email, today)

                var newStreak = currentUser.streakDays
                var newLastGoalAchievedDate = currentUser.lastGoalAchievedDate

                if (currentSum >= recalculatedGoal) {
                    if (currentUser.lastGoalAchievedDate != today) {
                        val yesterday = getYesterdayDateKey()
                        newStreak = if (currentUser.lastGoalAchievedDate == yesterday) {
                            currentUser.streakDays + 1
                        } else {
                            1
                        }
                        newLastGoalAchievedDate = today
                    }
                } else {
                    if (currentUser.lastGoalAchievedDate == today) {
                        newStreak = (currentUser.streakDays - 1).coerceAtLeast(0)
                        newLastGoalAchievedDate = ""
                    }
                }

                val updatedUser = currentUser.copy(
                    name = name.trim(),
                    weightKg = weightKg,
                    dailyGoalMl = recalculatedGoal,
                    reminderIntervalMinutes = reminderIntervalMinutes,
                    streakDays = newStreak,
                    lastGoalAchievedDate = newLastGoalAchievedDate
                )
                repository.updateUser(updatedUser)

                // Update notification timing
                HydrationNotificationHelper.scheduleReminder(
                    getApplication(),
                    reminderIntervalMinutes
                )

                _uiMessage.value = "SUCCESS_PROFILE_SAVED"
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao atualizar métricas: ${e.localizedMessage}"
            }
        }
    }

    fun logout() {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            // Cancel notification alarms
            HydrationNotificationHelper.cancelReminder(getApplication())

            repository.logoutActiveUser()
            _uiMessage.value = "SUCCESS_LOGGED_OUT"
        }
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }
}
