package com.shukun.birthdayreminder;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.shukun.birthdayreminder.alarm.ReminderScheduler;
import com.shukun.birthdayreminder.data.BirthdayRepository;
import com.shukun.birthdayreminder.lunar.LunarCalendarService;
import com.shukun.birthdayreminder.lunar.LunarDate;
import com.shukun.birthdayreminder.model.BirthdayPerson;
import com.shukun.birthdayreminder.notify.NotificationHelper;
import com.shukun.birthdayreminder.util.SolarDate;
import com.shukun.birthdayreminder.util.SolarDateRules;
import com.shukun.birthdayreminder.update.UpdateInfo;
import com.shukun.birthdayreminder.update.UpdateInstallerActivity;
import com.shukun.birthdayreminder.update.UpdateManager;
import com.shukun.birthdayreminder.update.UpdateNotificationHelper;
import com.shukun.birthdayreminder.update.UpdatePreferences;
import com.shukun.birthdayreminder.update.UpdateScheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 1001;

    private BirthdayRepository repository;
    private ReminderScheduler scheduler;
    private LunarCalendarService lunarService;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        repository = new BirthdayRepository(this);
        scheduler = new ReminderScheduler(this);
        lunarService = new LunarCalendarService();
        NotificationHelper.createChannel(this);
        initializeUpdatesSafely();
        render();
    }

    private void initializeUpdatesSafely() {
        try {
            UpdateNotificationHelper.createChannel(this);
            UpdateManager.cleanupInstalledUpdate(this);
            UpdateScheduler.schedule(this);
        } catch (RuntimeException error) {
            // Updating is optional: vendor-specific scheduler/download service failures
            // must never prevent the birthday reminder UI from opening.
            Log.e("BirthdayUpdates", "Unable to initialize app updates", error);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repository != null) {
            render();
            runInBackground(scheduler::rescheduleAll);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            render();
            runInBackground(scheduler::rescheduleAll);
        }
    }

    private void render() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(28), dp(20), dp(36));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView eyebrow = text("生日管家", 13, getColor(R.color.primary), Typeface.BOLD);
        eyebrow.setLetterSpacing(0.16f);
        content.addView(eyebrow);

        TextView title = text("双历生日提醒", 30, getColor(R.color.text_primary), Typeface.BOLD);
        content.addView(title, marginParams(0, 4, 0, 6));

        TextView subtitle = text("记住一个公历生日，同时守住每年的公历与农历那一天。", 15,
                getColor(R.color.text_secondary), Typeface.NORMAL);
        subtitle.setLineSpacing(0, 1.18f);
        content.addView(subtitle, marginParams(0, 0, 0, 20));

        content.addView(buildPermissionCard(), marginParams(0, 0, 0, 18));

        content.addView(buildUpdateCard(), marginParams(0, 0, 0, 18));

        Button addButton = new Button(this);
        addButton.setText("＋  添加生日");
        addButton.setTextSize(17);
        addButton.setAllCaps(false);
        addButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        addButton.setTextColor(Color.WHITE);
        addButton.getBackground().setTint(getColor(R.color.primary));
        addButton.setMinHeight(dp(54));
        addButton.setOnClickListener(view -> showAddDialog());
        content.addView(addButton, marginParams(0, 0, 0, 26));

        List<BirthdayPerson> people = repository.getAll();
        List<PersonBirthdayStatus> birthdayStatuses = buildBirthdayStatuses(
                people, System.currentTimeMillis());
        TextView section = text("生日列表  ·  " + people.size(), 18,
                getColor(R.color.text_primary), Typeface.BOLD);
        content.addView(section, marginParams(0, 0, 0, 12));

        if (people.isEmpty()) {
            content.addView(buildEmptyState(), marginParams(0, 0, 0, 18));
        } else {
            for (PersonBirthdayStatus status : birthdayStatuses) {
                content.addView(buildPersonCard(status), marginParams(0, 0, 0, 14));
            }
        }

        TextView note = text("说明：提醒时间是设备所在时区的当天 00:00。2 月 29 日在非闰年按 2 月 28 日提醒；农历三十遇小月按廿九提醒。",
                12, getColor(R.color.text_secondary), Typeface.NORMAL);
        note.setLineSpacing(0, 1.2f);
        content.addView(note, marginParams(2, 8, 2, 0));

        setContentView(scrollView);
    }

    private View buildPermissionCard() {
        boolean notificationReady = hasNotificationPermission();
        boolean exactReady = scheduler.canScheduleExact();
        boolean ready = notificationReady && exactReady;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(ready ? R.drawable.bg_permission_ok : R.drawable.bg_permission_warning);

        String status = ready ? "提醒已就绪" : "需要完成提醒授权";
        TextView heading = text((ready ? "✓  " : "!  ") + status, 15,
                getColor(ready ? R.color.success : R.color.warning), Typeface.BOLD);
        card.addView(heading);

        String detail;
        if (ready) {
            detail = "通知和精确闹钟权限正常，将按 00:00 安排提醒。";
        } else if (!notificationReady && !exactReady) {
            detail = "还需要通知权限和“闹钟和提醒”权限。";
        } else if (!notificationReady) {
            detail = "还需要通知权限，否则提醒无法显示。";
        } else {
            detail = "还需要“闹钟和提醒”权限，才能尽量准时在 00:00 弹出。";
        }
        TextView body = text(detail, 13, getColor(R.color.text_secondary), Typeface.NORMAL);
        body.setLineSpacing(0, 1.15f);
        card.addView(body, marginParams(0, 5, 0, ready ? 0 : 8));

        if (!notificationReady) {
            Button button = compactButton("允许通知");
            button.setOnClickListener(view -> requestNotificationPermission());
            card.addView(button, wrapParams(Gravity.START));
        }
        if (!exactReady) {
            Button button = compactButton("允许精确提醒");
            button.setOnClickListener(view -> openExactAlarmSettings());
            card.addView(button, wrapParams(Gravity.START));
        }
        return card;
    }

    private View buildEmptyState() {
        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(24), dp(34), dp(24), dp(34));
        empty.setBackgroundResource(R.drawable.bg_card);

        TextView cake = text("🎂", 38, getColor(R.color.text_primary), Typeface.NORMAL);
        empty.addView(cake);
        TextView title = text("还没有生日", 17, getColor(R.color.text_primary), Typeface.BOLD);
        empty.addView(title, marginParams(0, 8, 0, 4));
        TextView body = text("添加一个人的姓名与公历出生日期，农历生日会自动换算。", 13,
                getColor(R.color.text_secondary), Typeface.NORMAL);
        body.setGravity(Gravity.CENTER);
        empty.addView(body);
        return empty;
    }

    private View buildUpdateCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);

        TextView heading = text("软件更新", 15, getColor(R.color.text_primary), Typeface.BOLD);
        card.addView(heading);

        long downloadId = UpdatePreferences.downloadId(this);
        int status = UpdateManager.downloadStatus(this, downloadId);
        boolean ready = status == DownloadManager.STATUS_SUCCESSFUL
                && UpdatePreferences.isVerified(this);
        String detail;
        if (ready) {
            detail = "版本 " + UpdatePreferences.downloadVersion(this) + " 已下载并通过安全校验。";
        } else if (status == DownloadManager.STATUS_PENDING
                || status == DownloadManager.STATUS_RUNNING) {
            detail = "正在下载版本 " + UpdatePreferences.downloadVersion(this) + "。";
        } else {
            detail = "当前版本 " + BuildConfig.VERSION_NAME + "，每天自动检查 GitHub Releases。";
        }
        TextView body = text(detail, 13, getColor(R.color.text_secondary), Typeface.NORMAL);
        card.addView(body, marginParams(0, 5, 0, 6));

        Switch autoDownload = new Switch(this);
        autoDownload.setText("发现新版后自动下载");
        autoDownload.setTextColor(getColor(R.color.text_primary));
        autoDownload.setChecked(UpdatePreferences.autoDownload(this));
        autoDownload.setOnCheckedChangeListener((button, checked) -> {
            UpdatePreferences.setAutoDownload(this, checked);
            render();
        });
        card.addView(autoDownload);

        Switch wifiOnly = new Switch(this);
        wifiOnly.setText("仅在 Wi-Fi 下自动下载");
        wifiOnly.setTextColor(getColor(R.color.text_primary));
        wifiOnly.setChecked(UpdatePreferences.wifiOnly(this));
        wifiOnly.setEnabled(UpdatePreferences.autoDownload(this));
        wifiOnly.setOnCheckedChangeListener((button, checked) ->
                UpdatePreferences.setWifiOnly(this, checked));
        card.addView(wifiOnly);

        Button action = compactButton(ready ? "安装已下载版本" : "立即检查更新");
        if (ready) {
            action.setOnClickListener(view -> startActivity(
                    new Intent(this, UpdateInstallerActivity.class)
                            .putExtra(UpdateInstallerActivity.EXTRA_DOWNLOAD_ID, downloadId)));
        } else {
            action.setOnClickListener(view -> checkForUpdates(action));
        }
        card.addView(action, wrapParams(Gravity.START));
        return card;
    }

    private void checkForUpdates(Button button) {
        button.setEnabled(false);
        button.setText("正在检查…");
        UpdateManager.checkAsync(this, (update, error) -> runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            button.setEnabled(true);
            button.setText("立即检查更新");
            if (error != null) {
                Toast.makeText(this, "检查失败，请确认网络正常后重试", Toast.LENGTH_LONG).show();
                return;
            }
            if (update == null) {
                Toast.makeText(this, "当前已经是最新版本", Toast.LENGTH_SHORT).show();
                return;
            }
            handleAvailableUpdate(update);
        }));
    }

    private void handleAvailableUpdate(UpdateInfo update) {
        if (!UpdatePreferences.autoDownload(this)) {
            UpdateNotificationHelper.showAvailable(this, update);
            new AlertDialog.Builder(this)
                    .setTitle("发现新版本 " + update.version)
                    .setMessage(update.notes.isEmpty() ? "可以前往 GitHub 下载新版。" : update.notes)
                    .setNegativeButton("以后再说", null)
                    .setPositiveButton("打开发布页", (dialog, which) ->
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl))))
                    .show();
            return;
        }
        try {
            UpdateManager.download(this, update);
            UpdateNotificationHelper.showDownloading(this, update.version);
            Toast.makeText(this, "已开始下载版本 " + update.version, Toast.LENGTH_LONG).show();
            render();
        } catch (Exception error) {
            Toast.makeText(this, "无法自动下载：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private List<PersonBirthdayStatus> buildBirthdayStatuses(
            List<BirthdayPerson> people, long nowMillis) {
        long birthdayReference = SolarDateRules.justBeforeTodayMillis(nowMillis);
        List<PersonBirthdayStatus> statuses = new ArrayList<>();
        for (BirthdayPerson person : people) {
            SolarDate nextSolar = SolarDateRules.nextBirthday(
                    person.birthMonth, person.birthDay, birthdayReference);
            SolarDate nextLunar = null;
            try {
                nextLunar = lunarService.nextLunarBirthday(
                        new LunarDate(person.lunarMonth, person.lunarDay,
                                person.lunarLeapMonth, 1), birthdayReference);
            } catch (IllegalStateException ignored) {
                // The solar birthday remains a reliable fallback for sorting and countdown.
            }
            SolarDate nearest = nextLunar != null && nextLunar.compareTo(nextSolar) < 0
                    ? nextLunar : nextSolar;
            statuses.add(new PersonBirthdayStatus(
                    person, nextSolar, nextLunar,
                    SolarDateRules.daysUntil(nearest, nowMillis)));
        }
        statuses.sort(Comparator
                .comparingInt((PersonBirthdayStatus status) -> status.daysUntil)
                .thenComparing(status -> status.person.name));
        return statuses;
    }

    private View buildPersonCard(PersonBirthdayStatus status) {
        BirthdayPerson person = status.person;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = text(person.name, 21, getColor(R.color.text_primary), Typeface.BOLD);
        top.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Switch enabled = new Switch(this);
        enabled.setContentDescription("启用或暂停" + person.name + "的生日提醒");
        enabled.setChecked(person.enabled);
        enabled.setOnCheckedChangeListener((button, checked) -> {
            BirthdayPerson updated = person.withEnabled(checked);
            repository.upsert(updated);
            runInBackground(() -> {
                if (checked) scheduler.schedulePerson(updated);
                else scheduler.cancelPerson(updated.id);
            });
        });
        top.addView(enabled);
        card.addView(top);

        LunarDate birthLunar = lunarService.solarToLunar(
                person.birthYear, person.birthMonth, person.birthDay);
        String solarText = String.format(Locale.CHINA, "公历  %04d年%02d月%02d日",
                person.birthYear, person.birthMonth, person.birthDay);
        String lunarText = "农历  " + lunarService.format(birthLunar, true);
        card.addView(text(solarText, 14, getColor(R.color.text_secondary), Typeface.NORMAL),
                marginParams(0, 9, 0, 3));
        card.addView(text(lunarText, 14, getColor(R.color.text_secondary), Typeface.NORMAL));

        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.divider));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dividerParams.topMargin = dp(14);
        dividerParams.bottomMargin = dp(12);
        card.addView(divider, dividerParams);

        String nextText;
        if (!person.enabled) {
            nextText = "提醒已暂停";
        } else {
            if (status.nextLunar == null) {
                nextText = "下次公历  " + status.nextSolar + "\n农历日期暂时无法计算";
            } else if (status.nextSolar.equals(status.nextLunar)) {
                nextText = "下次提醒  " + status.nextSolar + "（双历同日）";
            } else {
                nextText = "下次公历  " + status.nextSolar
                        + "\n下次农历  " + status.nextLunar;
            }
        }
        TextView next = text(nextText, 13,
                getColor(person.enabled ? R.color.primary : R.color.text_secondary), Typeface.BOLD);
        next.setLineSpacing(0, 1.25f);
        card.addView(next);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.BOTTOM);

        TextView edit = actionButton("修改");
        edit.setOnClickListener(view -> showPersonEditor(person));
        actions.addView(edit);

        View actionSpacer = new View(this);
        actions.addView(actionSpacer, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout deleteColumn = new LinearLayout(this);
        deleteColumn.setOrientation(LinearLayout.VERTICAL);
        deleteColumn.setGravity(Gravity.CENTER_HORIZONTAL);

        String countdownText = status.daysUntil == 0
                ? "今天生日"
                : "距下次生日还有 " + status.daysUntil + " 天";
        TextView countdown = text(countdownText, 12, getColor(R.color.primary), Typeface.BOLD);
        countdown.setGravity(Gravity.CENTER);
        deleteColumn.addView(countdown, marginParams(0, 0, 0, 3));

        TextView delete = actionButton("删除");
        delete.setOnClickListener(view -> confirmDelete(person));
        deleteColumn.addView(delete);
        actions.addView(deleteColumn);
        card.addView(actions, marginParams(0, 5, 0, 0));
        return card;
    }

    private static final class PersonBirthdayStatus {
        final BirthdayPerson person;
        final SolarDate nextSolar;
        final SolarDate nextLunar;
        final int daysUntil;

        PersonBirthdayStatus(BirthdayPerson person, SolarDate nextSolar,
                             SolarDate nextLunar, int daysUntil) {
            this.person = person;
            this.nextSolar = nextSolar;
            this.nextLunar = nextLunar;
            this.daysUntil = daysUntil;
        }
    }

    private void showAddDialog() {
        showPersonEditor(null);
    }

    private void showPersonEditor(BirthdayPerson existing) {
        boolean editing = existing != null;
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), 0);

        TextView nameLabel = text("姓名", 13, getColor(R.color.text_secondary), Typeface.BOLD);
        form.addView(nameLabel);
        EditText nameInput = new EditText(this);
        nameInput.setHint("例如：妈妈");
        nameInput.setSingleLine(true);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        if (editing) nameInput.setText(existing.name);
        form.addView(nameInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView dateLabel = text("公历出生日期", 13, getColor(R.color.text_secondary), Typeface.BOLD);
        form.addView(dateLabel, marginParams(0, 14, 0, 3));

        int[] selected = editing
                ? new int[]{existing.birthYear, existing.birthMonth, existing.birthDay}
                : new int[]{1990, 1, 1};
        Button dateButton = new Button(this);
        dateButton.setAllCaps(false);
        updateDateButton(dateButton, selected);
        dateButton.setOnClickListener(view -> showDatePicker(dateButton, selected));
        form.addView(dateButton);

        TextView hint = text("保存后会自动得到对应农历生日，并安排两套年度提醒。", 12,
                getColor(R.color.text_secondary), Typeface.NORMAL);
        form.addView(hint, marginParams(0, 8, 0, 0));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? "修改生日" : "添加生日")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String personName = nameInput.getText().toString().trim();
                    if (personName.isEmpty()) {
                        nameInput.setError("请输入姓名或称呼");
                        return;
                    }
                    if (isFutureDate(selected[0], selected[1], selected[2])) {
                        Toast.makeText(this, "出生日期不能晚于今天", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    LunarDate lunarDate = lunarService.solarToLunar(selected[0], selected[1], selected[2]);
                    BirthdayPerson person = new BirthdayPerson(
                            editing ? existing.id : UUID.randomUUID().toString(),
                            personName,
                            selected[0], selected[1], selected[2],
                            lunarDate.month, lunarDate.day, lunarDate.leapMonth,
                            !editing || existing.enabled
                    );
                    repository.upsert(person);
                    runInBackground(() -> scheduler.schedulePerson(person));
                    dialog.dismiss();
                    render();
                    Toast.makeText(this,
                            (editing ? "已更新，农历" : "已换算为农历")
                                    + lunarService.format(lunarDate, false),
                            Toast.LENGTH_LONG).show();
                    if (!editing) requestNotificationPermission();
                }));
        dialog.show();
    }

    private void showDatePicker(Button dateButton, int[] selected) {
        LinearLayout pickerContent = new LinearLayout(this);
        pickerContent.setOrientation(LinearLayout.VERTICAL);
        pickerContent.setPadding(dp(20), dp(8), dp(20), 0);

        TextView lunarPreview = text("", 16, getColor(R.color.primary), Typeface.BOLD);
        lunarPreview.setGravity(Gravity.CENTER);
        lunarPreview.setLineSpacing(0, 1.18f);
        pickerContent.addView(lunarPreview, marginParams(0, 0, 0, 8));

        DatePicker picker = new DatePicker(this);
        Calendar minimum = Calendar.getInstance();
        minimum.clear();
        minimum.set(1901, Calendar.JANUARY, 1);
        picker.setMinDate(minimum.getTimeInMillis());
        picker.setMaxDate(System.currentTimeMillis());
        picker.init(
                selected[0], selected[1] - 1, selected[2],
                (view, year, zeroBasedMonth, day) -> updateLunarPreview(
                        lunarPreview, year, zeroBasedMonth + 1, day)
        );
        updateLunarPreview(lunarPreview, selected[0], selected[1], selected[2]);
        pickerContent.addView(picker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("选择公历出生日期")
                .setView(pickerContent)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog, which) -> {
                    selected[0] = picker.getYear();
                    selected[1] = picker.getMonth() + 1;
                    selected[2] = picker.getDayOfMonth();
                    updateDateButton(dateButton, selected);
                })
                .show();
    }

    private void updateDateButton(Button button, int[] selected) {
        LunarDate lunar = lunarService.solarToLunar(selected[0], selected[1], selected[2]);
        button.setText(String.format(Locale.CHINA, "公历  %04d 年 %02d 月 %02d 日\n农历  %s",
                selected[0], selected[1], selected[2], lunarService.format(lunar, true)));
    }

    private void updateLunarPreview(TextView preview, int year, int month, int day) {
        LunarDate lunar = lunarService.solarToLunar(year, month, day);
        preview.setText(String.format(Locale.CHINA, "所选：%04d年%02d月%02d日\n农历：%s",
                year, month, day, lunarService.format(lunar, true)));
    }

    private void confirmDelete(BirthdayPerson person) {
        new AlertDialog.Builder(this)
                .setTitle("删除“" + person.name + "”？")
                .setMessage("对应的公历和农历提醒也会一并取消。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    scheduler.cancelPerson(person.id);
                    repository.delete(person.id);
                    render();
                })
                .show();
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return getSystemService(NotificationManager.class).areNotificationsEnabled();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        } else if (!getSystemService(NotificationManager.class).areNotificationsEnabled()) {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            } else {
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
            }
            startActivity(intent);
        }
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception error) {
            Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            fallback.setData(Uri.parse("package:" + getPackageName()));
            startActivity(fallback);
        }
    }

    private boolean isFutureDate(int year, int month, int day) {
        Calendar chosen = Calendar.getInstance();
        chosen.clear();
        chosen.set(year, month - 1, day, 0, 0, 0);
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 23);
        today.set(Calendar.MINUTE, 59);
        today.set(Calendar.SECOND, 59);
        return chosen.after(today);
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private Button compactButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(12), dp(5), dp(12), dp(5));
        return button;
    }

    private TextView actionButton(String label) {
        TextView button = text(label, 14, getColor(R.color.primary), Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.bg_action_button);
        button.setMinWidth(dp(96));
        button.setMinHeight(dp(40));
        button.setPadding(dp(18), dp(8), dp(18), dp(8));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private LinearLayout.LayoutParams marginParams(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private LinearLayout.LayoutParams wrapParams(int gravity) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = gravity;
        return params;
    }

    private void runInBackground(Runnable runnable) {
        new Thread(runnable, "birthday-scheduler").start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
