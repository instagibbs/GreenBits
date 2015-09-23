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

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.ui.PeerListFragment;
import com.greenaddress.greenbits.ui.R;

import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;

import java.util.ArrayList;
import java.util.List;

import de.schildbach.wallet.util.ViewPagerTabs;
//import de.schildbach.wallet_test.R;

/**
 * @author Andreas Schildbach
 */
public final class NetworkMonitorActivity extends FragmentActivity
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

    protected GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    protected GaService getGAService() {
        return getGAApp().gaService;
    }
}