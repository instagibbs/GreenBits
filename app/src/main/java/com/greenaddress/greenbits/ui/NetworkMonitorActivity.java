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

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.ListView;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

import java.util.Observable;
import java.util.Observer;

/**
 * @author Andreas Schildbach
 */
public final class NetworkMonitorActivity extends FragmentActivity implements Observer
{
    private PeerListFragment peerListFragment;
    //private BlockListFragment blockListFragment;

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_network);

        ListView view = (ListView) findViewById(R.id.peerlistview);
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

        if (getGAService() == null) {
            finish();
            return;
        }

        getGAApp().getConnectionObservable().addObserver(this);
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
}