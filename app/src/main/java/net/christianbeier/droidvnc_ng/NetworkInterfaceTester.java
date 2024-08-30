/*
 * DroidVNC-NG broadcast receiver that listens for boot-completed events
 * and starts MainService in turn.
 *
 * Author: elluisian <elluisian@yandex.com>
 *
 * Copyright (C) 2020, 2023 Christian Beier (info@christianbeier.net>).
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

import android.net.LinkProperties;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.NetworkCapabilities;

import android.content.Context;
import android.util.Log;


import java.net.SocketException;
import java.net.NetworkInterface;
import java.util.List;
import java.util.ArrayList;



public class NetworkInterfaceTester extends ConnectivityManager.NetworkCallback {
    public static final String TAG = "NetworkInterfaceTester";

    public static interface OnNetworkStateChangedListener {
        public void onNetworkStateChanged(NetworkInterfaceTester nit, NetworkInterface iface, boolean enabled);
    }


    public static class NetIfData {
        private NetworkInterface nic;
        private String name;
        private String displayName;
        private String friendlyName;

        private NetIfData(Context context) {
            this(null, context);
        }

        private NetIfData(NetworkInterface nic, Context context) {
            this.nic = nic;

            if (nic == null) {
                this.name = "0.0.0.0";
                this.displayName = context.getResources().getString(R.string.main_activity_settings_listenif_spin_any);

            } else {
                this.name = this.nic.getName();
                this.displayName = this.nic.getDisplayName();

            }
        }

        public static NetIfData getAnyOption(Context context) {
            if (NetworkInterfaceTester.IF_ANY == null) {
                NetworkInterfaceTester.IF_ANY = new NetIfData(context);
            }
            return NetworkInterfaceTester.IF_ANY;
        }

        public static NetIfData getOptionForNic(NetworkInterface nic, Context context) {
            return new NetIfData(nic, context);
        }



        public String getName() {
            return this.name;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String toString() {
            return this.getName();
        }
    }



    private static NetIfData IF_ANY;


    private ArrayList<NetworkInterface> netIf;
    private ArrayList<Network> networks;
    private ArrayList<Boolean> netIfEnabled;
    private int netIfSize;

    private Context context;
    private ConnectivityManager manager;

    private List<OnNetworkStateChangedListener> listeners;



    public NetworkInterfaceTester(Context context) {
        this.context = context;

        this.netIf = Utils.getAvailableNICs();
        this.netIfSize = this.netIf.size();

        this.netIfEnabled = new ArrayList<>();
        this.networks = new ArrayList<>();
        for (int i = 0; i < this.netIfSize; i++) {
            try {
                this.netIfEnabled.add(this.netIf.get(i).isUp());
            } catch (SocketException ex) {
                // unused
            }
            this.networks.add(null);
        }


        this.listeners = new ArrayList<>();

        this.manager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.manager.registerNetworkCallback(
            new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN).build(),
            this
        );
    }




    public ArrayList<NetIfData> getAvailableInterfaces() {
        ArrayList<NetIfData> ls = new ArrayList<>();

        ls.add(NetIfData.getAnyOption(this.context));
        for (int i = 0; i < this.netIfSize; i++) {
            if (this.netIfEnabled.get(i)) {
                NetIfData nid = NetIfData.getOptionForNic(this.netIf.get(i), this.context);
                ls.add(nid);
            }
        }

        return ls;
    }




    @Override
    public void onAvailable(Network network) {
        super.onAvailable(network);
        int i = this.storeNetworkHashCode(network);
        this.setEnabled(i, true);
        this.updateListener(i, true);
    }


    @Override
    public void onLost(Network network) {
        super.onLost(network);
        int i = this.getFromNetworkHashCode(network);
        this.setEnabled(i, false);
        this.updateListener(i, false);
    }



    private int storeNetworkHashCode(Network network) {
        LinkProperties prop = this.manager.getLinkProperties(network);

        NetworkInterface iface = null;
        try {
            iface = NetworkInterface.getByName(prop.getInterfaceName());
        } catch (SocketException ex) {
            // unused
        }

        int i = 0;
        boolean found = false;
        for (i = 0; !found && i < this.netIfSize; i++) {
            if (iface.getName().equals(this.netIf.get(i).getName())) {
                this.networks.set(i, network);
                i--;
                found = true;
            }
        }

        if (found) {
            Log.d(TAG, "Added network " + iface.getName());
        } else {
            Log.d(TAG, "No network found");
        }

        return found ? i : -1;
    }



    private int getFromNetworkHashCode(Network network) {
        int i = this.networks.indexOf(network);

        if (i != -1) {
            Log.d(TAG, "Removed network " + this.netIf.get(i).getName());
        } else {
            Log.d(TAG, "Network to remove not found");
        }

        return i;
    }



    private void setEnabled(int i, boolean enabled) {
        this.netIfEnabled.set(i, enabled);
    }




    public void addOnNetworkStateChangedListener(OnNetworkStateChangedListener onscl) {
        if (this.listeners.indexOf(onscl) == -1) {
            this.listeners.add(onscl);
        }
    }


    private void updateListener(int i, boolean enabled) {
        for (OnNetworkStateChangedListener onscl : this.listeners) {
            onscl.onNetworkStateChanged(this, this.netIf.get(i), enabled);
        }
    }
}