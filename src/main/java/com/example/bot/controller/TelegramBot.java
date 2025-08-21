package com.example.bot.controller;

import com.example.bot.config.BotConfig;
import com.example.bot.entity.WeightRecord;
import com.example.bot.service.WeightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private WeightService weightService;

    final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start" -> startCommand(chatId);
                case "/menu" -> sendMenu(chatId);
                default -> {
                    if (messageText.startsWith("/")) {
                        sendMessage(chatId, "Неизвестная команда. Используйте /menu");
                    } else {
                        handleWeightInput(chatId, messageText);
                    }
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            handleCallback(chatId, callbackData);
        }
    }

    private void startCommand(long chatId) {
        sendMessage(chatId, "Привет! Я помогу тебе вести учёт веса при гемодиализе.");
        sendMenu(chatId);
    }

    private void sendMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите действие:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton preWeightBtn = new InlineKeyboardButton("Ввести вес ДО диализа");
        preWeightBtn.setCallbackData("pre_weight");

        InlineKeyboardButton postWeightBtn = new InlineKeyboardButton("Ввести вес ПОСЛЕ диализа");
        postWeightBtn.setCallbackData("post_weight");

        InlineKeyboardButton dryWeightBtn = new InlineKeyboardButton("Установить \"сухой\" вес");
        dryWeightBtn.setCallbackData("dry_weight");

        InlineKeyboardButton statsBtn = new InlineKeyboardButton("Статистика за месяц");
        statsBtn.setCallbackData("stats");

        rows.add(List.of(preWeightBtn));
        rows.add(List.of(postWeightBtn));
        rows.add(List.of(dryWeightBtn));
        rows.add(List.of(statsBtn));

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String waitingFor = ""; // Для отслеживания ожидаемого ввода

    private void handleCallback(long chatId, String callbackData) {
        switch (callbackData) {
            case "pre_weight" -> {
                waitingFor = "pre_weight";
                askForWeight(chatId, "Введите вес ДО диализа (кг):");
            }
            case "post_weight" -> {
                waitingFor = "post_weight";
                askForWeight(chatId, "Введите вес ПОСЛЕ диализа (кг):");
            }
            case "dry_weight" -> {
                waitingFor = "dry_weight";
                askForWeight(chatId, "Введите \"сухой\" вес (кг):");
            }
            case "stats" -> showStats(chatId);
        }
    }

    private void handleWeightInput(long chatId, String messageText) {
        try {
            double weight = Double.parseDouble(messageText);
            WeightRecord lastRecord = weightService.getLastRecord();
            WeightRecord newRecord = new WeightRecord();

            switch (waitingFor) {
                case "pre_weight" -> {
                    newRecord.setPreWeight(weight);
                    if (lastRecord != null && lastRecord.getPostWeight() != null) {
                        double diff = weight - lastRecord.getPostWeight();
                        sendMessage(chatId, String.format("Вес до диализа: %.2f кг\nРазница с предыдущим весом после диализа: %.2f кг", weight, diff));
                    } else {
                        sendMessage(chatId, String.format("Вес до диализа: %.2f кг", weight));
                    }
                }
                case "post_weight" -> {
                    newRecord.setPostWeight(weight);
                    if (lastRecord != null && lastRecord.getPreWeight() != null) {
                        double diff = lastRecord.getPreWeight() - weight;
                        sendMessage(chatId, String.format("Вес после диализа: %.2f кг\nУбрано веса: %.2f кг", weight, diff));
                    } else {
                        sendMessage(chatId, String.format("Вес после диализа: %.2f кг", weight));
                    }
                }
                case "dry_weight" -> {
                    newRecord.setDryWeight(weight);
                    sendMessage(chatId, String.format("Установлен \"сухой\" вес: %.2f кг", weight));
                }
            }

            weightService.save(newRecord);
            waitingFor = "";
            sendMenu(chatId);

        } catch (NumberFormatException e) {
            sendMessage(chatId, "Пожалуйста, введите корректное число.");
        }
    }

    private void askForWeight(long chatId, String text) {
        sendMessage(chatId, text);
    }

    private void showStats(long chatId) {
        List<WeightRecord> records = weightService.getLastMonthRecords();
        if (records.isEmpty()) {
            sendMessage(chatId, "Нет данных за последний месяц.");
            return;
        }

        StringBuilder sb = new StringBuilder("📊 Статистика за последний месяц:\n");
        for (WeightRecord r : records) {
            sb.append(String.format("📅 %s\n", r.getCreatedAt().toLocalDate()));
            if (r.getPreWeight() != null) sb.append("🔹 Вес до: ").append(String.format("%.2f", r.getPreWeight())).append(" кг\n");
            if (r.getPostWeight() != null) sb.append("🔹 Вес после: ").append(String.format("%.2f", r.getPostWeight())).append(" кг\n");
            if (r.getDryWeight() != null) sb.append("🔹 Сухой вес: ").append(String.format("%.2f", r.getDryWeight())).append(" кг\n");

            // Подсчет убранного веса
            if (r.getPreWeight() != null && r.getPostWeight() != null) {
                double removed = r.getPreWeight() - r.getPostWeight();
                sb.append("🔻 Убрано: ").append(String.format("%.2f", removed)).append(" кг\n");
            }
            sb.append("---\n");
        }

        sendMessage(chatId, sb.toString());
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}