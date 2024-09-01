/*
 * DroidVNC-NG ListenIfAdapter.
 *
 * Author: elluisian <elluisian@yandex.com>
 *
 * Copyright (C) 2020 Christian Beier.
 *
 * You can redistribute and/or modify this program under the terms of the
 * GNU General Public License version 2 as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place Suite 330, Boston, MA 02111-1307, USA.
 */
package net.christianbeier.droidvnc_ng;


import java.net.NetworkInterface;
import java.util.ArrayList;

import android.content.res.Resources;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Spinner;
import android.view.LayoutInflater;



public class ListenIfAdapter extends ArrayAdapter<NetworkInterfaceTester.NetIfData> implements NetworkInterfaceTester.OnNetworkStateChangedListener {
    // This adapter uses the ViewHolder pattern
    private static class ViewHolder {
        public TextView txtLabel;
    }

    // Data to be shown with the adapter
    private ArrayList<String> dataStr;
    private ArrayList<NetworkInterfaceTester.NetIfData> data;
    private int dataSize;


    // Some context data for "easy retrieval"
    private Context mContext;
    private LayoutInflater mInflater;


    // UI related
    private Handler handler;



    public ListenIfAdapter(NetworkInterfaceTester nit, Context context) {
        super(context, R.layout.spinner_row, R.id.spinner_text);

        this.mContext = context;
        this.mInflater = LayoutInflater.from(this.mContext);

        this.handler = new Handler(Looper.getMainLooper());

        nit.addOnNetworkStateChangedListener(this);
        this.onNetworkStateChanged(nit);
    }



    public int getItemPositionByIfName(String ifName) {
        int i = 0;
        for (NetworkInterfaceTester.NetIfData nid : this.data) {
            if (nid.getName().equals(ifName)) {
                return i;
            }
            i++;
        }

        return 0;
    }



    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return this.handleViewRecreation(position, convertView, parent);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return this.handleViewRecreation(position, convertView, parent);
    }


    private View handleViewRecreation(int position, View convertView, ViewGroup parent) {
        if (convertView == null) { // Check if view must be recreated using the famous ViewHolder pattern
            convertView = this.mInflater.inflate(R.layout.spinner_row, parent, false);

            ViewHolder vh = new ViewHolder();
            vh.txtLabel = convertView.findViewById(R.id.spinner_text);
            convertView.setTag(vh);
        }

        ViewHolder vh = (ViewHolder)convertView.getTag();
        String label = this.dataStr.get(position);
        vh.txtLabel.setText(label);

        return convertView;
    }



    @Override
    public NetworkInterfaceTester.NetIfData getItem(int position) {
        if (0 <= position && position < this.getCount()) {
            return this.data.get(position);
        }
        return null;
    }



    @Override
    public int getCount() {
        return this.dataSize;
    }



    public void onNetworkStateChanged(NetworkInterfaceTester nit) {
        this.data = nit.getAvailableInterfaces();
        this.dataSize = this.data.size();

        this.dataStr = new ArrayList<>();
        for (NetworkInterfaceTester.NetIfData nid : this.data) {
            String actualName = " (" + nid.getName() + ")";
            String displayName = nid.getDisplayName();

            if (nid.getName().equals("0.0.0.0")) {
                displayName = this.mContext.getResources().getString(R.string.main_activity_settings_listenif_spin_any);
            }

            this.dataStr.add(displayName + actualName);
        }

        // update Spinner
        this.handler.post(new Runnable() {
            public void run() {
                ListenIfAdapter.this.notifyDataSetChanged();
            }
        });
    }
}