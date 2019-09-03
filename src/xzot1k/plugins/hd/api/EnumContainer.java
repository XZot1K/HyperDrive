package xzot1k.plugins.hd.api;

public final class EnumContainer
{

    public enum JSONAction
    {
        CLICK_EVENT, HOVER_EVENT
    }

    public enum JSONClickAction
    {
        OPEN_URL, OPEN_FILE, RUN_COMMAND, SUGGEST_COMMAND, CHANGE_PAGE
    }

    public enum JSONHoverAction
    {
        SHOW_TEXT, SHOW_ACHIEVEMENT, SHOW_ITEM, SHOW_ENTITY
    }

    public enum MenuType
    {
        EDIT, LIST, PLAYER_SELECTION, LIKE, CUSTOM
    }

    public enum Status
    {
        PUBLIC, PRIVATE, ADMIN
    }

    public enum Animation
    {
        CONE, HELIX, CIRCLE, VORTEX, RING
    }

}
