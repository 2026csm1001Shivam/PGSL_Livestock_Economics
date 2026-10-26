# Team split: GADVASU Livestock Economics App

Each person owns one enterprise plus part of the shared app. **Every file has exactly one owner**, so everyone can commit their own work without merge conflicts.

| # | Member | Enterprise | Also owns |
|---|--------|------------|-----------|
| 1 | Vaibhav | **Poultry** | Language, Login, Home and Settings screens; DailyEntryActivity (also used by Shivam's Livestock screen) |
| 2 | Shivam | **Livestock** | MainActivity (bottom navigation), BaseActivity (top bar), theme and colours, app icon, common strings |
| 3 | Pranav | **Goatery** | Batch screens (also used for Broiler and Fishery), FarmRepository (data storage, where Firebase will go), core data models, AndroidManifest, Gradle setup, TEAM.md |
| 4 | Vivek | **Fishery** | Calc (profit formulas), Ent (settings for all 5 enterprises), Dashboard, Reports and CSV export, Views.kt (lists, cards, profit slip), Fmt and Ext helpers |

## How the parts connect

- The app only builds once all four parts are in the repository: the screens call each other (for example Home opens Livestock, Poultry and the batch screens).
- Broiler, Goatery and Fishery share the batch screens (Pranav). What differs between them (unit name, count label, purchase-cost label, pond area, expense categories) is set in `Ent.kt` (Vivek).
- Livestock (Shivam) and Layer (Vaibhav) share `DailyEntryActivity`, owned by Vaibhav.
- Strings are split per feature (`strings_livestock.xml`, `strings_poultry.xml`, ...) in `values`, `values-hi` and `values-pa`. Add new text to **your own** strings file in all three folders.
- Need a change in a file you don't own? Ask its owner, or agree first, so two people don't edit the same file.

## Upload order (GitHub)

1. **Pranav** creates the repository and pushes his part first (it has the Gradle setup, manifest and this file).
2. **Vaibhav, Shivam and Vivek** each clone the repository, copy their own `GADVASUApp` folder over the clone (no file is replaced, only added), then commit and push from their own GitHub account.
3. When all four parts are in, open the `GADVASUApp` folder in Android Studio and run it.

```
git clone <repo-url>
# copy the contents of your part's GADVASUApp folder into the cloned GADVASUApp folder
git add .
git commit -m "Add <enterprise> module"
git pull --rebase
git push
```

## Files per person

### 1. Vaibhav: Poultry

Poultry chooser and Layer (egg) daily entry, the shared daily-entry logic, and the first screens a farmer sees: Language, Login, Home tiles, plus Settings.

- `app/src/main/java/com/gadvasu/livestockapp/ui/PoultryActivity.kt`
- `app/src/main/java/com/gadvasu/livestockapp/ui/LayerActivity.kt`
- `app/src/main/java/com/gadvasu/livestockapp/ui/DailyEntryActivity.kt`
- `app/src/main/java/com/gadvasu/livestockapp/ui/LanguageActivity.kt`
- `app/src/main/java/com/gadvasu/livestockapp/ui/LoginActivity.kt`
- `app/src/main/java/com/gadvasu/livestockapp/ui/HomeFragment.kt`
- `app/src/main/java/com/gadvasu/livestockapp/ui/SettingsFragment.kt`
- `app/src/main/res/layout/activity_poultry.xml`
- `app/src/main/res/layout/activity_layer.xml`
- `app/src/main/res/layout/activity_language.xml`
- `app/src/main/res/layout/activity_login.xml`
- `app/src/main/res/layout/fragment_home.xml`
- `app/src/main/res/layout/fragment_settings.xml`
- `app/src/main/res/drawable/ic_hen.xml`
- `app/src/main/res/drawable/ic_egg.xml`
- `app/src/main/res/drawable/ic_chick.xml`
- `app/src/main/res/drawable/bg_langbtn.xml`
- `app/src/main/res/drawable/bg_tile.xml`
- `app/src/main/res/drawable/dot_amber.xml`
- `app/src/main/res/color/segment_bg.xml`
- `app/src/main/res/color/segment_text.xml`
- `app/src/main/res/values/strings_poultry.xml`
- `app/src/main/res/values/strings_start.xml`
- `app/src/main/res/values-hi/strings_poultry.xml`
- `app/src/main/res/values-hi/strings_start.xml`
- `app/src/main/res/values-pa/strings_poultry.xml`
- `app/src/main/res/values-pa/strings_start.xml`

### 2. Shivam: Livestock

Livestock dairy entry (cows and buffaloes, cow and buffalo milk at today's price, feed list), plus the app shell: bottom navigation, top bar, theme, colours, icons and the common strings.

- `app/src/main/java/com/gadvasu/livestockapp/ui/LivestockActivity.kt`
- `app/src/main/java/com/gadvasu/livestockapp/data/LivestockModels.kt`
- `app/src/main/java/com/gadvasu/livestockapp/ui/MainActivity.kt`
- `app/src/main/java/com/gadvasu/livestockapp/ui/BaseActivity.kt`
- `app/src/main/res/layout/activity_livestock.xml`
- `app/src/main/res/layout/item_feed_row.xml`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/view_topbar.xml`
- `app/src/main/res/drawable/ic_cow.xml`
- `app/src/main/res/drawable/bg_section_cow.xml`
- `app/src/main/res/drawable/bg_section_buffalo.xml`
- `app/src/main/res/drawable/bg_section_feed.xml`
- `app/src/main/res/drawable/bg_langchip.xml`
- `app/src/main/res/drawable/bg_card.xml`
- `app/src/main/res/drawable/bg_input.xml`
- `app/src/main/res/drawable/bg_notice.xml`
- `app/src/main/res/drawable/ic_back.xml`
- `app/src/main/res/drawable/ic_leaf.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_home.xml`
- `app/src/main/res/drawable/ic_dash.xml`
- `app/src/main/res/drawable/ic_report.xml`
- `app/src/main/res/drawable/ic_gear.xml`
- `app/src/main/res/drawable/ic_check.xml`
- `app/src/main/res/drawable/ic_plus.xml`
- `app/src/main/res/drawable/ic_trash.xml`
- `app/src/main/res/drawable/ic_calendar.xml`
- `app/src/main/res/drawable/ic_calendar_muted.xml`
- `app/src/main/res/menu/bottom_nav.xml`
- `app/src/main/res/color/nav_item.xml`
- `app/src/main/res/mipmap-anydpi/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/bools.xml`
- `app/src/main/res/values-night/colors.xml`
- `app/src/main/res/values-night/bools.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/strings_livestock.xml`
- `app/src/main/res/values-hi/strings.xml`
- `app/src/main/res/values-hi/strings_livestock.xml`
- `app/src/main/res/values-pa/strings.xml`
- `app/src/main/res/values-pa/strings_livestock.xml`

### 3. Pranav: Goatery

Goatery and the batch screens it uses (list, start batch, add expenses, close after sale). Broiler and Fishery reuse these screens. Also data storage and project setup.

- `app/src/main/java/com/gadvasu/livestockapp/ui/BatchListActivity.kt`
- `app/src/main/java/com/gadvasu/livestockapp/ui/BatchNewActivity.kt`
- `app/src/main/java/com/gadvasu/livestockapp/ui/BatchDetailActivity.kt`
- `app/src/main/java/com/gadvasu/livestockapp/ui/BatchCloseActivity.kt`
- `app/src/main/java/com/gadvasu/livestockapp/data/BatchModels.kt`
- `app/src/main/java/com/gadvasu/livestockapp/data/Models.kt`
- `app/src/main/java/com/gadvasu/livestockapp/data/FarmRepository.kt`
- `app/src/main/java/com/gadvasu/livestockapp/GadvasuApp.kt`
- `app/src/main/res/layout/activity_batch_list.xml`
- `app/src/main/res/layout/activity_batch_new.xml`
- `app/src/main/res/layout/activity_batch_detail.xml`
- `app/src/main/res/layout/activity_batch_close.xml`
- `app/src/main/res/layout/view_money_field.xml`
- `app/src/main/res/drawable/ic_goat.xml`
- `app/src/main/res/values/strings_batch.xml`
- `app/src/main/res/values-hi/strings_batch.xml`
- `app/src/main/res/values-pa/strings_batch.xml`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle`
- `app/proguard-rules.pro`
- `build.gradle`
- `settings.gradle`
- `gradle.properties`
- `.gitignore`
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/*`
- `TEAM.md`

### 4. Vivek: Fishery

Fishery (fish seed, pond area) through the enterprise settings, plus the calculation engine, Dashboard, Reports with CSV export, and the shared list, card and slip views.

- `app/src/main/java/com/gadvasu/livestockapp/data/Calc.kt`
- `app/src/main/java/com/gadvasu/livestockapp/data/Ent.kt`
- `app/src/main/java/com/gadvasu/livestockapp/data/Reports.kt`
- `app/src/main/java/com/gadvasu/livestockapp/ui/ReportsFragment.kt`
- `app/src/main/java/com/gadvasu/livestockapp/ui/DashboardFragment.kt`
- `app/src/main/java/com/gadvasu/livestockapp/ui/Views.kt`
- `app/src/main/java/com/gadvasu/livestockapp/util/Fmt.kt`
- `app/src/main/java/com/gadvasu/livestockapp/util/Ext.kt`
- `app/src/main/res/layout/fragment_reports.xml`
- `app/src/main/res/layout/fragment_dashboard.xml`
- `app/src/main/res/layout/item_record.xml`
- `app/src/main/res/layout/item_batch.xml`
- `app/src/main/res/layout/view_stat3.xml`
- `app/src/main/res/layout/view_empty.xml`
- `app/src/main/res/layout/view_button_secondary.xml`
- `app/src/main/res/layout/item_spinner.xml`
- `app/src/main/res/layout/item_spinner_dropdown.xml`
- `app/src/main/res/drawable/ic_fish.xml`
- `app/src/main/res/drawable/bg_chip_done.xml`
- `app/src/main/res/drawable/bg_chip_run.xml`
- `app/src/main/res/drawable/bg_empty.xml`
- `app/src/main/res/drawable/bg_stat.xml`
- `app/src/main/res/drawable/divider_h.xml`
- `app/src/main/res/drawable/dotted_leader.xml`
- `app/src/main/res/drawable/bg_spinner.xml`
- `app/src/main/res/drawable/ic_chevron_down.xml`
- `app/src/main/res/values/strings_fishery.xml`
- `app/src/main/res/values/strings_reports.xml`
- `app/src/main/res/values-hi/strings_fishery.xml`
- `app/src/main/res/values-hi/strings_reports.xml`
- `app/src/main/res/values-pa/strings_fishery.xml`
- `app/src/main/res/values-pa/strings_reports.xml`
