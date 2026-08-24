package com.shukun.birthdayreminder;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputFilter;
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
import com.shukun.birthdayreminder.data.BirthdayBackupService;
import com.shukun.birthdayreminder.data.BirthdayRepository;
import com.shukun.birthdayreminder.lunar.LunarCalendarService;
import com.shukun.birthdayreminder.lunar.LunarDate;
import com.shukun.birthdayreminder.model.BirthdayPerson;
import com.shukun.birthdayreminder.notify.NotificationHelper;
import com.shukun.birthdayreminder.util.SolarDate;
import com.shukun.birthdayreminder.util.SolarDateRules;
import com.shukun.birthdayreminder.util.NoteText;
import com.shukun.birthdayreminder.util.ZodiacSign;
import com.shukun.birthdayreminder.ui.HomeCards;
import com.shukun.birthdayreminder.ui.Views;
import com.shukun.birthdayreminder.update.UpdateInfo;
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
    private static final int REQUEST_EXPORT_BACKUP = 1002;
    private static final int REQUEST_IMPORT_BACKUP = 1003;

    private BirthdayRepository repository;
    private BirthdayBackupService backupService;
    private ReminderScheduler scheduler;
    private LunarCalendarService lunarService;
    private LinearLayout content;
    private ScrollView pageScroll;
    private final HomeCards.Listener homeCardsListener = new HomeCards.Listener() {
        @Override public boolean hasNotificationPermission() { return MainActivity.this.hasNotificationPermission(); }
        @Override public boolean canScheduleExact() { return scheduler.canScheduleExact(); }
        @Override public void requestNotificationPermission() { MainActivity.this.requestNotificationPermission(); }
        @Override public void openExactAlarmSettings() { MainActivity.this.openExactAlarmSettings(); }
        @Override public void chooseBackupDestination() { MainActivity.this.chooseBackupDestination(); }
        @Override public void chooseBackupFile() { MainActivity.this.chooseBackupFile(); }
        @Override public void checkForUpdates(Button button) { MainActivity.this.checkForUpdates(button); }
        @Override public void renderHome() { render(); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        repository = new BirthdayRepository(this);
        backupService = new BirthdayBackupService();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_EXPORT_BACKUP) {
            exportBackup(uri);
        } else if (requestCode == REQUEST_IMPORT_BACKUP) {
            importBackup(uri);
        }
    }

    private void render() {
        int previousScroll = pageScroll != null ? pageScroll.getScrollY() : 0;

        pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(true);
        pageScroll.setClipToPadding(false);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Views.dp(this, 20), Views.dp(this, 28), Views.dp(this, 20), Views.dp(this, 36));
        pageScroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView eyebrow = Views.text(this, "生日管家", 13, getColor(R.color.primary), Typeface.BOLD);
        eyebrow.setLetterSpacing(0.16f);
        content.addView(eyebrow);

        TextView title = Views.text(this, "双历生日提醒", 30, getColor(R.color.text_primary), Typeface.BOLD);
        content.addView(title, Views.marginParams(this, 0, 4, 0, 6));

        TextView subtitle = Views.text(this, "记住一个公历生日，同时守住每年的公历与农历那一天。", 15,
                getColor(R.color.text_secondary), Typeface.NORMAL);
        subtitle.setLineSpacing(0, 1.18f);
        content.addView(subtitle, Views.marginParams(this, 0, 0, 0, 20));

        content.addView(HomeCards.permissionCard(this, homeCardsListener), Views.marginParams(this, 0, 0, 0, 18));

        content.addView(HomeCards.updateCard(this, homeCardsListener), Views.marginParams(this, 0, 0, 0, 18));

        content.addView(HomeCards.backupCard(this, homeCardsListener), Views.marginParams(this, 0, 0, 0, 18));

        Button addButton = new Button(this);
        addButton.setText("＋  添加生日");
        addButton.setTextSize(17);
        addButton.setAllCaps(false);
        addButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        addButton.setTextColor(Color.WHITE);
        addButton.getBackground().setTint(getColor(R.color.primary));
        addButton.setMinHeight(Views.dp(this, 54));
        addButton.setOnClickListener(view -> showAddDialog());
        content.addView(addButton, Views.marginParams(this, 0, 0, 0, 26));

        List<BirthdayPerson> people = repository.getAll();
        List<PersonBirthdayStatus> birthdayStatuses = buildBirthdayStatuses(
                people, System.currentTimeMillis());
        TextView section = Views.text(this, "生日列表  ·  " + people.size(), 18,
                getColor(R.color.text_primary), Typeface.BOLD);
        content.addView(section, Views.marginParams(this, 0, 0, 0, 12));

        if (people.isEmpty()) {
            content.addView(buildEmptyState(), Views.marginParams(this, 0, 0, 0, 18));
        } else {
            for (PersonBirthdayStatus status : birthdayStatuses) {
                content.addView(buildPersonCard(status), Views.marginParams(this, 0, 0, 0, 14));
            }
        }

        TextView note = Views.text(this, "说明：提醒时间是设备所在时区的当天 00:00。2 月 29 日在非闰年按 2 月 28 日提醒；农历三十遇小月按廿九提醒。",
                12, getColor(R.color.text_secondary), Typeface.NORMAL);
        note.setLineSpacing(0, 1.2f);
        content.addView(note, Views.marginParams(this, 2, 8, 2, 0));

        setContentView(pageScroll);

        if (previousScroll > 0) {
            pageScroll.post(() -> pageScroll.scrollTo(0, previousScroll));
        }
    }

    private View buildEmptyState() {
        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(Views.dp(this,24), Views.dp(this,34), Views.dp(this,24), Views.dp(this,34));
        empty.setBackgroundResource(R.drawable.bg_card);

        TextView cake = Views.text(this,"🎂", 38, getColor(R.color.text_primary), Typeface.NORMAL);
        empty.addView(cake);
        TextView title = Views.text(this,"还没有生日", 17, getColor(R.color.text_primary), Typeface.BOLD);
        empty.addView(title, Views.marginParams(this,0, 8, 0, 4));
        TextView body = Views.text(this,"添加一个人的姓名与公历出生日期，农历生日会自动换算。", 13,
                getColor(R.color.text_secondary), Typeface.NORMAL);
        body.setGravity(Gravity.CENTER);
        empty.addView(body);
        return empty;
    }

    private void chooseBackupDestination() {
        Calendar now = Calendar.getInstance();
        String fileName = String.format(Locale.CHINA,
                "生日管家备份-%04d%02d%02d.json",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH));
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json")
                .putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, REQUEST_EXPORT_BACKUP);
    }

    private void chooseBackupFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*");
        startActivityForResult(intent, REQUEST_IMPORT_BACKUP);
    }

    private void exportBackup(Uri destination) {
        List<BirthdayPerson> snapshot = repository.getAll();
        new Thread(() -> {
            try {
                backupService.write(this, destination, snapshot);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this,
                            "已导出 " + snapshot.size() + " 人，卸载后备份文件仍会保留",
                            Toast.LENGTH_LONG).show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this,
                            "导出失败：" + readableError(error), Toast.LENGTH_LONG).show();
                });
            }
        }, "birthday-backup-export").start();
    }

    private void importBackup(Uri source) {
        new Thread(() -> {
            try {
                List<BirthdayBackupService.BackupPerson> people = backupService.read(this, source);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (people.isEmpty()) {
                        Toast.makeText(this, "备份文件中没有生日记录", Toast.LENGTH_LONG).show();
                    } else {
                        new ImportSession(people).continueImport();
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this,
                            "导入失败：" + readableError(error), Toast.LENGTH_LONG).show();
                });
            }
        }, "birthday-backup-import").start();
    }

    private String readableError(Exception error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? "文件无法处理" : message;
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

        TextView name = Views.text(this,person.name, 21, getColor(R.color.text_primary), Typeface.BOLD);
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
        String zodiac = ZodiacSign.signName(person.birthMonth, person.birthDay);
        String solarText = String.format(Locale.CHINA, "公历  %04d年%02d月%02d日  ·  %s",
                person.birthYear, person.birthMonth, person.birthDay, zodiac);
        String lunarText = "农历  " + lunarService.format(birthLunar, true);
        card.addView(Views.text(this,solarText, 14, getColor(R.color.text_secondary), Typeface.NORMAL),
                Views.marginParams(this,0, 9, 0, 3));
        card.addView(Views.text(this,lunarText, 14, getColor(R.color.text_secondary), Typeface.NORMAL));

        if (!person.note.isEmpty()) {
            TextView note = Views.text(this,"备注  " + NoteText.preview(person.note, 10) + "  ›", 13,
                    getColor(R.color.primary), Typeface.BOLD);
            note.setSingleLine(true);
            note.setContentDescription("查看" + person.name + "的完整备注");
            note.setOnClickListener(view -> showNoteDialog(person));
            card.addView(note, Views.marginParams(this,0, 9, 0, 0));
        }

        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.divider));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Views.dp(this,1));
        dividerParams.topMargin = Views.dp(this,14);
        dividerParams.bottomMargin = Views.dp(this,12);
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
        TextView next = Views.text(this,nextText, 13,
                getColor(person.enabled ? R.color.primary : R.color.text_secondary), Typeface.BOLD);
        next.setLineSpacing(0, 1.25f);
        card.addView(next);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.BOTTOM);

        TextView edit = Views.actionButton(this,"修改");
        edit.setMinWidth(Views.dp(this,76));
        edit.setOnClickListener(view -> showPersonEditor(person));
        actions.addView(edit);

        TextView editNote = Views.actionButton(this,"备注");
        editNote.setMinWidth(Views.dp(this,76));
        editNote.setContentDescription((person.note.isEmpty() ? "添加" : "修改")
                + person.name + "的备注");
        editNote.setOnClickListener(view -> showNoteEditor(person));
        LinearLayout.LayoutParams noteParams = Views.wrapParams(Gravity.BOTTOM);
        noteParams.leftMargin = Views.dp(this,8);
        actions.addView(editNote, noteParams);

        View actionSpacer = new View(this);
        actions.addView(actionSpacer, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout deleteColumn = new LinearLayout(this);
        deleteColumn.setOrientation(LinearLayout.VERTICAL);
        deleteColumn.setGravity(Gravity.CENTER_HORIZONTAL);

        String countdownText = status.daysUntil == 0
                ? "今天生日"
                : "距下次生日还有 " + status.daysUntil + " 天";
        TextView countdown = Views.text(this,countdownText, 12, getColor(R.color.primary), Typeface.BOLD);
        countdown.setGravity(Gravity.CENTER);
        deleteColumn.addView(countdown, Views.marginParams(this,0, 0, 0, 3));

        TextView delete = Views.actionButton(this,"删除");
        delete.setOnClickListener(view -> confirmDelete(person));
        deleteColumn.addView(delete);
        actions.addView(deleteColumn);
        card.addView(actions, Views.marginParams(this,0, 5, 0, 0));
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

    private final class ImportSession {
        private final List<BirthdayBackupService.BackupPerson> entries;
        private int index;
        private int imported;
        private int skipped;

        ImportSession(List<BirthdayBackupService.BackupPerson> entries) {
            this.entries = entries;
        }

        void continueImport() {
            while (index < entries.size()) {
                BirthdayBackupService.BackupPerson incoming = entries.get(index++);
                BirthdayPerson existing = repository.findByName(incoming.name);
                if (existing == null) {
                    saveIncoming(incoming, incoming.name, UUID.randomUUID().toString());
                    imported++;
                } else {
                    showConflict(incoming, existing);
                    return;
                }
            }
            finishImport();
        }

        private void showConflict(BirthdayBackupService.BackupPerson incoming,
                                  BirthdayPerson existing) {
            String currentDate = String.format(Locale.CHINA, "%04d-%02d-%02d",
                    existing.birthYear, existing.birthMonth, existing.birthDay);
            String incomingDate = String.format(Locale.CHINA, "%04d-%02d-%02d",
                    incoming.birthYear, incoming.birthMonth, incoming.birthDay);
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("发现同名人员：“" + incoming.name + "”")
                    .setMessage("现有生日：" + currentDate + "\n导入生日：" + incomingDate
                            + "\n\n请选择如何处理这条记录。")
                    .setPositiveButton("覆盖原记录", (ignored, which) -> {
                        saveIncoming(incoming, existing.name, existing.id);
                        imported++;
                        continueImport();
                    })
                    .setNeutralButton("修改导入姓名", (ignored, which) ->
                            showRenameDialog(incoming, existing))
                    .setNegativeButton("跳过", (ignored, which) -> {
                        skipped++;
                        continueImport();
                    })
                    .create();
            dialog.setOnCancelListener(ignored -> {
                skipped++;
                continueImport();
            });
            dialog.show();
        }

        private void showRenameDialog(BirthdayBackupService.BackupPerson incoming,
                                      BirthdayPerson existing) {
            EditText input = new EditText(MainActivity.this);
            input.setSingleLine(true);
            input.setText(incoming.name + "（导入）");
            input.setSelectAllOnFocus(true);
            int padding = Views.dp(MainActivity.this, 22);
            input.setPadding(padding, Views.dp(MainActivity.this, 6), padding, 0);

            AlertDialog renameDialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("修改导入姓名")
                    .setMessage("输入一个没有在当前列表中使用的姓名或称呼。")
                    .setView(input)
                    .setNegativeButton("返回", null)
                    .setPositiveButton("确认导入", null)
                    .create();
            renameDialog.setOnShowListener(ignored -> {
                renameDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    String renamed = input.getText().toString().trim();
                    if (renamed.isEmpty()) {
                        input.setError("请输入姓名或称呼");
                        return;
                    }
                    if (renamed.length() > 80) {
                        input.setError("姓名或称呼不能超过 80 个字符");
                        return;
                    }
                    if (repository.findByName(renamed) != null) {
                        input.setError("这个名字仍然已存在，请换一个名字");
                        return;
                    }
                    saveIncoming(incoming, renamed, UUID.randomUUID().toString());
                    imported++;
                    renameDialog.dismiss();
                    continueImport();
                });
                renameDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> {
                    renameDialog.dismiss();
                    showConflict(incoming, existing);
                });
            });
            renameDialog.setOnCancelListener(ignored -> showConflict(incoming, existing));
            renameDialog.show();
        }

        private void saveIncoming(BirthdayBackupService.BackupPerson incoming,
                                  String name, String id) {
            LunarDate lunar = lunarService.solarToLunar(
                    incoming.birthYear, incoming.birthMonth, incoming.birthDay);
            repository.upsert(new BirthdayPerson(
                    id, name,
                    incoming.birthYear, incoming.birthMonth, incoming.birthDay,
                    lunar.month, lunar.day, lunar.leapMonth,
                    incoming.note,
                    incoming.enabled));
        }

        private void finishImport() {
            render();
            runInBackground(scheduler::rescheduleAll);
            Toast.makeText(MainActivity.this,
                    "导入完成：已导入或更新 " + imported + " 人，跳过 " + skipped + " 人",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showAddDialog() {
        showPersonEditor(null);
    }

    private void showPersonEditor(BirthdayPerson existing) {
        boolean editing = existing != null;
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Views.dp(this,24), Views.dp(this,8), Views.dp(this,24), 0);

        TextView nameLabel = Views.text(this,"姓名", 13, getColor(R.color.text_secondary), Typeface.BOLD);
        form.addView(nameLabel);
        EditText nameInput = new EditText(this);
        nameInput.setHint("例如：妈妈");
        nameInput.setSingleLine(true);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        if (editing) nameInput.setText(existing.name);
        form.addView(nameInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView dateLabel = Views.text(this,"公历出生日期", 13, getColor(R.color.text_secondary), Typeface.BOLD);
        form.addView(dateLabel, Views.marginParams(this,0, 14, 0, 3));

        int[] selected = editing
                ? new int[]{existing.birthYear, existing.birthMonth, existing.birthDay}
                : new int[]{1990, 1, 1};
        Button dateButton = new Button(this);
        dateButton.setAllCaps(false);
        updateDateButton(dateButton, selected);
        dateButton.setOnClickListener(view -> showDatePicker(dateButton, selected));
        form.addView(dateButton);

        TextView noteLabel = Views.text(this,"备注（可选）", 13,
                getColor(R.color.text_secondary), Typeface.BOLD);
        form.addView(noteLabel, Views.marginParams(this,0, 14, 0, 3));
        EditText noteInput = new EditText(this);
        noteInput.setHint("例如：喜欢的礼物、饮食偏好或其他信息");
        noteInput.setSingleLine(true);
        noteInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        noteInput.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(BirthdayPerson.MAX_NOTE_LENGTH)});
        if (editing) noteInput.setText(existing.note);
        form.addView(noteInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = Views.text(this,"保存后会自动得到对应农历生日，并安排两套年度提醒。", 12,
                getColor(R.color.text_secondary), Typeface.NORMAL);
        form.addView(hint, Views.marginParams(this,0, 8, 0, 0));

        ScrollView formScroll = new ScrollView(this);
        formScroll.addView(form, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? "修改生日" : "添加生日")
                .setView(formScroll)
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
                            noteInput.getText().toString().trim(),
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

    private void showNoteDialog(BirthdayPerson person) {
        new AlertDialog.Builder(this)
                .setTitle(person.name + "的备注")
                .setMessage(person.note)
                .setNegativeButton("关闭", null)
                .setPositiveButton("修改备注", (dialog, which) -> showNoteEditor(person))
                .show();
    }

    private void showNoteEditor(BirthdayPerson person) {
        BirthdayPerson current = repository.findById(person.id);
        if (current == null) return;

        EditText input = new EditText(this);
        input.setHint("输入备注内容");
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setMinLines(4);
        input.setMaxLines(8);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(BirthdayPerson.MAX_NOTE_LENGTH)});
        input.setBackgroundResource(R.drawable.bg_card);
        input.setText(current.note);
        input.setSelection(input.length());

        LinearLayout inputContainer = new LinearLayout(this);
        inputContainer.setPadding(Views.dp(this,20), Views.dp(this,8), Views.dp(this,20), 0);
        inputContainer.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle(current.note.isEmpty() ? "添加备注" : "修改备注")
                .setView(inputContainer)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    BirthdayPerson latest = repository.findById(current.id);
                    if (latest == null) return;
                    repository.upsert(latest.withNote(input.getText().toString().trim()));
                    render();
                    Toast.makeText(this, "备注已保存", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showDatePicker(Button dateButton, int[] selected) {
        LinearLayout pickerContent = new LinearLayout(this);
        pickerContent.setOrientation(LinearLayout.VERTICAL);
        pickerContent.setPadding(Views.dp(this,20), Views.dp(this,8), Views.dp(this,20), 0);

        TextView lunarPreview = Views.text(this,"", 16, getColor(R.color.primary), Typeface.BOLD);
        lunarPreview.setGravity(Gravity.CENTER);
        lunarPreview.setLineSpacing(0, 1.18f);
        pickerContent.addView(lunarPreview, Views.marginParams(this,0, 0, 0, 8));

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

    private void runInBackground(Runnable runnable) {
        new Thread(runnable, "birthday-scheduler").start();
    }
}
