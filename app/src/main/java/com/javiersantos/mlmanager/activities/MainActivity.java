package com.javiersantos.mlmanager.activities;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.javiersantos.mlmanager.AppInfo;
import com.javiersantos.mlmanager.R;
import com.javiersantos.mlmanager.adapters.AppAdapter;
import com.javiersantos.mlmanager.utils.AppPreferences;
import com.javiersantos.mlmanager.utils.UtilsApp;
import com.javiersantos.mlmanager.utils.UtilsUI;
import com.mikepenz.materialdrawer.Drawer;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import xyz.danoz.recyclerviewfastscroller.vertical.VerticalRecyclerViewFastScroller;


public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    // Load Settings
    private AppPreferences appPreferences;

    // General variables
    private List<String> appListName = new ArrayList<>();
    private List<String> appListAPK = new ArrayList<>();
    private List<String> appListVersion = new ArrayList<>();
    private List<String> appListSource = new ArrayList<>();
    private List<String> appListData = new ArrayList<>();
    private List<Drawable> appListIcon = new ArrayList<>();
    private List<String> appSystemListName = new ArrayList<>();
    private List<String> appSystemListAPK = new ArrayList<>();
    private List<String> appSystemListVersion = new ArrayList<>();
    private List<String> appSystemListSource = new ArrayList<>();
    private List<String> appSystemListData = new ArrayList<>();
    private List<Drawable> appSystemListIcon = new ArrayList<>();

    private AppAdapter appAdapter;
    private AppAdapter appSystemAdapter;

    // Configuration variables
    private Boolean doubleBackToExitPressedOnce = false;
    private Toolbar toolbar;
    private Context context;
    private RecyclerView recyclerView;
    private ProgressWheel progressWheel;
    private Drawer drawer;
    private MenuItem searchItem;
    private SearchView searchView;
    private static VerticalRecyclerViewFastScroller fastScroller;
    private static LinearLayout noResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.appPreferences = new AppPreferences(getApplicationContext());
        this.context = this;

        setInitialConfiguration();
        setAppDir();

        recyclerView = (RecyclerView) findViewById(R.id.appList);
        fastScroller = (VerticalRecyclerViewFastScroller) findViewById(R.id.fast_scroller);
        progressWheel = (ProgressWheel) findViewById(R.id.progress);
        noResults = (LinearLayout) findViewById(R.id.noResults);

        fastScroller.setRecyclerView(recyclerView);
        recyclerView.setOnScrollListener(fastScroller.getOnScrollListener());

        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        setNavigationDrawer(appAdapter, appSystemAdapter, recyclerView);

        progressWheel.setBarColor(appPreferences.getPrimaryColorPref());
        progressWheel.setVisibility(View.VISIBLE);
        new getInstalledApps().execute();

    }

    private void setInitialConfiguration() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(UtilsUI.darker(appPreferences.getPrimaryColorPref(), 0.8));
            toolbar.setBackgroundColor(appPreferences.getPrimaryColorPref());
            if (!appPreferences.getNavigationBlackPref()) {
                getWindow().setNavigationBarColor(appPreferences.getPrimaryColorPref());
            }
        }
    }

    private void setNavigationDrawer(AppAdapter appAdapter, AppAdapter appSystemAdapter, RecyclerView recyclerView) {
        drawer = UtilsUI.setNavigationDrawer(this, getApplicationContext(), toolbar, appAdapter, appSystemAdapter, recyclerView);
    }

    class getInstalledApps extends AsyncTask<Void, String, Void> {
        private Integer totalApps;
        private Integer actualApps;

        public getInstalledApps() {
            actualApps = 0;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final PackageManager packageManager = getPackageManager();
            List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
            totalApps = packages.size();
            // Get Sort Mode
            switch (appPreferences.getSortMode()) {
                default:
                    // Comparator by Name (default)
                    Collections.sort(packages, new Comparator<PackageInfo>() {
                        @Override
                        public int compare(PackageInfo p1, PackageInfo p2) {
                            return packageManager.getApplicationLabel(p1.applicationInfo).toString().toLowerCase().compareTo(packageManager.getApplicationLabel(p2.applicationInfo).toString().toLowerCase());
                        }
                    });
                    break;
                case "2":
                    // Comparator by Size
                    Collections.sort(packages, new Comparator<PackageInfo>() {
                        @Override
                        public int compare(PackageInfo p1, PackageInfo p2) {
                            Long size1 = new File(p1.applicationInfo.sourceDir).length();
                            Long size2 = new File(p2.applicationInfo.sourceDir).length();
                            return size2.compareTo(size1);
                        }
                    });
                    break;
                case "3":
                    // Comparator by Installation Date (default)
                    Collections.sort(packages, new Comparator<PackageInfo>() {
                        @Override
                        public int compare(PackageInfo p1, PackageInfo p2) {
                            return Long.toString(p2.firstInstallTime).compareTo(Long.toString(p1.firstInstallTime));
                        }
                    });
                    break;
                case "4":
                    // Comparator by Last Update
                    Collections.sort(packages, new Comparator<PackageInfo>() {
                        @Override
                        public int compare(PackageInfo p1, PackageInfo p2) {
                            return Long.toString(p2.lastUpdateTime).compareTo(Long.toString(p1.lastUpdateTime));
                        }
                    });
                    break;
            }
            for (PackageInfo packageInfo : packages) {
                if (!(packageManager.getApplicationLabel(packageInfo.applicationInfo).equals("") || packageInfo.packageName.equals(""))) {
                    if (packageManager.getLaunchIntentForPackage(packageInfo.packageName) != null) {
                        try {
                            // Non System Apps
                            appListName.add(packageManager.getApplicationLabel(packageInfo.applicationInfo).toString());
                            appListAPK.add(packageInfo.packageName);
                            appListVersion.add(packageInfo.versionName);
                            appListSource.add(packageInfo.applicationInfo.sourceDir);
                            appListData.add(packageInfo.applicationInfo.dataDir);
                            appListIcon.add(packageManager.getApplicationIcon(packageInfo.applicationInfo));
                        } catch (OutOfMemoryError e) {
                            //TODO Workaround to avoid FC on some devices (OutOfMemoryError). Drawable should be cached before.
                            appListIcon.add(getResources().getDrawable(R.drawable.ic_android));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            // System Apps
                            appSystemListName.add(packageManager.getApplicationLabel(packageInfo.applicationInfo).toString());
                            appSystemListAPK.add(packageInfo.packageName);
                            appSystemListVersion.add(packageInfo.versionName);
                            appSystemListSource.add(packageInfo.applicationInfo.sourceDir);
                            appSystemListData.add(packageInfo.applicationInfo.dataDir);
                            appSystemListIcon.add(packageManager.getApplicationIcon(packageInfo.applicationInfo));
                        } catch (OutOfMemoryError e) {
                            //TODO Workaround to avoid FC on some devices (OutOfMemoryError). Drawable should be cached before.
                            appSystemListIcon.add(getResources().getDrawable(R.drawable.ic_android));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                actualApps++;
                publishProgress(Double.toString((actualApps * 100) / totalApps));
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            super.onProgressUpdate(progress);
            progressWheel.setProgress(Float.parseFloat(progress[0]));
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            appAdapter = new AppAdapter(createList(appListName, appListAPK, appListVersion, appListSource, appListData, appListIcon, false), context);
            appSystemAdapter = new AppAdapter(createList(appSystemListName, appSystemListAPK, appSystemListVersion, appSystemListSource, appSystemListData, appSystemListIcon, true), context);

            fastScroller.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(appAdapter);
            progressWheel.setVisibility(View.GONE);
            searchItem.setVisible(true);

            setNavigationDrawer(appAdapter, appSystemAdapter, recyclerView);
        }

    }

    private void setAppDir() {
        File appDir = UtilsApp.getAppFolder();
        if(!appDir.exists()) {
            appDir.mkdir();
        }
    }

    private List<AppInfo> createList(List<String> apps, List<String> apks, List<String> versions, List<String> sources, List<String> data, List<Drawable> icons, Boolean isSystem) {
        List<AppInfo> res = new ArrayList<AppInfo>();
        for (int i=0; i < apps.size(); i++) {
            AppInfo appInfo = new AppInfo(apps.get(i), apks.get(i), versions.get(i), sources.get(i), data.get(i), icons.get(i), isSystem);
            res.add(appInfo);
        }

        return res;
    }

    @Override
    public boolean onQueryTextChange(String search) {
        if (search.isEmpty()) {
            ((AppAdapter) recyclerView.getAdapter()).getFilter().filter("");
        } else {
            ((AppAdapter) recyclerView.getAdapter()).getFilter().filter(search);
        }

        return false;
    }

    public static void setResultsMessage(Boolean result) {
        if (result) {
            noResults.setVisibility(View.VISIBLE);
            fastScroller.setVisibility(View.GONE);
        } else {
            noResults.setVisibility(View.GONE);
            fastScroller.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else if (searchItem.isVisible() && !searchView.isIconified()) {
            searchView.onActionViewCollapsed();
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, R.string.tap_exit, Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
    }

}
