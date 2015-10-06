package com.greenaddress.greenbits.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.utils.MonetaryFormat;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.TimeZone;

import javax.annotation.Nullable;


public class MainFragment extends GAFragment implements Observer {
    public static final int P2SH_FORTIFIED_OUT = 10;
    Observer wiFiObserver = null;
    boolean wiFiObserverRequired = false;
    MaterialDialog spvStatusDialog = null;
    private View rootView;
    private List<Transaction> currentList;
    private Observer curBalanceObserver;
    private int curSubaccount;
    private Observer txVerifiedObservable;
    private View.OnClickListener unconfirmedClickListener;

    private Transaction processGATransaction(final Map<String, Object> txJSON, final int curBlock) throws ParseException {

        final List eps = (List) txJSON.get("eps");
        final String txhash = (String) txJSON.get("txhash");

        final String memo = txJSON.containsKey("memo") ?
                (String) txJSON.get("memo") : null;

        final Integer blockHeight = txJSON.containsKey("block_height") && txJSON.get("block_height") != null ?
                (int) txJSON.get("block_height") : null;

        String counterparty = null;
        long amount = 0;
        int type;
        boolean isSpent = true;
        String receivedOn = null;
        for (int i = 0; i < eps.size(); ++i) {
            final Map<String, Object> ep = (Map<String, Object>) eps.get(i);
            if (ep.get("social_destination") != null) {
                Map<String, Object> social_destination = null;
                try {
                    social_destination = new MappingJsonFactory().getCodec().readValue(
                            (String) ep.get("social_destination"), Map.class);
                } catch (final IOException e) {
                    //e.printStackTrace();
                }

                if (social_destination != null) {
                    counterparty = social_destination.get("type").equals("voucher") ?
                            "Voucher" : (String) social_destination.get("name");
                } else {
                    counterparty = (String) ep.get("social_destination");
                }
            }
            if (((Boolean) ep.get("is_relevant")).booleanValue()) {
                if (((Boolean) ep.get("is_credit")).booleanValue()) {
                    final boolean external_social = ep.get("social_destination") != null &&
                            ((Number) ep.get("script_type")).intValue() != P2SH_FORTIFIED_OUT;
                    if (!external_social) {
                        amount += Long.valueOf((String) ep.get("value")).longValue();
                        if (!((Boolean) ep.get("is_spent")).booleanValue()) {
                            isSpent = false;
                        }
                    }
                    if (receivedOn == null) {
                        receivedOn = (String) ep.get("ad");
                    } else {
                        receivedOn += ", " + ep.get("ad");
                    }
                } else {
                    amount -= Long.valueOf((String) ep.get("value"));
                }
            }
        }
        if (amount >= 0) {
            type = Transaction.TYPE_IN;
            for (int i = 0; i < eps.size(); ++i) {
                final Map<String, Object> ep = (Map<String, Object>) eps.get(i);
                if (!((Boolean) ep.get("is_credit")).booleanValue() && ep.get("social_source") != null) {
                    counterparty = (String) ep.get("social_source");
                }
            }
        } else {
            receivedOn = null; // don't show change addresses
            final List<Map<String, Object>> recip_eps = new ArrayList<>();
            for (int i = 0; i < eps.size(); ++i) {
                final Map<String, Object> ep = (Map<String, Object>) eps.get(i);
                if (((Boolean) ep.get("is_credit")).booleanValue() &&
                        (!((Boolean) ep.get("is_relevant")).booleanValue() ||
                                ep.get("social_destination") != null)) {
                    recip_eps.add(ep);
                }
            }
            if (recip_eps.size() > 0) {
                type = Transaction.TYPE_OUT;
                if (counterparty == null) {
                    counterparty = (String) recip_eps.get(0).get("ad");
                }
                if (recip_eps.size() > 1) {
                    counterparty += ", ...";
                }
            } else {
                type = Transaction.TYPE_REDEPOSIT;
            }
        }
        boolean spvVerified = getGAApp().getSharedPreferences("verified_utxo_"
                + getGAService().getReceivingId(), Context.MODE_PRIVATE).getBoolean(txhash, false);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return new Transaction(type, amount, counterparty,
                df.parse((String) txJSON.get("created_at")), txhash, memo, curBlock, blockHeight, spvVerified, isSpent,
                receivedOn);
    }

