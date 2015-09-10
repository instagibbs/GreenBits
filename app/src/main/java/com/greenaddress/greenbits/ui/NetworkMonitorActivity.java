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

        EditText txt = (EditText) findViewById(R.id.edit_message);
        Editable edit = txt.getText();
        edit.clear();
        edit.append("newstring");
        ArrayList<String> items = new ArrayList<String>();
        //PeerGroup group = this.getGAApp().gaService.getPeerGroup();
        //List<Peer> peers = this.getGAApp().gaService.getPeerGroup().getConnectedPeers();
        //for(Peer peer : peers){
        //    items.add(peer.toString());
        //}
        //items.add(0, "Hello");
        //items.add(1, "There");
        //System.out.println(peers.size());

/*
        final ViewPager pager = (ViewPager) findViewById(R.id.network_monitor_pager);

        final FragmentManager fm = getFragmentManager();

        if (pager != null)
        {
            final ViewPagerTabs pagerTabs = (ViewPagerTabs) findViewById(R.id.network_monitor_pager_tabs);
            pagerTabs.addTabLabels(R.string.network_monitor_peer_list_title, R.string.network_monitor_block_list_title);

            final PagerAdapter pagerAdapter = new PagerAdapter(fm);

            pager.setAdapter(pagerAdapter);
            pager.setOnPageChangeListener(pagerTabs);
            pager.setPageMargin(2);
            pager.setPageMarginDrawable(R.color.whiteSecondary);

            //peerListFragment = new PeerListFragment();
            //blockListFragment = new BlockListFragment();
        }
        else
        {
            //peerListFragment = (PeerListFragment) fm.findFragmentById(R.id.peer_list_fragment);
            //blockListFragment = (BlockListFragment) fm.findFragmentById(R.id.block_list_fragment);
        }*/
    }
/*
    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

    private class PagerAdapter extends FragmentStatePagerAdapter
    {
        public PagerAdapter(final FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public int getCount()
        {
            return 2;
        }

        @Override
        public Fragment getItem(final int position)
        {
            //if (position == 0)
            //    return peerListFragment;
            //else
            //    return peerListFragment; //For now just the one.
            return null;
        }
    }

    protected GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    protected GaService getGAService() {
        return getGAApp().gaService;
    }
}