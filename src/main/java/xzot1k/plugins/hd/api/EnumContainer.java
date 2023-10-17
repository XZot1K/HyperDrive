/*
 * Copyright (c) 2020. All rights reserved.
 */

package xzot1k.plugins.hd.api;

import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.hd.HyperDrive;

public final class EnumContainer {

    public enum MenuType {
        EDIT, LIST, PLAYER_SELECTION, LIKE, CUSTOM
    }

    public enum Status {
        PUBLIC, PRIVATE, ADMIN;

        public int getIndex() {
            for (int i = -1; ++i < values().length; )
                if (values()[i] == this) return i;
            return 0;
        }

    }

    public enum Filter {

        PUBLIC, PRIVATE, ADMIN, OWNED, FEATURED, SEARCH;

        public static Filter get(int index) {
            for (int i = -1; ++i < values().length; )
                if (i == index) return values()[i];
            return null;
        }

        public static Filter getByName(@NotNull String filter) {
            for (int i = -1; ++i < values().length; ) {
                final Filter foundFilter = values()[i];
                if (filter.toLowerCase().contains(foundFilter.name().toLowerCase())
                        || filter.toLowerCase().contains(foundFilter.getFormat().toLowerCase()))
                    return foundFilter;
            }
            return null;
        }

        public int getIndex() {
            for (int i = -1; ++i < values().length; )
                if (values()[i] == this) return i;
            return 0;
        }

        public Filter getNext() {
            final int index = (getIndex() + 1);
            if (index < values().length) return values()[index];
            return PUBLIC;
        }

        public String getFormat() {
            switch (this) {
                case PRIVATE: {return HyperDrive.getPluginInstance().getMenusConfig().getString("list-menu-section.private-status-format");}

                case ADMIN: {return HyperDrive.getPluginInstance().getMenusConfig().getString("list-menu-section.admin-status-format");}

                case OWNED: {return HyperDrive.getPluginInstance().getMenusConfig().getString("list-menu-section.own-status-format");}

                case FEATURED: {return HyperDrive.getPluginInstance().getMenusConfig().getString("list-menu-section.featured-status-format");}

                case SEARCH: {return HyperDrive.getPluginInstance().getMenusConfig().getString("list-menu-section.search-status-format");}

                default: {return HyperDrive.getPluginInstance().getMenusConfig().getString("list-menu-section.public-status-format");}
            }
        }

    }

    public enum Animation {
        CONE, HELIX, CIRCLE, VORTEX, RING
    }

    public enum VoteType {
        LIKE, DISLIKE
    }

}