    private void updateBalance() {
        final String btcUnit = (String) getGAService().getAppearanceValue("unit");
        final MonetaryFormat bitcoinFormat = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
        final TextView balanceBitcoinIcon = (TextView) rootView.findViewById(R.id.mainBalanceBitcoinIcon);
        final TextView bitcoinScale = (TextView) rootView.findViewById(R.id.mainBitcoinScaleText);
        bitcoinScale.setText(Html.fromHtml(CurrencyMapper.mapBtcUnitToPrefix(btcUnit)));
        if (btcUnit == null || btcUnit.equals("bits")) {
            balanceBitcoinIcon.setText("");
            bitcoinScale.setText("bits ");
        } else {
            balanceBitcoinIcon.setText(Html.fromHtml("&#xf15a; "));
        }
        final String btcBalance = bitcoinFormat.noCode().format(
                getGAService().getBalanceCoin(curSubaccount)).toString();
        final String btcBalanceVerified;
        if (getGAService().getVerifiedBalanceCoin(curSubaccount) != null) {
            btcBalanceVerified = bitcoinFormat.noCode().format(
                    getGAService().getVerifiedBalanceCoin(curSubaccount)).toString();
        } else {
            btcBalanceVerified = bitcoinFormat.noCode().format(Coin.valueOf(0)).toString();
        }
        final String fiatBalance =
                MonetaryFormat.FIAT.minDecimals(2).noCode().format(
                        getGAService().getBalanceFiat(curSubaccount))
                        .toString();
        final String fiatCurrency = getGAService().getFiatCurrency();
        final String converted = CurrencyMapper.map(fiatCurrency);

        final TextView balanceText = (TextView) rootView.findViewById(R.id.mainBalanceText);
        final TextView balanceQuestionMark = (TextView) rootView.findViewById(R.id.mainBalanceQuestionMark);
        if (!getGAApp().getSharedPreferences("SPV", Context.MODE_PRIVATE).getBoolean("enabled", true)
                || btcBalance.equals(btcBalanceVerified)) {
            balanceQuestionMark.setVisibility(View.GONE);
        } else {
            balanceQuestionMark.setVisibility(View.VISIBLE);
        }
        final TextView balanceFiatText = (TextView) rootView.findViewById(R.id.mainLocalBalanceText);
        final FontAwesomeTextView balanceFiatIcon = (FontAwesomeTextView) rootView.findViewById(R.id.mainLocalBalanceIcon);
        final DecimalFormat formatter = new DecimalFormat("#,###.########");
        try {
            balanceText.setText(formatter.format(formatter.parse(btcBalance)));
        } catch (final ParseException e) {
            balanceText.setText(btcBalance);
        }

        final int nChars = balanceText.getText().length() + balanceQuestionMark.getText().length() + bitcoinScale.getText().length() + balanceBitcoinIcon.getText().length();
        final int size = Math.min(50 - nChars, 34);
        balanceText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        bitcoinScale.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        balanceBitcoinIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);

        try {
            balanceFiatText.setText(formatter.format(formatter.parse(fiatBalance)));

        } catch (final ParseException e) {
            balanceFiatText.setText(fiatBalance);
        }

