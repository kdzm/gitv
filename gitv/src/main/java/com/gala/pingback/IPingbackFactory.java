package com.gala.pingback;

public interface IPingbackFactory {
    public static final int BITSTREAM_AD = 48;
    public static final int BITSTREAM_LOGIN_CLICK = 55;
    public static final int BITSTREAM_LOGIN_SHOW = 54;
    public static final int BOTTOM_EPISODE_CLICK = 40;
    public static final int BOTTOM_RECOMMAND_CLICK = 41;
    public static final int CARDPAGE_SHOW = 15;
    public static final int CAROUSELCHANNEL_CLICK = 23;
    public static final int CAROUSELCHANNEL_SHOW = 25;
    public static final int CAROUSELINFO_SHOW = 26;
    public static final int CAROUSELPRO_CLICK = 22;
    public static final int CAROUSELPRO_SHOW = 24;
    public static final int CUSTOMER_DETAIL_EXIT = 43;
    public static final int CUSTOMER_DETAIL_LOADED = 42;
    public static final int DATA_REQUEST = 13;
    public static final int DETAIL_ARTICLE_PAGE_CLICK = 35;
    public static final int DETAIL_ARTICLE_PAGE_SHOW = 37;
    public static final int DETAIL_BUY_CLICK = 4;
    public static final int DETAIL_CARD_PAGE_CLICK = 34;
    public static final int DETAIL_FAV_CLICK = 5;
    public static final int DETAIL_PAGE_SHOW = 11;
    public static final int DETAIL_PLAYBTN_CLICK = 2;
    public static final int DETAIL_STAR_PAGE_SHOW = 36;
    public static final int EPISODE_AD_SHOW = 53;
    public static final int EPISODE_CLICK = 3;
    public static final int EPISODE_SHOW = 32;
    public static final int EXIT_DIALOG_PAGE_CLICKED = 39;
    public static final int EXIT_DIALOG_PAGE_SHOW = 38;
    public static final int GUESSYOULIKE_CLICK = 6;
    public static final int GUESSYOULIKE_SHOW = 12;
    public static final int HDR_GUIDE_PAGE_CLICK = 46;
    public static final int HDR_GUIDE_PAGE_SHOW = 44;
    public static final int INIT_PINGBACK = 1;
    public static final int KEYEVENT = 27;
    public static final int LIVE_INTERACTION = 21;
    public static final int LOAD_PAGE = 28;
    public static final int MENUPANEL_AD = 49;
    public static final int MENUPANEL_BITSTREAM_CLICK = 31;
    public static final int MENUPANEL_SHOW = 17;
    public static final int MENU_PROGRAM_SHOW = 52;
    public static final int NEWSDATA_CLICK = 29;
    public static final int NEWSTABDATA_SHOW = 30;
    public static final int NEWS_ITEM_PAGE_CLICK = 33;
    public static final int PAGE_EXIT = 14;
    public static final int PANEL_HDR_TOGGLE_PAGE_CLICK = 47;
    public static final int PANEL_HDR_TOGGLE_PAGE_SHOW = 45;
    public static final int PLAY_WINDOW_CLICK = 9;
    public static final int RECOMMEND_CLICK = 7;
    public static final int SCREEN_RATIO_SHOW = 51;
    public static final int SELECTIONS_SHOW = 18;
    public static final int SKIPHEADTAILER_CLICK = 20;
    public static final int SKIP_HEADER_SHOW = 50;
    public static final int SUMMARY_CLICK = 10;
    public static final int SUPERALBUM_CLICK = 8;
    public static final int SUPERALBUM_SHOW = 16;
    public static final int VIDEORATIO_CLICK = 19;

    IPingback createPingback(int i);
}