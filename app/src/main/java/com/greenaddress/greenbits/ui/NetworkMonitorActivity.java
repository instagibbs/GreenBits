/*
 * Copyright 2013-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.greenaddress.greenbits.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

import java.util.Observable;
import java.util.Observer;
import android.content.IntentFilter;

import org.w3c.dom.Text;

/**
 * @author Andreas Schildbach
 */
public final class NetworkMonitorActivity extends FragmentActivity implements Observer
{
    private String bloominfo_text;
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        registerReceiver(uiUpdated, new IntentFilter("BLOOMINFO_UPDATED"));
        setContentView(R.layout.activity_network);

        bloominfo_text = "BLRGSADSAD";

        ListView view = (ListView) findViewById(R.id.peerlistview);

        //TextView tview=(TextView) view.findViewById(android.R.id.text1);
        //view.setEmptyView(tview);
        //TextView tView = new TextView(this.getApplicationContext());
        //tView.setText("No peers connected");
        //tView.setTextColor(Color.BLACK);
        view.setEmptyView(findViewById(R.id.empty_list_view));
        view.setAdapter(getGAService().peerListAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();

        getGAApp().getConnectionObservable().deleteObserver(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        TextView tview = (TextView) findViewById(R.id.bloominfo);
        //if(bloominfo_text != null && !bloominfo_text.equals("BLRGSADSAD"))
        tview.setText(getGAService().getBloomInfo());
        //tview.setTextColor(Color.BLACK);

        testKickedOut();
        if (getGAService() == null) {
            finish();
            return;
        }

        getGAApp().getConnectionObservable().addObserver(this);
    }

    private void testKickedOut() {
        if (getGAApp().getConnectionObservable().getIsForcedLoggedOut() || getGAApp().getConnectionObservable().getIsForcedTimeout()) {
            // FIXME: Should pass flag to activity so it shows it was forced logged out
            final Intent firstScreenActivity = new Intent(NetworkMonitorActivity.this, FirstScreenActivity.class);
            startActivity(firstScreenActivity);
            finish();
        }
    }

    protected GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    protected GaService getGAService() {
        return getGAApp().gaService;
    }

    @Override
    public void update(Observable observable, Object data) {

    }

    private BroadcastReceiver uiUpdated= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            TextView tview = (TextView) findViewById(R.id.bloominfo);
            bloominfo_text = intent.getExtras().getString("bloominfo");
            tview.setText(bloominfo_text);


        }
    };
}