package com.shukun.birthdayreminder.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.shukun.birthdayreminder.model.BirthdayPerson;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class BirthdayRepository {
    private static final String PREFS = "birthday_people";
    private static final String KEY_PEOPLE = "people";

    private final SharedPreferences preferences;
    private List<BirthdayPerson> cache;

    public BirthdayRepository(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized List<BirthdayPerson> getAll() {
        return new ArrayList<>(ensureLoaded());
    }

    public synchronized BirthdayPerson findById(String id) {
        for (BirthdayPerson person : ensureLoaded()) {
            if (person.id.equals(id)) {
                return person;
            }
        }
        return null;
    }

    public synchronized BirthdayPerson findByName(String name) {
        String expected = name == null ? "" : name.trim();
        for (BirthdayPerson person : ensureLoaded()) {
            if (person.name.trim().equalsIgnoreCase(expected)) {
                return person;
            }
        }
        return null;
    }

    public synchronized void upsert(BirthdayPerson person) {
        List<BirthdayPerson> people = ensureLoaded();
        boolean replaced = false;
        for (int index = 0; index < people.size(); index++) {
            if (people.get(index).id.equals(person.id)) {
                people.set(index, person);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            people.add(person);
        }
        persist(people);
    }

    public synchronized void delete(String id) {
        List<BirthdayPerson> people = ensureLoaded();
        people.removeIf(person -> person.id.equals(id));
        persist(people);
    }

    private List<BirthdayPerson> ensureLoaded() {
        if (cache == null) {
            cache = parse(preferences.getString(KEY_PEOPLE, "[]"));
        }
        return cache;
    }

    private List<BirthdayPerson> parse(String raw) {
        List<BirthdayPerson> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int index = 0; index < array.length(); index++) {
                try {
                    result.add(BirthdayPerson.fromJson(array.getJSONObject(index)));
                } catch (JSONException ignored) {
                    // Keep other valid records if one entry was damaged.
                }
            }
        } catch (JSONException ignored) {
            return result;
        }
        Collections.sort(result, Comparator.comparing(person -> person.name));
        return result;
    }

    private void persist(List<BirthdayPerson> people) {
        Collections.sort(people, Comparator.comparing(person -> person.name));
        JSONArray array = new JSONArray();
        for (BirthdayPerson person : people) {
            try {
                array.put(person.toJson());
            } catch (JSONException ignored) {
                // A record made from strongly typed fields should always serialize.
            }
        }
        preferences.edit().putString(KEY_PEOPLE, array.toString()).apply();
        cache = people;
    }
}
