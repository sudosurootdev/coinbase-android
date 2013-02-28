package com.siriusapplications.coinbase;

import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.siriusapplications.coinbase.CoinbaseActivity.RequiresAuthentication;
import com.siriusapplications.coinbase.api.LoginManager;
import com.slidingmenu.lib.SlidingMenu;

@RequiresAuthentication
public class MainActivity extends CoinbaseActivity {
  
  public static final String ACTION_SCAN = "com.siriusapplications.coinbase.MainActivity.ACTION_SCAN";
  public static final String ACTION_TRANSFER = "com.siriusapplications.coinbase.MainActivity.ACTION_TRANSFER";
  public static final String ACTION_TRANSACTIONS = "com.siriusapplications.coinbase.MainActivity.ACTION_TRANSACTIONS";

  public static class SignOutFragment extends DialogFragment {


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage(R.string.sign_out_confirm);

      builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          // Sign out
          ((MainActivity) getActivity()).changeAccount(-1);
        }
      });

      builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          // Dismiss
        }
      });

      return builder.create();
    }
  }

  private int[] mFragmentTitles = new int[] {
      R.string.title_section1,
      R.string.title_section2,
      R.string.title_section3,
      R.string.title_section4,
  };

  SectionsPagerAdapter mSectionsPagerAdapter;
  CustomViewPager mViewPager;
  TransactionsFragment mTransactionsFragment;
  BuySellFragment mBuySellFragment;
  TransferFragment mTransferFragment;
  AccountSettingsFragment mSettingsFragment;
  SlidingMenu mSlidingMenu;
  MenuItem mRefreshItem;
  boolean mRefreshItemState = false;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mTransactionsFragment = new TransactionsFragment();
    mBuySellFragment = new BuySellFragment();
    mTransferFragment = new TransferFragment();
    mSettingsFragment = new AccountSettingsFragment();

    mTransactionsFragment.setParent(this);
    mBuySellFragment.setParent(this);
    mTransferFragment.setParent(this);

    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

    // Set up the ViewPager
    mViewPager = (CustomViewPager) findViewById(R.id.pager);
    mViewPager.setAdapter(mSectionsPagerAdapter);
    mViewPager.setPagingEnabled(false);

    onNewIntent(getIntent());

    // configure the SlidingMenu
    mSlidingMenu = new SlidingMenu(this);
    mSlidingMenu.setMode(SlidingMenu.LEFT);
    mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
    mSlidingMenu.setShadowWidthRes(R.dimen.main_menu_shadow_width);
    mSlidingMenu.setShadowDrawable(R.drawable.defaultshadow);
    mSlidingMenu.setBehindWidthRes(R.dimen.main_menu_width);
    mSlidingMenu.setFadeDegree(0f);
    mSlidingMenu.setBehindScrollScale(0);
    mSlidingMenu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
    mSlidingMenu.setMenu(R.layout.activity_main_menu);

    mSlidingMenu.setOnCloseListener(new SlidingMenu.OnCloseListener() {

      @Override
      public void onClose() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      }
    });

    mSlidingMenu.setOnOpenListener(new SlidingMenu.OnOpenListener() {

      @Override
      public void onOpen() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
      }
    });

    // Set up Sliding Menu list
    ListView slidingList = (ListView) mSlidingMenu.findViewById(android.R.id.list);
    slidingList.setAdapter(new SectionsListAdapter());
    slidingList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
          long arg3) {

        mViewPager.setCurrentItem(arg2, false);
        mSlidingMenu.showContent();
      }
    });

    // Refresh everything on app launch
    new Thread(new Runnable() {
      public void run() {
        runOnUiThread(new Runnable() {
          public void run() {
            refresh();
          }
        });
      }
    }).start();

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    // Screen width may have changed so this needs to be set again
    mSlidingMenu.setBehindWidthRes(R.dimen.main_menu_width);
  }

  @Override
  protected void onNewIntent(Intent intent) {

    super.onNewIntent(intent);
    setIntent(intent);

    if(intent.getData() != null && "bitcoin".equals(intent.getData().getScheme())) {
      // Handle bitcoin: URI
      mViewPager.setCurrentItem(2); // Switch to transfer fragment
      mTransferFragment.fillFormForBitcoinUri(getIntent().getData());
    } else if(ACTION_SCAN.equals(intent.getAction())) {
      // Scan barcode
      startBarcodeScan();
    } else if(ACTION_TRANSFER.equals(intent.getAction())) {

      mViewPager.setCurrentItem(2); // Switch to transfer fragment
    } else if(ACTION_TRANSACTIONS.equals(intent.getAction())) {

      mViewPager.setCurrentItem(0); // Switch to transactions fragment
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getSupportMenuInflater().inflate(R.menu.activity_main, menu);
    mRefreshItem = menu.findItem(R.id.menu_refresh);
    setRefreshButtonAnimated(mRefreshItemState);
    return true;
  }


  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch(item.getItemId()) {
    case R.id.menu_accounts:
      new AccountsFragment().show(getSupportFragmentManager(), "accounts");
      return true;
    case R.id.menu_sign_out:
      new SignOutFragment().show(getSupportFragmentManager(), "signOut");
      return true;
    case R.id.menu_about:
      startActivity(new Intent(this, AboutActivity.class));
      return true;
    case R.id.menu_barcode:
      startBarcodeScan();
      return true;
    case R.id.menu_refresh:
      refresh();
      return true;
    case android.R.id.home:
      mSlidingMenu.showMenu();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
   * sections of the app.
   */
  public class SectionsPagerAdapter extends FragmentPagerAdapter {

    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int i) {
      switch (i) {
      case 0: return mTransactionsFragment;
      case 1: return mBuySellFragment;
      case 2: return mTransferFragment;
      case 3: return mSettingsFragment;
      }
      return null;
    }

    @Override
    public int getCount() {
      return 4;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return getString(mFragmentTitles[position]).toUpperCase(Locale.CANADA);
    }
  }

  public class SectionsListAdapter extends BaseAdapter {

    @Override
    public int getCount() {
      return 4;
    }

    @Override
    public Object getItem(int position) {
      return position;
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

      if(convertView == null) {
        convertView = View.inflate(MainActivity.this, R.layout.activity_main_menu_item, null);
      }

      String name = getString(mFragmentTitles[position]);

      ((TextView) convertView.findViewById(R.id.main_menu_item_title)).setText(name);

      return convertView;
    }

  }

  public void changeAccount(int account) {

    if(account == -1) {

      // Delete current account
      LoginManager.getInstance().deleteCurrentAccount(this);
    } else {

      // Change active account
      LoginManager.getInstance().switchActiveAccount(this, account);
    }

    finish();
    startActivity(new Intent(this, MainActivity.class));
  }

  public void addAccount() {

    startActivity(new Intent(this, LoginActivity.class));
  }

  public void startBarcodeScan() {

    Intent intent = new Intent(this, com.google.zxing.client.android.CaptureActivity.class);
    intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
    startActivityForResult(intent, 0);
  }

  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (requestCode == 0) {
      /*
       * Barcode scan
       */
      if (resultCode == RESULT_OK) {

        String contents = intent.getStringExtra("SCAN_RESULT");
        String format = intent.getStringExtra("SCAN_RESULT_FORMAT");

        Uri uri = Uri.parse(contents);
        if(uri != null && "bitcoin".equals(uri.getScheme())) {
          // Is bitcoin URI
          mViewPager.setCurrentItem(2); // Switch to transfer fragment
          mTransferFragment.fillFormForBitcoinUri(uri);
        }

      } else if (resultCode == RESULT_CANCELED) {
        // Barcode scan was cancelled
      }
    } else if(requestCode == 1) {
      /*
       * Transaction details
       */
      if(resultCode == RESULT_OK) {
        // Refresh needed
        refresh();
      }
    }
  }

  public BuySellFragment getBuySellFragment() {
    return mBuySellFragment;
  }

  public TransferFragment getTransferFragment() {
    return mTransferFragment;
  }

  public void setRefreshButtonAnimated(boolean animated) {

    mRefreshItemState = animated;

    if(mRefreshItem == null) {
      return;
    }

    if(animated) {
      mRefreshItem.setEnabled(false);
      mRefreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
    } else {
      mRefreshItem.setEnabled(true);
      mRefreshItem.setActionView(null);
    }
  }

  public void refresh() {
    mTransactionsFragment.refresh();
    mBuySellFragment.refresh();
    mTransferFragment.refresh();
  }
}