        if (converted != null) {
            balanceFiatIcon.setText(Html.fromHtml(converted + " "));
            balanceFiatIcon.setAwesomeTypeface();
            balanceFiatIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        } else {
            balanceFiatIcon.setText(fiatCurrency);
            balanceFiatIcon.setDefaultTypeface();
            balanceFiatIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }
    }

    @Override
    public View onGACreateView(final LayoutInflater inflater, final ViewGroup container,
                               final Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main, container, false);
        curSubaccount = getGAApp().getSharedPreferences("main", Context.MODE_PRIVATE).getInt("curSubaccount", 0);

        final TextView firstP = (TextView) rootView.findViewById(R.id.mainFirstParagraphText);
        final TextView secondP = (TextView) rootView.findViewById(R.id.mainSecondParagraphText);
        final TextView thirdP = (TextView) rootView.findViewById(R.id.mainThirdParagraphText);

        firstP.setMovementMethod(LinkMovementMethod.getInstance());
        secondP.setMovementMethod(LinkMovementMethod.getInstance());
        thirdP.setMovementMethod(LinkMovementMethod.getInstance());


        /* currentSize = balanceText.getTextSize();
        maxSize = currentSize;
        minSize = currentSize / 2.0f; */


        final TextView balanceText = (TextView) rootView.findViewById(R.id.mainBalanceText);
        final TextView balanceQuestionMark = (TextView) rootView.findViewById(R.id.mainBalanceQuestionMark);
        unconfirmedClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spvStatusDialog != null) {
                    return;
                }
                if (balanceQuestionMark.getVisibility() != View.VISIBLE) {
                    // show the message only if question mark is visible
                    return;
                }
                final String blocksLeft;
                if (wiFiObserver != null) {
                    blocksLeft = getResources().getString(R.string.unconfirmedBalanceSpvNotAvailable) + "\n\n" +
                            getResources().getString(R.string.unconfirmedBalanceDoSyncWithoutWiFi);
                } else {
                    // no observer means we're synchronizing
                    String numblocksLeft = String.valueOf(getGAService().getSpvBlocksLeft());
                    if (numblocksLeft.equals(String.valueOf(Integer.MAX_VALUE))){
                        blocksLeft = "Not yet connected to SPV!";
                    }else{
                        blocksLeft = numblocksLeft;
                    }
                }
                MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity())
                        .title(getResources().getString(R.string.unconfirmedBalanceTitle))
                        .content(getResources().getString(R.string.unconfirmedBalanceText) + " " + blocksLeft)
                        .positiveColorRes(R.color.accent)
                        .negativeColorRes(R.color.white)
                        .titleColorRes(R.color.white)
                        .contentColorRes(android.R.color.white)
                        .theme(Theme.DARK);
                if (wiFiObserver != null) {
                    builder.negativeText(R.string.NO)
                            .positiveText(R.string.YES);
                }
                builder.cancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        spvStatusDialog = null;
                    }
                });
                builder.callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onNegative(MaterialDialog materialDialog) {
                        spvStatusDialog = null;
                    }

                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        spvStatusDialog = null;
                        getGAApp().getConnectionObservable().deleteObserver(wiFiObserver);
                        wiFiObserver = null;
                        wiFiObserverRequired = false;
                        getGAService().startSpvSync();
                    }
                });
                spvStatusDialog = builder.build();
                spvStatusDialog.show();
                if (wiFiObserver == null) {
                    final Handler handler = new Handler();
                    final Runnable updateContent = new Runnable() {
                        @Override
                        public void run() {
                            if (spvStatusDialog != null) {
                                try {
                                    if(getGAService().getSpvBlocksLeft() != Integer.MAX_VALUE) {
                                        spvStatusDialog.setContent(getResources().getString(R.string.unconfirmedBalanceText) + " " +
                                                getGAService().getSpvBlocksLeft());
                                    }
                                    else{
                                        spvStatusDialog.setContent(getResources().getString(R.string.unconfirmedBalanceText) + " " +
                                                "Not yet connected to SPV!");
                                    }
                                    handler.postDelayed(this, 2000);
                                } catch (IllegalStateException e) {
                                    e.printStackTrace();
                                    // can happen if the activity is terminated
                                    // ("Fragment MainFragment not attached to Activity")
                                }
                            }
                        }
                    };
                    handler.postDelayed(updateContent, 2000);
                }
            }
        };
        balanceText.setOnClickListener(unconfirmedClickListener);
        balanceQuestionMark.setOnClickListener(unconfirmedClickListener);

        curBalanceObserver = makeBalanceObserver();

        getGAService().getBalanceObservables().get(new Long(curSubaccount)).addObserver(curBalanceObserver);

        if (getGAService().getBalanceCoin(curSubaccount) != null) {
            updateBalance();
        }

        reloadTransactions(getActivity());

        getGAApp().configureSubaccountsFooter(
                curSubaccount,
                getActivity(),
                (TextView) rootView.findViewById(R.id.sendAccountName),
                (LinearLayout) rootView.findViewById(R.id.mainFooter),
                (LinearLayout) rootView.findViewById(R.id.footerClickableArea),
                new Function<Integer, Void>() {
                    @Nullable
                    @Override
                    public Void apply(@Nullable Integer input) {
                        getGAService().getBalanceObservables().get(new Long(curSubaccount)).deleteObserver(curBalanceObserver);
                        curSubaccount = input;
                        curBalanceObserver = makeBalanceObserver();
                        getGAService().getBalanceObservables().get(new Long(curSubaccount)).addObserver(curBalanceObserver);
                        reloadTransactions(getActivity());
                        updateBalance();

                        final SharedPreferences.Editor mEditor = getGAApp().getSharedPreferences("main", Context.MODE_PRIVATE).edit();
                        mEditor.putInt("curSubaccount", curSubaccount);
                        mEditor.apply();

                        final SharedPreferences.Editor sEditor = getGAApp().getSharedPreferences("send", Context.MODE_PRIVATE).edit();
                        sEditor.putInt("curSubaccount", curSubaccount);
                        sEditor.apply();

                        final SharedPreferences.Editor rEditor = getGAApp().getSharedPreferences("receive", Context.MODE_PRIVATE).edit();
                        rEditor.putInt("curSubaccount", curSubaccount);
                        rEditor.apply();

                        //SendFragment sendFrag = new SendFragment();
                        //TextView viewGroup = ((TextView) rootView.findViewById(R.id.send));
                        ((TabbedMainActivity)getActivity()).refreshSend(curSubaccount);
                        //((SendFragment)getFragmentManager().findFragmentById(R.id.sendFrag)).configSendFooter();
                        /*getGAApp().configureSubaccountsFooter(
                                curSubaccount,
                                getActivity(),
                                (TextView) ((View)rootView.getParent()).findViewById(R.id.sendAccountName),
                                (LinearLayout) ((View)rootView.getParent()).findViewById(R.id.sendFooter),
                                (LinearLayout) ((View)rootView.getParent()).findViewById(R.id.footerClickableArea),
                                new Function<Integer, Void>() {
                                    @Nullable
                                    @Override
                                    public Void apply(@Nullable Integer input) {
                                        getGAService().getBalanceObservables().get(new Long(curSubaccount)).deleteObserver(curBalanceObserver);
                                        curSubaccount = input.intValue();
                                        //hideInstantIf2of3();
                                        final SharedPreferences.Editor editor = getGAApp().getSharedPreferences("send", Context.MODE_PRIVATE).edit();
                                        editor.putInt("curSubaccount", curSubaccount);
                                        editor.apply();
                                        curBalanceObserver = makeBalanceObserver();
                                        getGAService().getBalanceObservables().get(new Long(curSubaccount)).addObserver(curBalanceObserver);
                                        Futures.addCallback(getGAService().getSubaccountBalance(curSubaccount), new FutureCallback<Map<?, ?>>() {
                                            @Override
                                            public void onSuccess(@Nullable Map<?, ?> result) {
                                                Coin coin = Coin.valueOf(Long.valueOf((String) result.get("satoshi")).longValue());
                                                final String btcUnit = (String) getGAService().getAppearanceValue("unit");
                                                final TextView sendSubAccountBalance = (TextView) ((View)rootView.getParent()).findViewById(R.id.sendSubAccountBalance);
                                                MonetaryFormat format = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
                                                final String btcBalance = format.noCode().format(coin).toString();
                                                final DecimalFormat formatter = new DecimalFormat("#,###.########");
                                                try {
                                                    sendSubAccountBalance.setText(formatter.format(formatter.parse(btcBalance)));
                                                } catch (final ParseException e) {
                                                    sendSubAccountBalance.setText(btcBalance);
                                                }
                                            }

                                            @Override
                                            public void onFailure(Throwable t) {

                                            }
                                        });
                                        return null;
                                    }
                                },
                                rootView.findViewById(R.id.sendNoTwoFacFooter)
                        );*/

                        //TabbedMainActivity.instance.getFragmentManager().findFragmentById(R.id.sendAccountName).conf
                        return null;
                    }
                },
                rootView.findViewById(R.id.mainNoTwoFacFooter)
        );

        return rootView;
    }

    private Observer makeBalanceObserver() {
        return new Observer() {
            @Override
            public void update(final Observable observable, final Object o) {
                final Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateBalance();
                            reloadTransactions(activity, true);  // newAdapter for unit change
                        }
                    });
                }
            }
        };
    }

    @Override
    public void onPause() {
        super.onPause();
        getGAService().getNewTransactionsObservable().deleteObserver(this);
        getGAService().getNewTxVerifiedObservable().deleteObserver(txVerifiedObservable);
        final ConnectivityObservable connObservable = getGAApp().getConnectionObservable();
        if (wiFiObserver != null) {
            connObservable.deleteObserver(wiFiObserver);
            wiFiObserver = null;
        }
    }

    @Override
    public void onGAResume() {
        getGAService().getNewTransactionsObservable().addObserver(this);
        getGAService().getNewTxVerifiedObservable().addObserver(makeTxVerifiedObservable());
        if (wiFiObserverRequired) makeWiFiObserver();
    }

    private Observer makeTxVerifiedObservable() {
        txVerifiedObservable = new Observer() {
            @Override
            public void update(Observable observable, Object data) {
                if (currentList == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // "Make sure the content of your adapter is not modified from a background
                        //  thread, but only from the UI thread. Make sure your adapter calls
                        //  notifyDataSetChanged() when its content changes."
                        for (Transaction tx : currentList) {
                            tx.spvVerified = getGAApp().getSharedPreferences("verified_utxo_"
                                            + (getGAService().getReceivingId()),
                                    Context.MODE_PRIVATE).getBoolean(tx.txhash, false);
                        }
                        final ListView listView = (ListView) rootView.findViewById(R.id.mainTransactionList);
                        ((ListTransactionsAdapter) listView.getAdapter()).notifyDataSetChanged();
                        listView.invalidateViews();  // hopefully we don't need http://stackoverflow.com/a/19655916
                    }
                });
            }
        };
        return txVerifiedObservable;
    }

    private void reloadTransactions(final Activity activity) {
        reloadTransactions(activity, false);
    }


    private void reloadTransactions(final Activity activity, boolean newAdapter) {
        final ListView listView = (ListView) rootView.findViewById(R.id.mainTransactionList);
        final LinearLayout mainEmptyTransText = (LinearLayout) rootView.findViewById(R.id.mainEmptyTransText);
        final String btcUnit = (String) getGAService().getAppearanceValue("unit");

        if (currentList == null || newAdapter) {
            currentList = new ArrayList<>();
            listView.setAdapter(new ListTransactionsAdapter(activity, R.layout.list_element_transaction, currentList, btcUnit));
        }

        final ListenableFuture<Map<?, ?>> txFuture = getGAService().getMyTransactions(curSubaccount);

        Futures.addCallback(txFuture, new FutureCallback<Map<?, ?>>() {
            @Override
            public void onSuccess(@Nullable final Map<?, ?> result) {
                final List resultList = (List) result.get("list");
                final int curBlock = ((Number) result.get("cur_block")).intValue();
                getGAService().setCurBlock(curBlock);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // "Make sure the content of your adapter is not modified from a background
                        //  thread, but only from the UI thread. Make sure your adapter calls
                        //  notifyDataSetChanged() when its content changes."

                        final GaService gaService = getGAService();
                        final ConnectivityObservable connObservable = getGAApp().getConnectionObservable();
                        if (gaService.getSharedPreferences("SPV", getActivity().MODE_PRIVATE).getBoolean("enabled", true)) {
                            gaService.setUpSPV();
                            if (!gaService.getIsSpvSyncing()) {
                                if (curBlock - gaService.getSpvHeight() > 1000) {
                                    if (connObservable.isWiFiUp()) {
                                        gaService.startSpvSync();
                                    } else {
                                        // no wifi - do we want to sync?
                                        askUserForSpvNoWiFi();
                                    }
                                } else {
                                    gaService.startSpvSync();
                                }
                            }
                        }
                        if (resultList != null && resultList.size() > 0) {
                            listView.setVisibility(View.VISIBLE);
                            mainEmptyTransText.setVisibility(View.GONE);
                        } else {
                            listView.setVisibility(View.GONE);
                            mainEmptyTransText.setVisibility(View.VISIBLE);
                        }

                        String oldFirstTxHash = null;
                        if (currentList.size() > 0) {
                            oldFirstTxHash = currentList.get(0).txhash;
                        }
                        currentList.clear();
                        for (int i = 0; i < resultList.size(); ++i) {
                            try {
                                currentList.add(processGATransaction((Map<String, Object>) resultList.get(i), (Integer) result.get("cur_block")));
                            } catch (final ParseException e) {
                                e.printStackTrace();
                            }
                        }
                        String newFirstTxHash = null;
                        final boolean scrollToTop;
                        if (currentList.size() > 0) {
                            newFirstTxHash = currentList.get(0).txhash;
                        }
                        if (oldFirstTxHash != null && newFirstTxHash != null) {
                            // scroll to top when new tx comes in
                            scrollToTop = !oldFirstTxHash.equals(newFirstTxHash);
                        } else {
                            scrollToTop = false;
                        }

                        ((ListTransactionsAdapter) listView.getAdapter()).notifyDataSetChanged();
                        if (scrollToTop) {
                            listView.smoothScrollToPosition(0);
                        }

                    }
                });

            }

            @Override
            public void onFailure(final Throwable t) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView.setVisibility(View.GONE);
                        mainEmptyTransText.setVisibility(View.VISIBLE);
                    }
                });
                t.printStackTrace();

            }
        }, getGAService().es);
    }

    private void askUserForSpvNoWiFi() {
        if (getGAService().getSpvWiFiDialogShown()) return;
        if (!getActivity().getSharedPreferences(
                "SPV",
                getActivity().MODE_PRIVATE
        ).getBoolean("enabled", true)) {
            return;
        }
        getGAService().setSpvWiFiDialogShown(true);
        new MaterialDialog.Builder(getActivity())
                .title(getResources().getString(R.string.spvNoWiFiTitle))
                .content(getResources().getString(R.string.spvNoWiFiText))
                .positiveText(R.string.spvNoWiFiSyncAnyway)
                .negativeText(R.string.spvNoWifiWaitForWiFi)
                .positiveColorRes(R.color.accent)
                .negativeColorRes(R.color.white)
                .titleColorRes(R.color.white)
                .contentColorRes(android.R.color.white)
                .theme(Theme.DARK)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onNegative(MaterialDialog materialDialog) {
                        getGAService().setSpvWiFiDialogShown(false);
                        makeWiFiObserver();
                    }

                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        getGAService().startSpvSync();
                    }
                })
                .build().show();
    }

    private void makeWiFiObserver() {
        if (wiFiObserver != null) return;
        final Activity activity = getActivity();
        final GaService gaService = getGAService();
        final ConnectivityObservable connObservable = getGAApp().getConnectionObservable();
        if (connObservable.isWiFiUp()) {
            gaService.startSpvSync();
            wiFiObserverRequired = false;
            return;
        }
        wiFiObserver = new Observer() {
            @Override
            public void update(Observable observable, Object data) {
                if (connObservable.isWiFiUp()) {
                    gaService.startSpvSync();
                    wiFiObserverRequired = false;
                    connObservable.deleteObserver(wiFiObserver);
                    wiFiObserver = null;
                }
            }
        };
        connObservable.addObserver(wiFiObserver);
        wiFiObserverRequired = true;
    }

    @Override
    public void update(final Observable observable, final Object data) {
        reloadTransactions(getActivity());
    }
}
