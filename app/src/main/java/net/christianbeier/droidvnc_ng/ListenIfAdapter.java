/*
 * DroidVNC-NG ListenIfAdapter.
 *
 * Author: Christian Beier <info@christianbeier.net
 *
 * Copyright (C) 2020 Kitchen Armor.
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

import android.content.res.Resources;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Spinner;
import android.view.LayoutInflater;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.NetworkInterface;
import java.util.ArrayList;


public class ListenIfAdapter extends ArrayAdapter<ListenIfAdapter.NetworkInterfaceData> {
    // Class representing the data to show
    static class NetworkInterfaceData {
        // This represents the "Any" option
        public static NetworkInterfaceData ANY;


        private NetworkInterface nic;
        private String techName;
        private String friendlyName;


        public NetworkInterfaceData(ListenIfAdapter lia) {
            this(null, lia);
        }


        public NetworkInterfaceData(NetworkInterface nic, ListenIfAdapter lia) {
            this.nic = nic;

            if (nic == null) {
                this.techName = "0.0.0.0";
                this.friendlyName = lia.getStringForAny();

            } else {
                this.techName = this.nic.getName();
                String[] ifNameInfo = lia.extractIfInfo(this.techName);
                if (ifNameInfo == null) { // No info found, consider "techName" as friendly
                    this.friendlyName = this.techName;

                } else {
                    this.friendlyName = lia.getFriendlyStringForInterface(ifNameInfo[0]);
                    if (ifNameInfo.length > 1) {
                        this.friendlyName += " " + ifNameInfo[1];
                    }
                }
            }
        }


        public static NetworkInterfaceData getAnyOption(ListenIfAdapter lia) {
            if (ANY == null) {
                ANY = new NetworkInterfaceData(lia);
            }
            return ANY;
        }


        public String getName() {
            return this.techName;
        }


        public String getFriendlyName() {
            return this.friendlyName;
        }


        public String toString() {
            return this.friendlyName + " (" + this.techName + ")";
        }
    }






    // This adapter uses the ViewHolder pattern
    private static class ViewHolder {
        public TextView txtLabel;
    }

    // Data to be shown with the adapter
    private ArrayList<NetworkInterfaceData> data;
    private int dataSize;


    // Some context data for "easy retrieval"
    private Context mContext;
    private Resources mResources;
    private LayoutInflater mInflater;




    public ListenIfAdapter(ArrayList<NetworkInterface> ifs, Context context) {
        super(context, R.layout.spinner_row, R.id.spinner_text);

        this.mContext = context;
        this.mResources = this.mContext.getResources();
        this.mInflater = LayoutInflater.from(this.mContext);

        this.data = new ArrayList<>();
        this.data.add(NetworkInterfaceData.getAnyOption(this));
        for (NetworkInterface nic : ifs) {
            this.data.add(new NetworkInterfaceData(nic, this));
        }
        this.dataSize = this.data.size();
    }



    public int getItemPositionByIfName(String ifName) {
        int i = 0;
        for (NetworkInterfaceData nid : this.data) {
            if (nid.getName().equals(ifName)) {
                return i;
            }
            i++;
        }

        return 0;
    }




    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) { // Check if view must be recreated, using the famous ViewHolder pattern
            convertView = this.mInflater.inflate(R.layout.spinner_row, parent, false);

            ViewHolder vh = new ViewHolder();
            vh.txtLabel = convertView.findViewById(R.id.spinner_text);
            convertView.setTag(vh);
        }

        ViewHolder vh = (ViewHolder)convertView.getTag();
        NetworkInterfaceData nid = this.getItem(position);
        vh.txtLabel.setText(nid.toString());

        return convertView;
    }


    @Override
    public NetworkInterfaceData getItem(int position) {
        if (0 <= position && position < this.getCount()) {
            return this.data.get(position);
        }
        return null;
    }



    @Override
    public int getCount() {
        return this.dataSize;
    }




    private String getStringForAny() {
        return this.mResources.getString(R.string.main_activity_settings_listenif_spin_any);
    }


    /**
     * Since the majority of the interfaces have standard Linux names, this method tries to get a possible friendly name.
     * Special thanks go to the pages:
     * - https://stackoverflow.com/questions/33747787/android-networkinterface-what-are-the-meanings-of-names-of-networkinterface
     * - https://stackoverflow.com/questions/47488435/meaning-of-network-interface-rmnet-ipa0
     * for some of the obscure interface names
     * @param ifName name (technical) of the interface
     * @return if known, a friendly name for the interface, otherwise, null
     */
    private String getFriendlyStringForInterface(String ifName) {
        String ifFriendlyName = null; // Null if no friendly name is known

        if (ifName.equals("eth")) {
            ifFriendlyName = this.mResources.getString(R.string.main_activity_settings_listenif_spin_eth);

        } else if (ifName.equals("wlan")) {
            ifFriendlyName = this.mResources.getString(R.string.main_activity_settings_listenif_spin_wifi);

        } else if (ifName.equals("lo")) {
            ifFriendlyName = this.mResources.getString(R.string.main_activity_settings_listenif_spin_lpback);

        } else if (ifName.equals("tun")) {
            ifFriendlyName = this.mResources.getString(R.string.main_activity_settings_listenif_spin_tun);

        } else if (ifName.equals("dummy")) {
            ifFriendlyName = this.mResources.getString(R.string.main_activity_settings_listenif_spin_dum);

        } else if (ifName.equals("rmnet_data")) {
            ifFriendlyName = this.mResources.getString(R.string.main_activity_settings_listenif_spin_rmndata);

        } else if (ifName.equals("rmnet_ipa")) {
            ifFriendlyName = this.mResources.getString(R.string.main_activity_settings_listenif_spin_rmnipa);

        }


        return ifFriendlyName;
    }




    /*
     * This method is used to extract the name and the number of the particular interface
     * from a full interface name (e.g. tun0 becomes "tun" and "0").
     * Null if no info was found (id est, interface is not known)
     */
    private static final String IFS_REGEX = "(eth|wlan|tun|dummy|rmnet_data|rmnet_ipa)([0-9+])";

    private String[] extractIfInfo(String ifName) {
        Pattern pt = Pattern.compile(IFS_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher mc = pt.matcher(ifName);

        if (mc.matches()) {
            return new String[] { mc.group(1), mc.group(2) };
        }

        // If not found, check for loopback
        if (ifName.equalsIgnoreCase("lo")) {
            return new String[] { "lo" };
        }

        // No useful info was found
        return null;
    }
}