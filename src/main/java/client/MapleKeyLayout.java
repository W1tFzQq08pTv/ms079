/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client;

import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.Serializable;

import tools.data.output.MaplePacketLittleEndianWriter;

import tools.Pair;

public class MapleKeyLayout implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private Map<Integer, Pair<Byte, Integer>> keymap;
    private long changeVersion;
    private long savedVersion;

    public MapleKeyLayout() {
        keymap = new HashMap<Integer, Pair<Byte, Integer>>();
    }

    public MapleKeyLayout(Map<Integer, Pair<Byte, Integer>> keys) {
        keymap = keys;
    }

    public synchronized final Map<Integer, Pair<Byte, Integer>> Layout() {
        changeVersion++;
        return keymap;
    }

    public synchronized final void writeData(final MaplePacketLittleEndianWriter mplew) {
        Pair<Byte, Integer> binding;
        for (int x = 0; x < 90; x++) {
            binding = keymap.get(Integer.valueOf(x));
            if (binding != null) {
                mplew.write(binding.getLeft());
                mplew.writeInt(binding.getRight());
            } else {
                mplew.write(0);
                mplew.writeInt(0);
            }
        }
    }

    public synchronized final void changeKey(final int key, final byte type, final int action) {
        if (type != 0) {
            keymap.put(key, new Pair<Byte, Integer>(type, action));
        } else {
            keymap.remove(key);
        }
        changeVersion++;
    }

    public synchronized final long saveKeys(final Connection con, final int charid) throws SQLException {
        final long version = changeVersion;
        if (version == savedVersion) {
            return version;
        }

        try (PreparedStatement delete = con.prepareStatement("DELETE FROM keymap WHERE characterid = ?")) {
            delete.setInt(1, charid);
            delete.executeUpdate();
        }

        if (!keymap.isEmpty()) {
            try (PreparedStatement insert = con.prepareStatement(
                    "INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)")) {
                for (Entry<Integer, Pair<Byte, Integer>> keybinding : keymap.entrySet()) {
                    insert.setInt(1, charid);
                    insert.setInt(2, keybinding.getKey());
                    insert.setByte(3, keybinding.getValue().getLeft());
                    insert.setInt(4, keybinding.getValue().getRight());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        }
        return version;
    }

    public synchronized final void markSaved(final long version) {
        if (version > savedVersion) {
            savedVersion = version;
        }
    }
}
